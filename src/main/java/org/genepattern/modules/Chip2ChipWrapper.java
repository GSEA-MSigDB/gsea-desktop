/*
 *  Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.Conf;
import xtools.api.AbstractTool;
import xtools.chip2chip.Chip2Chip;

/**
 * Chip2ChipWrapper parses the command line arguments passed in by GP Server's run task page, creates a new parameter file, and passes that
 * parameter file to the GSEA tool's main method. Upon completion of Chip2Chip, Chip2ChipWrapper creates a zip file containing results and
 * then cleans up the working directory so it only contains the zip file, the output files, and the input files that were uploaded by the
 * run task page.
 */
public class Chip2ChipWrapper extends AbstractModule {
    // Suppressing the static-access warnings because this is the recommended usage according to the Commons-CLI docs.
    @SuppressWarnings("static-access")
    private static Options setupCliOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("chipPlatform").hasArg().create("chip"));
        options.addOption(OptionBuilder.withArgName("geneSetsDatabase").hasArg().create("gmx"));
        options.addOption(OptionBuilder.withArgName("geneSetMatrixFormat").hasArg().create("genesetmatrix_format"));
        options.addOption(OptionBuilder.withArgName("showEtiology").hasArg().create("show_etiology"));
        options.addOption(OptionBuilder.withArgName("selectedGeneSets").hasArg().create("selected_gene_sets"));
        options.addOption(OptionBuilder.withArgName("altDelim").hasArg().create("altDelim"));
        options.addOption(OptionBuilder.withArgName("createZip").hasArg().create("create_zip"));
        options.addOption(OptionBuilder.withArgName("devMode").hasArg().create("dev_mode"));
        return options;
    }

    public static void main(final String[] args) throws Exception {
        // Turn off debugging in the GSEA code and tell it not to create directories
        // TODO: confirm the "mkdir" property works as expected
        System.setProperty("debug", "false");
        System.setProperty("mkdir", "false");

        // Define a working directory, to be cleaned up on exit. The name starts with a '.' so it's hidden from GP & file system.
        // Also, define a dedicated directory for building the report output
        final File cwd = new File(System.getProperty("user.dir"));
        final File tmp_working = new File(".tmp_gsea");
        final File analysis = new File(tmp_working, "analysis");

        // Success flag. We set this to *false* until proven otherwise by a successful Tool run. This saves having to catch
        // all manner of exceptions along the way; just allow them to propagate to the top-level handler.
        boolean success = false;

        AbstractTool tool = null;

        try {
            Options opts = setupCliOptions();
            CommandLineParser parser = new PosixParser();
            CommandLine cl = null;

            try {
                cl = parser.parse(opts, args);
            } catch (ParseException pe) {
                System.err.println("ParseException: " + pe.getMessage());
                success = true;
                System.exit(1);
            }

            boolean paramProcessingError = false;

            analysis.mkdirs();

            // Enable any developer-only settings. For now, this just disables the update check; may do more in the future (verbosity level,
            // etc)
            boolean devMode = StringUtils.equalsIgnoreCase(cl.getOptionValue("dev_mode"), "true");
            if (devMode) {
                System.setProperty("MAKE_GSEA_UPDATE_CHECK", "false");
            } else {
                System.setProperty("MAKE_GSEA_UPDATE_CHECK", "true");
                // Set the GSEA update check String to show this is coming from the modules.
                System.setProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GP_MODULES");
            }

            boolean createZip = StringUtils.equalsIgnoreCase(cl.getOptionValue("create_zip"), "true");

            // Convert the format string passed by GP into the tokens expected by GSEA.
            String outputFileFormat = cl.getOptionValue("genesetmatrix_format");
            outputFileFormat = (StringUtils.equalsIgnoreCase(outputFileFormat, "gmx")) ? "GeneSetMatrix[gmx]"
                    : "GeneSetMatrix_Transposed[gmt]";

            String chipPlatformFileName = cl.getOptionValue("chip");
            if (StringUtils.isNotBlank(chipPlatformFileName)) {
                chipPlatformFileName = copyFileWithoutBadChars(chipPlatformFileName, tmp_working);
                paramProcessingError |= (chipPlatformFileName == null);
            } else {
                System.err.println("Required parameter 'chip.platform.file' not found");
                paramProcessingError = true;
            }

            // List of Gene Sets Database files
            String geneSetDBsParam = cl.getOptionValue("gmx");

            if (StringUtils.isBlank(geneSetDBsParam)) {
                System.err.println("No Gene Sets Databases files were specified.");
                System.err.println("Please provide one or more values to the 'gene.sets.database' parameter.");
                paramProcessingError = true;
            }

            List<String> geneSetDBs = (StringUtils.isBlank(geneSetDBsParam)) ? Collections.emptyList()
                    : FileUtils.readLines(new File(geneSetDBsParam), (Charset) null);

            List<String> safeNameGeneSetDBs = new ArrayList<String>(geneSetDBs.size());
            for (String geneSetDB : geneSetDBs) {
                String renamedFile = copyFileWithoutBadChars(geneSetDB, tmp_working);
                if (renamedFile != null) {
                    safeNameGeneSetDBs.add(renamedFile);
                } else {
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
                    System.err.println(
                            "Invalid alt.delim '" + altDelim + "' specified. This must be only a single character and no whitespace.");
                    paramProcessingError = true;
                } else {
                    delim = altDelim;
                    delimPattern = Pattern.compile(delim);
                }
            }

            String selectedGeneSetsParam = cl.getOptionValue("selected_gene_sets");
            List<String> selectedGeneSets = (StringUtils.isBlank(selectedGeneSetsParam)) ? Collections.emptyList()
                    : Arrays.asList(delimPattern.split(selectedGeneSetsParam));

            // Join up all of the Gene Set DBs or the selections to be passed in the paramProps.
            List<String> geneSetsSelection = (selectedGeneSets.isEmpty()) ? safeNameGeneSetDBs
                    : selectGeneSetsFromFiles(safeNameGeneSetDBs, selectedGeneSets);
            paramProcessingError |= (geneSetsSelection == null);

            String geneSetsSelector = StringUtils.join(geneSetsSelection, delim);

            if (paramProcessingError) {
                // Should probably use BadParamException and set an errorCode, use it to look up a Wiki Help page.
                throw new Exception("There were one or more errors with the job parameters.  Please check stderr.txt for details.");
            }

            Properties paramProps = new Properties();

            System.out.println("Parameters passing to Chip2Chip tool:");
            setParam("gmx", geneSetsSelector, paramProps);
            setParam("chip_target", chipPlatformFileName, paramProps);
            setParam("out", analysis.getPath(), paramProps);
            setParam("rpt_label", "my_analysis", paramProps);
            setParam("genesetmatrix_format", outputFileFormat, paramProps);
            setParam("zip_report", Boolean.toString(createZip), paramProps);

            if (StringUtils.isNotBlank(altDelim)) {
                setParam("altDelim", altDelim, paramProps);
            }

            // Finally, load up the remaining simple parameters. We'll let Chip2Chip validate these.
            setOptionValueAsParam("show_etiology", cl, paramProps);

            setParam("gui", "false", paramProps);

            tool = new Chip2Chip(paramProps);
            try {
                success = AbstractTool.module_main(tool);
            } finally {
                try {
                    if (!analysis.exists()) return;
                    copyAnalysisToCurrentDir(cwd, analysis, createZip, "chip2chip_results.zip");
                } catch (IOException ioe) {
                    System.err.println("Error during clean-up:");
                    ioe.printStackTrace(System.err);
                }
            }
        } finally {
            try {
                cleanUpAnalysisDirs(cwd, tmp_working);
            } finally {
                Conf.exitSystem(!success);
            }
        }
    }
}
