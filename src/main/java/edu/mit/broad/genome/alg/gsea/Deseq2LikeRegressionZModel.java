/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.alg.DatasetStatsCore.TwoClassMarkerStats;
import edu.mit.broad.genome.math.AddressedVector;
import edu.mit.broad.genome.math.DoubleElement;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.ScoredDatasetImpl;
import edu.mit.broad.genome.objects.Template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.special.Gamma;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Deseq2LikeRegressionZModel {
    private static final Logger log = LoggerFactory.getLogger(Deseq2LikeRegressionZModel.class);

    private static final double TARGET_FDR = 0.10d;
    private static final int FILTER_GRID_POINTS = 50;
    private static final double FILTER_SMOOTHING_SPAN = 0.20d;
    private static final double LOCAL_DISPERSION_SPAN = 0.70d;
    private static final double LOWESS_DELTA_FRACTION = 0.01d;
    private static final double DEFAULT_COOKS_QUANTILE = 0.99d;

    private static final double EPS = 1.0e-8d;
    private static final double MIN_DISP = 1.0e-8d;
    private static final double DISP_PRIOR_VAR_MIN = 0.25d;
    private static final int DISP_GRID_SIZE = 20;
    private static final int DISP_MAX_ITERS = 100;
    private static final double DISP_TOL = 1.0e-6d;
    private static final double DISP_ARMIJO_EPSILON = 1.0e-4d;
    private static final double DISP_KAPPA0 = 1.0d;
    private static final double ROBUST_NORMAL_SD_QUANTILE = 0.6744897501960817d;
    private static final double DISP_PRIOR_KL_MAX = 8.0d;
    private static final int DISP_PRIOR_KL_GRID_SIZE = 200;
    private static final int DISP_PRIOR_KL_FINE_GRID_SIZE = 1000;
    private static final int DISP_PRIOR_KL_SAMPLE_SIZE = 10000;
    private static final double DISP_PRIOR_KL_HISTOGRAM_MIN = -10.0d;
    private static final double DISP_PRIOR_KL_HISTOGRAM_MAX = 10.0d;
    private static final double DISP_PRIOR_KL_HISTOGRAM_STEP = 0.5d;
    private static final long DISP_PRIOR_KL_SEED = 2L;
    private static final double MIN_LOG_ALPHA_PROPOSE = -30.0d;
    private static final double MAX_LOG_ALPHA = 10.0d;
    private static final double MIN_DISP_LINE_SEARCH = MIN_DISP / 10.0d;
    private static final double MIN_DISP_EVAL = Math.exp(MIN_LOG_ALPHA_PROPOSE);
    private static final int COEF_MAX_ITERS = 100;
    private static final double COEF_TOL = 1.0e-8d;
    private static final double MIN_MU = 0.5d;
    /** Default {@code lambda} (prior precision) on log2 scale in {@code fitNbinomGLMs} (R). */
    private static final double WIDE_PRIOR_LAMBDA_LOG2 = 1.0e-6d;
    private static final double WIDE_RIDGE = WIDE_PRIOR_LAMBDA_LOG2 / (Math.log(2.0d) * Math.log(2.0d));
    private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution(0.0d, 1.0d);
    private static final double ROBUST_COOKS_MIN_DISP = 0.04d;
    private static final int MIN_REPLICATES_FOR_REPLACE = 7;
    private static final double OUTLIER_REPLACEMENT_TRIM = 0.20d;

    private final Dataset ds;
    private final Dataset fitDs;
    private final double[] sizeFactors;
    private final double[] meanNormCounts;
    private final double[] shrunkDispersion;
    private final double independentFilterThreshold;
    private final boolean[] replacedRows;
    private final boolean[] replaceableSamples;
    private final boolean anyRowsReplaced;

    private Deseq2LikeRegressionZModel(final Dataset ds,
                                       final Dataset fitDs,
                                       final double[] sizeFactors,
                                       final double[] meanNormCounts,
                                       final double[] shrunkDispersion,
                                       final double independentFilterThreshold,
                                       final boolean[] replacedRows,
                                       final boolean[] replaceableSamples) {
        this.ds = ds;
        this.fitDs = fitDs;
        this.sizeFactors = sizeFactors;
        this.meanNormCounts = meanNormCounts;
        this.shrunkDispersion = shrunkDispersion;
        this.independentFilterThreshold = independentFilterThreshold;
        this.replacedRows = replacedRows;
        this.replaceableSamples = replaceableSamples;
        this.anyRowsReplaced = anyTrue(replacedRows);
    }

    public static Deseq2LikeRegressionZModel fit(final Dataset ds,
                                                 final Template realTemplate,
                                                 final Map<String, TwoClassMarkerStats> markerScores) {
        if (markerScores == null) {
            throw new IllegalArgumentException("markerScores cannot be null for Wald_Z (DESeq2-like count) scoring");
        }

        validateRawIntegerCounts(ds);

        final int colCount = ds.getNumCol();
        final double[] sizeFactors = computeSizeFactors(ds);
        final double[] conditionReal = createConditionVector(realTemplate, colCount);
        final boolean[] replaceableSamples = samplesEligibleForReplacement(conditionReal, MIN_REPLICATES_FOR_REPLACE);
        return fitInternal(ds,
                ds,
                markerScores,
                sizeFactors,
                conditionReal,
                new boolean[ds.getNumRow()],
                replaceableSamples,
                true,
                null);
    }

    /**
     * Carries trend-fit parameters and dispersion-prior statistics from the first fitting pass so that
     * the outlier-replacement refit pass can reuse them, mirroring DESeq2's {@code refitWithoutOutliers}:
     * on refit, R keeps the original dispersion trend function and the original {@code dispPriorVar},
     * and only re-estimates gene-wise & MAP dispersions for the replaced genes.
     */
    private static final class FrozenDispersionContext {
        final double trendA;
        final double trendB;
        final boolean useParametricTrend;
        final double[] localTrendByRow; // non-null only when the parametric fit fell back to local
        final double priorVar;
        final double varLogDispEsts;

        FrozenDispersionContext(final double trendA,
                                 final double trendB,
                                 final boolean useParametricTrend,
                                 final double[] localTrendByRow,
                                 final double priorVar,
                                 final double varLogDispEsts) {
            this.trendA = trendA;
            this.trendB = trendB;
            this.useParametricTrend = useParametricTrend;
            this.localTrendByRow = localTrendByRow;
            this.priorVar = priorVar;
            this.varLogDispEsts = varLogDispEsts;
        }
    }

    private static Deseq2LikeRegressionZModel fitInternal(final Dataset ds,
                                                          final Dataset fitDs,
                                                          final Map<String, TwoClassMarkerStats> markerScores,
                                                          final double[] sizeFactors,
                                                          final double[] conditionReal,
                                                          final boolean[] replacedRows,
                                                          final boolean[] replaceableSamples,
                                                          final boolean allowOutlierReplacement,
                                                          final FrozenDispersionContext frozen) {
        final int rowCount = fitDs.getNumRow();
        final int colCount = fitDs.getNumCol();
        final double maxDisp = Math.max(10.0d, colCount);
        final double[] meanNormCounts = computeMeanNormalizedCounts(fitDs, sizeFactors);
        final double meanInverseSizeFactor = meanInverseSizeFactor(sizeFactors);
        final boolean linearMu = canUseLinearMuForGeneWiseDispersion(conditionReal);

        final double[] dispersionRaw = new double[rowCount];
        final double[][] geneWiseMu = new double[rowCount][];
        Arrays.fill(dispersionRaw, Double.NaN);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final TwoClassMarkerStats markerScore = markerScores.get(ds.getRowName(rowIndex));
            if (markerScore == null || markerScore.omit || !Double.isFinite(meanNormCounts[rowIndex]) || meanNormCounts[rowIndex] <= 0.0d) {
                continue;
            }

            final double alphaStart = initialDispersionStart(
                    fitDs,
                    rowIndex,
                    sizeFactors,
                    conditionReal,
                    meanNormCounts[rowIndex],
                    meanInverseSizeFactor,
                    maxDisp);
            final double[] muHat;
            if (linearMu) {
                muHat = linearModelMuNormalizedForRow(fitDs, rowIndex, conditionReal, sizeFactors);
            } else {
                final CoefFit meanFit = fitMeanParametersForRow(fitDs, rowIndex, conditionReal, sizeFactors, alphaStart);
                muHat = meanFit.valid ? meanFit.muHat : null;
            }
            if (muHat == null) {
                continue;
            }
            geneWiseMu[rowIndex] = muHat;

            final DispersionFit dispersionFit = fitDispersionForRowMLE(
                    fitDs,
                    rowIndex,
                    conditionReal,
                    muHat,
                    alphaStart,
                    maxDisp);
            double dispGeneEst = Math.min(dispersionFit.alpha, maxDisp);
            if (dispersionFit.lastLogPosterior < dispersionFit.initialLogPosterior
                    + (Math.abs(dispersionFit.initialLogPosterior) / 1.0e6d)) {
                dispGeneEst = alphaStart;
            }

            final boolean dispGeneEstConv = dispersionFit.iterations < DISP_MAX_ITERS && dispersionFit.iterations != 1;
            if (!dispGeneEstConv && dispGeneEst > (MIN_DISP * 10.0d)) {
                dispGeneEst = fitDispersionGrid(
                        fitDs,
                        rowIndex,
                        conditionReal,
                        muHat,
                        Double.NaN,
                        Double.NaN,
                        false,
                        maxDisp);
            }
            dispersionRaw[rowIndex] = clamp(dispGeneEst, MIN_DISP, maxDisp);
        }

        // On the refit-after-outlier-replacement pass, DESeq2 reuses the ORIGINAL trend function and
        // the ORIGINAL dispPriorVar (refitWithoutOutliers keeps dispersionFunction() and
        // attr(dispersionFunction(object),"dispPriorVar") from the first fit). Mirror that here so the
        // Wald SEs on refitted rows match R exactly: the refitted MAP dispersion depends on the
        // original trend and prior variance, not on values re-estimated from the replaced counts.
        final FrozenDispersionContext frozenOut;
        final double[] fittedDispersionTrend;
        final double priorVar;
        final double varLogDispEsts;
        if (frozen != null) {
            fittedDispersionTrend = new double[rowCount];
            for (int i = 0; i < rowCount; i++) {
                if (!Double.isFinite(meanNormCounts[i]) || meanNormCounts[i] <= EPS) {
                    fittedDispersionTrend[i] = Double.NaN;
                    continue;
                }
                if (frozen.useParametricTrend) {
                    fittedDispersionTrend[i] = trendDispersion(meanNormCounts[i], frozen.trendA, frozen.trendB);
                } else if (frozen.localTrendByRow != null && i < frozen.localTrendByRow.length) {
                    fittedDispersionTrend[i] = frozen.localTrendByRow[i];
                } else {
                    fittedDispersionTrend[i] = trendDispersion(meanNormCounts[i], 0.1d, 1.0d);
                }
            }
            priorVar = frozen.priorVar;
            varLogDispEsts = frozen.varLogDispEsts;
            frozenOut = frozen;
        } else {
            final double[] trendParamsUsed = new double[2];
            final boolean[] usedParametricRef = new boolean[1];
            final double[][] localTrendHolder = new double[1][];
            fittedDispersionTrend = fitDispersionTrendValuesCapturing(meanNormCounts, dispersionRaw, trendParamsUsed, usedParametricRef, localTrendHolder);
            final double[] priorStats = estimateDispersionPriorStats(colCount, meanNormCounts, dispersionRaw, fittedDispersionTrend);
            priorVar = priorStats[0];
            varLogDispEsts = priorStats[1];
            frozenOut = new FrozenDispersionContext(
                    trendParamsUsed[0],
                    trendParamsUsed[1],
                    usedParametricRef[0],
                    localTrendHolder[0],
                    priorVar,
                    varLogDispEsts);
        }
        final double[] shrunkDispersion = new double[rowCount];
        Arrays.fill(shrunkDispersion, Double.NaN);
        final double outlierCutoff = 2.0d * Math.sqrt(varLogDispEsts);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            if (!Double.isFinite(meanNormCounts[rowIndex]) || meanNormCounts[rowIndex] <= 0.0d) {
                continue;
            }
            final double trend = clamp(fittedDispersionTrend[rowIndex], MIN_DISP, maxDisp);
            if (!Double.isFinite(dispersionRaw[rowIndex]) || dispersionRaw[rowIndex] <= 0.0d) {
                shrunkDispersion[rowIndex] = trend;
                continue;
            }
            if (geneWiseMu[rowIndex] == null) {
                shrunkDispersion[rowIndex] = trend;
                continue;
            }

            final double raw = clamp(dispersionRaw[rowIndex], MIN_DISP, maxDisp);
            final double dispMapStart = raw > (0.1d * trend) ? raw : trend;
                final DispersionFit dispMapFit = fitDispersionMAP(
                    fitDs,
                    rowIndex,
                    conditionReal,
                    geneWiseMu[rowIndex],
                    dispMapStart,
                    trend,
                    priorVar,
                    maxDisp);
                double dispMap = Math.min(dispMapFit.alpha, maxDisp);
                if (!(dispMapFit.iterations < DISP_MAX_ITERS)) {
                dispMap = fitDispersionGrid(
                    fitDs,
                    rowIndex,
                    conditionReal,
                    geneWiseMu[rowIndex],
                    Math.log(trend),
                    priorVar,
                    true,
                    maxDisp);
                }
                dispMap = clamp(dispMap, MIN_DISP, maxDisp);
            final double residual = Math.log(raw) - Math.log(trend);
            final double finalDisp = (Double.isFinite(residual) && residual > outlierCutoff) ? raw : dispMap;
            shrunkDispersion[rowIndex] = clamp(finalDisp, MIN_DISP, maxDisp);
        }

        final Deseq2LikeRegressionZModel provisionalModel =
                new Deseq2LikeRegressionZModel(ds,
                        fitDs,
                        sizeFactors,
                        meanNormCounts,
                        shrunkDispersion,
                        Double.NaN,
                        replacedRows,
                        replaceableSamples);

        if (allowOutlierReplacement && anyTrue(replaceableSamples)) {
            final ReplacementResult replacement = provisionalModel.replaceOutlierCounts(conditionReal, markerScores);
            if (replacement != null) {
                return fitInternal(ds,
                        replacement.dataset,
                        markerScores,
                        sizeFactors,
                        conditionReal,
                        replacement.replacedRows,
                        replaceableSamples,
                        false,
                        frozenOut);
            }
        }

        final ResultsContext resultsContext = provisionalModel.buildResultsContext(conditionReal);
        final double[] realPValues = new double[rowCount];
        Arrays.fill(realPValues, Double.NaN);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final TwoClassMarkerStats markerScore = markerScores.get(ds.getRowName(rowIndex));
            if (markerScore == null || markerScore.omit) {
                continue;
            }
            if (replacedRows[rowIndex] && meanNormCounts[rowIndex] == 0.0d) {
                realPValues[rowIndex] = 1.0d;
                continue;
            }
            if (!Double.isFinite(shrunkDispersion[rowIndex])) {
                continue;
            }
            final WaldFit mleWald = fitWaldForRowMleWide(
                    fitDs, rowIndex, conditionReal, sizeFactors, shrunkDispersion[rowIndex]);
            if (!mleWald.valid) {
                continue;
            }
            final CooksResult cooks = provisionalModel.computeCooksResult(rowIndex, mleWald, resultsContext);
            final boolean contrastAllZero = provisionalModel.contrastAllZeroForRow(rowIndex, conditionReal);
            // Match DESeq2 default (betaPrior=FALSE): p-value derived from MLE Wald z.
            realPValues[rowIndex] = cooks.outlier ? Double.NaN : (contrastAllZero ? 1.0d : twoSidedPValueFromZ(mleWald.z));
        }

        final double independentFilterThreshold = selectIndependentFilteringThreshold(meanNormCounts, realPValues);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final TwoClassMarkerStats markerScore = markerScores.get(ds.getRowName(rowIndex));
            if (markerScore == null) {
                continue;
            }
            markerScore.lowInformationChecked = true;
            markerScore.lowInformationThreshold = independentFilterThreshold;
            if (markerScore.omit) {
                continue;
            }
            markerScore.lowInformation = !Double.isFinite(meanNormCounts[rowIndex])
                    || (Double.isFinite(independentFilterThreshold) && meanNormCounts[rowIndex] < independentFilterThreshold);
        }

        return new Deseq2LikeRegressionZModel(ds,
                fitDs,
                sizeFactors,
                meanNormCounts,
                shrunkDispersion,
                independentFilterThreshold,
                replacedRows,
                replaceableSamples);
    }

    public ScoredDataset scoreForTemplate(final Template template,
                                          final SortMode sort,
                                          final Order order,
                                          final Map<String, TwoClassMarkerStats> markerScores) {
        final int rowCount = ds.getNumRow();
        final double[] condition = createConditionVector(template, ds.getNumCol());
        final ResultsContext resultsContext = buildResultsContext(condition);
        final DoubleElement[] elements = new DoubleElement[rowCount];
        int neutralizedRows = 0;

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final TwoClassMarkerStats markerScore = markerScores != null ? markerScores.get(ds.getRowName(rowIndex)) : null;
            if (markerScore != null) {
                markerScore.lowInformationChecked = true;
                markerScore.lowInformationThreshold = independentFilterThreshold;
                if (!markerScore.omit) {
                    markerScore.lowInformation = !Double.isFinite(meanNormCounts[rowIndex])
                            || (Double.isFinite(independentFilterThreshold) && meanNormCounts[rowIndex] < independentFilterThreshold);
                }
            }

            double score = 0.0d;
            if (Double.isFinite(shrunkDispersion[rowIndex])) {
                // Match DESeq2 default (betaPrior=FALSE): rank genes by the unshrunk MLE Wald z.
                final WaldFit mleWald = fitWaldForRowMleWide(
                        fitDs, rowIndex, condition, sizeFactors, shrunkDispersion[rowIndex]);
                if (mleWald.valid) {
                    final boolean contrastAllZero = contrastAllZeroForRow(rowIndex, condition);
                    final CooksResult cooks = computeCooksResult(rowIndex, mleWald, resultsContext);
                    score = (contrastAllZero || cooks.outlier) ? 0.0d : mleWald.z;
                    if (!Double.isFinite(score)) {
                        score = 0.0d;
                    }
                }
            }
            if (score == 0.0d) {
                neutralizedRows++;
            }
            elements[rowIndex] = new DoubleElement(rowIndex, score);
        }

        final DoubleElement.DoubleElementComparator baseComparator =
                new DoubleElement.DoubleElementComparator(sort, order.isAscending());
        Arrays.parallelSort(elements, (left, right) -> {
            final int cmp = baseComparator.compare(left, right);
            return cmp != 0 ? cmp : Integer.compare(left.fIndex, right.fIndex);
        });

        if (neutralizedRows > 0 && log.isDebugEnabled()) {
            log.debug("Wald_Z neutralized {} row(s) due to invalid fits or Cook's outliers", neutralizedRows);
        }

        return new ScoredDatasetImpl(new AddressedVector(Arrays.asList(elements)), ds);
    }

    List<MainStat> computeMainStatsForTemplate(final Template template,
                                               final Map<String, TwoClassMarkerStats> markerScores) {
        final int rowCount = ds.getNumRow();
        final double[] condition = createConditionVector(template, ds.getNumCol());
        final ResultsContext resultsContext = buildResultsContext(condition);
        final double[] pValues = new double[rowCount];
        final double[] log2FoldChanges = new double[rowCount];
        final double[] lfcSEs = new double[rowCount];
        final double[] stats = new double[rowCount];
        final double[] maxCooks = new double[rowCount];
        final boolean[] cooksOutlier = new boolean[rowCount];
        final boolean[] lowInformation = new boolean[rowCount];
        final boolean[] omit = new boolean[rowCount];
        Arrays.fill(pValues, Double.NaN);
        Arrays.fill(log2FoldChanges, Double.NaN);
        Arrays.fill(lfcSEs, Double.NaN);
        Arrays.fill(stats, Double.NaN);
        Arrays.fill(maxCooks, Double.NaN);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final TwoClassMarkerStats markerScore = markerScores != null ? markerScores.get(ds.getRowName(rowIndex)) : null;
            omit[rowIndex] = markerScore != null && markerScore.omit;
            lowInformation[rowIndex] = !Double.isFinite(meanNormCounts[rowIndex])
                    || (Double.isFinite(independentFilterThreshold) && meanNormCounts[rowIndex] < independentFilterThreshold);

            if (markerScore != null) {
                markerScore.lowInformationChecked = true;
                markerScore.lowInformationThreshold = independentFilterThreshold;
                if (!markerScore.omit) {
                    markerScore.lowInformation = lowInformation[rowIndex];
                }
            }

            if (omit[rowIndex] || !Double.isFinite(shrunkDispersion[rowIndex])) {
                if (replacedRows[rowIndex] && meanNormCounts[rowIndex] == 0.0d) {
                    log2FoldChanges[rowIndex] = 0.0d;
                    lfcSEs[rowIndex] = 0.0d;
                    stats[rowIndex] = 0.0d;
                    pValues[rowIndex] = 1.0d;
                }
                continue;
            }

            // DESeq2 default: DESeq(dds) uses betaPrior=FALSE; results() reports the *unshrunk* MLE Wald
            // LFC, SE and stat. See thelovelab/DESeq2 nbinomWaldTest: when betaPrior=FALSE the reported
            // betas/SEs come from fitNbinomGLMs with the wide default lambda (1e-6) — i.e. our mleWald.
            final WaldFit mleWald = fitWaldForRowMleWide(
                    fitDs, rowIndex, condition, sizeFactors, shrunkDispersion[rowIndex]);
            if (!mleWald.valid) {
                continue;
            }

            final boolean contrastAllZero = contrastAllZeroForRow(rowIndex, condition);
            log2FoldChanges[rowIndex] = contrastAllZero ? 0.0d : mleWald.beta1 / Math.log(2.0d);
            lfcSEs[rowIndex] = mleWald.se1 / Math.log(2.0d);
            stats[rowIndex] = contrastAllZero ? 0.0d : mleWald.z;

            final CooksResult cooks = computeCooksResult(rowIndex, mleWald, resultsContext);
            maxCooks[rowIndex] = cooks.maxCooks;
            cooksOutlier[rowIndex] = cooks.outlier;
            pValues[rowIndex] = cooks.outlier ? Double.NaN : (contrastAllZero ? 1.0d : twoSidedPValueFromZ(mleWald.z));
        }

        final double[] padj = applyIndependentFilteringAtThreshold(meanNormCounts, pValues, independentFilterThreshold);
        final List<MainStat> rows = new ArrayList<MainStat>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            rows.add(new MainStat(
                    ds.getRowName(rowIndex),
                    meanNormCounts[rowIndex],
                    log2FoldChanges[rowIndex],
                    lfcSEs[rowIndex],
                    stats[rowIndex],
                    pValues[rowIndex],
                    padj[rowIndex],
                    shrunkDispersion[rowIndex],
                    maxCooks[rowIndex],
                    cooksOutlier[rowIndex],
                    lowInformation[rowIndex],
                    omit[rowIndex]));
        }
        return rows;
    }

    static final class MainStat {
        final String feature;
        final double baseMean;
        final double log2FoldChange;
        final double lfcSE;
        final double stat;
        final double pvalue;
        final double padj;
        final double dispersion;
        final double maxCooks;
        final boolean cooksOutlier;
        final boolean lowInformation;
        final boolean omit;

        MainStat(final String feature,
                 final double baseMean,
                 final double log2FoldChange,
                 final double lfcSE,
                 final double stat,
                 final double pvalue,
                 final double padj,
                 final double dispersion,
                 final double maxCooks,
                 final boolean cooksOutlier,
                 final boolean lowInformation,
                 final boolean omit) {
            this.feature = feature;
            this.baseMean = baseMean;
            this.log2FoldChange = log2FoldChange;
            this.lfcSE = lfcSE;
            this.stat = stat;
            this.pvalue = pvalue;
            this.padj = padj;
            this.dispersion = dispersion;
            this.maxCooks = maxCooks;
            this.cooksOutlier = cooksOutlier;
            this.lowInformation = lowInformation;
            this.omit = omit;
        }
    }

    private static final class WaldFit {
        final double beta1;
        final double se1;
        final double z;
        final double[] muHat;
        final double[] hatDiagonals;
        final boolean valid;

        WaldFit(final double beta1,
                final double se1,
                final double z,
                final double[] muHat,
                final double[] hatDiagonals,
                final boolean valid) {
            this.beta1 = beta1;
            this.se1 = se1;
            this.z = z;
            this.muHat = muHat;
            this.hatDiagonals = hatDiagonals;
            this.valid = valid;
        }
    }

    private static final class CoefFit {
        final double beta0;
        final double beta1;
        final double[] muHat;
        final double[] fittedMuHat;
        final boolean valid;

        CoefFit(final double beta0,
                final double beta1,
                final double[] muHat,
                final double[] fittedMuHat,
                final boolean valid) {
            this.beta0 = beta0;
            this.beta1 = beta1;
            this.muHat = muHat;
            this.fittedMuHat = fittedMuHat;
            this.valid = valid;
        }
    }

    private static final class DispersionFit {
        final double alpha;
        final int iterations;
        final double initialLogPosterior;
        final double lastLogPosterior;

        DispersionFit(final double alpha,
                      final int iterations,
                      final double initialLogPosterior,
                      final double lastLogPosterior) {
            this.alpha = alpha;
            this.iterations = iterations;
            this.initialLogPosterior = initialLogPosterior;
            this.lastLogPosterior = lastLogPosterior;
        }
    }

    private static final class ResultsContext {
        final double[] robustDispersion;
        final boolean[] samplesForCooks;
        final double cooksCutoff;

        ResultsContext(final double[] robustDispersion,
                       final boolean[] samplesForCooks,
                       final double cooksCutoff) {
            this.robustDispersion = robustDispersion;
            this.samplesForCooks = samplesForCooks;
            this.cooksCutoff = cooksCutoff;
        }
    }

    private static final class CooksResult {
        final double maxCooks;
        final boolean outlier;

        CooksResult(final double maxCooks, final boolean outlier) {
            this.maxCooks = maxCooks;
            this.outlier = outlier;
        }
    }

    private static final class ReplacementResult {
        final Dataset dataset;
        final boolean[] replacedRows;

        ReplacementResult(final Dataset dataset, final boolean[] replacedRows) {
            this.dataset = dataset;
            this.replacedRows = replacedRows;
        }
    }

    private static final class GammaIdentityFit {
        final double a;
        final double b;
        final boolean converged;
        final boolean valid;

        GammaIdentityFit(final double a,
                         final double b,
                         final boolean converged,
                         final boolean valid) {
            this.a = a;
            this.b = b;
            this.converged = converged;
            this.valid = valid;
        }
    }

    private static final class LowessPointFit {
        final double y;
        final boolean ok;

        LowessPointFit(final double y, final boolean ok) {
            this.y = y;
            this.ok = ok;
        }
    }

    private ResultsContext buildResultsContext(final double[] condition) {
        final boolean[] samplesForCooks = samplesEligibleForCooks(condition);
        final double cooksCutoff = defaultCooksCutoff(condition.length, 2, samplesForCooks);
        final double[] robustDispersion = robustMethodOfMomentsDispersion(condition, samplesForCooks);
        return new ResultsContext(robustDispersion, samplesForCooks, cooksCutoff);
    }

    private CooksResult computeCooksResult(final int rowIndex,
                                           final WaldFit fit,
                                           final ResultsContext resultsContext) {
        if (!fit.valid || !Double.isFinite(resultsContext.cooksCutoff)) {
            return new CooksResult(Double.NaN, false);
        }

        if (anyRowsReplaced && allTrue(replaceableSamples)) {
            return new CooksResult(Double.NaN, false);
        }

        final double[] cooksValues = computeCooksValues(rowIndex, fit, resultsContext);
        final int colCount = ds.getNumCol();

        double maxEligibleCooks = Double.NEGATIVE_INFINITY;
        int overallMaxIndex = -1;
        double overallMaxCooks = Double.NEGATIVE_INFINITY;
        boolean anyEligible = false;

        for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
            final double cooks = cooksValues[columnIndex];
            if (!Double.isFinite(cooks)) {
                continue;
            }

            if (cooks > overallMaxCooks) {
                overallMaxCooks = cooks;
                overallMaxIndex = columnIndex;
            }
            if (resultsContext.samplesForCooks[columnIndex]) {
                anyEligible = true;
                final double cooksForMax = (anyRowsReplaced && replaceableSamples[columnIndex]) ? 0.0d : cooks;
                if (cooksForMax > maxEligibleCooks) {
                    maxEligibleCooks = cooksForMax;
                }
            }
        }

        if (!anyEligible || !Double.isFinite(maxEligibleCooks)) {
            return new CooksResult(Double.NaN, false);
        }

        boolean outlier = maxEligibleCooks > resultsContext.cooksCutoff;
        if (outlier && overallMaxIndex >= 0 && hasThreeCountsLargerThanOutlier(rowIndex, overallMaxIndex)) {
            outlier = false;
        }
        return new CooksResult(maxEligibleCooks, outlier);
    }

    private double[] computeCooksValues(final int rowIndex,
                                        final WaldFit fit,
                                        final ResultsContext resultsContext) {
        final double[] cooksValues = new double[fitDs.getNumCol()];
        Arrays.fill(cooksValues, Double.NaN);
        if (!fit.valid) {
            return cooksValues;
        }

        final double alpha = Math.max(ROBUST_COOKS_MIN_DISP, resultsContext.robustDispersion[rowIndex]);
        // DESeq2 calculateCooksDistance uses counts aligned with fitted mu (same assay as Wald fit).
        final Vector row = fitDs.getRow(rowIndex);
        final int p = 2;

        for (int columnIndex = 0; columnIndex < fitDs.getNumCol(); columnIndex++) {
            final double y = row.getElement(columnIndex);
            final double mu = fit.muHat[columnIndex];
            final double hat = fit.hatDiagonals[columnIndex];
            if (!Double.isFinite(y) || !Double.isFinite(mu) || !Double.isFinite(hat) || hat >= 1.0d) {
                continue;
            }

            final double variance = mu + alpha * mu * mu;
            if (!Double.isFinite(variance) || variance <= 0.0d) {
                continue;
            }

            final double pearsonSq = ((y - mu) * (y - mu)) / variance;
            final double cooks = (pearsonSq / p) * hat / ((1.0d - hat) * (1.0d - hat));
            if (Double.isFinite(cooks)) {
                cooksValues[columnIndex] = cooks;
            }
        }
        return cooksValues;
    }

    private boolean hasThreeCountsLargerThanOutlier(final int rowIndex, final int outlierSampleIndex) {
        final Vector row = ds.getRow(rowIndex);
        final double outlierCount = row.getElement(outlierSampleIndex);
        if (!Double.isFinite(outlierCount)) {
            return false;
        }

        int largerCount = 0;
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double count = row.getElement(columnIndex);
            if (Double.isFinite(count) && count > outlierCount) {
                largerCount++;
            }
        }
        return largerCount >= 3;
    }

    private ReplacementResult replaceOutlierCounts(final double[] condition,
                                                   final Map<String, TwoClassMarkerStats> markerScores) {
        final ResultsContext resultsContext = buildResultsContext(condition);
        if (!Double.isFinite(resultsContext.cooksCutoff)) {
            return null;
        }

        final int rowCount = fitDs.getNumRow();
        final int colCount = fitDs.getNumCol();
        final Matrix matrix = new Matrix(rowCount, colCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
                matrix.setElement(rowIndex, columnIndex, fitDs.getElement(rowIndex, columnIndex));
            }
        }

        final boolean[] replaced = new boolean[rowCount];
        boolean changed = false;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final TwoClassMarkerStats markerScore = markerScores.get(ds.getRowName(rowIndex));
            if (markerScore == null || markerScore.omit || !Double.isFinite(shrunkDispersion[rowIndex])) {
                continue;
            }

            final WaldFit fit = fitWaldForRowMleWide(fitDs, rowIndex, condition, sizeFactors, shrunkDispersion[rowIndex]);
            if (!fit.valid) {
                continue;
            }

            final double[] cooksValues = computeCooksValues(rowIndex, fit, resultsContext);
            final double replacementValue = trimmedReplacementValue(rowIndex);
            boolean rowChanged = false;
            for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
                if (!replaceableSamples[columnIndex]) {
                    continue;
                }
                final boolean flagged = Double.isFinite(cooksValues[columnIndex])
                        && cooksValues[columnIndex] > resultsContext.cooksCutoff;
                if (!flagged) {
                    continue;
                }
                matrix.setElement(rowIndex,
                        columnIndex,
                        (float) Math.floor(Math.max(0.0d, replacementValue * sizeFactors[columnIndex])));
                rowChanged = true;
            }

            replaced[rowIndex] = rowChanged;
            changed = changed || rowChanged;
        }

        if (!changed) {
            return null;
        }

        return new ReplacementResult(createDatasetLike(fitDs, matrix), replaced);
    }

    private double trimmedReplacementValue(final int rowIndex) {
        final double[] normalized = new double[fitDs.getNumCol()];
        int count = 0;
        for (int columnIndex = 0; columnIndex < fitDs.getNumCol(); columnIndex++) {
            final double value = fitDs.getElement(rowIndex, columnIndex);
            if (!Double.isFinite(value)) {
                continue;
            }
            normalized[count++] = value / sizeFactors[columnIndex];
        }

        final double trimmed = trimmedMean(normalized, count, OUTLIER_REPLACEMENT_TRIM);
        return Double.isFinite(trimmed) ? Math.max(0.0d, trimmed) : 0.0d;
    }

    private static Dataset createDatasetLike(final Dataset source, final Matrix matrix) {
        return new DefaultDataset(source.getName(), matrix, source.getRowNames(), source.getColumnNames(), source.getAnnot());
    }

    private double[] robustMethodOfMomentsDispersion(final double[] condition, final boolean[] samplesForCooks) {
        final int rowCount = ds.getNumRow();
        final double[] alpha = new double[rowCount];
        final boolean anyThreeOrMore = anyTrue(samplesForCooks);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final double mean = meanNormCounts[rowIndex];
            if (!Double.isFinite(mean) || mean <= EPS) {
                alpha[rowIndex] = ROBUST_COOKS_MIN_DISP;
                continue;
            }

            final double variance = anyThreeOrMore
                    ? trimmedCellVarianceForRow(rowIndex, condition, samplesForCooks)
                    : trimmedVarianceForRow(rowIndex);
            if (!Double.isFinite(variance)) {
                alpha[rowIndex] = ROBUST_COOKS_MIN_DISP;
                continue;
            }

            final double disp = (variance - mean) / (mean * mean);
            alpha[rowIndex] = Math.max(ROBUST_COOKS_MIN_DISP, Double.isFinite(disp) ? disp : ROBUST_COOKS_MIN_DISP);
        }

        return alpha;
    }

    private double trimmedCellVarianceForRow(final int rowIndex,
                                             final double[] condition,
                                             final boolean[] samplesForCooks) {
        final Vector row = fitDs.getRow(rowIndex);
        double maxVariance = Double.NaN;

        for (int group = 0; group <= 1; group++) {
            final double[] values = new double[ds.getNumCol()];
            int count = 0;
            for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
                final double groupValue = condition[columnIndex];
                if (!samplesForCooks[columnIndex] || !Double.isFinite(groupValue)) {
                    continue;
                }
                final boolean inGroup = group == 0
                        ? Math.abs(groupValue) <= EPS
                        : Math.abs(groupValue - 1.0d) <= EPS;
                if (!inGroup) {
                    continue;
                }
                final double value = row.getElement(columnIndex);
                if (!Double.isFinite(value)) {
                    continue;
                }
                values[count++] = value / sizeFactors[columnIndex];
            }

            if (count == 0) {
                continue;
            }

            final double trim = trimRatioForSampleCount(count);
            final double mean = trimmedMean(values, count, trim);
            if (!Double.isFinite(mean)) {
                continue;
            }

            final double[] sqError = new double[count];
            for (int i = 0; i < count; i++) {
                final double delta = values[i] - mean;
                sqError[i] = delta * delta;
            }
            final double variance = scaleForTrimmedVariance(count) * trimmedMean(sqError, count, trim);
            if (!Double.isFinite(maxVariance) || variance > maxVariance) {
                maxVariance = variance;
            }
        }

        return Double.isFinite(maxVariance) ? maxVariance : trimmedVarianceForRow(rowIndex);
    }

    private double trimmedVarianceForRow(final int rowIndex) {
        final Vector row = fitDs.getRow(rowIndex);
        final double[] values = new double[ds.getNumCol()];
        int count = 0;
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double value = row.getElement(columnIndex);
            if (!Double.isFinite(value)) {
                continue;
            }
            values[count++] = value / sizeFactors[columnIndex];
        }

        if (count == 0) {
            return Double.NaN;
        }

        final double mean = trimmedMean(values, count, 1.0d / 8.0d);
        if (!Double.isFinite(mean)) {
            return Double.NaN;
        }

        final double[] sqError = new double[count];
        for (int i = 0; i < count; i++) {
            final double delta = values[i] - mean;
            sqError[i] = delta * delta;
        }
        return 1.51d * trimmedMean(sqError, count, 1.0d / 8.0d);
    }

    private static boolean[] samplesEligibleForCooks(final double[] condition) {
        int zeros = 0;
        int ones = 0;
        for (double value : condition) {
            if (!Double.isFinite(value)) {
                continue;
            }
            if (Math.abs(value) <= EPS) {
                zeros++;
            } else if (Math.abs(value - 1.0d) <= EPS) {
                ones++;
            }
        }

        final boolean[] eligible = new boolean[condition.length];
        for (int i = 0; i < condition.length; i++) {
            final double value = condition[i];
            if (!Double.isFinite(value)) {
                eligible[i] = false;
            } else if (Math.abs(value) <= EPS) {
                eligible[i] = zeros >= 3;
            } else if (Math.abs(value - 1.0d) <= EPS) {
                eligible[i] = ones >= 3;
            } else {
                eligible[i] = false;
            }
        }
        return eligible;
    }

    private static boolean[] samplesEligibleForReplacement(final double[] condition, final int minReplicates) {
        int zeros = 0;
        int ones = 0;
        for (double value : condition) {
            if (!Double.isFinite(value)) {
                continue;
            }
            if (Math.abs(value) <= EPS) {
                zeros++;
            } else if (Math.abs(value - 1.0d) <= EPS) {
                ones++;
            }
        }

        final boolean[] eligible = new boolean[condition.length];
        for (int i = 0; i < condition.length; i++) {
            final double value = condition[i];
            if (!Double.isFinite(value)) {
                eligible[i] = false;
            } else if (Math.abs(value) <= EPS) {
                eligible[i] = zeros >= minReplicates;
            } else if (Math.abs(value - 1.0d) <= EPS) {
                eligible[i] = ones >= minReplicates;
            } else {
                eligible[i] = false;
            }
        }
        return eligible;
    }

    private static boolean anyTrue(final boolean[] values) {
        for (boolean value : values) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    private static boolean allTrue(final boolean[] values) {
        for (boolean value : values) {
            if (!value) {
                return false;
            }
        }
        return values.length > 0;
    }

    private static double defaultCooksCutoff(final int sampleCount,
                                             final int coefficientCount,
                                             final boolean[] samplesForCooks) {
        if (sampleCount <= coefficientCount || !anyTrue(samplesForCooks)) {
            return Double.NaN;
        }
        return new FDistribution(coefficientCount, sampleCount - coefficientCount)
                .inverseCumulativeProbability(DEFAULT_COOKS_QUANTILE);
    }

    private static void validateRawIntegerCounts(final Dataset ds) {
        for (int rowIndex = 0; rowIndex < ds.getNumRow(); rowIndex++) {
            final Vector row = ds.getRow(rowIndex);
            for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
                final double value = row.getElement(columnIndex);
                if (!Double.isFinite(value)) {
                    continue;
                }
                if (value < 0.0d) {
                    throw new IllegalArgumentException("Wald_Z requires non-negative raw count data");
                }
                final double nearestInteger = Math.rint(value);
                final float nearestAsFloat = (float) nearestInteger;
                final double tolerance = Math.max(1.0e-4d, 4.0d * (double) Math.ulp(nearestAsFloat == 0.0f ? 1.0f : nearestAsFloat));
                if (Math.abs(value - nearestInteger) > tolerance) {
                    throw new IllegalArgumentException("Wald_Z requires raw integer count data");
                }
            }
        }
    }

    private static double[] computeMeanNormalizedCounts(final Dataset ds,
                                                        final double[] sizeFactors) {
        final double[] means = new double[ds.getNumRow()];
        Arrays.fill(means, Double.NaN);
        for (int rowIndex = 0; rowIndex < ds.getNumRow(); rowIndex++) {
            final Vector row = ds.getRow(rowIndex);
            double sum = 0.0d;
            int count = 0;
            for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
                final double value = row.getElement(columnIndex);
                if (!Double.isFinite(value)) {
                    continue;
                }
                sum += value / sizeFactors[columnIndex];
                count++;
            }
            if (count > 0) {
                means[rowIndex] = sum / count;
            }
        }
        return means;
    }

    /**
     * Median-of-ratios size factors: DESeq2 {@code estimateSizeFactorsForMatrix}
     * with {@code type="ratio"} when reference genes exist (all-nonzero rows),
     * else {@code type="poscounts"} (Anders & Huber 2010; DESeq2 default fallback).
     */
    private static double[] computeSizeFactors(final Dataset ds) {
        final double[] ratio = computeSizeFactorsRatio(ds);
        if (ratio != null) {
            return ratio;
        }
        return computeSizeFactorsPoscounts(ds);
    }

    /** DESeq2 ratio type: genes with a zero in any sample are excluded from the reference set. */
    private static double[] computeSizeFactorsRatio(final Dataset ds) {
        final int rowCount = ds.getNumRow();
        final int colCount = ds.getNumCol();
        final double[] logGeometricMeans = new double[rowCount];
        final boolean[] usableRows = new boolean[rowCount];
        int usableRowCount = 0;

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            double sumLog = 0.0d;
            boolean usable = true;
            final Vector row = ds.getRow(rowIndex);
            for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
                final double value = row.getElement(columnIndex);
                if (!Double.isFinite(value) || value <= 0.0d) {
                    usable = false;
                    break;
                }
                sumLog += Math.log(value);
            }
            if (usable) {
                logGeometricMeans[rowIndex] = sumLog / colCount;
                usableRows[rowIndex] = true;
                usableRowCount++;
            } else {
                logGeometricMeans[rowIndex] = Double.NaN;
            }
        }

        if (usableRowCount == 0) {
            return null;
        }

        final double[] sizeFactors = new double[colCount];
        for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
            final double[] logRatios = new double[usableRowCount];
            int count = 0;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                if (!usableRows[rowIndex]) {
                    continue;
                }
                final double value = ds.getRow(rowIndex).getElement(columnIndex);
                logRatios[count++] = Math.log(value) - logGeometricMeans[rowIndex];
            }
            Arrays.sort(logRatios, 0, count);
            sizeFactors[columnIndex] = Math.exp(medianOfSorted(logRatios, count));
        }
        return sizeFactors;
    }

    /**
     * DESeq2 {@code estimateSizeFactorsForMatrix(..., type = "poscounts")}:
     * reference log-means use {@code rowMeans(replace_non_finite(log(counts), 0))},
     * all-zero rows get {@code -Inf} and are excluded from each column's median.
     */
    private static double[] computeSizeFactorsPoscounts(final Dataset ds) {
        final int rowCount = ds.getNumRow();
        final int colCount = ds.getNumCol();
        final double[] logGeoMeans = new double[rowCount];

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final Vector row = ds.getRow(rowIndex);
            double sumLc = 0.0d;
            boolean allZero = true;
            for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
                final double value = row.getElement(columnIndex);
                if (!Double.isFinite(value) || value < 0.0d) {
                    throw new IllegalArgumentException("Wald_Z requires non-negative raw count data");
                }
                if (value > 0.0d) {
                    allZero = false;
                    final double logv = Math.log(value);
                    sumLc += Double.isFinite(logv) ? logv : 0.0d;
                }
            }
            logGeoMeans[rowIndex] = allZero ? Double.NEGATIVE_INFINITY : sumLc / colCount;
        }

        boolean anyFiniteRef = false;
        for (final double g : logGeoMeans) {
            if (Double.isFinite(g)) {
                anyFiniteRef = true;
                break;
            }
        }
        if (!anyFiniteRef) {
            throw new IllegalArgumentException(
                    "every gene contains at least one zero, cannot compute size factors (poscounts)");
        }

        final double[] sizeFactors = new double[colCount];
        for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
            final double[] logRatios = new double[rowCount];
            int count = 0;
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                if (!Double.isFinite(logGeoMeans[rowIndex])) {
                    continue;
                }
                final double cnt = ds.getRow(rowIndex).getElement(columnIndex);
                if (!Double.isFinite(cnt) || cnt <= 0.0d) {
                    continue;
                }
                logRatios[count++] = Math.log(cnt) - logGeoMeans[rowIndex];
            }
            if (count == 0) {
                throw new IllegalArgumentException("no positive counts in a sample for poscounts size factors");
            }
            Arrays.sort(logRatios, 0, count);
            sizeFactors[columnIndex] = Math.exp(medianOfSorted(logRatios, count));
        }
        return sizeFactors;
    }

    private static double initialDispersionStart(final Dataset ds,
                                                 final int rowIndex,
                                                 final double[] sizeFactors,
                                                 final double[] condition,
                                                 final double meanNormCount,
                                                 final double meanInverseSizeFactor,
                                                 final double maxDisp) {
        if (!Double.isFinite(meanNormCount) || meanNormCount <= 0.0d) {
            return MIN_DISP;
        }

        final double[] normalized = normalizedCountsForRow(ds, rowIndex, sizeFactors);
        final double roughDisp = roughDispersionEstimate(normalized, condition);
        final double baseVariance = sampleVariance(normalized);
        final double momentsDisp = (baseVariance - (meanInverseSizeFactor * meanNormCount)) / (meanNormCount * meanNormCount);

        double alpha = Double.NaN;
        if (Double.isFinite(roughDisp)) {
            alpha = roughDisp;
        }
        if (Double.isFinite(momentsDisp)) {
            alpha = Double.isFinite(alpha) ? Math.min(alpha, momentsDisp) : momentsDisp;
        }
        return clamp(Double.isFinite(alpha) ? alpha : MIN_DISP, MIN_DISP, maxDisp);
    }

    private static double meanInverseSizeFactor(final double[] sizeFactors) {
        double sum = 0.0d;
        int count = 0;
        for (double sizeFactor : sizeFactors) {
            if (Double.isFinite(sizeFactor) && sizeFactor > 0.0d) {
                sum += 1.0d / sizeFactor;
                count++;
            }
        }
        return count == 0 ? 1.0d : sum / count;
    }

    private static boolean canUseLinearMuForGeneWiseDispersion(final double[] condition) {
        boolean hasReference = false;
        boolean hasInterest = false;
        for (double value : condition) {
            if (!Double.isFinite(value)) {
                continue;
            }
            if (Math.abs(value) <= EPS) {
                hasReference = true;
            } else if (Math.abs(value - 1.0d) <= EPS) {
                hasInterest = true;
            } else {
                return false;
            }
        }
        return hasReference && hasInterest;
    }

    private static double[] linearModelMuNormalizedForRow(final Dataset ds,
                                                          final int rowIndex,
                                                          final double[] condition,
                                                          final double[] sizeFactors) {
        final double[] normalizedFit = fittedNormalizedCountsForRow(normalizedCountsForRow(ds, rowIndex, sizeFactors), condition);
        if (normalizedFit == null) {
            return null;
        }

        final double[] muHat = new double[normalizedFit.length];
        for (int columnIndex = 0; columnIndex < normalizedFit.length; columnIndex++) {
            final double normalizedValue = normalizedFit[columnIndex];
            if (!Double.isFinite(normalizedValue)) {
                muHat[columnIndex] = Double.NaN;
                continue;
            }
            muHat[columnIndex] = Math.max(MIN_MU, normalizedValue * sizeFactors[columnIndex]);
        }
        return muHat;
    }

    private static double roughDispersionEstimate(final double[] normalizedCounts,
                                                  final double[] condition) {
        final double[] fittedNormalized = fittedNormalizedCountsForRow(normalizedCounts, condition);
        if (fittedNormalized == null) {
            return Double.NaN;
        }

        double sum = 0.0d;
        int used = 0;
        for (int i = 0; i < normalizedCounts.length; i++) {
            final double observed = normalizedCounts[i];
            final double fitted = fittedNormalized[i];
            if (!Double.isFinite(observed) || !Double.isFinite(fitted)) {
                continue;
            }

            final double mu = Math.max(1.0d, fitted);
            sum += (((observed - mu) * (observed - mu)) - mu) / (mu * mu);
            used++;
        }

        if (used <= 2) {
            return Double.NaN;
        }
        return Math.max(sum / (used - 2.0d), 0.0d);
    }

    private static double[] normalizedCountsForRow(final Dataset ds,
                                                   final int rowIndex,
                                                   final double[] sizeFactors) {
        final double[] normalized = new double[ds.getNumCol()];
        final Vector row = ds.getRow(rowIndex);
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double value = row.getElement(columnIndex);
            normalized[columnIndex] = Double.isFinite(value) ? value / sizeFactors[columnIndex] : Double.NaN;
        }
        return normalized;
    }

    private static double[] fittedNormalizedCountsForRow(final double[] normalizedCounts,
                                                         final double[] condition) {
        double sumReference = 0.0d;
        double sumInterest = 0.0d;
        int countReference = 0;
        int countInterest = 0;

        for (int i = 0; i < normalizedCounts.length; i++) {
            final double observed = normalizedCounts[i];
            final double group = condition[i];
            if (!Double.isFinite(observed) || !Double.isFinite(group)) {
                continue;
            }
            if (Math.abs(group) <= EPS) {
                sumReference += observed;
                countReference++;
            } else {
                sumInterest += observed;
                countInterest++;
            }
        }

        if (countReference == 0 || countInterest == 0) {
            return null;
        }

        final double meanReference = sumReference / countReference;
        final double meanInterest = sumInterest / countInterest;
        final double[] fitted = new double[condition.length];
        for (int i = 0; i < condition.length; i++) {
            final double group = condition[i];
            if (!Double.isFinite(group)) {
                fitted[i] = Double.NaN;
            } else if (Math.abs(group) <= EPS) {
                fitted[i] = meanReference;
            } else {
                fitted[i] = meanInterest;
            }
        }
        return fitted;
    }

    private static double[] estimateDispersionPriorStats(final int sampleCount,
                                                         final double[] meanNormCounts,
                                                         final double[] dispersionRaw,
                                                         final double[] fittedDispersionTrend) {
        final double[] residuals = new double[dispersionRaw.length];
        int count = 0;
        for (int rowIndex = 0; rowIndex < dispersionRaw.length; rowIndex++) {
            if (!Double.isFinite(dispersionRaw[rowIndex]) || dispersionRaw[rowIndex] < MIN_DISP * 100.0d || !Double.isFinite(meanNormCounts[rowIndex])) {
                continue;
            }
            final double trend = fittedDispersionTrend[rowIndex];
            if (!Double.isFinite(trend) || trend <= 0.0d) {
                continue;
            }
            residuals[count++] = Math.log(dispersionRaw[rowIndex]) - Math.log(trend);
        }

        final int residualDf = sampleCount - 2;
        if (count == 0) {
            final double priorVar = residualDf > 0 ? DISP_PRIOR_VAR_MIN : 0.0d;
            return new double[] { priorVar, 0.0d };
        }

        final double madSd = robustMadSd(residuals, count);
        final double varLogDispEsts = Double.isFinite(madSd) ? madSd * madSd : 0.0d;

        if (residualDf > 0 && residualDf <= 3) {
            final int histogramBins = (int) ((DISP_PRIOR_KL_HISTOGRAM_MAX - DISP_PRIOR_KL_HISTOGRAM_MIN)
                    / DISP_PRIOR_KL_HISTOGRAM_STEP);
            final int[] observedHistogram = new int[histogramBins];
            int observedCount = 0;
            for (int i = 0; i < count; i++) {
                final double residual = residuals[i];
                if (residual > DISP_PRIOR_KL_HISTOGRAM_MIN && residual < DISP_PRIOR_KL_HISTOGRAM_MAX) {
                    final int bin = (int) ((residual - DISP_PRIOR_KL_HISTOGRAM_MIN) / DISP_PRIOR_KL_HISTOGRAM_STEP);
                    if (bin >= 0 && bin < histogramBins) {
                        observedHistogram[bin]++;
                        observedCount++;
                    }
                }
            }

            if (observedCount > 0) {
                final double[] observedDensity = new double[histogramBins];
                final double observedScale = 1.0d / (observedCount * DISP_PRIOR_KL_HISTOGRAM_STEP);
                double smallestObservedDensity = Double.POSITIVE_INFINITY;
                for (int bin = 0; bin < histogramBins; bin++) {
                    observedDensity[bin] = observedHistogram[bin] * observedScale;
                    if (observedDensity[bin] > 0.0d && observedDensity[bin] < smallestObservedDensity) {
                        smallestObservedDensity = observedDensity[bin];
                    }
                }

                final double[] obsVarGrid = new double[DISP_PRIOR_KL_GRID_SIZE];
                final double[] klDivergences = new double[DISP_PRIOR_KL_GRID_SIZE];
                final MersenneTwister random = new MersenneTwister((int) DISP_PRIOR_KL_SEED);
                final ChiSquaredDistribution chiSquared = new ChiSquaredDistribution(random, residualDf);
                final double logResidualDf = Math.log(residualDf);

                for (int gridIndex = 0; gridIndex < DISP_PRIOR_KL_GRID_SIZE; gridIndex++) {
                    final double variance = (DISP_PRIOR_KL_MAX * gridIndex) / (DISP_PRIOR_KL_GRID_SIZE - 1.0d);
                    obsVarGrid[gridIndex] = variance;

                    final double simulatedSd = Math.sqrt(variance);
                    final int[] simulatedHistogram = new int[histogramBins];
                    int simulatedCount = 0;
                    for (int sampleIndex = 0; sampleIndex < DISP_PRIOR_KL_SAMPLE_SIZE; sampleIndex++) {
                        final double simulated = Math.log(chiSquared.sample())
                                + (random.nextGaussian() * simulatedSd)
                                - logResidualDf;
                        if (simulated > DISP_PRIOR_KL_HISTOGRAM_MIN && simulated < DISP_PRIOR_KL_HISTOGRAM_MAX) {
                            final int bin = (int) ((simulated - DISP_PRIOR_KL_HISTOGRAM_MIN)
                                    / DISP_PRIOR_KL_HISTOGRAM_STEP);
                            if (bin >= 0 && bin < histogramBins) {
                                simulatedHistogram[bin]++;
                                simulatedCount++;
                            }
                        }
                    }

                    final double[] simulatedDensity = new double[histogramBins];
                    final double simulatedScale = simulatedCount > 0
                            ? 1.0d / (simulatedCount * DISP_PRIOR_KL_HISTOGRAM_STEP)
                            : 0.0d;
                    double smallestDensity = smallestObservedDensity;
                    for (int bin = 0; bin < histogramBins; bin++) {
                        simulatedDensity[bin] = simulatedHistogram[bin] * simulatedScale;
                        if (simulatedDensity[bin] > 0.0d && simulatedDensity[bin] < smallestDensity) {
                            smallestDensity = simulatedDensity[bin];
                        }
                    }
                    if (!Double.isFinite(smallestDensity)) {
                        smallestDensity = Double.MIN_NORMAL;
                    }

                    double kl = 0.0d;
                    for (int bin = 0; bin < histogramBins; bin++) {
                        if (observedDensity[bin] <= 0.0d) {
                            continue;
                        }
                        kl += observedDensity[bin]
                                * (Math.log(observedDensity[bin] + smallestDensity)
                                - Math.log(simulatedDensity[bin] + smallestDensity));
                    }
                    klDivergences[gridIndex] = kl;
                }

                double argminKl = obsVarGrid[0];
                double minKl = klDivergences[0];
                for (int gridIndex = 1; gridIndex < klDivergences.length; gridIndex++) {
                    if (klDivergences[gridIndex] < minKl) {
                        minKl = klDivergences[gridIndex];
                        argminKl = obsVarGrid[gridIndex];
                    }
                }

                try {
                    final UnivariateFunction klFit = new LoessInterpolator(FILTER_SMOOTHING_SPAN, 0)
                            .interpolate(obsVarGrid, klDivergences);
                    double minFit = Double.POSITIVE_INFINITY;
                    for (int gridIndex = 0; gridIndex < DISP_PRIOR_KL_FINE_GRID_SIZE; gridIndex++) {
                        final double variance = (DISP_PRIOR_KL_MAX * gridIndex)
                                / (DISP_PRIOR_KL_FINE_GRID_SIZE - 1.0d);
                        final double fitted = klFit.value(variance);
                        if (Double.isFinite(fitted) && fitted < minFit) {
                            minFit = fitted;
                            argminKl = variance;
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Fall back to the raw KL grid if local smoothing fails.
                }

                return new double[] { Math.max(argminKl, DISP_PRIOR_VAR_MIN), varLogDispEsts };
            }
        }

        if (residualDf > 0) {
            final double samplingVar = Gamma.trigamma(residualDf / 2.0d);
            return new double[] { Math.max(DISP_PRIOR_VAR_MIN, varLogDispEsts - samplingVar), varLogDispEsts };
        }
        return new double[] { varLogDispEsts, varLogDispEsts };
    }

    private static DispersionFit fitDispersionForRowMLE(final Dataset ds,
                                                        final int rowIndex,
                                                        final double[] condition,
                                                        final double[] muHat,
                                                        final double alphaStart,
                                                        final double maxDisp) {
        return fitDispersionLineSearch(ds, rowIndex, condition, muHat, alphaStart, Double.NaN, Double.NaN, false, maxDisp);
    }

    private static DispersionFit fitDispersionMAP(final Dataset ds,
                                                  final int rowIndex,
                                                  final double[] condition,
                                                  final double[] muHat,
                                                  final double alphaStart,
                                                  final double trend,
                                                  final double priorVar,
                                                  final double maxDisp) {
        return fitDispersionLineSearch(ds, rowIndex, condition, muHat, alphaStart, Math.log(trend), priorVar, true, maxDisp);
    }

    private static DispersionFit fitDispersionLineSearch(final Dataset ds,
                                                         final int rowIndex,
                                                         final double[] condition,
                                                         final double[] muHat,
                                                         final double alphaStart,
                                                         final double logAlphaPriorMean,
                                                         final double logAlphaPriorSigmaSq,
                                                         final boolean usePrior,
                                                         final double maxDisp) {
        final double minLogAlpha = Math.log(MIN_DISP_LINE_SEARCH);
        double a = clamp(Math.log(clamp(alphaStart, MIN_DISP, maxDisp)), minLogAlpha, MAX_LOG_ALPHA);
        double lp = logPosteriorForLogAlpha(ds, rowIndex, condition, muHat, a, logAlphaPriorMean, logAlphaPriorSigmaSq, usePrior);
        double dlp = dLogPosteriorForLogAlpha(ds, rowIndex, condition, muHat, a, logAlphaPriorMean, logAlphaPriorSigmaSq, usePrior);
        final double initialLp = lp;
        double lastLp = lp;

        double kappa = DISP_KAPPA0;
        int acceptedSteps = 0;
        int iterations = 0;
        for (int iter = 0; iter < DISP_MAX_ITERS; iter++) {
            iterations++;

            final double aPropose = a + (kappa * dlp);
            if (aPropose < MIN_LOG_ALPHA_PROPOSE) {
                kappa = (MIN_LOG_ALPHA_PROPOSE - a) / dlp;
            }
            if (aPropose > MAX_LOG_ALPHA) {
                kappa = (MAX_LOG_ALPHA - a) / dlp;
            }

            final double thetaKappa = -logPosteriorForLogAlpha(
                    ds,
                    rowIndex,
                    condition,
                    muHat,
                    a + (kappa * dlp),
                    logAlphaPriorMean,
                    logAlphaPriorSigmaSq,
                    usePrior);
            final double thetaHatKappa = -lp - (kappa * DISP_ARMIJO_EPSILON * dlp * dlp);

            if (Double.isFinite(thetaKappa) && thetaKappa <= thetaHatKappa) {
                acceptedSteps++;
                a += kappa * dlp;
                final double lpNew = logPosteriorForLogAlpha(ds, rowIndex, condition, muHat, a, logAlphaPriorMean, logAlphaPriorSigmaSq, usePrior);
                lastLp = lpNew;
                final double change = lpNew - lp;
                if (change < DISP_TOL) {
                    lp = lpNew;
                    break;
                }
                if (a < minLogAlpha) {
                    lp = lpNew;
                    break;
                }
                lp = lpNew;
                dlp = dLogPosteriorForLogAlpha(ds, rowIndex, condition, muHat, a, logAlphaPriorMean, logAlphaPriorSigmaSq, usePrior);
                kappa = Math.min(kappa * 1.1d, DISP_KAPPA0);
                if ((acceptedSteps % 5) == 0) {
                    kappa *= 0.5d;
                }
            } else {
                kappa *= 0.5d;
            }
        }

        return new DispersionFit(Math.min(Math.exp(a), maxDisp), iterations, initialLp, lastLp);
    }

    private static double fitDispersionGrid(final Dataset ds,
                                            final int rowIndex,
                                            final double[] condition,
                                            final double[] muHat,
                                            final double logAlphaPriorMean,
                                            final double logAlphaPriorSigmaSq,
                                            final boolean usePrior,
                                            final double maxDisp) {
        final double minLogAlpha = Math.log(MIN_DISP_LINE_SEARCH);
        final double maxLogAlpha = Math.log(maxDisp);
        final double delta = (maxLogAlpha - minLogAlpha) / (DISP_GRID_SIZE - 1.0d);
        double bestLogAlpha = minLogAlpha;
        double bestLogPosterior = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < DISP_GRID_SIZE; i++) {
            final double logAlpha = minLogAlpha + (delta * i);
            final double logPosterior = logPosteriorForLogAlpha(ds, rowIndex, condition, muHat, logAlpha, logAlphaPriorMean, logAlphaPriorSigmaSq, usePrior);
            if (Double.isFinite(logPosterior) && logPosterior > bestLogPosterior) {
                bestLogPosterior = logPosterior;
                bestLogAlpha = logAlpha;
            }
        }

        final double fineStart = bestLogAlpha - delta;
        final double fineEnd = bestLogAlpha + delta;
        for (int i = 0; i < DISP_GRID_SIZE; i++) {
            final double t = (double) i / (double) (DISP_GRID_SIZE - 1);
            final double logAlpha = fineStart + ((fineEnd - fineStart) * t);
            final double logPosterior = logPosteriorForLogAlpha(ds, rowIndex, condition, muHat, logAlpha, logAlphaPriorMean, logAlphaPriorSigmaSq, usePrior);
            if (Double.isFinite(logPosterior) && logPosterior > bestLogPosterior) {
                bestLogPosterior = logPosterior;
                bestLogAlpha = logAlpha;
            }
        }

        return clamp(Math.exp(bestLogAlpha), MIN_DISP, maxDisp);
    }

    private static double logPosteriorForLogAlpha(final Dataset ds,
                                                  final int rowIndex,
                                                  final double[] condition,
                                                  final double[] muHat,
                                                  final double logAlpha,
                                                  final double logAlphaPriorMean,
                                                  final double logAlphaPriorSigmaSq,
                                                  final boolean usePrior) {
        final double alpha = Math.exp(logAlpha);
        final double alphaInv = 1.0d / Math.max(MIN_DISP_EVAL, alpha);
        double logLikelihood = 0.0d;
        final double lgammaAlphaInv = Gamma.logGamma(alphaInv);
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double y = ds.getRow(rowIndex).getElement(columnIndex);
            final double mu = muHat[columnIndex];
            if (!Double.isFinite(y) || !Double.isFinite(mu)) {
                continue;
            }
            // Match DESeq2's C++ log_posterior formulation exactly. The NB log-likelihood as written
            // in DESeq2.cpp drops the y*log(mu) and lgamma(y+1) terms that are constant in alpha.
            // Retaining them shifts |initial_lp| relative to R, which in turn shifts the threshold
            // |initial_lp|/1e6 used by estimateDispersionsGeneEst's "noIncrease" revert-to-alpha_init
            // rule, causing Java to keep tiny MLE moves that R rejects and thereby biasing the
            // distribution of dispGeneEst (and hence MAD-derived varLogDispEsts) downward.
            logLikelihood += Gamma.logGamma(y + alphaInv)
                    - lgammaAlphaInv
                    - (y * Math.log(mu + alphaInv))
                    - (alphaInv * Math.log1p(mu * alpha));
        }

        if (!Double.isFinite(logLikelihood)) {
            return Double.NEGATIVE_INFINITY;
        }

        final double coxReid = coxReidAdjustment(condition, muHat, alpha);
        if (!Double.isFinite(coxReid)) {
            return Double.NEGATIVE_INFINITY;
        }

        final double prior = usePrior
                ? -0.5d * ((logAlpha - logAlphaPriorMean) * (logAlpha - logAlphaPriorMean)) / logAlphaPriorSigmaSq
                : 0.0d;
        return logLikelihood + coxReid + prior;
    }

    private static double dLogPosteriorForLogAlpha(final Dataset ds,
                                                   final int rowIndex,
                                                   final double[] condition,
                                                   final double[] muHat,
                                                   final double logAlpha,
                                                   final double logAlphaPriorMean,
                                                   final double logAlphaPriorSigmaSq,
                                                   final boolean usePrior) {
        final double alpha = Math.exp(logAlpha);
        final double alphaInv = 1.0d / alpha;
        final double alphaInvSq = alphaInv * alphaInv;

        double llPart = 0.0d;
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double y = ds.getRow(rowIndex).getElement(columnIndex);
            final double mu = muHat[columnIndex];
            if (!Double.isFinite(y) || !Double.isFinite(mu)) {
                continue;
            }
            llPart += Gamma.digamma(alphaInv)
                    + Math.log1p(mu * alpha)
                    - ((mu * alpha) / (1.0d + mu * alpha))
                    - Gamma.digamma(y + alphaInv)
                    + (y / (mu + alphaInv));
        }
        llPart *= alphaInvSq;

        final double crPart = coxReidDerivative(condition, muHat, alpha);
        final double priorPart = usePrior ? -((logAlpha - logAlphaPriorMean) / logAlphaPriorSigmaSq) : 0.0d;
        return ((llPart + crPart) * alpha) + priorPart;
    }

    private static double coxReidAdjustment(final double[] condition,
                                            final double[] muHat,
                                            final double alpha) {
        double s00 = 0.0d;
        double s01 = 0.0d;
        double s11 = 0.0d;
        for (int columnIndex = 0; columnIndex < condition.length; columnIndex++) {
            final double x1 = condition[columnIndex];
            final double mu = muHat[columnIndex];
            if (!Double.isFinite(x1) || !Double.isFinite(mu)) {
                continue;
            }
            final double weight = 1.0d / ((1.0d / mu) + alpha);
            s00 += weight;
            s01 += weight * x1;
            s11 += weight * x1 * x1;
        }
        final double det = (s00 * s11) - (s01 * s01);
        return det > 0.0d ? -0.5d * Math.log(det) : Double.NEGATIVE_INFINITY;
    }

    private static double coxReidDerivative(final double[] condition,
                                            final double[] muHat,
                                            final double alpha) {
        double s00 = 0.0d;
        double s01 = 0.0d;
        double s11 = 0.0d;
        double ds00 = 0.0d;
        double ds01 = 0.0d;
        double ds11 = 0.0d;

        for (int columnIndex = 0; columnIndex < condition.length; columnIndex++) {
            final double x1 = condition[columnIndex];
            final double mu = muHat[columnIndex];
            if (!Double.isFinite(x1) || !Double.isFinite(mu)) {
                continue;
            }

            final double base = (1.0d / mu) + alpha;
            final double weight = 1.0d / base;
            final double dWeight = -1.0d / (base * base);

            s00 += weight;
            s01 += weight * x1;
            s11 += weight * x1 * x1;
            ds00 += dWeight;
            ds01 += dWeight * x1;
            ds11 += dWeight * x1 * x1;
        }

        final double det = (s00 * s11) - (s01 * s01);
        if (!(det > 0.0d)) {
            return Double.NaN;
        }

        final double inv00 = s11 / det;
        final double inv01 = -s01 / det;
        final double inv11 = s00 / det;
        final double trace = (inv00 * ds00) + (2.0d * inv01 * ds01) + (inv11 * ds11);
        return -0.5d * trace;
    }

    /** Wide-prior (MLE) Wald fit — DESeq2 first {@code fitNbinomGLMs} pass for Cook's / hat diagonals. */
    private static WaldFit fitWaldForRowMleWide(final Dataset ds,
                                               final int rowIndex,
                                               final double[] condition,
                                               final double[] sizeFactors,
                                               final double dispersion) {
        return fitWaldForRow(
                ds,
                rowIndex,
                condition,
                sizeFactors,
                dispersion,
                WIDE_PRIOR_LAMBDA_LOG2,
                WIDE_PRIOR_LAMBDA_LOG2);
    }

    private static WaldFit fitWaldForRow(final Dataset ds,
                                         final int rowIndex,
                                         final double[] condition,
                                         final double[] sizeFactors,
                                         final double dispersion,
                                         final double lambda0Log2,
                                         final double lambda1Log2) {
        if (!Double.isFinite(dispersion) || dispersion < 0.0d) {
            return invalidWaldFit(ds.getNumCol());
        }

        final CoefFit init = initializeCoefficientsForRow(ds, rowIndex, condition, sizeFactors);
        if (!init.valid) {
            return invalidWaldFit(ds.getNumCol());
        }

        final CoefFit coefFit = fitCoefficientsForRow(
                ds,
                rowIndex,
                condition,
                sizeFactors,
                dispersion,
                init.beta0,
                init.beta1,
                lambda0Log2,
                lambda1Log2);
        if (!coefFit.valid) {
            return invalidWaldFit(ds.getNumCol());
        }

        final double logTwo = Math.log(2.0d);
        final double ridge0 = lambda0Log2 / (logTwo * logTwo);
        final double ridge1 = lambda1Log2 / (logTwo * logTwo);

        final int colCount = ds.getNumCol();
        // Do not mutate coefFit.muHat; DESeq2 fitBeta uses the same samples as IRLS for X'WX.
        final double[] workingMuHat = Arrays.copyOf(coefFit.muHat, colCount);
        final double[] hatDiagonals = new double[colCount];
        double m00 = 0.0d;
        double m01 = 0.0d;
        double m11 = 0.0d;

        for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
            final double x1 = condition[columnIndex];
            final double y = ds.getRow(rowIndex).getElement(columnIndex);
            if (!Double.isFinite(x1)) {
                workingMuHat[columnIndex] = Double.NaN;
                hatDiagonals[columnIndex] = Double.NaN;
                continue;
            }

            if (!Double.isFinite(y)) {
                hatDiagonals[columnIndex] = Double.NaN;
                continue;
            }

            if (!Double.isFinite(workingMuHat[columnIndex])) {
                hatDiagonals[columnIndex] = Double.NaN;
                continue;
            }

            // DESeq2 fitBeta.cpp: mu_hat(j) = fmax(mu_hat(j), minmu) BEFORE computing w_vec.
            // This prevents 1/W blow-up and SE inflation for low-count genes (e.g. baseMean<1).
            final double mu = Math.max(MIN_MU, workingMuHat[columnIndex]);
            final double weight = mu / (1.0d + dispersion * mu);

            m00 += weight;
            m01 += weight * x1;
            m11 += weight * x1 * x1;
        }

        final double a00 = m00 + ridge0;
        final double a01 = m01;
        final double a11 = m11 + ridge1;
        final double det = a00 * a11 - a01 * a01;
        if (!Double.isFinite(det) || Math.abs(det) <= EPS) {
            return invalidWaldFit(ds.getNumCol());
        }

        final double inv00 = a11 / det;
        final double inv01 = -a01 / det;
        final double inv11 = a00 / det;

        for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
            final double x1 = condition[columnIndex];
            final double y = ds.getRow(rowIndex).getElement(columnIndex);
            final double muRaw = workingMuHat[columnIndex];
            if (!Double.isFinite(x1) || !Double.isFinite(y) || !Double.isFinite(muRaw)) {
                hatDiagonals[columnIndex] = Double.NaN;
                continue;
            }

            // DESeq2 fitBeta.cpp: hat matrix diagonals use floored mu (same W as X'WX above).
            final double mu = Math.max(MIN_MU, muRaw);
            final double weight = mu / (1.0d + dispersion * mu);
            final double quadratic = inv00 + (2.0d * x1 * inv01) + (x1 * x1 * inv11);
            hatDiagonals[columnIndex] = weight * quadratic;
        }

        // Match DESeq2 fitBeta C++: sigma = (X'WX + ridge)^{-1} (X'WX) (X'WX + ridge)^{-1}
        // and SE for beta1 is sqrt(c' sigma c) with c = [0, 1].
        final double sigma11 = inv01 * inv01 * m00 + 2.0d * inv01 * inv11 * m01 + inv11 * inv11 * m11;
        // DESeq2: betaSE uses sqrt(pmax(beta_var_mat, 0)); non-positive variance rows go to optim/refit.
        if (!Double.isFinite(sigma11) || sigma11 <= 0.0d) {
            return invalidWaldFit(ds.getNumCol());
        }
        final double se1 = Math.sqrt(sigma11);
        final double z = coefFit.beta1 / se1;
        if (!Double.isFinite(se1) || !Double.isFinite(z)) {
            return invalidWaldFit(ds.getNumCol());
        }

        return new WaldFit(coefFit.beta1, se1, z, coefFit.fittedMuHat, hatDiagonals, true);
    }

    private static WaldFit invalidWaldFit(final int colCount) {
        final double[] muHat = new double[colCount];
        final double[] hatDiagonals = new double[colCount];
        Arrays.fill(muHat, Double.NaN);
        Arrays.fill(hatDiagonals, Double.NaN);
        return new WaldFit(Double.NaN, Double.NaN, Double.NaN, muHat, hatDiagonals, false);
    }

    private static CoefFit initializeCoefficientsForRow(final Dataset ds,
                                                        final int rowIndex,
                                                        final double[] condition,
                                                        final double[] sizeFactors) {
        // DESeq2 fitNbinomGLMs: beta_mat <- t(solve(R, t(Q) %*% y)) with y = log(counts(normalized)+0.1)
        // per sample — equivalent to OLS on z ~ 1 + x1 for full-rank 2-column design.
        double s00 = 0.0d;
        double s01 = 0.0d;
        double s11 = 0.0d;
        double r0 = 0.0d;
        double r1 = 0.0d;
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double x1 = condition[columnIndex];
            final double value = ds.getRow(rowIndex).getElement(columnIndex);
            if (!Double.isFinite(x1) || !Double.isFinite(value) || value < 0.0d) {
                continue;
            }

            final double z = Math.log((value / Math.max(EPS, sizeFactors[columnIndex])) + 0.1d);
            s00 += 1.0d;
            s01 += x1;
            s11 += x1 * x1;
            r0 += z;
            r1 += x1 * z;
        }

        if (s00 < 2.0d) {
            return new CoefFit(Double.NaN, Double.NaN, null, null, false);
        }

        final double det = (s00 * s11) - (s01 * s01);
        if (!Double.isFinite(det) || Math.abs(det) <= EPS) {
            return new CoefFit(Double.NaN, Double.NaN, null, null, false);
        }

        final double beta0 = ((r0 * s11) - (r1 * s01)) / det;
        final double beta1 = ((s00 * r1) - (s01 * r0)) / det;
        return new CoefFit(beta0, beta1, null, null, Double.isFinite(beta0) && Double.isFinite(beta1));
    }

    private static CoefFit fitMeanParametersForRow(final Dataset ds,
                                                   final int rowIndex,
                                                   final double[] condition,
                                                   final double[] sizeFactors,
                                                   final double dispersion) {
        final CoefFit init = initializeCoefficientsForRow(ds, rowIndex, condition, sizeFactors);
        if (!init.valid) {
            return new CoefFit(Double.NaN, Double.NaN, null, null, false);
        }
        return fitCoefficientsForRow(
                ds,
                rowIndex,
                condition,
                sizeFactors,
                dispersion,
                init.beta0,
                init.beta1,
                WIDE_PRIOR_LAMBDA_LOG2,
                WIDE_PRIOR_LAMBDA_LOG2);
    }

    /**
     * DESeq2 {@code fitBeta} with {@code useQR=true}: QR on augmented
     * {@code join_cols(X.each_col() % sqrt(w), diag(sqrt(lambda)))} and rhs
     * {@code [sqrt(w)*z ; 0]} (see DESeq2.cpp).
     */
    private static double[] ridgeWlsQr2Cols(final double[] sqrtW,
                                            final double[] x1,
                                            final double[] zWork,
                                            final int n,
                                            final double sqrtLambda0,
                                            final double sqrtLambda1) {
        if (n <= 0 || sqrtW.length < n || x1.length < n || zWork.length < n) {
            return null;
        }
        final int rows = n + 2;
        final double[][] a = new double[rows][2];
        final double[] b = new double[rows];
        for (int i = 0; i < n; i++) {
            final double sw = sqrtW[i];
            if (!Double.isFinite(sw) || sw < 0.0d) {
                return null;
            }
            a[i][0] = sw;
            a[i][1] = sw * x1[i];
            b[i] = zWork[i] * sw;
        }
        a[n][0] = sqrtLambda0;
        a[n][1] = 0.0d;
        b[n] = 0.0d;
        a[n + 1][0] = 0.0d;
        a[n + 1][1] = sqrtLambda1;
        b[n + 1] = 0.0d;
        try {
            final RealMatrix mat = new Array2DRowRealMatrix(a, false);
            final RealVector vec = new ArrayRealVector(b, false);
            final RealVector sol = new QRDecomposition(mat).getSolver().solve(vec);
            final double b0 = sol.getEntry(0);
            final double b1 = sol.getEntry(1);
            if (!Double.isFinite(b0) || !Double.isFinite(b1)) {
                return null;
            }
            return new double[] { b0, b1 };
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static CoefFit fitCoefficientsForRow(final Dataset ds,
                                                 final int rowIndex,
                                                 final double[] condition,
                                                 final double[] sizeFactors,
                                                 final double dispersion,
                                                 final double beta0Start,
                                                 final double beta1Start,
                                                 final double lambda0Log2,
                                                 final double lambda1Log2) {
        double beta0 = beta0Start;
        double beta1 = beta1Start;
        double previousDeviance = Double.NaN;
        final double logTwo = Math.log(2.0d);
        boolean useCurrentIrlsStartForOptimization = false;
        final int colCount = ds.getNumCol();
        final double[] irlsSqrtW = new double[colCount];
        final double[] irlsX1 = new double[colCount];
        final double[] irlsZ = new double[colCount];
        final double ridge0 = lambda0Log2 / (logTwo * logTwo);
        final double ridge1 = lambda1Log2 / (logTwo * logTwo);
        final double sqrtRidge0 = Math.sqrt(ridge0);
        final double sqrtRidge1 = Math.sqrt(ridge1);

        for (int iter = 0; iter < COEF_MAX_ITERS; iter++) {
            double a00 = 0.0d;
            double a01 = 0.0d;
            double a11 = 0.0d;
            double b0 = 0.0d;
            double b1 = 0.0d;
            int used = 0;

            for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
                final double x1 = condition[columnIndex];
                final double y = ds.getRow(rowIndex).getElement(columnIndex);
                if (!Double.isFinite(x1) || !Double.isFinite(y)) {
                    continue;
                }

                final double offset = Math.log(Math.max(EPS, sizeFactors[columnIndex]));
                final double eta = beta0 + beta1 * x1 + offset;
                // DESeq2 fitBeta: mu = nf * exp(X %*% beta), then fmax(mu, minmu) — no eta clamp before exp.
                final double mu = Math.max(MIN_MU, Math.exp(eta));
                final double weight = mu / (1.0d + dispersion * mu);
                // DESeq2 fitBeta (DESeq2.cpp): z = log(mu_hat / nf) + (y - mu_hat) / mu_hat
                final double zWork = Math.log(mu / Math.max(EPS, sizeFactors[columnIndex])) + ((y - mu) / mu);

                a00 += weight;
                a01 += weight * x1;
                a11 += weight * x1 * x1;
                b0 += weight * zWork;
                b1 += weight * x1 * zWork;

                final double sqrtW = Math.sqrt(weight);
                irlsSqrtW[used] = sqrtW;
                irlsX1[used] = x1;
                irlsZ[used] = zWork;
                used++;
            }

            if (used < 2) {
                return new CoefFit(Double.NaN, Double.NaN, null, null, false);
            }

            a00 += ridge0;
            a11 += ridge1;
            final double det = a00 * a11 - a01 * a01;

            double newBeta0;
            double newBeta1;
            final double[] qrSol = ridgeWlsQr2Cols(irlsSqrtW, irlsX1, irlsZ, used, sqrtRidge0, sqrtRidge1);
            if (qrSol != null) {
                newBeta0 = qrSol[0];
                newBeta1 = qrSol[1];
            } else if (Double.isFinite(det) && Math.abs(det) > EPS) {
                newBeta0 = (b0 * a11 - b1 * a01) / det;
                newBeta1 = (a00 * b1 - a01 * b0) / det;
            } else {
                useCurrentIrlsStartForOptimization = false;
                break;
            }

            if (!Double.isFinite(newBeta0) || !Double.isFinite(newBeta1)) {
                useCurrentIrlsStartForOptimization = false;
                break;
            }

            if (Math.abs(newBeta0) > 30.0d
                    || Math.abs(newBeta1) > 30.0d) {
                useCurrentIrlsStartForOptimization = false;
                break;
            }

            beta0 = newBeta0;
            beta1 = newBeta1;

            final double deviance = -2.0d * rowLogLikelihood(ds, rowIndex, condition, sizeFactors, dispersion, beta0, beta1, true);
            if (!Double.isFinite(deviance)) {
                useCurrentIrlsStartForOptimization = false;
                break;
            }
            useCurrentIrlsStartForOptimization = true;

            if (Double.isFinite(previousDeviance)) {
                final double convergence = Math.abs(deviance - previousDeviance) / (Math.abs(deviance) + 0.1d);
                if (Double.isFinite(convergence) && convergence < COEF_TOL) {
                    return new CoefFit(
                            beta0,
                            beta1,
                            fittedMeansForRow(ds.getNumCol(), condition, sizeFactors, beta0, beta1, true),
                            fittedMeansForRow(ds.getNumCol(), condition, sizeFactors, beta0, beta1, false),
                            true);
                }
            }
            previousDeviance = deviance;
        }

        final double optimizeStart0;
        final double optimizeStart1;
        if (useCurrentIrlsStartForOptimization) {
            optimizeStart0 = beta0;
            optimizeStart1 = beta1;
        } else {
            optimizeStart0 = beta0Start;
            optimizeStart1 = beta1Start;
        }

        final CoefFit optimized = dampedNewtonFitForRow(
                ds,
                rowIndex,
                condition,
                sizeFactors,
                dispersion,
                optimizeStart0,
                optimizeStart1,
                lambda0Log2,
                lambda1Log2);
        return optimized.valid ? optimized : new CoefFit(Double.NaN, Double.NaN, null, null, false);
    }

    /**
     * Fallback coefficient fit using damped Newton with the analytical NB-GLM observed Hessian.
     *
     * <p>For a two-parameter NB GLM with log link, the negative log-posterior is strictly convex over
     * {@code R^2} (ridge prior makes the Hessian strictly positive definite). Damped Newton with the
     * observed information matrix therefore converges to the unique global optimum — the MLE/MAP that
     * DESeq2's {@code fitNbinomGLMs} recovers via its Fortran L-BFGS-B {@code optim} fallback. Unlike
     * the expected-information IRLS step (which uses weight {@code mu/(1+α·mu)}), the observed Hessian
     * uses {@code mu·r·(r+y)/(r+mu)^2}, which dampens step magnitude on outlier observations and avoids
     * the overshoot behavior that triggers the {@code |β| > 30} break in {@link #fitCoefficientsForRow}.
     *
     * <p>Inputs {@code beta0Start} / {@code beta1Start} are on the natural-log scale (consistent with
     * DESeq2 {@code fitBeta}); the box constraint {@code [-30·log2, 30·log2]} matches {@code optim}'s
     * {@code lower=-30}, {@code upper=30} on log2 coefficients.
     */
    private static CoefFit dampedNewtonFitForRow(final Dataset ds,
                                                 final int rowIndex,
                                                 final double[] condition,
                                                 final double[] sizeFactors,
                                                 final double dispersion,
                                                 final double beta0Start,
                                                 final double beta1Start,
                                                 final double lambda0Log2,
                                                 final double lambda1Log2) {
        final double logTwo = Math.log(2.0d);
        final double lambda0Nat = lambda0Log2 / (logTwo * logTwo);
        final double lambda1Nat = lambda1Log2 / (logTwo * logTwo);
        final double betaBound = 30.0d * logTwo;
        final int colCount = ds.getNumCol();
        final double r = 1.0d / Math.max(MIN_DISP_EVAL, dispersion);

        double beta0 = Math.max(-betaBound, Math.min(betaBound,
                Double.isFinite(beta0Start) ? beta0Start : 0.0d));
        double beta1 = Math.max(-betaBound, Math.min(betaBound,
                Double.isFinite(beta1Start) ? beta1Start : 0.0d));

        double currentObj = evalNegLogPosteriorNat(ds, rowIndex, condition, sizeFactors, dispersion,
                beta0, beta1, lambda0Nat, lambda1Nat);
        if (!Double.isFinite(currentObj)) {
            return new CoefFit(Double.NaN, Double.NaN, null, null, false);
        }

        final int newtonMaxIters = 200;
        for (int iter = 0; iter < newtonMaxIters; iter++) {
            double s0 = 0.0d;
            double s1 = 0.0d;
            double h00 = 0.0d;
            double h01 = 0.0d;
            double h11 = 0.0d;
            int used = 0;
            for (int c = 0; c < colCount; c++) {
                final double x1 = condition[c];
                final double y = ds.getRow(rowIndex).getElement(c);
                if (!Double.isFinite(x1) || !Double.isFinite(y) || y < 0.0d) {
                    continue;
                }
                final double offset = Math.log(Math.max(EPS, sizeFactors[c]));
                final double eta = beta0 + beta1 * x1 + offset;
                final double mu = Math.exp(eta);
                if (!Double.isFinite(mu) || mu <= 0.0d) {
                    return new CoefFit(Double.NaN, Double.NaN, null, null, false);
                }
                final double denom = r + mu;
                final double scoreTerm = (r * (y - mu)) / denom;
                s0 += scoreTerm;
                s1 += scoreTerm * x1;
                final double w = (mu * r * (r + y)) / (denom * denom);
                h00 += w;
                h01 += w * x1;
                h11 += w * x1 * x1;
                used++;
            }
            if (used < 2) {
                return new CoefFit(Double.NaN, Double.NaN, null, null, false);
            }

            s0 -= lambda0Nat * beta0;
            s1 -= lambda1Nat * beta1;
            h00 += lambda0Nat;
            h11 += lambda1Nat;

            // Active-set handling for the box constraint [-betaBound, betaBound]. A coord is
            // "active" (treated as frozen) when the unconstrained score wants to push it further
            // past the bound it already sits on. This matches DESeq2's Fortran L-BFGS-B fallback,
            // which is the projected-gradient/quasi-Newton active-set method. Without this, the
            // full 2-D Newton direction is projected onto the feasible set and step-halving stalls
            // without making progress on the free coord.
            final double atBoundTol = 1.0e-10d;
            final boolean atUpper0 = beta0 >= betaBound - atBoundTol;
            final boolean atLower0 = beta0 <= -betaBound + atBoundTol;
            final boolean atUpper1 = beta1 >= betaBound - atBoundTol;
            final boolean atLower1 = beta1 <= -betaBound + atBoundTol;
            final boolean fix0 = (atUpper0 && s0 > 0.0d) || (atLower0 && s0 < 0.0d);
            final boolean fix1 = (atUpper1 && s1 > 0.0d) || (atLower1 && s1 < 0.0d);

            final double d0;
            final double d1;
            if (fix0 && fix1) {
                break;
            } else if (fix0) {
                if (!Double.isFinite(h11) || h11 < EPS) {
                    return new CoefFit(Double.NaN, Double.NaN, null, null, false);
                }
                d0 = 0.0d;
                d1 = s1 / h11;
            } else if (fix1) {
                if (!Double.isFinite(h00) || h00 < EPS) {
                    return new CoefFit(Double.NaN, Double.NaN, null, null, false);
                }
                d0 = s0 / h00;
                d1 = 0.0d;
            } else {
                final double det = h00 * h11 - h01 * h01;
                if (!Double.isFinite(det) || det < EPS) {
                    return new CoefFit(Double.NaN, Double.NaN, null, null, false);
                }
                d0 = (h11 * s0 - h01 * s1) / det;
                d1 = (-h01 * s0 + h00 * s1) / det;
            }
            if (!Double.isFinite(d0) || !Double.isFinite(d1)) {
                return new CoefFit(Double.NaN, Double.NaN, null, null, false);
            }

            final double gradMax = Math.max(Math.abs(s0), Math.abs(s1));

            double step = 1.0d;
            boolean accepted = false;
            double newBeta0 = beta0;
            double newBeta1 = beta1;
            double newObj = currentObj;
            for (int inner = 0; inner < 40; inner++) {
                final double trial0 = Math.max(-betaBound, Math.min(betaBound, beta0 + step * d0));
                final double trial1 = Math.max(-betaBound, Math.min(betaBound, beta1 + step * d1));
                final double trialObj = evalNegLogPosteriorNat(ds, rowIndex, condition, sizeFactors,
                        dispersion, trial0, trial1, lambda0Nat, lambda1Nat);
                final double required = currentObj - 1.0e-14d * Math.max(1.0d, Math.abs(currentObj));
                if (Double.isFinite(trialObj) && trialObj <= required) {
                    newBeta0 = trial0;
                    newBeta1 = trial1;
                    newObj = trialObj;
                    accepted = true;
                    break;
                }
                step *= 0.5d;
            }

            if (!accepted) {
                break;
            }

            final double deltaMax = Math.max(Math.abs(newBeta0 - beta0), Math.abs(newBeta1 - beta1));
            beta0 = newBeta0;
            beta1 = newBeta1;
            currentObj = newObj;

            if (gradMax < 1.0e-10d || deltaMax < 1.0e-12d) {
                break;
            }
        }

        return new CoefFit(
                beta0,
                beta1,
                fittedMeansForRow(colCount, condition, sizeFactors, beta0, beta1, true),
                fittedMeansForRow(colCount, condition, sizeFactors, beta0, beta1, false),
                true);
    }

    private static double evalNegLogPosteriorNat(final Dataset ds,
                                                 final int rowIndex,
                                                 final double[] condition,
                                                 final double[] sizeFactors,
                                                 final double dispersion,
                                                 final double beta0,
                                                 final double beta1,
                                                 final double lambda0Nat,
                                                 final double lambda1Nat) {
        final double ll = rowLogLikelihood(ds, rowIndex, condition, sizeFactors, dispersion,
                beta0, beta1, false);
        if (!Double.isFinite(ll)) {
            return Double.POSITIVE_INFINITY;
        }
        final double penalty = 0.5d * (lambda0Nat * beta0 * beta0 + lambda1Nat * beta1 * beta1);
        return -ll + penalty;
    }

    private static double[] fittedMeansForRow(final int colCount,
                                              final double[] condition,
                                              final double[] sizeFactors,
                                              final double beta0,
                                              final double beta1,
                                              final boolean clampMu) {
        final double[] muHat = new double[colCount];
        for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
            final double x1 = condition[columnIndex];
            if (!Double.isFinite(x1)) {
                muHat[columnIndex] = Double.NaN;
                continue;
            }
            final double offset = Math.log(Math.max(EPS, sizeFactors[columnIndex]));
            final double eta = beta0 + (beta1 * x1) + offset;
            final double muExp = Math.exp(eta);
            final double mu = clampMu ? Math.max(MIN_MU, muExp) : muExp;
            muHat[columnIndex] = Double.isFinite(mu) ? mu : Double.NaN;
        }
        return muHat;
    }

    private static double rowLogLikelihood(final Dataset ds,
                                           final int rowIndex,
                                           final double[] condition,
                                           final double[] sizeFactors,
                                           final double dispersion,
                                           final double beta0,
                                           final double beta1,
                                           final boolean clampMu) {
        double logLikelihood = 0.0d;
        int used = 0;
        for (int columnIndex = 0; columnIndex < ds.getNumCol(); columnIndex++) {
            final double x1 = condition[columnIndex];
            final double y = ds.getRow(rowIndex).getElement(columnIndex);
            if (!Double.isFinite(x1) || !Double.isFinite(y) || y < 0.0d) {
                continue;
            }

            final double offset = Math.log(Math.max(EPS, sizeFactors[columnIndex]));
            final double eta = beta0 + beta1 * x1 + offset;
            final double muExp = Math.exp(eta);
            if (!Double.isFinite(muExp) || muExp <= 0.0d) {
                return Double.NaN;
            }
            final double mu = clampMu ? Math.max(MIN_MU, muExp) : muExp;
            logLikelihood += nbLogPmf(y, mu, dispersion);
            used++;
        }
        return used == 0 ? Double.NaN : logLikelihood;
    }

    private static double nbLogPmf(final double y, final double mu, final double alpha) {
        final double r = 1.0d / Math.max(MIN_DISP_EVAL, alpha);
        final double logDenom = Math.log(r + mu);
        final double logP = Math.log(r) - logDenom;
        final double logOneMinusP = Math.log(mu) - logDenom;
        return Gamma.logGamma(y + r) - Gamma.logGamma(r) - Gamma.logGamma(y + 1.0d)
                + r * logP
                + y * logOneMinusP;
    }

    private static double[] fitDispersionTrendValues(final double[] means, final double[] alphaRaw) {
        return fitDispersionTrendValuesCapturing(means, alphaRaw, null, null, null);
    }

    /**
     * Variant of {@link #fitDispersionTrendValues} that also surfaces the parameters that were used to
     * produce the trend values. The captured parameters are required to replicate DESeq2's
     * {@code refitWithoutOutliers}, which keeps the original trend function for the refit pass so the
     * refitted Wald SEs match the first-pass trend-dependent MAP shrinkage.
     */
    private static double[] fitDispersionTrendValuesCapturing(final double[] means,
                                                              final double[] alphaRaw,
                                                              final double[] trendParamsOut,
                                                              final boolean[] usedParametricOut,
                                                              final double[][] localTrendOut) {
        try {
            final double[] trendParams = fitDispersionTrend(means, alphaRaw);
            final double[] fittedTrend = new double[means.length];
            for (int i = 0; i < means.length; i++) {
                fittedTrend[i] = Double.isFinite(means[i]) && means[i] > EPS
                        ? trendDispersion(means[i], trendParams[0], trendParams[1])
                        : Double.NaN;
            }
            if (trendParamsOut != null && trendParamsOut.length >= 2) {
                trendParamsOut[0] = trendParams[0];
                trendParamsOut[1] = trendParams[1];
            }
            if (usedParametricOut != null && usedParametricOut.length >= 1) {
                usedParametricOut[0] = true;
            }
            if (localTrendOut != null && localTrendOut.length >= 1) {
                localTrendOut[0] = null;
            }
            return fittedTrend;
        } catch (RuntimeException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Parametric dispersion trend fit failed; using local trend fallback", ex);
            }
            final double[] localTrend = fitLocalDispersionTrend(means, alphaRaw);
            if (localTrend != null) {
                if (usedParametricOut != null && usedParametricOut.length >= 1) {
                    usedParametricOut[0] = false;
                }
                if (localTrendOut != null && localTrendOut.length >= 1) {
                    localTrendOut[0] = localTrend.clone();
                }
                return localTrend;
            }

            final double[] defaultTrend = new double[means.length];
            for (int i = 0; i < means.length; i++) {
                defaultTrend[i] = Double.isFinite(means[i]) && means[i] > EPS
                        ? trendDispersion(means[i], 0.1d, 1.0d)
                        : Double.NaN;
            }
            if (trendParamsOut != null && trendParamsOut.length >= 2) {
                trendParamsOut[0] = 0.1d;
                trendParamsOut[1] = 1.0d;
            }
            if (usedParametricOut != null && usedParametricOut.length >= 1) {
                usedParametricOut[0] = true; // (0.1, 1.0) is still the parametric form
            }
            if (localTrendOut != null && localTrendOut.length >= 1) {
                localTrendOut[0] = null;
            }
            return defaultTrend;
        }
    }

    private static double[] fitLocalDispersionTrend(final double[] means, final double[] alphaRaw) {
        int usable = 0;
        for (int i = 0; i < means.length; i++) {
            if (Double.isFinite(means[i]) && means[i] > EPS
                    && Double.isFinite(alphaRaw[i]) && alphaRaw[i] >= 100.0d * MIN_DISP) {
                usable++;
            }
        }

        if (usable == 0) {
            return null;
        }

        final double[][] fitData = new double[usable][3];
        int fitIndex = 0;
        for (int i = 0; i < means.length; i++) {
            if (!(Double.isFinite(means[i]) && means[i] > EPS
                    && Double.isFinite(alphaRaw[i]) && alphaRaw[i] >= 100.0d * MIN_DISP)) {
                continue;
            }
            fitData[fitIndex][0] = Math.log(means[i]);
            fitData[fitIndex][1] = Math.log(alphaRaw[i]);
            fitData[fitIndex][2] = means[i];
            fitIndex++;
        }
        Arrays.sort(fitData, (left, right) -> Double.compare(left[0], right[0]));

        final double[] logMeans = new double[usable];
        final double[] logDisps = new double[usable];
        final double[] priorWeights = new double[usable];
        for (int i = 0; i < usable; i++) {
            logMeans[i] = fitData[i][0];
            logDisps[i] = fitData[i][1];
            priorWeights[i] = fitData[i][2];
        }

        final int neighborCount = Math.min(usable, Math.max(3, (int) Math.ceil(LOCAL_DISPERSION_SPAN * usable)));
        final double[] fittedTrend = new double[means.length];
        for (int i = 0; i < means.length; i++) {
            if (!(Double.isFinite(means[i]) && means[i] > EPS)) {
                fittedTrend[i] = Double.NaN;
                continue;
            }

            final double logMean = Math.log(means[i]);
            int insertion = Arrays.binarySearch(logMeans, logMean);
            if (insertion < 0) {
                insertion = -insertion - 1;
            }

            int left = Math.max(0, Math.min(usable - neighborCount, insertion - (neighborCount / 2)));
            int right = left + neighborCount - 1;
            while (right < usable - 1) {
                final double distanceLeft = logMean - logMeans[left];
                final double distanceRight = logMeans[right + 1] - logMean;
                if (distanceLeft > distanceRight) {
                    left++;
                    right++;
                    continue;
                }
                break;
            }

            final double bandwidth = Math.max(logMean - logMeans[left], logMeans[right] - logMean);
            double sumWeights = 0.0d;
            double weightedMean = 0.0d;
            double s0 = 0.0d;
            double s1 = 0.0d;
            double s2 = 0.0d;
            double s3 = 0.0d;
            double s4 = 0.0d;
            double r0 = 0.0d;
            double r1 = 0.0d;
            double r2 = 0.0d;

            for (int j = left; j <= right; j++) {
                final double distance = Math.abs(logMeans[j] - logMean);
                double weight = priorWeights[j];
                if (bandwidth > 0.0d) {
                    final double scaledDistance = distance / bandwidth;
                    if (!(scaledDistance < 1.0d)) {
                        continue;
                    }
                    weight *= cube(1.0d - cube(scaledDistance));
                }
                if (!(weight > 0.0d)) {
                    continue;
                }

                final double centered = logMeans[j] - logMean;
                final double centered2 = centered * centered;
                sumWeights += weight;
                weightedMean += weight * logDisps[j];
                s0 += weight;
                s1 += weight * centered;
                s2 += weight * centered2;
                s3 += weight * centered2 * centered;
                s4 += weight * centered2 * centered2;
                r0 += weight * logDisps[j];
                r1 += weight * logDisps[j] * centered;
                r2 += weight * logDisps[j] * centered2;
            }

            double fittedLogDisp = Double.NaN;
            if (sumWeights > 0.0d) {
                weightedMean /= sumWeights;

                final double det3 = s0 * ((s2 * s4) - (s3 * s3))
                        - s1 * ((s1 * s4) - (s2 * s3))
                        + s2 * ((s1 * s3) - (s2 * s2));
                final double det3Scale = Math.abs(s0 * s2 * s4)
                        + Math.abs(s1 * s1 * s4)
                        + Math.abs(s2 * s2 * s2)
                        + 1.0d;
                if (Double.isFinite(det3) && Math.abs(det3) > (1.0e-12d * det3Scale)) {
                    final double numerator = r0 * ((s2 * s4) - (s3 * s3))
                            - s1 * ((r1 * s4) - (s3 * r2))
                            + s2 * ((r1 * s3) - (s2 * r2));
                    fittedLogDisp = numerator / det3;
                } else {
                    final double det2 = (s0 * s2) - (s1 * s1);
                    final double det2Scale = Math.abs(s0 * s2) + Math.abs(s1 * s1) + 1.0d;
                    if (Double.isFinite(det2) && Math.abs(det2) > (1.0e-12d * det2Scale)) {
                        fittedLogDisp = ((r0 * s2) - (s1 * r1)) / det2;
                    } else {
                        fittedLogDisp = weightedMean;
                    }
                }
            }

            fittedTrend[i] = Double.isFinite(fittedLogDisp) ? Math.exp(fittedLogDisp) : Double.NaN;
        }
        return fittedTrend;
    }

    private static double[] fitDispersionTrend(final double[] means, final double[] alphaRaw) {
        final int n = means.length;
        final boolean[] use = new boolean[n];
        int usable = 0;
        for (int i = 0; i < n; i++) {
            final boolean valid = Double.isFinite(means[i])
                    && means[i] > EPS
                    && Double.isFinite(alphaRaw[i])
                    && alphaRaw[i] >= 100.0d * MIN_DISP;
            use[i] = valid;
            if (valid) {
                usable++;
            }
        }

        if (usable < 3) {
            throw new IllegalStateException("parametric dispersion fit failed");
        }

        double a = 0.1d;
        double b = 1.0d;
        int iter = 0;
        while (true) {
            final boolean[] good = new boolean[n];
            int goodCount = 0;
            for (int i = 0; i < n; i++) {
                if (!use[i]) {
                    continue;
                }
                final double trend = a + b / Math.max(EPS, means[i]);
                final double ratio = alphaRaw[i] / trend;
                if (Double.isFinite(ratio) && ratio > 1.0e-4d && ratio < 15.0d) {
                    good[i] = true;
                    goodCount++;
                }
            }

            if (goodCount < 3) {
                throw new IllegalStateException("parametric dispersion fit failed");
            }

            final double oldA = a;
            final double oldB = b;
            final GammaIdentityFit fit = fitGammaIdentityTrend(means, alphaRaw, good, a, b);
            if (!fit.valid || !(fit.a > 0.0d) || !(fit.b > 0.0d)) {
                throw new IllegalStateException("parametric dispersion fit failed");
            }

            a = fit.a;
            b = fit.b;
            if (sumSquaredLogChange(a, oldA, b, oldB) < 1.0e-6d && fit.converged) {
                break;
            }

            iter++;
            if (iter > 10) {
                throw new IllegalStateException("dispersion fit did not converge");
            }
        }
        return new double[] { a, b };
    }

    private static GammaIdentityFit fitGammaIdentityTrend(final double[] means,
                                                          final double[] alphaRaw,
                                                          final boolean[] use,
                                                          final double aStart,
                                                          final double bStart) {
        double a = aStart;
        double b = bStart;
        double previousDeviance = Double.NaN;

        for (int iter = 0; iter < 100; iter++) {
            double s00 = 0.0d;
            double s01 = 0.0d;
            double s11 = 0.0d;
            double t0 = 0.0d;
            double t1 = 0.0d;
            int used = 0;

            for (int i = 0; i < means.length; i++) {
                if (!use[i]) {
                    continue;
                }

                final double x1 = 1.0d / Math.max(EPS, means[i]);
                final double mu = a + b * x1;
                if (!Double.isFinite(mu) || mu <= 0.0d) {
                    return new GammaIdentityFit(a, b, false, false);
                }

                final double weight = 1.0d / (mu * mu);
                s00 += weight;
                s01 += weight * x1;
                s11 += weight * x1 * x1;
                t0 += weight * alphaRaw[i];
                t1 += weight * alphaRaw[i] * x1;
                used++;
            }

            if (used < 2) {
                return new GammaIdentityFit(a, b, false, false);
            }

            final double det = s00 * s11 - s01 * s01;
            if (!Double.isFinite(det) || Math.abs(det) <= EPS) {
                return new GammaIdentityFit(a, b, false, false);
            }

            final double nextA = (t0 * s11 - t1 * s01) / det;
            final double nextB = (s00 * t1 - s01 * t0) / det;
            if (!Double.isFinite(nextA) || !Double.isFinite(nextB) || nextA <= 0.0d || nextB <= 0.0d) {
                return new GammaIdentityFit(a, b, false, false);
            }

            a = nextA;
            b = nextB;

            double deviance = 0.0d;
            for (int i = 0; i < means.length; i++) {
                if (!use[i]) {
                    continue;
                }
                final double mu = a + b / Math.max(EPS, means[i]);
                if (!Double.isFinite(mu) || mu <= 0.0d || !Double.isFinite(alphaRaw[i]) || alphaRaw[i] <= 0.0d) {
                    continue;
                }
                deviance += 2.0d * (-Math.log(alphaRaw[i] / mu) + ((alphaRaw[i] - mu) / mu));
            }

            if (Double.isFinite(previousDeviance)) {
                final double convergence = Math.abs(deviance - previousDeviance) / (Math.abs(deviance) + 0.1d);
                if (Double.isFinite(convergence) && convergence < 1.0e-8d) {
                    return new GammaIdentityFit(a, b, true, true);
                }
            }
            previousDeviance = deviance;
        }

        return new GammaIdentityFit(a, b, false, true);
    }

    private static double sumSquaredLogChange(final double newA,
                                              final double oldA,
                                              final double newB,
                                              final double oldB) {
        final double deltaA = Math.log(newA / oldA);
        final double deltaB = Math.log(newB / oldB);
        return (deltaA * deltaA) + (deltaB * deltaB);
    }

    private static double trendDispersion(final double mean, final double a, final double b) {
        return Math.max(MIN_DISP, a + b / Math.max(EPS, mean));
    }

    private static double selectIndependentFilteringThreshold(final double[] filter, final double[] pValues) {
        final double[] safeFilter = sanitizeFilter(filter);
        final double lowerQuantile = meanZeroFraction(safeFilter);
        final double upperQuantile = lowerQuantile < 0.95d ? 0.95d : 1.0d;
        if (upperQuantile <= lowerQuantile + EPS) {
            return quantile(safeFilter, lowerQuantile);
        }

        final double[] theta = sequence(lowerQuantile, upperQuantile, FILTER_GRID_POINTS);
        final double[] numRej = new double[theta.length];
        for (int i = 0; i < theta.length; i++) {
            final double cutoff = quantile(safeFilter, theta[i]);
            final double[] filteredPadj = applyIndependentFilteringAtSafeThreshold(safeFilter, pValues, cutoff);
            numRej[i] = countRejections(filteredPadj, TARGET_FDR);
        }

        final int selectedIndex;
        if (max(numRej) <= 10.0d) {
            selectedIndex = 0;
        } else {
            final double[] smooth = smoothRejectionCurve(theta, numRej);
            final double[] residual = positiveResiduals(numRej, smooth);
            final double maxFit = max(smooth);
            final double residualRmse = rmse(residual);
            final double threshold = maxFit - residualRmse;
            if (anyGreaterThan(numRej, threshold)) {
                selectedIndex = firstGreaterThan(numRej, threshold);
            } else if (anyGreaterThan(numRej, 0.9d * maxFit)) {
                selectedIndex = firstGreaterThan(numRej, 0.9d * maxFit);
            } else if (anyGreaterThan(numRej, 0.8d * maxFit)) {
                selectedIndex = firstGreaterThan(numRej, 0.8d * maxFit);
            } else {
                selectedIndex = 0;
            }
        }

        final double selectedThreshold = quantile(safeFilter, theta[selectedIndex]);
        if (log.isDebugEnabled()) {
            log.debug("Selected independent filtering threshold {}", selectedThreshold);
        }
        return selectedThreshold;
    }

    private static double[] applyIndependentFilteringAtThreshold(final double[] filter,
                                                                 final double[] pValues,
                                                                 final double threshold) {
        if (!Double.isFinite(threshold)) {
            return benjaminiHochberg(pValues);
        }
        return applyIndependentFilteringAtSafeThreshold(sanitizeFilter(filter), pValues, threshold);
    }

    private static double[] applyIndependentFilteringAtSafeThreshold(final double[] safeFilter,
                                                                     final double[] pValues,
                                                                     final double threshold) {
        final double[] adjusted = new double[pValues.length];
        Arrays.fill(adjusted, Double.NaN);

        int selected = 0;
        for (int i = 0; i < pValues.length; i++) {
            if (safeFilter[i] >= threshold && Double.isFinite(pValues[i])) {
                selected++;
            }
        }
        if (selected == 0) {
            return adjusted;
        }

        final int[] indices = new int[selected];
        final double[] selectedPValues = new double[selected];
        int pos = 0;
        for (int i = 0; i < pValues.length; i++) {
            if (safeFilter[i] >= threshold && Double.isFinite(pValues[i])) {
                indices[pos] = i;
                selectedPValues[pos] = pValues[i];
                pos++;
            }
        }

        final double[] selectedAdjusted = benjaminiHochberg(selectedPValues);
        for (int i = 0; i < selected; i++) {
            adjusted[indices[i]] = selectedAdjusted[i];
        }
        return adjusted;
    }

    private static double[] benjaminiHochberg(final double[] pValues) {
        final double[] adjusted = new double[pValues.length];
        Arrays.fill(adjusted, Double.NaN);

        int finite = 0;
        for (double pValue : pValues) {
            if (Double.isFinite(pValue)) {
                finite++;
            }
        }
        if (finite == 0) {
            return adjusted;
        }

        final Integer[] order = new Integer[finite];
        int pos = 0;
        for (int i = 0; i < pValues.length; i++) {
            if (Double.isFinite(pValues[i])) {
                order[pos++] = i;
            }
        }
        Arrays.sort(order, (left, right) -> Double.compare(pValues[left], pValues[right]));

        double running = 1.0d;
        for (int rank = finite - 1; rank >= 0; rank--) {
            final int index = order[rank];
            final double raw = pValues[index] * ((double) finite / (double) (rank + 1));
            running = Math.min(running, raw);
            adjusted[index] = clamp(running, 0.0d, 1.0d);
        }
        return adjusted;
    }

    private static double[] sanitizeFilter(final double[] filter) {
        final double[] safe = new double[filter.length];
        for (int i = 0; i < filter.length; i++) {
            final double value = filter[i];
            safe[i] = Double.isFinite(value) && value > 0.0d ? value : 0.0d;
        }
        return safe;
    }

    private static double meanZeroFraction(final double[] values) {
        int zeros = 0;
        for (double value : values) {
            if (value == 0.0d) {
                zeros++;
            }
        }
        return values.length == 0 ? 0.0d : ((double) zeros / (double) values.length);
    }

    private static double[] sequence(final double start, final double end, final int length) {
        final double[] values = new double[length];
        if (length == 1) {
            values[0] = start;
            return values;
        }
        for (int i = 0; i < length; i++) {
            values[i] = start + ((end - start) * i / (length - 1));
        }
        return values;
    }

    private static double[] smoothRejectionCurve(final double[] theta, final double[] numRej) {
        try {
            if (theta.length != numRej.length || theta.length == 0) {
                return new double[0];
            }
            final double range = theta[theta.length - 1] - theta[0];
            final double delta = range > 0.0d ? LOWESS_DELTA_FRACTION * range : 0.0d;
            return lowess(theta, numRej, FILTER_SMOOTHING_SPAN, 3, delta);
        } catch (RuntimeException ex) {
            final double[] copy = new double[numRej.length];
            System.arraycopy(numRej, 0, copy, 0, numRej.length);
            return copy;
        }
    }

    private static double[] lowess(final double[] x,
                                   final double[] y,
                                   final double f,
                                   final int nsteps,
                                   final double delta) {
        return lowess(x, y, f, nsteps, delta, null);
    }

    private static double[] lowess(final double[] x,
                                   final double[] y,
                                   final double f,
                                   final int nsteps,
                                   final double delta,
                                   final double[] priorWeights) {
        final int n = x.length;
        final double[] ys = Arrays.copyOf(y, n);
        if (n < 2) {
            return ys;
        }

        final int ns = Math.max(2, Math.min(n, (int) (f * n + 1.0e-7d)));
        final double[] robustnessWeights = new double[n];
        final double[] residuals = new double[n];
        final double[] weights = new double[n];

        int iter = 1;
        while (iter <= nsteps + 1) {
            int nleft = 0;
            int nright = ns - 1;
            int last = -1;
            int i = 0;

            while (true) {
                if (nright < n - 1) {
                    final double d1 = x[i] - x[nleft];
                    final double d2 = x[nright + 1] - x[i];
                    if (d1 > d2) {
                        nleft++;
                        nright++;
                        continue;
                    }
                }

                final LowessPointFit fit = lowessPoint(x, y, x[i], nleft, nright, weights, iter > 1, robustnessWeights, priorWeights);
                ys[i] = fit.ok ? fit.y : y[i];

                if (last < i - 1) {
                    final double denom = x[i] - x[last];
                    for (int j = last + 1; j < i; j++) {
                        final double alpha = Math.abs(denom) <= EPS ? 0.0d : (x[j] - x[last]) / denom;
                        ys[j] = alpha * ys[i] + (1.0d - alpha) * ys[last];
                    }
                }

                last = i;
                final double cut = x[last] + delta;
                int nextI;
                for (nextI = last + 1; nextI < n; nextI++) {
                    if (x[nextI] > cut) {
                        break;
                    }
                    if (x[nextI] == x[last]) {
                        ys[nextI] = ys[last];
                        last = nextI;
                    }
                }
                i = Math.max(last + 1, nextI - 1);
                if (last >= n - 1) {
                    break;
                }
            }

            double scale = 0.0d;
            for (int j = 0; j < n; j++) {
                residuals[j] = y[j] - ys[j];
                scale += Math.abs(residuals[j]);
            }
            scale /= n;

            if (iter > nsteps) {
                break;
            }

            final double[] absResiduals = new double[n];
            for (int j = 0; j < n; j++) {
                absResiduals[j] = Math.abs(residuals[j]);
            }
            Arrays.sort(absResiduals);

            final double cmad;
            if ((n & 1) == 0) {
                final int m1 = n / 2;
                final int m2 = n - m1 - 1;
                cmad = 3.0d * (absResiduals[m1] + absResiduals[m2]);
            } else {
                cmad = 6.0d * absResiduals[n / 2];
            }

            if (cmad < 1.0e-7d * scale) {
                break;
            }

            final double c9 = 0.999d * cmad;
            final double c1 = 0.001d * cmad;
            for (int j = 0; j < n; j++) {
                final double r = Math.abs(residuals[j]);
                if (r <= c1) {
                    robustnessWeights[j] = 1.0d;
                } else if (r <= c9) {
                    final double value = 1.0d - square(r / cmad);
                    robustnessWeights[j] = square(value);
                } else {
                    robustnessWeights[j] = 0.0d;
                }
            }
            iter++;
        }

        return ys;
    }

    private static LowessPointFit lowessPoint(final double[] x,
                                              final double[] y,
                                              final double xs,
                                              final int nleft,
                                              final int nright,
                                              final double[] weights,
                                              final boolean useRobustnessWeights,
                                              final double[] robustnessWeights,
                                              final double[] priorWeights) {
        final int n = x.length;
        final double range = x[n - 1] - x[0];
        final double h = Math.max(xs - x[nleft], x[nright] - xs);
        final double h9 = 0.999d * h;
        final double h1 = 0.001d * h;

        double weightSum = 0.0d;
        int j = nleft;
        while (j < n) {
            weights[j] = 0.0d;
            final double r = Math.abs(x[j] - xs);
            if (r <= h9) {
                if (r <= h1) {
                    weights[j] = 1.0d;
                } else {
                    weights[j] = cube(1.0d - cube(r / h));
                }
                if (priorWeights != null) {
                    weights[j] *= Math.max(0.0d, priorWeights[j]);
                }
                if (useRobustnessWeights) {
                    weights[j] *= robustnessWeights[j];
                }
                weightSum += weights[j];
            } else if (x[j] > xs) {
                break;
            }
            j++;
        }

        final int nrt = j - 1;
        if (!(weightSum > 0.0d)) {
            return new LowessPointFit(Double.NaN, false);
        }

        for (j = nleft; j <= nrt; j++) {
            weights[j] /= weightSum;
        }
        if (h > 0.0d) {
            double weightedCenter = 0.0d;
            for (j = nleft; j <= nrt; j++) {
                weightedCenter += weights[j] * x[j];
            }
            double b = xs - weightedCenter;
            double c = 0.0d;
            for (j = nleft; j <= nrt; j++) {
                c += weights[j] * square(x[j] - weightedCenter);
            }
            if (Math.sqrt(c) > 0.001d * range) {
                b /= c;
                for (j = nleft; j <= nrt; j++) {
                    weights[j] *= (b * (x[j] - weightedCenter)) + 1.0d;
                }
            }
        }

        double ys = 0.0d;
        for (j = nleft; j <= nrt; j++) {
            ys += weights[j] * y[j];
        }
        return new LowessPointFit(ys, true);
    }

    private static double square(final double value) {
        return value * value;
    }

    private static double cube(final double value) {
        return value * value * value;
    }

    private static double[] positiveResiduals(final double[] numRej, final double[] smooth) {
        int count = 0;
        for (double value : numRej) {
            if (value > 0.0d) {
                count++;
            }
        }
        if (count == 0) {
            return new double[]{0.0d};
        }

        final double[] residual = new double[count];
        int pos = 0;
        for (int i = 0; i < numRej.length; i++) {
            if (numRej[i] > 0.0d) {
                residual[pos++] = numRej[i] - smooth[i];
            }
        }
        return residual;
    }

    private static double rmse(final double[] values) {
        if (values.length == 0) {
            return 0.0d;
        }
        double sumSq = 0.0d;
        for (double value : values) {
            sumSq += value * value;
        }
        return Math.sqrt(sumSq / values.length);
    }

    private static double countRejections(final double[] adjustedPValues, final double alpha) {
        int count = 0;
        for (double value : adjustedPValues) {
            if (Double.isFinite(value) && value < alpha) {
                count++;
            }
        }
        return count;
    }

    private static boolean anyGreaterThan(final double[] values, final double threshold) {
        for (double value : values) {
            if (value > threshold) {
                return true;
            }
        }
        return false;
    }

    private static int firstGreaterThan(final double[] values, final double threshold) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] > threshold) {
                return i;
            }
        }
        return 0;
    }

    private static double quantile(final double[] values, final double probability) {
        final double[] sorted = values.clone();
        Arrays.sort(sorted);
        if (sorted.length == 0) {
            return Double.NaN;
        }
        if (probability <= 0.0d) {
            return sorted[0];
        }
        if (probability >= 1.0d) {
            return sorted[sorted.length - 1];
        }

        final double h = 1.0d + ((sorted.length - 1.0d) * probability);
        final int lower = (int) Math.floor(h) - 1;
        final double fraction = h - Math.floor(h);
        final double lowerValue = sorted[Math.max(0, lower)];
        final double upperValue = sorted[Math.min(sorted.length - 1, lower + 1)];
        return lowerValue + fraction * (upperValue - lowerValue);
    }

    private static double medianOfSorted(final double[] values, final int length) {
        if (length <= 0) {
            return Double.NaN;
        }
        if ((length & 1) == 1) {
            return values[length / 2];
        }
        return (values[(length / 2) - 1] + values[length / 2]) / 2.0d;
    }

    private static double trimmedMean(final double[] values, final int length, final double trim) {
        if (length <= 0) {
            return Double.NaN;
        }
        final double[] copy = Arrays.copyOf(values, length);
        Arrays.sort(copy);
        final int trimCount = (int) Math.floor(trim * length);
        final int start = Math.min(trimCount, length - 1);
        final int end = Math.max(start + 1, length - trimCount);
        double sum = 0.0d;
        int count = 0;
        for (int i = start; i < end; i++) {
            sum += copy[i];
            count++;
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    private static double trimRatioForSampleCount(final int sampleCount) {
        if (sampleCount <= 3) {
            return 1.0d / 3.0d;
        }
        if (sampleCount <= 23) {
            return 1.0d / 4.0d;
        }
        return 1.0d / 8.0d;
    }

    private static double scaleForTrimmedVariance(final int sampleCount) {
        if (sampleCount <= 3) {
            return 2.04d;
        }
        if (sampleCount <= 23) {
            return 1.86d;
        }
        return 1.51d;
    }

    private static double[] createConditionVector(final Template template, final int numCols) {
        final double[] condition = new double[numCols];
        Arrays.fill(condition, Double.NaN);

        final int classOfInterestIndex = template.getClassOfInterestIndex();
        final int interestClass = classOfInterestIndex == 0 ? 0 : 1;
        final int referenceClass = interestClass == 0 ? 1 : 0;

        final Template.Class reference = template.getClass(referenceClass);
        for (Template.Item item : reference.getItemsOrderedByProfilePos()) {
            final int index = item.getProfilePosition();
            if (index >= 0 && index < numCols) {
                condition[index] = 0.0d;
            }
        }

        final Template.Class interest = template.getClass(interestClass);
        for (Template.Item item : interest.getItemsOrderedByProfilePos()) {
            final int index = item.getProfilePosition();
            if (index >= 0 && index < numCols) {
                condition[index] = 1.0d;
            }
        }
        return condition;
    }

    private boolean contrastAllZeroForRow(final int rowIndex, final double[] condition) {
        final Vector row = fitDs.getRow(rowIndex);
        boolean anySelected = false;
        boolean selectedAllZero = true;
        boolean anyPositive = false;

        for (int columnIndex = 0; columnIndex < fitDs.getNumCol(); columnIndex++) {
            final double value = row.getElement(columnIndex);
            if (!Double.isFinite(value) || value < 0.0d) {
                continue;
            }

            if (value > 0.0d) {
                anyPositive = true;
            }

            if (!Double.isFinite(condition[columnIndex])) {
                continue;
            }

            anySelected = true;
            if (value != 0.0d) {
                selectedAllZero = false;
            }
        }

        return anySelected && selectedAllZero && anyPositive;
    }

    private static double sampleVariance(final double[] values) {
        double sum = 0.0d;
        int count = 0;
        for (double value : values) {
            if (Double.isFinite(value)) {
                sum += value;
                count++;
            }
        }
        if (count < 2) {
            return Double.NaN;
        }

        final double mean = sum / count;
        double sumSq = 0.0d;
        for (double value : values) {
            if (Double.isFinite(value)) {
                final double delta = value - mean;
                sumSq += delta * delta;
            }
        }
        return sumSq / (count - 1);
    }

    private static double robustMadSd(final double[] values, final int length) {
        if (length <= 0) {
            return Double.NaN;
        }
        final double[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        final double median = medianOfSorted(sorted, length);
        final double[] absDev = new double[length];
        for (int i = 0; i < length; i++) {
            absDev[i] = Math.abs(sorted[i] - median);
        }
        Arrays.sort(absDev);
        final double mad = medianOfSorted(absDev, length);
        final double sd = mad / ROBUST_NORMAL_SD_QUANTILE;
        return Double.isFinite(sd) && sd >= 0.0d ? sd : 0.0d;
    }

    private static double twoSidedPValueFromZ(final double z) {
        if (!Double.isFinite(z)) {
            return Double.NaN;
        }
        final double p = Erf.erfc(Math.abs(z) / Math.sqrt(2.0d));
        return clamp(p, 0.0d, 1.0d);
    }

    private static double max(final double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }
}