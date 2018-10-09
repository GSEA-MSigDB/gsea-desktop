/*
 *  Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import xtools.gsea.LeadingEdgeTool;
import edu.mit.broad.genome.utils.ZipUtility;

public class LeadingEdgeToolWrapper {

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
        options.addOption(OptionBuilder.withArgName("enrichmentResult").hasArg().create("enrichment_zip"));
        options.addOption(OptionBuilder.withArgName("geneSets").hasArg().create("gsets"));
        options.addOption(OptionBuilder.withArgName("outputFileName").hasArg().create("output_file_name"));
        options.addOption(OptionBuilder.withArgName("imgFormat").hasArg().create("imgFormat"));
        options.addOption(OptionBuilder.withArgName("altDelim").hasArg().create("altDelim"));
        options.addOption(OptionBuilder.withArgName("extraPlots").hasArg().create("extraPlots"));
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
        // Also, define a dedicated directory for building the report output, etc.
        final File cwd = new File(System.getProperty("user.dir"));
        final File tmp_working = new File(cwd, ".tmp_gsea");
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

        final boolean createZip = StringUtils.equalsIgnoreCase(cl.getOptionValue("create_zip"), "true");

        String outputFileName = cl.getOptionValue("output_file_name");
        if (StringUtils.isNotBlank(outputFileName)) {
            if (!outputFileName.toLowerCase().endsWith(".zip")) {
                outputFileName += ".zip";
            }
        }
        else {
            outputFileName = "leading_edge_report.zip";
        }
        final String zipFileName = outputFileName;
        
        // Set a couple of shutdownHooks to finish the job and clean up. LET exits after running
        // and does not return control back to the module code. These are done as two separate hooks
        // because: 1) copying the ZIP & results and cleaning up tmp_working need to be ordered to happen
        // one after the other; and 2) deleteEmptyDirectories() is independent of those two tasks.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (!analysis.exists()) return;
                    try {
                        if (createZip) {
                            copyZipToJobIfPresent(analysis, zipFileName, cwd);
                        }
                    }
                    finally {
                        FileUtils.copyDirectory(analysis, cwd);
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

        // Enrichment resultfile
        String enrichmentResultZip = cl.getOptionValue("enrichment_zip");
        if (StringUtils.isNotBlank(enrichmentResultZip)) {
            enrichmentResultZip = copyFileWithoutBadChars(enrichmentResultZip, tmp_working);
            paramProcessingError |= (enrichmentResultZip == null);
        }
        else {
            System.err.println("Required parameter 'enrichment.result.zip.file' not found");
            paramProcessingError = true;
        }

        if (paramProcessingError) {
            System.out.println("There were one or more errors with the job parameters.  Please check stderr.txt for details.");
            System.exit(1);
        }

        // Unzip the input file to the working directory
        // NOTE: ZipUtility is a utility class in the GSEA JAR. This and Zipper (above) may be refactored into a single class in the future.
        final File inputExpanded = new File(tmp_working, "inputExpanded");

        final ZipUtility zipUtility = new ZipUtility();
        zipUtility.unzip(new File(enrichmentResultZip), inputExpanded);

        // Create a param file
        PrintWriter writer = null;
        File leadingEdgeReportParamFile = new File(tmp_working, "leadingEdgeReport_param_file.txt");
        try {
            writer = new PrintWriter(leadingEdgeReportParamFile);
            System.out.println("Parameters passing to LeadingEdgeTool.main:");
            printParam("out", analysis.getAbsolutePath(), writer);
            printParam("dir", inputExpanded.getAbsolutePath(), writer);

            // Set up the gene sets. Only add 'gsets' param if the user has provided any.
            // It's fine if none are provided - the underlying Tool will interpret that as
            // a directive to use all of them for the analysis.
            String geneSets = cl.getOptionValue("gsets", "").trim();
            if (StringUtils.isNotBlank(geneSets)) {
                printParam("gsets", geneSets, writer);
            }

            String altDelim = cl.getOptionValue("altDelim", "");
            if (StringUtils.isNotBlank(altDelim)) {
                if (altDelim.length() > 1) {
                    System.err.println("Invalid alt.delim '" + altDelim + "' specified. This must be only a single character and no whitespace.");
                    paramProcessingError = true;
                }
                else {
                    printParam("altDelim", altDelim, writer);
                }
            }

            // Finally, load up the remaining simple parameters. We'll let LeadingEdgeTool validate these.
            printOptionValueAsParam("imgFormat", cl, writer);
            printOptionValueAsParam("extraPlots", cl, writer);
            printParam("zip_report", Boolean.toString(createZip), writer);

            printParam("gui", "false", writer);
        }
        catch (IOException io) {
            System.err.println("Error creating parameter file");
            io.printStackTrace(System.err);
            System.exit(1);
        }
        finally {
            if (writer != null) writer.close();
        }

        String[] args1 = new String[] { "-param_file " + leadingEdgeReportParamFile };

        if (!paramProcessingError) {
            LeadingEdgeTool.main(args1);
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
}
