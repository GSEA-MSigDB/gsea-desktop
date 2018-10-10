/*
 *  Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.utils.ZipUtility;
import xtools.api.AbstractTool;
import xtools.gsea.LeadingEdgeTool;

public class LeadingEdgeToolWrapper extends AbstractModule {

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

            final boolean createZip = StringUtils.equalsIgnoreCase(cl.getOptionValue("create_zip"), "true");

            String outputFileName = cl.getOptionValue("output_file_name");
            if (StringUtils.isNotBlank(outputFileName)) {
                if (!outputFileName.toLowerCase().endsWith(".zip")) {
                    outputFileName += ".zip";
                }
            } else {
                outputFileName = "leading_edge_report.zip";
            }
            final String zipFileName = outputFileName;

            // Enrichment resultfile
            String enrichmentResultZip = cl.getOptionValue("enrichment_zip");
            if (StringUtils.isNotBlank(enrichmentResultZip)) {
                enrichmentResultZip = copyFileWithoutBadChars(enrichmentResultZip, tmp_working);
                paramProcessingError |= (enrichmentResultZip == null);
            } else {
                System.err.println("Required parameter 'enrichment.result.zip.file' not found");
                paramProcessingError = true;
            }

            if (paramProcessingError) {
                // Should probably use BadParamException and set an errorCode, use it to look up a Wiki Help page.
                throw new Exception("There were one or more errors with the job parameters.  Please check stderr.txt for details.");
            }

            // Unzip the input file to the working directory
            // NOTE: ZipUtility is a utility class in the GSEA JAR. This and Zipper (above) may be refactored into a single class in the
            // future.
            final File inputExpanded = new File(tmp_working, "inputExpanded");

            final ZipUtility zipUtility = new ZipUtility();
            zipUtility.unzip(new File(enrichmentResultZip), inputExpanded);

            Properties paramProps = new Properties();

            System.out.println("Parameters passing to LeadingEdgeTool.main:");
            setParam("out", analysis.getAbsolutePath(), paramProps);
            setParam("dir", inputExpanded.getAbsolutePath(), paramProps);

            // Set up the gene sets. Only add 'gsets' param if the user has provided any.
            // It's fine if none are provided - the underlying Tool will interpret that as
            // a directive to use all of them for the analysis.
            String geneSets = cl.getOptionValue("gsets", "").trim();
            if (StringUtils.isNotBlank(geneSets)) {
                setParam("gsets", geneSets, paramProps);
            }

            String altDelim = cl.getOptionValue("altDelim", "");
            if (StringUtils.isNotBlank(altDelim)) {
                if (altDelim.length() > 1) {
                    System.err.println(
                            "Invalid alt.delim '" + altDelim + "' specified. This must be only a single character and no whitespace.");
                    paramProcessingError = true;
                } else {
                    setParam("altDelim", altDelim, paramProps);
                }
            }

            // Finally, load up the remaining simple parameters. We'll let LeadingEdgeTool validate these.
            setOptionValueAsParam("imgFormat", cl, paramProps);
            setOptionValueAsParam("extraPlots", cl, paramProps);
            setParam("zip_report", Boolean.toString(createZip), paramProps);

            setParam("gui", "false", paramProps);

            tool = new LeadingEdgeTool(paramProps);
            try {
                success = AbstractTool.module_main(tool);
            } finally {
                try {
                    if (!analysis.exists()) return;
                    copyAnalysisToCurrentDir(cwd, analysis, createZip, zipFileName);
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
