/*
 *  Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.mit.broad.genome.Conf;
import xtools.api.AbstractTool;
import xtools.api.param.BadParamException;
import xtools.gsea.Gsea;

/**
 * When running in GenePattern mode:
 * GseaWrapper parses the command line arguments passed in by GP Server's run task page, creates a new parameter file, and passes that
 * parameter file to the GSEA tool's main method. Upon completion of GSEA, GseaWrapper creates a zip file containing results and then cleans
 * up the working directory so it only contains the zip file, the output files, and the input files that were uploaded by the run task page.
 */
public class GseaWrapper extends AbstractModule {
    private static final Logger klog = Logger.getLogger(GseaWrapper.class);
    
    // Suppressing the static-access warnings because this is the recommended usage according to the Commons-CLI docs.
    @SuppressWarnings("static-access")
    private static Options setupCliOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("expressionDataset").hasArg().create("res"));
        options.addOption(OptionBuilder.withArgName("phenotypeLabels").hasArg().create("cls"));
        options.addOption(OptionBuilder.withArgName("collapseDataset").hasArg().create("collapse"));
        options.addOption(OptionBuilder.withArgName("collapseMode").hasArg().create("mode"));
        options.addOption(OptionBuilder.withArgName("normalizationMode").hasArg().create("norm"));
        options.addOption(OptionBuilder.withArgName("numberOfPermutations").hasArg().create("nperm"));
        options.addOption(OptionBuilder.withArgName("permuteType").hasArg().create("permute"));
        options.addOption(OptionBuilder.withArgName("randomizationMode").hasArg().create("rnd_type"));
        options.addOption(OptionBuilder.withArgName("scoringScheme").hasArg().create("scoring_scheme"));
        options.addOption(OptionBuilder.withArgName("geneRankingMetric").hasArg().create("metric"));
        options.addOption(OptionBuilder.withArgName("geneListSortingMode").hasArg().create("sort"));
        options.addOption(OptionBuilder.withArgName("geneListOrderingMode").hasArg().create("order"));
        options.addOption(OptionBuilder.withArgName("omitFeaturesWithNoSymbolMatch").hasArg().create("include_only_symbols"));
        options.addOption(OptionBuilder.withArgName("makeDetailedGeneSetReport").hasArg().create("make_sets"));
        options.addOption(OptionBuilder.withArgName("medianForClassMetrics").hasArg().create("median"));
        options.addOption(OptionBuilder.withArgName("numberOfMarkers").hasArg().create("num"));
        options.addOption(OptionBuilder.withArgName("numberOftopSetsToPlot").hasArg().create("plot_top_x"));
        options.addOption(OptionBuilder.withArgName("randomSeed").hasArg().create("rnd_seed"));
        options.addOption(OptionBuilder.withArgName("saveRandomRankedLists").hasArg().create("save_rnd_lists"));
        options.addOption(OptionBuilder.withArgName("createSvgs").hasArg().create("create_svgs"));
        options.addOption(OptionBuilder.withArgName("createGcts").hasArg().create("create_gcts"));
        options.addOption(OptionBuilder.withArgName("maxGeneSetSize").hasArg().create("set_max"));
        options.addOption(OptionBuilder.withArgName("minGeneSetSize").hasArg().create("set_min"));
        options.addOption(OptionBuilder.withArgName("chipPlatform").hasOptionalArg().create("chip"));
        options.addOption(OptionBuilder.withArgName("geneSetsDatabaseList").hasOptionalArg().create("gmx_list"));
        options.addOption(OptionBuilder.withArgName("geneSetsDatabase").hasOptionalArg().create("gmx"));
        options.addOption(OptionBuilder.withArgName("targetProfile").hasOptionalArg().create("target_profile"));
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
            
            String expressionDataFileName = cl.getOptionValue("res");
            if (StringUtils.isNotBlank(expressionDataFileName)) {
                // Copy the file to get rid of problematic characters, if necessary. A null return value indicates something went wrong
                if (gpMode) expressionDataFileName = copyFileWithoutBadChars(expressionDataFileName, tmp_working);
                paramProcessingError |= (expressionDataFileName == null);
            } else {
                String paramName = (gpMode) ? "expression.dataset" : "-res";
                klog.error("Required parameter '" + paramName + "' not found.");
                paramProcessingError = true;
            }

            String classFileName = cl.getOptionValue("cls");
            if (StringUtils.isNotBlank(classFileName)) {
                if (gpMode) classFileName = copyFileWithoutBadChars(classFileName, tmp_working);
                paramProcessingError |= (classFileName == null);

                String targetProfile = cl.getOptionValue("target_profile");
                if (StringUtils.isNotBlank(targetProfile)) {
                    if (gpMode) {
                        // For continuous phenotype, GSEA expects the target profile
                        // to be appended to the cls file name, with '#" as separator.
                        classFileName = classFileName + "#" + targetProfile;
                    }
                    else {
                        klog.warn("-target_profile parameter ignored; only valid wih -run_as_genepattern true.");
                    }
                }
            } else {
                String paramName = (gpMode) ? "phenotype.labels" : "-cls";
                klog.error("Required parameter '"+ paramName +"' not found.");
                paramProcessingError = true;
            }

            String chipPlatformFileName = cl.getOptionValue("chip");
            String isCollapse = cl.getOptionValue("collapse");

            if (StringUtils.isNotBlank(chipPlatformFileName)) {
                if (gpMode) chipPlatformFileName = copyFileWithoutBadChars(chipPlatformFileName, tmp_working);
                paramProcessingError |= (chipPlatformFileName == null);
            } else if (isCollapse.equals("true")) {
                String paramName = (gpMode) ? "chip.platform.file" : "-chip";
                klog.error("collapse is set to true; a '"+ paramName + "' must be provided");
                paramProcessingError = true;
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
            
            String geneSetDBParam = cl.getOptionValue("gmx");
            String geneSetDBListParam = cl.getOptionValue("gmx_list");
            String selectedGeneSetsParam = cl.getOptionValue("selected_gene_sets");

            String altDelim = cl.getOptionValue("altDelim", "");
            if (StringUtils.isNotBlank(altDelim) && altDelim.length() > 1) {
                String paramName = (gpMode) ? "alt.delim" : "--altDelim";
                klog.error("Invalid " + paramName + " '" + altDelim
                        + "' specified. This must be only a single character and no whitespace.");
                paramProcessingError = true;
            }

            String geneSetsSelector = determineSelectorFromParams(geneSetDBParam, geneSetDBListParam, selectedGeneSetsParam, altDelim, gpMode, tmp_working, klog);
            paramProcessingError |= geneSetsSelector == null;

            if (paramProcessingError) {
                // Should probably use BadParamException and set an errorCode, use it to look up a Wiki Help page.
                throw new Exception("There were one or more errors with the job parameters.  Please check log output for details.");
            }

            klog.info("Parameters passed to GSEA tool:");
            setParam("gmx", geneSetsSelector, paramProps, klog);
            setParam("res", expressionDataFileName, paramProps, klog);
            setParam("cls", classFileName, paramProps, klog);
            setParam("rpt_label", rptLabel, paramProps, klog);
            setParam("collapse", isCollapse, paramProps, klog);
            setParam("zip_report", Boolean.toString(createZip), paramProps, klog);
            setParam("gui", "false", paramProps, klog);
            if (gpMode) setParam("out", analysis.getPath(), paramProps, klog);

            if (StringUtils.equalsIgnoreCase(isCollapse, "true")) {
                setParam("chip", chipPlatformFileName, paramProps, klog);
            }

            if (StringUtils.isNotBlank(altDelim)) {
                setParam("altDelim", altDelim, paramProps, klog);
            }

            // Finally, load up the remaining simple parameters. We'll let GSEA validate these.
            setOptionValueAsParam("mode", cl, paramProps, klog);
            setOptionValueAsParam("norm", cl, paramProps, klog);
            setOptionValueAsParam("nperm", cl, paramProps, klog);
            setOptionValueAsParam("permute", cl, paramProps, klog);
            setOptionValueAsParam("rnd_type", cl, paramProps, klog);
            setOptionValueAsParam("scoring_scheme", cl, paramProps, klog);
            setOptionValueAsParam("metric", cl, paramProps, klog);
            setOptionValueAsParam("sort", cl, paramProps, klog);
            setOptionValueAsParam("order", cl, paramProps, klog);
            setOptionValueAsParam("include_only_symbols", cl, paramProps, klog);
            setOptionValueAsParam("make_sets", cl, paramProps, klog);
            setOptionValueAsParam("median", cl, paramProps, klog);
            setOptionValueAsParam("num", cl, paramProps, klog);
            setOptionValueAsParam("plot_top_x", cl, paramProps, klog);
            setOptionValueAsParam("rnd_seed", cl, paramProps, klog);
            setOptionValueAsParam("save_rnd_lists", cl, paramProps, klog);
            setOptionValueAsParam("create_svgs", cl, paramProps, klog);
            setOptionValueAsParam("create_gcts", cl, paramProps, klog);
            setOptionValueAsParam("set_max", cl, paramProps, klog);
            setOptionValueAsParam("set_min", cl, paramProps, klog);

            tool = new Gsea(paramProps);
            try {
                success = AbstractTool.module_main(tool);
            } catch (BadParamException e) {
                String message = e.getMessage();
                if (message != null && message.contains("None of the gene sets passed the size thresholds")) {
                    klog.error("Please verify that the correct chip platform was provided.");
                    throw e;
                }
            } finally {
                if (gpMode) {
                    try {
                        if (!analysis.exists()) return;
                        copyAnalysisToCurrentDir(cwd, analysis, createZip, outputFileName);
                    } catch (IOException ioe) {
                        klog.error("Error during clean-up...");
                        throw ioe;
                    } finally {
                        cleanUpAnalysisDirs(cwd, tmp_working);
                    }
                }
            }
        } catch (Throwable t) {
            success = false;
            klog.error("Error while processng:");
            klog.error(t.getMessage());
            t.printStackTrace(System.err);
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
