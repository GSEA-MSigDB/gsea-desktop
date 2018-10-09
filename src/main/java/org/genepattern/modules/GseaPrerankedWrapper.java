/*
 *  Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;

import xtools.api.param.BadParamException;
import xtools.gsea.GseaPreranked;

/**
 * GseaPrerankedWrapper parses the command line arguments passed in by GP Server's run task page,
 * creates a new parameter file, and passes that parameter file to the GSEA tool's main
 * method. Upon return from GSEAPreranked.main, GseaPrerankedWrapper creates a zip file containing results
 * and then cleans up the working directory so it only contains the zip file, the output files, and the
 * input files that were uploaded by the run task page.
 */
public class GseaPrerankedWrapper {
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final Pattern HASH_PATTERN = Pattern.compile("#");

    private static String copyFileWithoutBadChars(String file, File working) {
        if (!StringUtils.containsAny(file, '#', '@')) {
            // Do nothing; the file does not need to be renamed
            return file;
        }

        // Work only with the file name itself. This naturally discards any offending characters in the full path
        // For any others we'll just substitute underscores
        File f = new File(file);
        String newName = StringUtils.replaceChars(f.getName(), "@#", "_");

        File newFile = new File(working, newName);
        System.out.println("Copying file '" + file + "' to '" + newName + "'");
        try {
            FileUtils.copyFile(f, newFile);
            return newFile.getPath();
        }
        catch (IOException io) {
            System.err.println("An error occurred trying to copy '" + file + "' to '" + newName + "'");
            io.printStackTrace(System.err);
            return null;
        }
    }

