/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.gsea;

import edu.mit.broad.genome.Headers;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.gsea.KSTests;
import edu.mit.broad.genome.math.*;
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
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import xtools.api.param.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Aravind Subramanian, David Eby
 */
public abstract class AbstractGsea2Tool extends AbstractGseaTool {

    protected final DatasetReqdParam fDatasetParam = new DatasetReqdParam();

    protected final TemplateSingleChooserParam fTemplateParam =
            new TemplateSingleChooserParam(Param.CLS, "Phenotype labels", TemplateMode.CATEGORICAL_2_CLASS_AND_NUMERIC, true);

    protected final MetricParam fMetricParam = new MetricParam(createMetricsForGsea(), false);
    protected final OrderParam fOrderParam = new OrderParam(false);
    protected final SortParam fSortParam = new SortParam(false);
    protected final PermuteTypeChooserParam fPermuteTypeParamType = PermuteTypeChooserParam.createTemplateOrGeneSet(true);
    protected final BooleanParam fMedianParam = new BooleanParam("median", "Median for class  metrics", "Use the median of each class instead of the mean for the class seperation metrics", XPreferencesFactory.kMedian.getBoolean(), false);
    protected final IntegerParam fNumMarkersParam = new IntegerParam("num", "Number of markers", "Number of markers", 100, false);

    protected final BooleanParam fSaveRndRankedListsParam = new BooleanParam("save_rnd_lists", "Save random ranked lists", "Save random ranked lists (might be very large)", false, false);

    protected final TemplateRandomizerTypeParam fRndTypeParam = new TemplateRandomizerTypeParam(
            TemplateRandomizerType.NO_BALANCE,
            new TemplateRandomizerType[]{TemplateRandomizerType.NO_BALANCE,
                    TemplateRandomizerType.EQUALIZE_AND_BALANCE}, true);

    protected AbstractGsea2Tool(String defCollapseMode) {
        super(defCollapseMode);
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
    }

    private EnrichmentDb execute_one(final CollapsedDetails.Data fullCd,
                                     final Template template, final GeneSet[] gsets,
                                     List<RankedList> store_rnd_ranked_lists_here_opt) throws Exception {
        final RandomSeedGenerator rst = fRndSeedTypeParam.createSeed();
        final DatasetTemplate dt = new DatasetGenerators().extract(fullCd.getDataset(), template);

        if (log.isDebugEnabled()) { log.debug(">>>>> Using samples: " + dt.getDataset().getColumnNames()); }

        final KSTests tests = new KSTests(getOutputStream());
        
        // If we have a RandomSeedGenerator.Timestamp instance, save the timestamp for later reference
        if (rst instanceof RandomSeedGenerators.Timestamp) {
            fReport.addComment("Timestamp used as random seed: " + 
                    ((RandomSeedGenerators.Timestamp)rst).getTimestamp());
        }

        return tests.executeGsea(dt, gsets, fNumPermParam.getIValue(), fMetricParam.getMetric(),
        		fSortParam.getMode(), fOrderParam.getOrder(), rst,
                fRndTypeParam.getRandomizerType(), getMetricParams(fMedianParam),
                fGcohGenReqdParam.createGeneSetCohortGenerator(), fPermuteTypeParamType.permuteTemplate(),
                fNumMarkersParam.getIValue(), store_rnd_ranked_lists_here_opt);

    }

    protected void execute_one_with_reporting(final CollapsedDetails.Data fullCd, final Template template, 
    		final GeneSet[] gsets, final HtmlReportIndexPage reportIndexPage, final boolean makeSubDir,
    		final GeneSet[] origGeneSets, final int showDetailsForTopXSets, final boolean makeZippedReport, 
    		final boolean makeGeneSetReports, final boolean createSvgs, final boolean createGcts) throws Exception {
        List<RankedList> store_rnd_ranked_lists_here_opt = fSaveRndRankedListsParam.isTrue() ? new ArrayList<RankedList>() : null;

        final EnrichmentDb edb = execute_one(fullCd, template, gsets, store_rnd_ranked_lists_here_opt);

        // -------------------------------------------------------------------------------------------- //
        // rest are for the reporting

        final Metric metric = fMetricParam.getMetric();
        final int minSize = fGeneSetMinSizeParam.getIValue();
        final int maxSize = fGeneSetMaxSizeParam.getIValue();

        final DatasetTemplate dt = new DatasetGenerators().extract(fullCd.getDataset(), template);

        // Make the report
        EnrichmentReports.Ret ret = EnrichmentReports.createGseaLikeReport(edb, getOutputStream(), fullCd,
        		reportIndexPage, makeSubDir, fReport, showDetailsForTopXSets, minSize, maxSize, makeGeneSetReports,
                makeZippedReport, createSvgs, createGcts, origGeneSets, metric.getName(), fNormModeParam.getNormModeName());

        // Save the rnd ranked lists
        // Note: carrying this list through until after the algorithm completes has negative memory usage implications.
        // This is fine with the way things are currently structured but could change if we restructure in other ways,
        // e.g. to generate the lists on demand as we go rather than up-front.  Then we could generate & save the list,
        // run the iteration, then drop it so it doesn't consume memory.
        if (store_rnd_ranked_lists_here_opt != null && !store_rnd_ranked_lists_here_opt.isEmpty()) {
            File dir = fReport.createSubDir("random_ranked_lists");
            for (int r = 0; r < store_rnd_ranked_lists_here_opt.size(); r++) {
                RankedList rl = store_rnd_ranked_lists_here_opt.get(r);
                // Prepend the list position for uniqueness; append the extension if necessary.
                String name = r + "_" + rl.getName();
                if (!StringUtils.endsWith(name, ".rnk")) name += ".rnk";
                File file = new File(dir, name);
                ParserFactory.save(rl, file);
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

    // result hack to allow setting mean / median
    public Map<String, Boolean> getMetricParams(BooleanParam medianParam) {
        Map<String, Boolean> params = new HashMap<String, Boolean>();
        params.put(Headers.USE_MEDIAN, XPreferencesFactory.kMedian.getBooleanO());
        params.put(Headers.FIX_LOW, XPreferencesFactory.kFixLowVar.getBooleanO());
        params.put(Headers.USE_BIASED, XPreferencesFactory.kBiasedVar.getBooleanO());
        params.put(Headers.USE_MEDIAN, (Boolean)medianParam.getValue());
        return Collections.unmodifiableMap(params);
    }
}