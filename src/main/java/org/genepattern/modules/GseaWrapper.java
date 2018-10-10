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
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.Conf;
import xtools.api.AbstractTool;
import xtools.api.param.BadParamException;
import xtools.gsea.Gsea;

/**
 * GseaWrapper parses the command line arguments passed in by GP Server's run task page, creates a new parameter file, and passes that
 * parameter file to the GSEA tool's main method. Upon completion of GSEA, GseaWrapper creates a zip file containing results and then cleans
 * up the working directory so it only contains the zip file, the output files, and the input files that were uploaded by the run task page.
 */
public class GseaWrapper extends AbstractModule {
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
        options.addOption(OptionBuilder.withArgName("geneSetsDatabase").hasOptionalArg().create("gmx"));
        options.addOption(OptionBuilder.withArgName("targetProfile").hasOptionalArg().create("target_profile"));
        options.addOption(OptionBuilder.withArgName("selectedGeneSets").hasOptionalArg().create("selected_gene_sets"));
        options.addOption(OptionBuilder.withArgName("outputFileName").hasOptionalArg().create("output_file_name"));
        options.addOption(OptionBuilder.withArgName("altDelim").hasOptionalArg().create("altDelim"));
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
            CommandLine cl = parser.parse(opts, args);

            // We want to check *all* params before reporting any errors so that the user sees everything that went wrong.
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

            String outputFileName = cl.getOptionValue("output_file_name");
            if (StringUtils.isNotBlank(outputFileName)) {
                if (!outputFileName.toLowerCase().endsWith(".zip")) {
                    outputFileName += ".zip";
                }
            } else {
                outputFileName = "gsea_analysis.zip";
            }
            final String zipFileName = outputFileName;

            String expressionDataFileName = cl.getOptionValue("res");
            if (StringUtils.isNotBlank(expressionDataFileName)) {
                // Copy the file to get rid of problematic character, if necessary. A null return value indicates something went wrong
                expressionDataFileName = copyFileWithoutBadChars(expressionDataFileName, tmp_working);
                paramProcessingError |= (expressionDataFileName == null);
            } else {
                System.err.println("Required parameter 'expression.dataset' not found.");
                paramProcessingError = true;
            }

            String classFileName = cl.getOptionValue("cls");
            if (StringUtils.isNotBlank(classFileName)) {
                classFileName = copyFileWithoutBadChars(classFileName, tmp_working);
                paramProcessingError |= (classFileName == null);

                String targetProfile = cl.getOptionValue("target_profile");
                if (StringUtils.isNotBlank(targetProfile)) {
                    // For continuous phenotype, GSEA expects the target profile
                    // to be appended to the cls file name, with '#" as separator.
                    classFileName = classFileName + "#" + targetProfile;
                }
            } else {
                System.err.println("Required parameter 'phenotype.labels' not found.");
                paramProcessingError = true;
            }

            String chipPlatformFileName = cl.getOptionValue("chip");
            String isCollapse = cl.getOptionValue("collapse");

            if (StringUtils.isNotBlank(chipPlatformFileName)) {
                chipPlatformFileName = copyFileWithoutBadChars(chipPlatformFileName, tmp_working);
                paramProcessingError |= (chipPlatformFileName == null);
            } else if (isCollapse.equals("true")) {
                System.err.println("collapse is set to true; a 'chip.platform.file' must be provided");
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

            System.out.println("Parameters passed to GSEA tool:");
            setParam("gmx", geneSetsSelector, paramProps);
            setParam("res", expressionDataFileName, paramProps);
            setParam("cls", classFileName, paramProps);
            setParam("out", analysis.getPath(), paramProps);

            setParam("rpt_label", FilenameUtils.getBaseName(outputFileName), paramProps);
            setParam("zip_report", Boolean.toString(createZip), paramProps);
            setParam("gui", "false", paramProps);

            // This may change in the future when/if we allow the user to provide their own CHIP as an annotation mechanism,
            // when we would pass through the CHIP regardless of collapse.
            setParam("collapse", isCollapse, paramProps);
            if (StringUtils.equalsIgnoreCase(isCollapse, "true")) {
                setParam("chip", chipPlatformFileName, paramProps);
            }

            if (StringUtils.isNotBlank(altDelim)) {
                setParam("altDelim", altDelim, paramProps);
            }

            // Finally, load up the remaining simple parameters. We'll let GSEA validate these.
            setOptionValueAsParam("mode", cl, paramProps);
            setOptionValueAsParam("norm", cl, paramProps);
            setOptionValueAsParam("nperm", cl, paramProps);
            setOptionValueAsParam("permute", cl, paramProps);
            setOptionValueAsParam("rnd_type", cl, paramProps);
            setOptionValueAsParam("scoring_scheme", cl, paramProps);
            setOptionValueAsParam("metric", cl, paramProps);
            setOptionValueAsParam("sort", cl, paramProps);
            setOptionValueAsParam("order", cl, paramProps);
            setOptionValueAsParam("include_only_symbols", cl, paramProps);
            setOptionValueAsParam("make_sets", cl, paramProps);
            setOptionValueAsParam("median", cl, paramProps);
            setOptionValueAsParam("num", cl, paramProps);
            setOptionValueAsParam("plot_top_x", cl, paramProps);
            setOptionValueAsParam("rnd_seed", cl, paramProps);
            setOptionValueAsParam("save_rnd_lists", cl, paramProps);
            setOptionValueAsParam("create_svgs", cl, paramProps);
            setOptionValueAsParam("create_gcts", cl, paramProps);
            setOptionValueAsParam("set_max", cl, paramProps);
            setOptionValueAsParam("set_min", cl, paramProps);

            tool = new Gsea(paramProps);
            try {
                success = AbstractTool.module_main(tool);
            } catch (BadParamException e) {
                String message = e.getMessage();
                if (message != null && message.contains("None of the gene sets passed the size thresholds")) {
                    System.err.print("Please verify that the correct chip platform was provided.");
                    throw e;
                }
            } finally {
                try {
                    if (!analysis.exists()) return;
                    copyAnalysisToCurrentDir(cwd, analysis, createZip, zipFileName);
                } catch (IOException ioe) {
                    System.err.println("Error during clean-up...");
                    throw ioe;
                } finally {
                    cleanUpAnalysisDirs(cwd, tmp_working);
                }
            }
        } catch (Throwable t) {
            success = false;
            System.err.println("Error while processng:");
            System.err.println(t.getMessage());
            t.printStackTrace(System.err);
        } finally {
            Conf.exitSystem(!success);
        }
    }
}
