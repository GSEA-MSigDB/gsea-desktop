/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.gsea;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.gsea.GeneSetCohortGenerator;
import edu.mit.broad.genome.alg.gsea.KSTests;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateMode;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.genome.objects.strucs.DatasetTemplate;
import edu.mit.broad.genome.objects.strucs.TemplateRandomizerType;
import edu.mit.broad.genome.parsers.EdbFolderParser;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.reports.pages.HtmlReportIndexPage;
import edu.mit.broad.vdb.chip.Chip;
import xtools.api.param.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class AbstractGsea2Tool extends AbstractGseaTool {

    protected final DatasetReqdParam fDatasetParam = new DatasetReqdParam();

    protected final TemplateSingleChooserParam fTemplateParam =
            new TemplateSingleChooserParam(Param.CLS, "Phenotype labels", TemplateMode.CATEGORICAL_2_CLASS_AND_NUMERIC, true);

    protected final MetricParam fMetricParam = new MetricParam(createMetricsForGsea(), false);
    protected final OrderParam fOrderParam = new OrderParam(false);
    protected final SortParam fSortParam = new SortParam(false);
    protected final PermuteTypeChooserParam fPermuteTypeParamType = PermuteTypeChooserParam.createTemplateOrGeneSet(true);
    protected final BooleanParam fMedianParam = ParamFactory.createMedianParam(false);
    protected final IntegerParam fNumMarkersParam = ParamFactory.createNumMarkersParam(100, false);

    protected final BooleanParam fSaveRndRankedListsParam = new BooleanParam("save_rnd_lists", "Save random ranked lists", "Save random ranked lists (might be very large)", false, false);

    protected final TemplateRandomizerTypeParam fRndTypeParam = new TemplateRandomizerTypeParam(
            TemplateRandomizerType.NO_BALANCE,
            new TemplateRandomizerType[]{TemplateRandomizerType.NO_BALANCE,
                    TemplateRandomizerType.EQUALIZE_AND_BALANCE}, true);

    protected ChipOptParam fChipParam = new ChipOptParam(false);
    protected final FeatureSpaceReqdParam fFeatureSpaceParam = new FeatureSpaceReqdParam();
    protected final ModeReqdParam fCollapseModeParam = new ModeReqdParam("mode", "Collapsing mode for probe sets => 1 gene", "Collapsing mode for probe sets => 1 gene", new String[]{"Max_probe", "Median_of_probes"});
    protected final BooleanParam fIncludeOnlySymbols = new BooleanParam("include_only_symbols", "Omit features with no symbol match", "If there is no known gene symbol match for a probe set omit if from the collapsed dataset", true, false);

    /**
     * Class constructor
     *
     * @param properties
     */
    protected AbstractGsea2Tool() {
    }

    protected void doAdditionalParams() {
        fParamSet.addParam(fDatasetParam);
        fParamSet.addParamPseudoReqd(fTemplateParam);
        fParamSet.addParamBasic(fMetricParam);
        fParamSet.addParamBasic(fOrderParam);
        fParamSet.addParamBasic(fSortParam);
        fParamSet.addParamPseudoReqd(fPermuteTypeParamType);
        fParamSet.addParamAdv(fMedianParam);
        fParamSet.addParamAdv(fNumMarkersParam);
        fParamSet.addParamAdv(fSaveRndRankedListsParam);
        fParamSet.addParamAdv(fRndTypeParam);
        
        // Collapse Dataset parameters
        fParamSet.addParamPseudoReqd(fChipParam);
        fParamSet.addParamPseudoReqd(fFeatureSpaceParam);
        fParamSet.addParamAdv(fIncludeOnlySymbols);
        fParamSet.addParamAdv(fCollapseModeParam);
    }

    protected EnrichmentDb execute_one(final CollapsedDetails.Data fullCd,
                                       final Template template,
                                       final GeneSet[] gsets,
                                       List store_rnd_ranked_lists_here_opt

    ) throws Exception {

        final int nperms = fNumPermParam.getIValue();
        final Metric metric = fMetricParam.getMetric();
        final SortMode sort = fSortParam.getMode();
        final Order order = fOrderParam.getOrder();

        final LabelledVectorProcessor lvp = new LabelledVectorProcessors.None(); // @note

        final RandomSeedGenerator rst = fRndSeedTypeParam.createSeed();
        final Map mps = fMetricParam.getMetricParams(fMedianParam);
        final GeneSetCohortGenerator gcohgen = fGcohGenReqdParam.createGeneSetCohortGenerator(false);

        final DatasetTemplate dt = new DatasetGenerators().extract(fullCd.getDataset(), template);

        log.debug(">>>>> Using samples: " + dt.getDataset().getColumnNames());

        final KSTests tests = new KSTests(getOutputStream());
        
        // If we have a RandomSeedGenerator.Timestamp instance, save the timestamp for later reference
        if (rst instanceof RandomSeedGenerators.Timestamp) {
            fReport.addComment("Timestamp used as random seed: " + 
                    ((RandomSeedGenerators.Timestamp)rst).getTimestamp());
        }

        return tests.executeGsea(
                dt,
                gsets,
                nperms,
                metric,
                sort,
                order,
                lvp,
                rst,
                fRndTypeParam.getRandomizerType(),
                mps,
                gcohgen,
                fPermuteTypeParamType.permuteTemplate(),
                fNumMarkersParam.getIValue(),
                store_rnd_ranked_lists_here_opt
        );

    }

    protected void execute_one_with_reporting(final CollapsedDetails.Data fullCd,
                                              final Template template,
                                              final GeneSet[] gsets,
                                              final HtmlReportIndexPage reportIndexPage,
                                              final boolean makeSubDir,
                                              final GeneSet[] origGeneSets,
                                              final int showDetailsForTopXSets,
                                              final boolean makeZippedReport,
                                              final boolean makeGeneSetReports,
                                              final boolean createSvgs,
                                              final boolean createGcts

    ) throws Exception {

        List store_rnd_ranked_lists_here_opt = null;
        if (fSaveRndRankedListsParam.isTrue()) {
            store_rnd_ranked_lists_here_opt = new ArrayList();
        }

        final EnrichmentDb edb = execute_one(fullCd, template, gsets, store_rnd_ranked_lists_here_opt);

        // -------------------------------------------------------------------------------------------- //
        // rest are for the reporting

        final Metric metric = fMetricParam.getMetric();
        final int minSize = fGeneSetMinSizeParam.getIValue();
        final int maxSize = fGeneSetMaxSizeParam.getIValue();

        final DatasetTemplate dt = new DatasetGenerators().extract(fullCd.getDataset(), template);

        // Make the report
        EnrichmentReports.Ret ret = EnrichmentReports.createGseaLikeReport(
                edb,
                getOutputStream(),
                fullCd,
                reportIndexPage,
                makeSubDir,
                fReport,
                showDetailsForTopXSets,
                minSize, maxSize,
                makeGeneSetReports,
                makeZippedReport,
                createSvgs,
                createGcts,
                origGeneSets,
                metric.getName(),
                fNormModeParam.getNormModeName());

        // Save the rnd ranked lists
        // Note: carrying this list through until after the algorithm completes has negative memory usage implications.
        // This is fine with the way things are currently structured but could change if we restructure in other ways,
        // e.g. to generate the lists on demand as we go rather than up-front.  Then we could generate & save the list,
        // run the iteration, then drop it so it doesn't consume memory.
        if (store_rnd_ranked_lists_here_opt != null && store_rnd_ranked_lists_here_opt.isEmpty() == false) {
            File dir = fReport.createSubDir("random_ranked_lists");
            for (int r = 0; r < store_rnd_ranked_lists_here_opt.size(); r++) {
                RankedList rl = (RankedList) store_rnd_ranked_lists_here_opt.get(r);
                // Prepend the list position for uniqueness; append the extension if necessary.
                String name = r + "_" + rl.getName();
                if (!StringUtils.endsWith(name, ".rnk")) name += ".rnk";
                File file = new File(dir, name);
                ParserFactory.save(rl, file, false);
            }
        }

        if (fPermuteTypeParamType.permuteTemplate() && dt.getTemplate().isCategorical()) {
            if (dt.getTemplate().getClass(0).getSize() < 7) {
                fReport.addComment("Warning: Phenotype permutation was performed but the number of samples in class A is < 7, phenotype: " + dt.getTemplateName());
            }

            if (dt.getTemplate().getClass(1).getSize() < 7) {
                fReport.addComment("Warning: Phenotype permutation was performed but the number of samples in class B is < 7, phenotype: " + dt.getTemplateName());
            }

            if (dt.getTemplate().getNumItems() < 14) {
                fReport.addComment("With small datasets, there might not be enough random permutations of sample labels to generate a sufficient null distribution. " +
                        "In such cases, gene_set randomization might be a better choice.");
            }
        }

        // Make an edb folder thing
        new EdbFolderParser().export(ret.edb, ret.savedInDir);
    }

    protected CollapsedDetails.Data getDataset(final Dataset origDs) throws Exception {
        CollapsedDetails.Data cd = new CollapsedDetails.Data();
        cd.orig = origDs;
    
        if (fFeatureSpaceParam.isSymbols()) {
            if (!fChipParam.isSpecified()) {
                // dont as the chip param isnt really reqd (and hence isnt caught in the usual way)
                //throw new MissingReqdParamException(_getMissingChipMessage());
                throw new BadParamException("Chip parameter must be specified as you asked to analyze" +
                        " in the space of gene symbols. Chip is used to collapse probe ids into symbols", 1002);
            }
    
            final Chip chip = fChipParam.getChipCombo();
            Dataset collapsed = new DatasetGenerators().collapse(origDs, chip,
                    fIncludeOnlySymbols.isTrue(), fCollapseModeParam.getStringIndexChoosen());
            log.info("Collapsing dataset was done. Original: " + origDs.getQuickInfo() + " collapsed: " + collapsed.getQuickInfo());
    
            cd.chip = chip;
            cd.wasCollapsed = true;
            cd.collapsed = collapsed;
            ParamFactory.checkIfCollapsedIsEmpty(cd);
    
        } else {
            cd.wasCollapsed = false;
            cd.collapsed = origDs;
            log.info("No dataset collapsing was done .. using original as is");
        }
    
        return cd;
    }

}    // End AbstractGsea2Tool