    // Suppressing the static-access warnings because this is the recommended usage according to the Commons-CLI docs.
    @SuppressWarnings("static-access")
    private static Options setupCliOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("geneSetsDatabase").hasOptionalArg().create("gmx"));
        options.addOption(OptionBuilder.withArgName("numberOfPermutations").hasArg().create("nperm"));
        options.addOption(OptionBuilder.withArgName("rankedList").hasArg().create("rnk"));
        options.addOption(OptionBuilder.withArgName("scoringScheme").hasArg().create("scoring_scheme"));
        options.addOption(OptionBuilder.withArgName("maxGeneSetSize").hasArg().create("set_max"));
        options.addOption(OptionBuilder.withArgName("minGeneSetSize").hasArg().create("set_min"));
        options.addOption(OptionBuilder.withArgName("normalizationMode").hasArg().create("norm"));
        options.addOption(OptionBuilder.withArgName("makeDetailedGeneSetReport").hasArg().create("make_sets"));
        options.addOption(OptionBuilder.withArgName("numberOftopSetsToPlot").hasArg().create("plot_top_x"));
        options.addOption(OptionBuilder.withArgName("createSvgs").hasArg().create("create_svgs"));
        options.addOption(OptionBuilder.withArgName("randomSeed").hasArg().create("rnd_seed"));
        options.addOption(OptionBuilder.withArgName("selectedGeneSets").hasOptionalArg().create("selected_gene_sets"));
        options.addOption(OptionBuilder.withArgName("outputFileName").hasOptionalArg().create("output_file_name"));
        options.addOption(OptionBuilder.withArgName("altDelim").hasOptionalArg().create("altDelim"));
        options.addOption(OptionBuilder.withArgName("createZip").hasArg().create("create_zip"));
        options.addOption(OptionBuilder.withArgName("devMode").hasArg().create("dev_mode"));
        return options;
    }

    public static void main(final String[] args) throws Exception {
        // Turn off debugging in the GSEA code and tell it not to create directories
        System.setProperty("debug", "false");
        System.setProperty("mkdir", "false");

        Options opts = setupCliOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cl = null;

        try {
            cl = parser.parse(opts, args);
        }
        catch (ParseException pe) {
            System.err.println("ParseException: " + pe.getMessage());
            System.exit(1);
        }

        boolean paramProcessingError = false;

        // Define a working directory, to be cleaned up on exit. The name starts with a '.' so it's hidden from GP & file system.
        // Also, define a dedicated directory for building the report output
        final File cwd = new File(System.getProperty("user.dir"));
        final File tmp_working = new File(".tmp_gsea");
        final File analysis = new File(tmp_working, "analysis");
        analysis.mkdirs();

        // Enable any developer-only settings.  For now, this just disables the update check; may do more in the future (verbosity level, etc)
        boolean devMode = StringUtils.equalsIgnoreCase(cl.getOptionValue("dev_mode"), "true");
        if (devMode) {
            System.setProperty("DMAKE_GSEA_UPDATE_CHECK", "false");
        }
        else {
            System.setProperty("DMAKE_GSEA_UPDATE_CHECK", "true");
            // Set the GSEA update check String to show this is coming from the modules.
            System.setProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GP_MODULES");
        }

        boolean createZip = StringUtils.equalsIgnoreCase(cl.getOptionValue("create_zip"), "true");

        String outputFileName = cl.getOptionValue("output_file_name");
        if (StringUtils.isNotBlank(outputFileName)) {
            if (!outputFileName.toLowerCase().endsWith(".zip")) {
                outputFileName += ".zip";
            }
        }
        else {
            outputFileName = "gsea_analysis.zip";
        }
        final String zipFileName = outputFileName;

        // Set a couple of shutdownHooks to finish the job and clean up. Preranked exits after running
        // and does not return control back to the module code. These are done as two separate hooks
        // because: 1) copying the ZIP & results and cleaning up tmp_working need to be ordered to happen
        // one after the other; and 2) deleteEmptyDirectories() is independent of those two tasks.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            // Nested try/finally structure to ensure all steps are taken.
            public void run() {
                try {
                    if (!analysis.exists()) return;
                    try {
                        if (createZip) {
                            copyZipToJobIfPresent(analysis, zipFileName, cwd);
                        }
                    }
                    finally {
                        try {
                            File edb = new File(analysis, "edb");
                            if (edb.exists()) {
                                FileUtils.copyDirectory(edb, cwd, FileFileFilter.FILE);
                            }
                        }
                        finally {
                            FileUtils.copyDirectory(analysis, cwd, FileFileFilter.FILE);
                        }
                    }
                }
                catch (IOException ioe) {
                    System.err.println("Error during clean-up:");
                    ioe.printStackTrace(System.err);
                }
                finally {
                    FileUtils.deleteQuietly(tmp_working);
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // delete empty directories; GSEA creates one named after the current date.
                deleteEmptyDirectories(cwd);
            }
        });

        // Ranked feature list file
        String rankedListFileName = cl.getOptionValue("rnk");
        if (StringUtils.isNotBlank(rankedListFileName)) {
            rankedListFileName = copyFileWithoutBadChars(rankedListFileName, tmp_working);
        }
        else {
            System.err.println("Required parameter 'ranked.list' not found.");
            paramProcessingError = true;
        }

        // List of Gene Sets Database files
        String geneSetDBsParam = cl.getOptionValue("gmx");

        if (StringUtils.isBlank(geneSetDBsParam)) {
            System.err.println("No Gene Sets Databases files were specified.");
            System.err.println("Please provide one or more values to the 'gene.sets.database' parameter.");
            paramProcessingError = true;
        }

        List<String> geneSetDBs = FileUtils.readLines(new File(geneSetDBsParam), (Charset) null);

        List<String> safeNameGeneSetDBs = new ArrayList<String>(geneSetDBs.size());
        for (String geneSetDB : geneSetDBs) {
            String renamedFile = copyFileWithoutBadChars(geneSetDB, tmp_working);
            if (renamedFile != null) {
                safeNameGeneSetDBs.add(renamedFile);
            }
            else {
                // Something went wrong. Use the original name just to complete checking parameters
                paramProcessingError = true;
                safeNameGeneSetDBs.add(geneSetDB);
            }
        }

        String delim = ",";
        String altDelim = cl.getOptionValue("altDelim", "");
        Pattern delimPattern = COMMA_PATTERN;
        if (StringUtils.isNotBlank(altDelim)) {
            if (altDelim.length() > 1) {
                System.err.println("Invalid alt.delim '" + altDelim + "' specified. This must be only a single character and no whitespace.");
                paramProcessingError = true;
            }
            else {
                delim = altDelim;
                delimPattern = Pattern.compile(delim);
            }
        }

        String selectedGeneSetsParam = cl.getOptionValue("selected_gene_sets");
        List<String> selectedGeneSets = (StringUtils.isBlank(selectedGeneSetsParam)) ? Collections.emptyList() : Arrays.asList(delimPattern.split(selectedGeneSetsParam));

        // Join up all of the Gene Set DBs or the selections to be passed in the param file.
        List<String> geneSetsSelection = (selectedGeneSets.isEmpty()) ? safeNameGeneSetDBs : selectGeneSetsFromFiles(safeNameGeneSetDBs, selectedGeneSets);
        paramProcessingError |= (geneSetsSelection == null);

        String geneSetsSelector = StringUtils.join(geneSetsSelection, delim);

        if (paramProcessingError) {
            System.out.println("There were one or more errors with the job parameters.  Please check stderr.txt for details.");
            System.exit(1);
        }

        // Parameter file to be created and passed to Preranked
        File gseaPrerankedParamFile = new File(tmp_working, "gsea_preranked_param_file.txt");

        // Create a parameters file since we've successfully loaded and checked the difficult items.
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(gseaPrerankedParamFile);

            System.out.println("Parameters passing to GSEAPreranked.main:");
            printParam("rnk", rankedListFileName, writer);
            printParam("gmx", geneSetsSelector, writer);
            printParam("out", analysis.getPath(), writer);

            printParam("rpt_label", FilenameUtils.getBaseName(outputFileName), writer);
            printParam("zip_report", Boolean.toString(createZip), writer);
            printParam("gui", "false", writer);

            if (StringUtils.isNotBlank(altDelim)) {
                printParam("altDelim", altDelim, writer);
            }

            // Finally, load up the remaining simple parameters. We'll let Preranked validate these.
            printOptionValueAsParam("norm", cl, writer);
            printOptionValueAsParam("nperm", cl, writer);
            printOptionValueAsParam("scoring_scheme", cl, writer);
            printOptionValueAsParam("make_sets", cl, writer);
            printOptionValueAsParam("plot_top_x", cl, writer);
            printOptionValueAsParam("rnd_seed", cl, writer);
            printOptionValueAsParam("create_svgs", cl, writer);
            printOptionValueAsParam("set_max", cl, writer);
            printOptionValueAsParam("set_min", cl, writer);
        }
        catch (IOException io) {
            System.err.println("Error creating parameter file");
            io.printStackTrace(System.err);
            System.exit(1);
        }
        finally {
            if (writer != null) writer.close();
        }
        
        try {
            String[] args1 = new String[] { "-param_file " + gseaPrerankedParamFile };
            GseaPreranked.main(args1);
        }
        catch (BadParamException e) {
            String message = e.getMessage();
            if (message != null && message.contains("None of the gene sets passed the size thresholds")) {
                System.err.print("Please verify that the correct chip platform was provided.");
                e.printStackTrace(System.err);
            }
        }
    }

    private static void printParam(String name, String value, PrintWriter writer) {
        System.out.println(name + "\t" + value);
        writer.println(name + "\t" + value);
        writer.println();
    }

    private static void printOptionValueAsParam(String optionName, CommandLine commandLine, PrintWriter writer) {
        printParam(optionName, commandLine.getOptionValue(optionName), writer);
    }

    private static void copyZipToJobIfPresent(final File analysis, String zipFileName, File cwd) throws InternalError {
        if (!analysis.exists()) return;

        Collection<File> zips = FileUtils.listFiles(analysis, new String[] { "zip" }, false);
        if (zips == null || zips.isEmpty()) return;

        // Check that we have exactly one ZIP. This should never happen.
        if (zips.size() > 1) {
            throw new InternalError("Internal Error: multiple ZIP files created");
        }
        File zip = zips.iterator().next();

        try {
            File dest = new File(cwd, zipFileName);
            FileUtils.moveFile(zip, dest);
        }
        catch (IOException ioe) {
            System.err.println("Internal error moving result ZIP: ");
            System.err.println(ioe.getMessage());
        }
    }

    private static void deleteEmptyDirectories(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    String[] filesInDir = files[i].list();
                    if (filesInDir == null || filesInDir.length == 0) {
                        files[i].delete();
                    }
                }
            }
        }
    }


    private static final List<String> selectGeneSetsFromFiles(List<String> geneSetDBs, List<String> selectedGeneSets) {
        // If there are no geneSet DBs at all just return an empty list. This is covering any possible earlier error conditions
        // that caused these lists to be wiped out (e.g. error in downloading MSigDB files). Those errors should have already
        // been flagged so we don't want to repeat ourselves here, nor do we want to cause spurious cascading errors.
        if (geneSetDBs.isEmpty()) return Collections.emptyList();

        try {
            Map<String, String> geneSetDBPathMap = buildFileNameToFilePathMap(geneSetDBs);

            // Do we have only a single Gene Set file? If so, the user is allowed to skip the fileName#geneSetName notation
            // in favor of simply using geneSetName. This is optional and both are supported.
            boolean haveSingleFile = (geneSetDBPathMap.size() == 1);

            // If there's only one file, pull it out of the Map values() and use it directly. Otherwise, build the selector using the Map.
            List<String> selected = new ArrayList<String>();
            for (String selectedGeneSet : selectedGeneSets) {
                String selector = haveSingleFile ? buildSelector(selectedGeneSet, geneSetDBPathMap.values().iterator().next()) : buildSelector(selectedGeneSet, geneSetDBPathMap);
                selected.add(selector);
            }

            return selected;
        }
        catch (IllegalArgumentException iae) {
            System.err.println("There was a problem processing the 'gene.set.selector' parameter");
            iae.printStackTrace(System.err);
            return null;
        }
    }

    private static final String buildSelector(String selectionToken, String filePath) {
        String fileName = FilenameUtils.getName(filePath);

        // Note that we *could* also validate that the geneSetName is present, but instead we'll leave that job to
        // the GSEA code. It's too complex to do here and a duplication of effort anyway.
        String[] specifier = HASH_PATTERN.split(selectionToken);
        if (specifier == null || (specifier.length != 1 && specifier.length != 2)) {
            throw new IllegalArgumentException("Gene Set selection specifier '" + selectionToken + "' is not valid. Selections must be separated with semicolon, and each must be a " + "file name + '#' + gene set name, e.g. my_file1.gmt#selected_gene_set1. The selector " + "can be shortened to selected_gene_set1 when there is only one Gene Set file.");
        }
        if (specifier.length == 2 && !StringUtils.equals(fileName, specifier[0])) {
            throw new IllegalArgumentException("Gene Set selection specifier '" + selectionToken + "' is not valid; Specified file name must match lone file '" + fileName + "' supplied as the Gene Set database.");
        }

        // geneSetName is always the last item in the specifier
        String geneSetName = specifier[specifier.length - 1];
        return filePath + '#' + geneSetName;
    }

    private static final String buildSelector(String selectionToken, Map<String, String> geneSetDBPathMap) {
        String[] specifier = HASH_PATTERN.split(selectionToken);
        if (specifier == null || specifier.length != 2) {
            throw new IllegalArgumentException("Gene Set selection specifier '" + selectionToken + "' is not valid. Selections must be separated with semicolon, and each must be a " + "file name + '#' + gene set name, e.g. my_file1.gmt#selected_gene_set1. The selector " + "can be shortened to selected_gene_set1 when there is only one Gene Set file.");
        }

        String fileName = specifier[0];
        String geneSetName = specifier[1];

        String userMatch = geneSetDBPathMap.get(fileName);
        if (userMatch != null) {
            return userMatch + "#" + geneSetName;
        }
        else {
            throw new IllegalArgumentException("Selected file name '" + fileName + "' not found in submitted Gene Set files.");
        }
    }

    private static Map<String, String> buildFileNameToFilePathMap(List<String> geneSetDBs) {
        Map<String, String> fileNameToPathMap = new HashMap<String, String>();
        for (String geneSetDB : geneSetDBs) {
            String fileName = FilenameUtils.getName(geneSetDB);
            if (fileNameToPathMap.containsKey(fileName)) {
                throw new IllegalArgumentException("Duplicated file name '" + fileName + "' found in submitted Gene Set files.  This is not allowed with selected.gene.gets.");
            }

            System.out.println("Adding baseName '" + fileName + "' to Map with full path '" + geneSetDB + "'");
            fileNameToPathMap.put(fileName, geneSetDB);
        }
        return fileNameToPathMap;
    }
}