/*
 *  Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import xtools.munge.CollapseDataset;

public class CollapseDatasetWrapper {

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
        options.addOption(OptionBuilder.withArgName("expressionDataset").hasArg().create("res"));
        options.addOption(OptionBuilder.withArgName("chipPlatform").hasArg().create("chip"));
        options.addOption(OptionBuilder.withArgName("collapseMode").hasArg().create("mode"));
        options.addOption(OptionBuilder.withArgName("omitFeaturesWithNoSymbolMatch").hasArg().create("include_only_symbols"));
        options.addOption(OptionBuilder.withArgName("devMode").hasArg().create("dev_mode"));
        return options;
    }

    public static void main(final String[] args) throws Exception {
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

        // Define a working directory, to be cleaned up on exit.
        final File cwd = new File(System.getProperty("user.dir"));
        final File tmp_working = new File(cwd, ".tmp_gsea");
        tmp_working.mkdirs();

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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    FileUtils.deleteQuietly(tmp_working);
                }
                finally {
                    // delete empty directories; GSEA creates one named after the current date.
                    deleteEmptyDirectories(cwd);
                }
            }
        });

        String expressionDataFileName = cl.getOptionValue("res");
        if (StringUtils.isNotBlank(expressionDataFileName)) {
            expressionDataFileName = copyFileWithoutBadChars(expressionDataFileName, tmp_working);
        }
        else {
            System.err.println("Required parameter 'expression.dataset' not found.");
            paramProcessingError = true;
        }

        String chipPlatformFileName = cl.getOptionValue("chip");
        if (StringUtils.isNotBlank(chipPlatformFileName)) {
            chipPlatformFileName = copyFileWithoutBadChars(chipPlatformFileName, tmp_working);
        }
        else {
            System.err.println("Required parameter 'chip.platform.file' not found.");
            paramProcessingError = true;
        }

        if (paramProcessingError) {
            System.out.println("There were one or more errors with the job parameters.  Please check stderr.txt for details.");
            System.exit(1);
        }

        // Parameter file to be created and passed to CollapseDataset
        final File collapseDatasetParamFile = new File(tmp_working, "collapseDataset_param_file.txt");

        // Create a parameters file since we've successfully loaded and checked the difficult items.
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(collapseDatasetParamFile);
            System.out.println("Parameters passing to CollapseDataset.main:");
            printParam("res", expressionDataFileName, writer);
            printParam("chip", chipPlatformFileName, writer);

            printParam("out", cwd.getPath(), writer);
            printParam("rpt_label", "my_analysis ", writer);

            printParam("gui", "false", writer);

            // Finally, load up the remaining simple parameters. We'll let GSEA validate these.
            printOptionValueAsParam("include_only_symbols", cl, writer);
            printOptionValueAsParam("mode", cl, writer);
        }
        catch (IOException io) {
            System.err.println("Error creating parameter file");
            io.printStackTrace(System.err);
            System.exit(1);
        }
        finally {
            if (writer != null) writer.close();
        }

        String[] args1 = new String[] { "-param_file " + collapseDatasetParamFile };
        CollapseDataset.main(args1);
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
}
