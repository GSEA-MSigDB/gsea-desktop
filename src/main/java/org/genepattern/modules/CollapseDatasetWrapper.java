/*
 *  Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.Conf;
import xtools.api.AbstractTool;
import xtools.munge.CollapseDataset;

public class CollapseDatasetWrapper extends AbstractModule {

    // Suppressing the static-access warnings because this is the recommended usage according to the Commons-CLI docs.
    @SuppressWarnings("static-access")
    private static Options setupCliOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("expressionDataset").hasArg().create("res"));
        options.addOption(OptionBuilder.withArgName("chipPlatform").hasArg().create("chip"));
        options.addOption(OptionBuilder.withArgName("collapseMode").hasArg().create("mode"));
        options.addOption(OptionBuilder.withArgName("omitFeaturesWithNoSymbolMatch").hasArg().create("include_only_symbols"));
        options.addOption(OptionBuilder.withArgName("outFile").hasArg().create("out"));
        options.addOption(OptionBuilder.withArgName("reportLabel").hasArg().create("rpt_label"));
        options.addOption(OptionBuilder.withArgName("devMode").hasArg().create("dev_mode"));
        options.addOption(OptionBuilder.withArgName("gpModuleMode").hasArg().create("run_as_genepattern"));
        return options;
    }

    public static void main(final String[] args) throws Exception {
        System.setProperty("debug", "false");
        System.setProperty("mkdir", "false");

        // Define a working directory, to be cleaned up on exit. The name starts with a '.' so it's hidden from GP & file system.
        // Also, define a dedicated directory for building the report output
        final File cwd = new File(System.getProperty("user.dir"));
        final File tmp_working = new File(".tmp_gsea");

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

            tmp_working.mkdirs();

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

            String expressionDataFileName = cl.getOptionValue("res");
            if (StringUtils.isNotBlank(expressionDataFileName)) {
                expressionDataFileName = copyFileWithoutBadChars(expressionDataFileName, tmp_working);
            } else {
                System.err.println("Required parameter 'expression.dataset' not found.");
                paramProcessingError = true;
            }

            String chipPlatformFileName = cl.getOptionValue("chip");
            if (StringUtils.isNotBlank(chipPlatformFileName)) {
                chipPlatformFileName = copyFileWithoutBadChars(chipPlatformFileName, tmp_working);
            } else {
                System.err.println("Required parameter 'chip.platform.file' not found.");
                paramProcessingError = true;
            }

            if (paramProcessingError) {
                // Should probably use BadParamException and set an errorCode, use it to look up a Wiki Help page.
                throw new Exception("There were one or more errors with the job parameters.  Please check stderr.txt for details.");
            }

            Properties paramProps = new Properties();

            System.out.println("Parameters passing to CollapseDataset.main:");
            setParam("res", expressionDataFileName, paramProps);
            setParam("chip", chipPlatformFileName, paramProps);

            setParam("out", cwd.getPath(), paramProps);
            setParam("rpt_label", "my_analysis ", paramProps);

            setParam("gui", "false", paramProps);

            // Finally, load up the remaining simple parameters. We'll let GSEA validate these.
            setOptionValueAsParam("include_only_symbols", cl, paramProps);
            setOptionValueAsParam("mode", cl, paramProps);

            tool = new CollapseDataset(paramProps);
            success = AbstractTool.module_main(tool);
        } finally {
            try {
                cleanUpAnalysisDirs(cwd, tmp_working);
            } finally {
                if (tool != null && tool.getParamSet().getGuiParam().isFalse()) {
                    Conf.exitSystem(!success);
                }
            }
        }
    }
}
