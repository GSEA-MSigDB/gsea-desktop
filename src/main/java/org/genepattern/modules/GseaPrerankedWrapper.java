/*
 *  Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
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
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.mit.broad.genome.Conf;
import xtools.api.AbstractTool;
import xtools.gsea.GseaPreranked;

/**
 * GseaPrerankedWrapper parses the command line arguments passed in by GP Server's run task page, creates a new parameter file, and passes
 * that parameter file to the GSEA tool's main method. Upon completion of GSEAPreranked, GseaPrerankedWrapper creates a zip file containing
 * results and then cleans up the working directory so it only contains the zip file, the output files, and the input files that were
 * uploaded by the run task page.
 */
public class GseaPrerankedWrapper extends AbstractModule {
    private static final Logger klog = Logger.getLogger(GseaPrerankedWrapper.class);
    
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
        options.addOption(OptionBuilder.withArgName("createZip").hasArg().create("zip_report"));
        options.addOption(OptionBuilder.withArgName("outFile").hasArg().create("out"));
        options.addOption(OptionBuilder.withArgName("reportLabel").hasArg().create("rpt_label"));
        options.addOption(OptionBuilder.withArgName("devMode").hasArg().create("dev_mode"));
        options.addOption(OptionBuilder.withArgName("gpModuleMode").hasArg().create("run_as_genepattern"));
        return options;
    }

    public static void main(final String[] args) throws Exception {
        // Success flag. We set this to *false* until proven otherwise by a successful Tool run. This saves having to catch
        // all manner of exceptions along the way; just allow them to propagate to the top-level handler.
        boolean success = false;

        AbstractTool tool = null;

        File analysis = null;
        File tmp_working = null;
        File cwd = null;
        try {
            Options opts = setupCliOptions();
            CommandLineParser parser = new PosixParser();
            CommandLine cl = parser.parse(opts, args);

            // We want to check *all* params before reporting any errors so that the user sees everything that went wrong.
            boolean paramProcessingError = false;

            // Properties object to gather parameter settings to be passed to the Tool
            Properties paramProps = new Properties();

            // The GP modules should declare they are running in GP mode.  This has minor effects on the error messages
            // and runtime behavior.
            boolean gpMode = StringUtils.equalsIgnoreCase(cl.getOptionValue("run_as_genepattern"), "true");

            if (gpMode) {
                // Turn off debugging in the GSEA code and tell it not to create directories
                // TODO: confirm the "mkdir" property works as expected
                System.setProperty("debug", "false");
                System.setProperty("mkdir", "false");

                String outOption = cl.getOptionValue("out");
                if (StringUtils.isNotBlank(outOption)) {
                    klog.warn("-out parameter ignored; only valid wih -run_as_genepattern false.");
                }
    
                // Define a working directory, to be cleaned up on exit. The name starts with a '.' so it's hidden from GP & file system.
                // Also, define a dedicated directory for building the report output
                cwd = new File(System.getProperty("user.dir"));
                tmp_working = new File(".tmp_gsea");
                analysis = new File(tmp_working, "analysis");
                analysis.mkdirs();
            } else {
                // Don't set these for regular CLI mode, just pass through -out
                setOptionValueAsParam("out", cl, paramProps, klog);
            }

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

            boolean createZip = StringUtils.equalsIgnoreCase(cl.getOptionValue("zip_report"), "true");

            String outputFileName = cl.getOptionValue("output_file_name");
            if (StringUtils.isNotBlank(outputFileName)) {
                if (!gpMode) {
                    klog.warn("-output_file_name parameter ignored; only valid wih -run_as_genepattern true.");
                }
                else if (!outputFileName.toLowerCase().endsWith(".zip")) {
                    outputFileName += ".zip";
                }
            } else {
                if (gpMode) outputFileName = "gsea_analysis.zip";
            }

            // Ranked feature list file
            String rankedListFileName = cl.getOptionValue("rnk");
            if (StringUtils.isNotBlank(rankedListFileName)) {
                if (gpMode) rankedListFileName = copyFileWithoutBadChars(rankedListFileName, tmp_working);
                paramProcessingError |= (rankedListFileName == null);
            } else {
                String paramName = (gpMode) ? "ranked.list" : "-rnk";
                klog.error("Required parameter '" + paramName + "' not found.");
                paramProcessingError = true;
            }

            // List of Gene Sets Database files
            String geneSetDBsParam = cl.getOptionValue("gmx");

            if (StringUtils.isBlank(geneSetDBsParam)) {
                String paramName = (gpMode) ? "gene.sets.database" : "-gmx";
                klog.error("No Gene Sets Databases files were specified.");
                klog.error("Please provide one or more values to the '"+ paramName + "' parameter.");
                paramProcessingError = true;
            }

            List<String> geneSetDBs = (StringUtils.isBlank(geneSetDBsParam)) ? Collections.emptyList()
                    : FileUtils.readLines(new File(geneSetDBsParam), (Charset) null);
            if (gpMode) {
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
                geneSetDBs = safeNameGeneSetDBs;
            }

            String rptLabel = cl.getOptionValue("rpt_label");
            if (StringUtils.isNotBlank(rptLabel)) {
                if (gpMode) {
                    klog.warn("-rpt_label parameter ignored; only valid wih -run_as_genepattern false.");
                }
            } else {
                if (!gpMode) rptLabel = "my_analysis";
            }
            if (gpMode) rptLabel = FilenameUtils.getBaseName(outputFileName);

            String delim = ",";
            String altDelim = cl.getOptionValue("altDelim", "");
            Pattern delimPattern = COMMA_PATTERN;
            if (StringUtils.isNotBlank(altDelim)) {
                if (altDelim.length() > 1) {
                    String paramName = (gpMode) ? "alt.delim" : "--altDelim";
                    klog.error("Invalid " + paramName + " '" + altDelim
                            + "' specified. This must be only a single character and no whitespace.");
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
            List<String> geneSetsSelection = (selectedGeneSets.isEmpty()) ? geneSetDBs
                    : selectGeneSetsFromFiles(geneSetDBs, selectedGeneSets, gpMode);
            paramProcessingError |= (geneSetsSelection == null);

            String geneSetsSelector = StringUtils.join(geneSetsSelection, delim);

            if (paramProcessingError) {
                // Should probably use BadParamException and set an errorCode, use it to look up a Wiki Help page.
                throw new Exception("There were one or more errors with the job parameters.  Please check log output for details.");
            }

            klog.info("Parameters passing to GSEAPreranked.main:");
            setParam("rnk", rankedListFileName, paramProps, klog);
            setParam("gmx", geneSetsSelector, paramProps, klog);
            setParam("rpt_label", rptLabel, paramProps, klog);
            setParam("zip_report", Boolean.toString(createZip), paramProps, klog);
            setParam("gui", "false", paramProps, klog);
            if (gpMode) setParam("out", analysis.getPath(), paramProps, klog);

            if (StringUtils.isNotBlank(altDelim)) {
                setParam("altDelim", altDelim, paramProps, klog);
            }

            // Finally, load up the remaining simple parameters. We'll let Preranked validate these.
            setOptionValueAsParam("norm", cl, paramProps, klog);
            setOptionValueAsParam("nperm", cl, paramProps, klog);
            setOptionValueAsParam("scoring_scheme", cl, paramProps, klog);
            setOptionValueAsParam("make_sets", cl, paramProps, klog);
            setOptionValueAsParam("plot_top_x", cl, paramProps, klog);
            setOptionValueAsParam("rnd_seed", cl, paramProps, klog);
            setOptionValueAsParam("create_svgs", cl, paramProps, klog);
            setOptionValueAsParam("set_max", cl, paramProps, klog);
            setOptionValueAsParam("set_min", cl, paramProps, klog);

            tool = new GseaPreranked(paramProps);
            try {
                success = AbstractTool.module_main(tool);
            } finally {
                if (gpMode) {
                    try {
                        if (!analysis.exists()) return;
                        copyAnalysisToCurrentDir(cwd, analysis, createZip, outputFileName);
                    } catch (IOException ioe) {
                        System.err.println("Error during clean-up:");
                        ioe.printStackTrace(System.err);
                    }
                }
            }
        } finally {
            try {
                if (cwd != null && tmp_working != null) {
                    cleanUpAnalysisDirs(cwd, tmp_working);
                }
            } finally {
                Conf.exitSystem(!success);
            }
        }
    }
}