/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mit.broad.genome.alg.DatasetStatsCore.TwoClassMarkerStats;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateImplFromSampleNames;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.special.Gamma;
import org.junit.jupiter.api.Test;

class Deseq2LikeRegressionZModelTest {

    /**
     * End-to-end invariant port of the {@code results()} assertions in
     * <a href="https://github.com/thelovelab/DESeq2/blob/devel/tests/testthat/test_results.R">
     * thelovelab/DESeq2 {@code tests/testthat/test_results.R}</a>:
     *
     * <pre>{@code
     *   counts(dds)[1,] <- rep(c(100L, 200L, 800L), each=4)
     *   sizeFactors(dds) <- rep(1, ncol(dds))
     *   dds <- DESeq(dds)
     *   expect_equal(results(dds, contrast=c("condition","1","3"))[1,2], -3, tolerance=1e-6)
     *   expect_equal(results(dds, contrast=c("condition","1","2"))[1,2], -1, tolerance=1e-6)
     *   expect_equal(results(dds, contrast=c("condition","2","3"))[1,2], -2, tolerance=1e-6)
     * }</pre>
     *
     * <p>The negative-binomial GLM MLE of the group mean equals the sample mean of that group,
     * so when counts are constant within each group and size factors are unity the log-2 fold
     * change reduces to {@code log2(mean_interest / mean_reference)} exactly and
     * <em>independently</em> of the dispersion estimate. To reproduce DESeq2's upstream invariant
     * without pre-setting size factors manually we construct a balanced count matrix where every
     * "background" gene has an identical count across all samples, which forces the
     * median-of-ratios size factor to {@code 1.0} exactly (matching
     * {@code sizeFactors(dds) <- rep(1, ncol(dds))}). This exercises the full
     * {@link Deseq2LikeRegressionZModel#fit} + dispersion-trend + Wald +
     * {@link Deseq2LikeRegressionZModel#computeMainStatsForTemplate} pipeline without any
     * external reference fixtures.
     *
     * <p>GSEA's two-class convention treats the <em>first</em> template class as the "interest"
     * (numerator) class and the second as the "reference" (denominator) class, so the Wald beta /
     * reported log2 fold change is {@code log2(mean_A / mean_B)}.
     */
    @Test
    void mainStats_reproducesDeseq2ConstantWithinGroupLfcInvariant() {
        final List<String> features = new ArrayList<String>();
        features.add("UP_3X");
        features.add("UP_1X");
        features.add("DOWN_2X");
        features.add("NULL_SPIKE");
        final int backgroundGeneCount = 24;
        for (int i = 1; i <= backgroundGeneCount; i++) {
            features.add(String.format("BG_%02d", i));
        }

        final int samplesPerGroup = 4;
        final int totalSamples = samplesPerGroup * 2;
        final float[][] counts = new float[features.size()][totalSamples];

        // Spike rows: constant within each group, so the NB GLM MLE of each group mean equals the
        // raw group mean and (with unit size factors) LFC = log2(mean_A / mean_B) exactly.
        for (int s = 0; s < samplesPerGroup; s++) {
            counts[0][s] = 100f;
            counts[0][s + samplesPerGroup] = 800f;
            counts[1][s] = 200f;
            counts[1][s + samplesPerGroup] = 400f;
            counts[2][s] = 400f;
            counts[2][s + samplesPerGroup] = 100f;
            counts[3][s] = 300f;
            counts[3][s + samplesPerGroup] = 300f;
        }

        // Background rows: strictly constant across ALL samples (gene-specific baseline). Each
        // such gene contributes ratio=1 at every sample when computing median-of-ratios size
        // factors, so with 24 background rows vs 4 spike rows the per-sample median is exactly
        // 1.0 regardless of the spike ratios. This reproduces DESeq2's explicit
        // sizeFactors(dds) <- rep(1, ncol(dds)) precondition.
        for (int gene = 0; gene < backgroundGeneCount; gene++) {
            final int baseline = 200 + (gene % 11) * 25;
            for (int sample = 0; sample < totalSamples; sample++) {
                counts[4 + gene][sample] = baseline;
            }
        }

        final List<String> samples = Arrays.asList(
                "A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4");
        final Dataset dataset = createDataset(counts, features, samples);
        final Template template = createTemplate(dataset,
                "A", new String[] { "A1", "A2", "A3", "A4" },
                "B", new String[] { "B1", "B2", "B3", "B4" });
        final Map<String, TwoClassMarkerStats> markerScores = createMarkerScores(dataset);

        final Deseq2LikeRegressionZModel model =
                Deseq2LikeRegressionZModel.fit(dataset, template, markerScores);

        // Precondition: the construction above must drive median-of-ratios size factors to unity,
        // matching DESeq2's test_size_factor.R "balanced library" invariant.
        final double[] sizeFactors = (double[]) readPrivateObjectField(model, "sizeFactors");
        for (int i = 0; i < sizeFactors.length; i++) {
            assertEquals(1.0d, sizeFactors[i], 1.0e-12d,
                    "Median-of-ratios SF for balanced libraries must be exactly 1.0 (sample " + i + ")");
        }

        final List<Deseq2LikeRegressionZModel.MainStat> rows =
                model.computeMainStatsForTemplate(template, markerScores);

        final double invLog2 = 1.0d / Math.log(2.0d);
        final double lfcUp3x = findRow(rows, "UP_3X").log2FoldChange;
        final double lfcUp1x = findRow(rows, "UP_1X").log2FoldChange;
        final double lfcDown2x = findRow(rows, "DOWN_2X").log2FoldChange;
        final double lfcNull = findRow(rows, "NULL_SPIKE").log2FoldChange;

        // LFC = log2(mean_A / mean_B) under GSEA's first-class-is-interest convention. Tolerance
        // is 1e-6 to match DESeq2's own test_results.R expect_equal(..., tolerance=1e-6).
        final double tol = 1.0e-6d;
        assertEquals(Math.log(100.0d / 800.0d) * invLog2, lfcUp3x, tol,
                "UP_3X: LFC must equal log2(100/800) = -3 (DESeq2 test_results.R invariant)");
        assertEquals(Math.log(200.0d / 400.0d) * invLog2, lfcUp1x, tol,
                "UP_1X: LFC must equal log2(200/400) = -1");
        assertEquals(Math.log(400.0d / 100.0d) * invLog2, lfcDown2x, tol,
                "DOWN_2X: LFC must equal log2(400/100) = +2");
        assertEquals(0.0d, lfcNull, tol,
                "NULL_SPIKE: equal-count rows must give LFC = 0 within DESeq2's 1e-6 tolerance");

        // Wald statistic sign must agree with LFC sign (stat = beta / SE).
        assertTrue(findRow(rows, "UP_3X").stat < 0.0d, "UP_3X Wald stat must be negative");
        assertTrue(findRow(rows, "UP_1X").stat < 0.0d, "UP_1X Wald stat must be negative");
        assertTrue(findRow(rows, "DOWN_2X").stat > 0.0d, "DOWN_2X Wald stat must be positive");

        // Contrast symmetry: swapping reference and interest negates the LFC exactly (direct
        // port of the "results(contrast=c('condition','1','3')) == -results(...,'3','1')"
        // identity asserted in DESeq2's test_results.R).
        final Template flipped = createTemplate(dataset,
                "B", new String[] { "B1", "B2", "B3", "B4" },
                "A", new String[] { "A1", "A2", "A3", "A4" });
        final Map<String, TwoClassMarkerStats> flippedScores = createMarkerScores(dataset);
        final Deseq2LikeRegressionZModel flippedModel =
                Deseq2LikeRegressionZModel.fit(dataset, flipped, flippedScores);
        final List<Deseq2LikeRegressionZModel.MainStat> flippedRows =
                flippedModel.computeMainStatsForTemplate(flipped, flippedScores);
        assertEquals(-lfcUp3x, findRow(flippedRows, "UP_3X").log2FoldChange, tol,
                "Contrast symmetry: swapping reference/interest must negate the LFC");
        assertEquals(-lfcDown2x, findRow(flippedRows, "DOWN_2X").log2FoldChange, tol,
                "Contrast symmetry (down-regulated row)");
    }


    @Test
    void fit_rejectsNonIntegerCounts() {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 10.5f, 12.0f, 9.0f, 40.0f, 42.0f, 44.0f }
                },
                Arrays.asList("geneA"));

        final Template template = createTemplate(dataset);

        assertThrows(IllegalArgumentException.class,
                () -> Deseq2LikeRegressionZModel.fit(dataset, template, createMarkerScores(dataset)));
    }

    @Test
    void computeSizeFactors_fallsBackToPoscountsWhenNoGeneIsPositiveInAllSamples() throws Exception {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 10f, 0f, 8f, 0f, 12f, 0f },
                        { 0f, 9f, 0f, 11f, 0f, 13f }
                },
                Arrays.asList("geneA", "geneB"));

        final Method computeSizeFactors = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "computeSizeFactors",
                Dataset.class);
        computeSizeFactors.setAccessible(true);
        final double[] sizeFactors = (double[]) invokeStaticObject(computeSizeFactors, dataset);
        assertEquals(6, sizeFactors.length);
        for (final double sf : sizeFactors) {
            assertTrue(Double.isFinite(sf));
            assertTrue(sf > 0.0d);
        }
    }

    @Test
    void fit_rejectsWhenEveryGeneIsAllZero() {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 0f, 0f, 0f },
                        { 0f, 0f, 0f }
                },
                Arrays.asList("geneA", "geneB"),
                Arrays.asList("A1", "A2", "B1"));

        final Template template = createTemplate(dataset, "A", new String[] { "A1", "A2" }, "B", new String[] { "B1" });
        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Deseq2LikeRegressionZModel.fit(dataset, template, createMarkerScores(dataset)));

        assertEquals(
                "every gene contains at least one zero, cannot compute size factors (poscounts)",
                error.getMessage());
    }

    @Test
    void computeSizeFactors_matchesDeseq2MedianRatioForEvenRowCount() throws Exception {
    final Dataset dataset = createDataset(
        new float[][] {
            { 1f, 1f },
            { 4f, 1f },
            { 16f, 1f },
            { 64f, 1f }
        },
        Arrays.asList("geneA", "geneB", "geneC", "geneD"),
        Arrays.asList("S1", "S2"));

    final Method computeSizeFactors = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
        "computeSizeFactors",
        Dataset.class);
    computeSizeFactors.setAccessible(true);

    final double[] sizeFactors = (double[]) invokeStaticObject(computeSizeFactors, dataset);

    assertEquals(Math.sqrt(8.0d), sizeFactors[0], 1.0e-12d);
    assertEquals(Math.sqrt(0.125d), sizeFactors[1], 1.0e-12d);
    }

    @Test
    void computeMainStats_masksCooksOutliersAndNeutralizesRanking() {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 1000f, 10f, 10f, 10f, 10f, 10f },
                        { 12f, 11f, 13f, 120f, 122f, 118f },
                        { 30f, 28f, 32f, 34f, 31f, 33f }
                },
                Arrays.asList("outlierGene", "signalGene", "backgroundGene"));

        final Template template = createTemplate(dataset);
        final Map<String, TwoClassMarkerStats> markerScores = createMarkerScores(dataset);

        final Deseq2LikeRegressionZModel model = Deseq2LikeRegressionZModel.fit(dataset, template, markerScores);
        final List<Deseq2LikeRegressionZModel.MainStat> rows = model.computeMainStatsForTemplate(template, markerScores);
        final Deseq2LikeRegressionZModel.MainStat outlierRow = findRow(rows, "outlierGene");
        final Deseq2LikeRegressionZModel.MainStat signalRow = findRow(rows, "signalGene");

        assertTrue(outlierRow.cooksOutlier);
        assertTrue(Double.isFinite(outlierRow.maxCooks));
        assertTrue(Double.isNaN(outlierRow.pvalue));
        assertTrue(Double.isNaN(outlierRow.padj));

        assertFalse(signalRow.cooksOutlier);
        assertTrue(Double.isFinite(signalRow.pvalue));

        final ScoredDataset scored = model.scoreForTemplate(template, edu.mit.broad.genome.math.SortMode.REAL,
                edu.mit.broad.genome.math.Order.DESCENDING, markerScores);
        assertEquals(0.0d, scored.getScore("outlierGene"), 1.0e-6d);
    }

        @Test
        void computeMainStats_zeroesContrastWithOnlyZeroCountsInComparedGroups() {
        final Dataset dataset = createDataset(
            new float[][] {
                    { 50f, 55f, 48f, 52f, 60f, 58f, 63f, 61f },
                { 100f, 110f, 0f, 0f, 90f, 95f, 0f, 0f },
                { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f }
            },
                Arrays.asList("sizeFactorAnchor", "contrastZeroGene", "allZeroGene"),
            Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2", "D1", "D2"));

        final Template template = createTemplate(dataset, "B", new String[] { "B1", "B2" }, "D",
            new String[] { "D1", "D2" });
        final Map<String, TwoClassMarkerStats> markerScores = createMarkerScores(dataset);

        final Deseq2LikeRegressionZModel model = Deseq2LikeRegressionZModel.fit(dataset, template, markerScores);
        final List<Deseq2LikeRegressionZModel.MainStat> rows = model.computeMainStatsForTemplate(template, markerScores);
        final Deseq2LikeRegressionZModel.MainStat contrastZeroRow = findRow(rows, "contrastZeroGene");
        final Deseq2LikeRegressionZModel.MainStat allZeroRow = findRow(rows, "allZeroGene");

        assertEquals(0.0d, contrastZeroRow.log2FoldChange, 1.0e-12d);
        assertEquals(0.0d, contrastZeroRow.stat, 1.0e-12d);
        assertEquals(1.0d, contrastZeroRow.pvalue, 1.0e-12d);

        assertTrue(Double.isNaN(allZeroRow.log2FoldChange));
        assertTrue(Double.isNaN(allZeroRow.pvalue));

        final ScoredDataset scored = model.scoreForTemplate(template, edu.mit.broad.genome.math.SortMode.REAL,
            edu.mit.broad.genome.math.Order.DESCENDING, markerScores);
        assertEquals(0.0d, scored.getScore("contrastZeroGene"), 1.0e-12d);
        }

        @Test
        void fit_replacesExtremeOutliersWhenReplicateCountAllows() throws Exception {
            final int backgroundGeneCount = 100;
            final int totalGenes = backgroundGeneCount + 3;
            final int samplesPerGroup = 6;
            final int totalSamples = samplesPerGroup * 2;
            final float[][] values = new float[totalGenes][totalSamples];
            final java.util.ArrayList<String> rowNames = new java.util.ArrayList<String>(totalGenes);

            for (int geneIndex = 0; geneIndex < backgroundGeneCount; geneIndex++) {
                rowNames.add("background" + geneIndex);
                final int base = 40 + ((geneIndex % 12) * 6);
                final int groupShift = geneIndex % 5;
                for (int sampleIndex = 0; sampleIndex < totalSamples; sampleIndex++) {
                final int sampleShift = (sampleIndex % 3) - 1;
                final int conditionShift = sampleIndex < samplesPerGroup ? 0 : groupShift;
                values[geneIndex][sampleIndex] = base + sampleShift + conditionShift;
                }
            }

            rowNames.add("allZeroGene");
            rowNames.add("replaceReduceGene");
            rowNames.add("replaceZeroGene");
            Arrays.fill(values[backgroundGeneCount], 0f);
            values[backgroundGeneCount + 1] = new float[] {
                100000f, 10f, 10f, 10f, 10f, 10f,
                10f, 10f, 10f, 10f, 10f, 10f
            };
            values[backgroundGeneCount + 2] = new float[] {
                100000f, 0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f, 0f
            };

            final List<String> sampleNames = Arrays.asList(
                "A1", "A2", "A3", "A4", "A5", "A6",
                "B1", "B2", "B3", "B4", "B5", "B6");
            final String[] referenceSamples = new String[] { "A1", "A2", "A3", "A4", "A5", "A6" };
            final String[] interestSamples = new String[] { "B1", "B2", "B3", "B4", "B5", "B6" };
            final Dataset dataset = createDataset(values, rowNames, sampleNames);

            final Template template = createTemplate(dataset, "A", referenceSamples, "B", interestSamples);
            final Map<String, TwoClassMarkerStats> markerScoresWithReplacement = createMarkerScores(dataset);
            final Map<String, TwoClassMarkerStats> markerScoresWithoutReplacement = createMarkerScores(dataset);

        final Method computeSizeFactors = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "computeSizeFactors",
            Dataset.class);
        computeSizeFactors.setAccessible(true);
        final double[] sizeFactors = (double[]) invokeStaticObject(computeSizeFactors, dataset);

        final Class<?> frozenCtxClass = Class.forName(
            "edu.mit.broad.genome.alg.gsea.Deseq2LikeRegressionZModel$FrozenDispersionContext");
        final Method fitInternal = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "fitInternal",
            Dataset.class,
            Dataset.class,
            Map.class,
            double[].class,
            double[].class,
            boolean[].class,
            boolean[].class,
            boolean.class,
            frozenCtxClass,
            boolean.class,
            double[].class,
            double.class);
        fitInternal.setAccessible(true);

        final double[] condition = new double[] {
            0d, 0d, 0d, 0d, 0d, 0d,
            1d, 1d, 1d, 1d, 1d, 1d
        };
        final boolean[] replaceableSamples = new boolean[dataset.getNumCol()];
        Arrays.fill(replaceableSamples, true);
        final Deseq2LikeRegressionZModel withReplacement = (Deseq2LikeRegressionZModel) invokeStaticObject(
            fitInternal,
            dataset,
            dataset,
            markerScoresWithReplacement,
            sizeFactors,
            condition,
            new boolean[dataset.getNumRow()],
            replaceableSamples.clone(),
            true,
            null,
            true,
            null,
            Double.NaN);
        final Deseq2LikeRegressionZModel withoutReplacement = (Deseq2LikeRegressionZModel) invokeStaticObject(
            fitInternal,
            dataset,
            dataset,
            markerScoresWithoutReplacement,
            sizeFactors,
            condition,
            new boolean[dataset.getNumRow()],
            replaceableSamples.clone(),
            false,
            null,
            true,
            null,
            Double.NaN);

        final List<Deseq2LikeRegressionZModel.MainStat> withReplacementRows =
            withReplacement.computeMainStatsForTemplate(template, markerScoresWithReplacement);
        final List<Deseq2LikeRegressionZModel.MainStat> withoutReplacementRows =
            withoutReplacement.computeMainStatsForTemplate(template, markerScoresWithoutReplacement);

        final Deseq2LikeRegressionZModel.MainStat withAllZero = findRow(withReplacementRows, "allZeroGene");
        final Deseq2LikeRegressionZModel.MainStat withReduced = findRow(withReplacementRows, "replaceReduceGene");
        final Deseq2LikeRegressionZModel.MainStat withZeroed = findRow(withReplacementRows, "replaceZeroGene");
        final Deseq2LikeRegressionZModel.MainStat withoutReduced = findRow(withoutReplacementRows, "replaceReduceGene");
        final Deseq2LikeRegressionZModel.MainStat withoutZeroed = findRow(withoutReplacementRows, "replaceZeroGene");

        assertTrue(Double.isNaN(withAllZero.pvalue));

        assertTrue(Double.isNaN(withoutReduced.pvalue));
        assertTrue(Double.isNaN(withoutZeroed.pvalue));

        assertFalse(withReduced.cooksOutlier);
        assertTrue(Double.isFinite(withReduced.pvalue));
        assertTrue(Math.abs(withReduced.log2FoldChange) < Math.abs(withoutReduced.log2FoldChange));

        assertFalse(withZeroed.cooksOutlier);
        assertEquals(0.0d, withZeroed.log2FoldChange, 1.0e-12d);
        assertEquals(0.0d, withZeroed.stat, 1.0e-12d);
        assertEquals(1.0d, withZeroed.pvalue, 1.0e-12d);
        }

        @Test
        void fitWald_preservesUnclampedMeansForCooksDistance() throws Exception {
        final Dataset dataset = createDataset(
            new float[][] {
                { 100f, 105f, 95f, 98f, 102f, 99f },
                { 0f, 0f, 0f, 1f, 1f, 1f }
            },
            Arrays.asList("anchorGene", "lowMeanGene"));
        final Template template = createTemplate(dataset);
        final Map<String, TwoClassMarkerStats> markerScores = createMarkerScores(dataset);
        final Deseq2LikeRegressionZModel model = Deseq2LikeRegressionZModel.fit(dataset, template, markerScores);

        final Method createConditionVector = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "createConditionVector",
            Template.class,
            int.class);
        createConditionVector.setAccessible(true);
        final double[] condition = (double[]) invokeStaticObject(createConditionVector, template, dataset.getNumCol());
        final double[] sizeFactors = (double[]) readPrivateObjectField(model, "sizeFactors");
        final double[] shrunkDispersion = (double[]) readPrivateObjectField(model, "shrunkDispersion");

        final Method fitMeanParametersForRow = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "fitMeanParametersForRow",
            Dataset.class,
            int.class,
            double[].class,
            double[].class,
            double.class);
        fitMeanParametersForRow.setAccessible(true);

        final Method fitWaldForRow = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "fitWaldForRow",
            Dataset.class,
            int.class,
            double[].class,
            double[].class,
            double.class,
            double.class,
            double.class);
        fitWaldForRow.setAccessible(true);
        final double wideL = 1.0e-6d;

        final Object coefFit = invokeStaticObject(
            fitMeanParametersForRow,
            dataset,
            1,
            condition,
            sizeFactors,
            shrunkDispersion[1]);
        final Object waldFit = invokeStaticObject(
            fitWaldForRow,
            dataset,
            1,
            condition,
            sizeFactors,
            shrunkDispersion[1],
            wideL,
            wideL);

        final double[] workingMuHat = (double[]) readPrivateObjectField(coefFit, "muHat");
        final double[] fittedMuHat = (double[]) readPrivateObjectField(coefFit, "fittedMuHat");
        final double[] cooksMuHat = (double[]) readPrivateObjectField(waldFit, "muHat");

        boolean hasUnclampedLowMean = false;
        boolean differsFromWorkingMu = false;
        for (int i = 0; i < fittedMuHat.length; i++) {
            if (Double.isFinite(fittedMuHat[i]) && fittedMuHat[i] > 0.0d && fittedMuHat[i] < 0.5d) {
            hasUnclampedLowMean = true;
            }
            if (Double.isFinite(workingMuHat[i]) && Double.isFinite(cooksMuHat[i])
                && Math.abs(workingMuHat[i] - cooksMuHat[i]) > 1.0e-9d) {
            differsFromWorkingMu = true;
            }
            assertEquals(fittedMuHat[i], cooksMuHat[i], 1.0e-12d);
        }

        assertTrue(hasUnclampedLowMean);
        assertTrue(differsFromWorkingMu);
        }

    @Test
    void fitMeanParameters_usesClampedMuInWorkingResponse() throws Exception {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 0f, 0f, 0f, 1f, 1f, 1f }
                },
                Arrays.asList("lowMeanGene"));
        final double[] condition = new double[] { 0d, 0d, 0d, 1d, 1d, 1d };
        final double[] sizeFactors = new double[] { 1d, 1d, 1d, 1d, 1d, 1d };
        final double dispersion = 0.2d;
        final double ridge = 1.0e-6d / (Math.log(2.0d) * Math.log(2.0d));

        double beta0 = Math.log(0.1d);
        double beta1 = 0.0d;
        double previousDeviance = Double.NaN;
        for (int iter = 0; iter < 100; iter++) {
            double a00 = ridge;
            double a01 = 0.0d;
            double a11 = ridge;
            double b0 = 0.0d;
            double b1 = 0.0d;

            for (int i = 0; i < condition.length; i++) {
                final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
                final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
                final double weight = mu / (1.0d + dispersion * mu);
                final double z = Math.log(mu / sizeFactors[i]) + (dataset.getRow(0).getElement(i) - mu) / mu;

                a00 += weight;
                a01 += weight * condition[i];
                a11 += weight * condition[i] * condition[i];
                b0 += weight * z;
                b1 += weight * condition[i] * z;
            }

            final double det = a00 * a11 - a01 * a01;
            final double newBeta0 = (b0 * a11 - b1 * a01) / det;
            final double newBeta1 = (a00 * b1 - a01 * b0) / det;

            beta0 = newBeta0;
            beta1 = newBeta1;

            double deviance = 0.0d;
            for (int i = 0; i < condition.length; i++) {
                final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
                final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
                deviance += -2.0d * nbLogLikelihood(dataset.getRow(0).getElement(i), mu, dispersion);
            }

            if (Double.isFinite(previousDeviance)) {
                final double convergence = Math.abs(deviance - previousDeviance) / (Math.abs(deviance) + 0.1d);
                if (convergence < 1.0e-8d) {
                    break;
                }
            }
            previousDeviance = deviance;
        }

        final Method fitMeanParametersForRow = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "fitMeanParametersForRow",
                Dataset.class,
                int.class,
                double[].class,
                double[].class,
                double.class);
        fitMeanParametersForRow.setAccessible(true);

        final Object coefFit = invokeStaticObject(
                fitMeanParametersForRow,
                dataset,
                0,
                condition,
                sizeFactors,
                dispersion);

        assertEquals(beta0, readPrivateDoubleField(coefFit, "beta0"), 1.0e-10d);
        assertEquals(beta1, readPrivateDoubleField(coefFit, "beta1"), 1.0e-10d);
    }

    @Test
    void linearModelMuNormalized_matchesTwoGroupGeneWiseFit() throws Exception {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 10f, 18f, 14f, 40f, 44f, 52f }
                },
                Arrays.asList("geneA"));
        final double[] condition = new double[] { 0d, 0d, 0d, 1d, 1d, 1d };
        final double[] sizeFactors = new double[] { 1d, 2d, 1d, 1d, 2d, 1d };

        final Method linearModelMu = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "linearModelMuNormalizedForRow",
                Dataset.class,
                int.class,
                double[].class,
                double[].class);
        linearModelMu.setAccessible(true);

        final double[] muHat = (double[]) invokeStaticObject(linearModelMu, dataset, 0, condition, sizeFactors);
        final double[] expected = new double[] { 11d, 22d, 11d, 38d, 76d, 38d };
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], muHat[i], 1.0e-10d);
        }
    }

    @Test
    void initialDispersionStart_matchesDESeq2RoughAndMomentsEstimate() throws Exception {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 10f, 18f, 14f, 40f, 44f, 52f }
                },
                Arrays.asList("geneA"));
        final double[] condition = new double[] { 0d, 0d, 0d, 1d, 1d, 1d };
        final double[] sizeFactors = new double[] { 1d, 2d, 1d, 1d, 2d, 1d };
        final double[] normalized = new double[] { 10d, 9d, 14d, 40d, 22d, 52d };
        final double[] fittedNormalized = new double[] { 11d, 11d, 11d, 38d, 38d, 38d };
        final double meanNormCount = 24.5d;
        final double meanInverseSizeFactor = (1d + 0.5d + 1d + 1d + 0.5d + 1d) / 6d;
        final double maxDisp = 10d;

        double roughSum = 0.0d;
        for (int i = 0; i < normalized.length; i++) {
            final double mu = Math.max(1.0d, fittedNormalized[i]);
            roughSum += (((normalized[i] - mu) * (normalized[i] - mu)) - mu) / (mu * mu);
        }
        final double roughDisp = Math.max(roughSum / 4.0d, 0.0d);

        double varianceSum = 0.0d;
        for (double value : normalized) {
            final double delta = value - meanNormCount;
            varianceSum += delta * delta;
        }
        final double baseVariance = varianceSum / (normalized.length - 1.0d);
        final double momentsDisp = (baseVariance - (meanInverseSizeFactor * meanNormCount)) / (meanNormCount * meanNormCount);
        final double expected = Math.max(1.0e-8d, Math.min(Math.min(roughDisp, momentsDisp), maxDisp));

        final Method initialDispersionStart = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "initialDispersionStart",
                Dataset.class,
                int.class,
                double[].class,
                double[].class,
                double.class,
                double.class,
                double.class);
        initialDispersionStart.setAccessible(true);

        final double alphaStart = invokeStaticDouble(
                initialDispersionStart,
                dataset,
                0,
                sizeFactors,
                condition,
                meanNormCount,
                meanInverseSizeFactor,
                maxDisp);
        assertEquals(expected, alphaStart, 1.0e-12d);
    }

    @Test
    void dispersionLineSearch_matchesNumericalPosteriorAndDerivative() throws Exception {
        final Dataset dataset = createDataset(
            new float[][] {
                { 20f, 23f, 18f, 19f, 22f, 31f, 29f, 35f, 28f, 30f }
            },
            Arrays.asList("geneA"),
            Arrays.asList("A1", "A2", "A3", "A4", "A5", "B1", "B2", "B3", "B4", "B5"));
        final double[] condition = new double[] { 0d, 0d, 0d, 0d, 0d, 1d, 1d, 1d, 1d, 1d };
        final double[] muHat = new double[] { 20d, 20d, 20d, 20d, 20d, 30d, 30d, 30d, 30d, 30d };
        final double logAlphaPriorMean = 0.5d;
        final double logAlphaPriorSigmaSq = 1.0d;
        final double maxDisp = 10.0d;
        final double h = 1.0e-5d;

        final Method logPosterior = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "logPosteriorForLogAlpha",
            Dataset.class,
            int.class,
            double[].class,
            double[].class,
            double.class,
            double.class,
            double.class,
            boolean.class);
        logPosterior.setAccessible(true);

        final Method dLogPosterior = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "dLogPosteriorForLogAlpha",
            Dataset.class,
            int.class,
            double[].class,
            double[].class,
            double.class,
            double.class,
            double.class,
            boolean.class);
        dLogPosterior.setAccessible(true);

        final Method fitDispersionLineSearch = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
            "fitDispersionLineSearch",
            Dataset.class,
            int.class,
            double[].class,
            double[].class,
            double.class,
            double.class,
            double.class,
            boolean.class,
            double.class);
        fitDispersionLineSearch.setAccessible(true);

        final double analyticDerivative = invokeStaticDouble(
            dLogPosterior,
            dataset,
            0,
            condition,
            muHat,
            0.0d,
            logAlphaPriorMean,
            logAlphaPriorSigmaSq,
            true);
        final double finiteDifference = (
            invokeStaticDouble(logPosterior, dataset, 0, condition, muHat, h / 2.0d, logAlphaPriorMean, logAlphaPriorSigmaSq, true)
                - invokeStaticDouble(logPosterior, dataset, 0, condition, muHat, -h / 2.0d, logAlphaPriorMean, logAlphaPriorSigmaSq, true))
            / h;
        assertEquals(finiteDifference, analyticDerivative, 1.0e-6d);

        final Object solverFit = invokeStaticObject(
            fitDispersionLineSearch,
            dataset,
            0,
            condition,
            muHat,
            1.0d,
            logAlphaPriorMean,
            logAlphaPriorSigmaSq,
            true,
            maxDisp);
        final double solverAlpha = readPrivateDoubleField(solverFit, "alpha");

        final BrentOptimizer optimizer = new BrentOptimizer(1.0e-12d, 1.0e-14d);
        final UnivariatePointValuePair optimum = optimizer.optimize(
            GoalType.MAXIMIZE,
            new MaxEval(2000),
            new SearchInterval(Math.log(1.0e-9d), Math.log(maxDisp)),
            new UnivariateObjectiveFunction(logAlpha -> invokeStaticDouble(
                logPosterior,
                dataset,
                0,
                condition,
                muHat,
                logAlpha,
                logAlphaPriorMean,
                logAlphaPriorSigmaSq,
                true)));

        assertEquals(optimum.getPoint(), Math.log(solverAlpha), 1.0e-4d);
        assertEquals(0.0d,
            invokeStaticDouble(dLogPosterior,
                dataset,
                0,
                condition,
                muHat,
                Math.log(solverAlpha),
                logAlphaPriorMean,
                logAlphaPriorSigmaSq,
                true),
            2.0e-4d);
    }

    @Test
    void dispersionSearchConstants_matchDeseq2WrapperBounds() throws Exception {
        final java.lang.reflect.Field minDispLineSearch = Deseq2LikeRegressionZModel.class.getDeclaredField("MIN_DISP_LINE_SEARCH");
        minDispLineSearch.setAccessible(true);
        final java.lang.reflect.Field dispersionGridSize = Deseq2LikeRegressionZModel.class.getDeclaredField("DISP_GRID_SIZE");
        dispersionGridSize.setAccessible(true);

        assertEquals(1.0e-9d, ((Double) minDispLineSearch.get(null)).doubleValue(), 1.0e-18d);
        assertEquals(20, ((Integer) dispersionGridSize.get(null)).intValue());
    }

    @Test
    void fitWaldForRow_matchesDeseq2StyleIrlsEquations() throws Exception {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 20f, 23f, 18f, 19f, 22f, 31f, 29f, 35f, 28f, 30f }
                },
                Arrays.asList("geneA"),
                Arrays.asList("A1", "A2", "A3", "A4", "A5", "B1", "B2", "B3", "B4", "B5"));
        final double[] condition = new double[] { 0d, 0d, 0d, 0d, 0d, 1d, 1d, 1d, 1d, 1d };
        final double[] sizeFactors = new double[] { 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d };
        final double dispersion = 0.5d;
        final double ridge = 1.0e-6d / (Math.log(2.0d) * Math.log(2.0d));

        double beta0 = 0.0d;
        double beta1 = 0.0d;
        for (int i = 0; i < condition.length; i++) {
            final double response = Math.log(dataset.getRow(0).getElement(i) + 0.1d);
            if (condition[i] < 0.5d) {
                beta0 += response / 5.0d;
            } else {
                beta1 += response / 5.0d;
            }
        }
        beta1 -= beta0;

        double previousDeviance = Double.NaN;
        for (int iter = 0; iter < 100; iter++) {
            double a00 = ridge;
            double a01 = 0.0d;
            double a11 = ridge;
            double b0 = 0.0d;
            double b1 = 0.0d;

            for (int i = 0; i < condition.length; i++) {
                final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
                final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
                final double weight = mu / (1.0d + dispersion * mu);
                final double z = Math.log(mu / sizeFactors[i]) + (dataset.getRow(0).getElement(i) - mu) / mu;

                a00 += weight;
                a01 += weight * condition[i];
                a11 += weight * condition[i] * condition[i];
                b0 += weight * z;
                b1 += weight * condition[i] * z;
            }

            final double det = a00 * a11 - a01 * a01;
            beta0 = (b0 * a11 - b1 * a01) / det;
            beta1 = (a00 * b1 - a01 * b0) / det;

            double deviance = 0.0d;
            for (int i = 0; i < condition.length; i++) {
                final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
                final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
                deviance += -2.0d * nbLogLikelihood(dataset.getRow(0).getElement(i), mu, dispersion);
            }

            if (Double.isFinite(previousDeviance)) {
                final double convergence = Math.abs(deviance - previousDeviance) / (Math.abs(deviance) + 0.1d);
                if (convergence < 1.0e-8d) {
                    break;
                }
            }
            previousDeviance = deviance;
        }

        double m00 = 0.0d;
        double m01 = 0.0d;
        double m11 = 0.0d;
        for (int i = 0; i < condition.length; i++) {
            final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
            final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
            final double weight = mu / (1.0d + dispersion * mu);
            m00 += weight;
            m01 += weight * condition[i];
            m11 += weight * condition[i] * condition[i];
        }

        final double a00 = m00 + ridge;
        final double a01 = m01;
        final double a11 = m11 + ridge;
        final double det = a00 * a11 - a01 * a01;
        final double inv01 = -a01 / det;
        final double inv11 = a00 / det;
        final double expectedSe1 = Math.sqrt(inv01 * inv01 * m00 + 2.0d * inv01 * inv11 * m01 + inv11 * inv11 * m11);

        final double wideL = 1.0e-6d;
        final Method fitWaldForRow = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "fitWaldForRow",
                Dataset.class,
                int.class,
                double[].class,
                double[].class,
                double.class,
                double.class,
                double.class);
        fitWaldForRow.setAccessible(true);

        final Object fit = invokeStaticObject(
                fitWaldForRow, dataset, 0, condition, sizeFactors, dispersion, wideL, wideL);
        assertEquals(beta1, readPrivateDoubleField(fit, "beta1"), 1.0e-10d);
        assertEquals(expectedSe1, readPrivateDoubleField(fit, "se1"), 1.0e-10d);
        assertEquals(beta1 / expectedSe1, readPrivateDoubleField(fit, "z"), 1.0e-10d);
    }

    @Test
    void fitMeanParameters_fallbackOptimizer_matchesDeseq2BackupOptimPath() throws Exception {
        final Dataset dataset = createDataset(
                new float[][] {
                        { 0f, 0f, 0f, 1.0e14f, 1.0e14f, 1.0e14f }
                },
                Arrays.asList("separatedGene"));
        final double[] condition = new double[] { 0d, 0d, 0d, 1d, 1d, 1d };
        final double[] sizeFactors = new double[] { 1d, 1d, 1d, 1d, 1d, 1d };
        final double dispersion = 0.1d;
        final double logTwo = Math.log(2.0d);
        final double ridge = 1.0e-6d / (Math.log(2.0d) * Math.log(2.0d));

        double sum0 = 0.0d;
        double sum1 = 0.0d;
        for (int i = 0; i < condition.length; i++) {
            final double response = Math.log((dataset.getRow(0).getElement(i) / sizeFactors[i]) + 0.1d);
            if (condition[i] < 0.5d) {
                sum0 += response;
            } else {
                sum1 += response;
            }
        }

        final double initBeta0 = sum0 / 3.0d;
        final double initBeta1 = (sum1 / 3.0d) - initBeta0;

        double beta0 = initBeta0;
        double beta1 = initBeta1;
        double previousDeviance = Double.NaN;
        boolean useCurrentIrlsStartForOptimization = false;

        for (int iter = 0; iter < 100; iter++) {
            double a00 = 0.0d;
            double a01 = 0.0d;
            double a11 = 0.0d;
            double b0 = 0.0d;
            double b1 = 0.0d;

            for (int i = 0; i < condition.length; i++) {
                final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
                final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
                final double weight = mu / (1.0d + dispersion * mu);
                final double z = Math.log(mu / sizeFactors[i]) + (dataset.getRow(0).getElement(i) - mu) / mu;

                a00 += weight;
                a01 += weight * condition[i];
                a11 += weight * condition[i] * condition[i];
                b0 += weight * z;
                b1 += weight * condition[i] * z;
            }

            a00 += ridge;
            a11 += ridge;
            final double det = a00 * a11 - a01 * a01;
            if (!Double.isFinite(det) || Math.abs(det) <= 1.0e-8d) {
                useCurrentIrlsStartForOptimization = false;
                break;
            }

            final double newBeta0 = (b0 * a11 - b1 * a01) / det;
            final double newBeta1 = (a00 * b1 - a01 * b0) / det;
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

            double deviance = 0.0d;
            for (int i = 0; i < condition.length; i++) {
                final double eta = beta0 + beta1 * condition[i] + Math.log(sizeFactors[i]);
                final double mu = Math.max(0.5d, Math.exp(Math.max(-30.0d, Math.min(30.0d, eta))));
                deviance += -2.0d * nbLogLikelihood(dataset.getRow(0).getElement(i), mu, dispersion);
            }

            if (!Double.isFinite(deviance)) {
                useCurrentIrlsStartForOptimization = false;
                break;
            }
            useCurrentIrlsStartForOptimization = true;

            if (Double.isFinite(previousDeviance)) {
                final double convergence = Math.abs(deviance - previousDeviance) / (Math.abs(deviance) + 0.1d);
                if (Double.isFinite(convergence) && convergence < 1.0e-8d) {
                    break;
                }
            }
            previousDeviance = deviance;
        }

        assertFalse(useCurrentIrlsStartForOptimization);

        // DESeq2 fitNbinomGLMsOptim: p is log2-scale; natural-log betas are p * log(2).
        // Reference optimum (BOBYQA) from QR init converted to log2 start â€” same as Java fallback when IRLS aborts early.
        final double[] start = new double[] {
                Math.max(-30.0d + 1.0e-6d, Math.min(30.0d - 1.0e-6d, initBeta0 / logTwo)),
                Math.max(-30.0d + 1.0e-6d, Math.min(30.0d - 1.0e-6d, initBeta1 / logTwo))
        };
        final MultivariateFunction objective = point -> {
            double logLikelihood = 0.0d;
            for (int i = 0; i < condition.length; i++) {
                final double eta = point[0] * logTwo + point[1] * logTwo * condition[i]
                        + Math.log(sizeFactors[i]);
                final double mu = Math.exp(eta);
                if (!Double.isFinite(mu) || mu <= 0.0d) {
                    return Double.POSITIVE_INFINITY;
                }
                logLikelihood += nbLogLikelihood(dataset.getRow(0).getElement(i), mu, dispersion);
            }

            final double posterior = logLikelihood - (0.5d * 1.0e-6d * ((point[0] * point[0]) + (point[1] * point[1])));
            return Double.isFinite(posterior) ? -posterior : Double.POSITIVE_INFINITY;
        };

        final PointValuePair optimum = new BOBYQAOptimizer(5).optimize(
                new MaxEval(2000),
                new ObjectiveFunction(objective),
                GoalType.MINIMIZE,
                new InitialGuess(start),
                new SimpleBounds(new double[] { -30.0d, -30.0d }, new double[] { 30.0d, 30.0d }));

        final double expectedBeta0 = optimum.getPoint()[0] * logTwo;
        final double expectedBeta1 = optimum.getPoint()[1] * logTwo;

        final Method dampedNewtonFitForRow = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "dampedNewtonFitForRow",
                Dataset.class,
                int.class,
                double[].class,
                double[].class,
                double.class,
                double.class,
                double.class,
                double.class,
                double.class);
        dampedNewtonFitForRow.setAccessible(true);
        final double wideL = 1.0e-6d;

        final Object fit = invokeStaticObject(
                dampedNewtonFitForRow,
                dataset,
                0,
                condition,
                sizeFactors,
                dispersion,
                initBeta0,
                initBeta1,
                wideL,
                wideL);

        // DESeq2: optim L-BFGS-B; our replacement is damped Newton with the analytical observed NB Hessian,
        // which is exact Fisher scoring for the strictly-concave 2D NB GLM + ridge prior and therefore
        // reaches the same global optimum as R's Fortran L-BFGS-B fallback on this separated-counts case.
        final double actualBeta0 = readPrivateDoubleField(fit, "beta0");
        final double actualBeta1 = readPrivateDoubleField(fit, "beta1");
        assertTrue(Double.isFinite(actualBeta0) && Double.isFinite(actualBeta1));
        assertTrue(Math.abs(actualBeta0) <= (30.0d * logTwo) + 1.0e-4d);
        assertTrue(Math.abs(actualBeta1) <= (30.0d * logTwo) + 1.0e-4d);
        double llA = 0.0d;
        double llB = 0.0d;
        for (int i = 0; i < condition.length; i++) {
            final double muA = Math.exp(actualBeta0 + actualBeta1 * condition[i] + Math.log(sizeFactors[i]));
            final double muB = Math.exp(expectedBeta0 + expectedBeta1 * condition[i] + Math.log(sizeFactors[i]));
            llA += nbLogLikelihood(dataset.getRow(0).getElement(i), muA, dispersion);
            llB += nbLogLikelihood(dataset.getRow(0).getElement(i), muB, dispersion);
        }
        final double logPostA = llA - (0.5d * 1.0e-6d * (Math.pow(actualBeta0 / logTwo, 2.0d) + Math.pow(actualBeta1 / logTwo, 2.0d)));
        final double logPostB = llB - (0.5d * 1.0e-6d * (Math.pow(expectedBeta0 / logTwo, 2.0d) + Math.pow(expectedBeta1 / logTwo, 2.0d)));
        assertTrue(logPostA >= logPostB - 1.0e-6d * (1.0d + Math.abs(logPostB)));
    }

    @Test
    void fitDispersionTrend_matchesParametricReferenceCase() throws Exception {
        final Method fitDispersionTrend = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "fitDispersionTrend",
                double[].class,
                double[].class);
        fitDispersionTrend.setAccessible(true);

        final double[] means = new double[] { 5d, 7d, 10d, 15d, 25d, 40d, 65d, 100d, 160d, 250d };
        final double[] alphaRaw = new double[] { 5.1d, 4.0d, 3.4d, 2.6d, 2.05d, 1.82d, 1.62d, 1.50d, 1.58d, 1.76d };

        final double[] fit = (double[]) invokeStaticObject(fitDispersionTrend, means, alphaRaw);

        assertEquals(1.4485337835228993d, fit[0], 1.0e-12d);
        assertEquals(17.82157256074167d, fit[1], 1.0e-12d);
    }

    @Test
    void fitDispersionTrendValues_fallsBackToLocalFitWhenParametricFitFails() throws Exception {
        final Method fitDispersionTrend = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "fitDispersionTrend",
                double[].class,
                double[].class);
        fitDispersionTrend.setAccessible(true);

        final Method fitDispersionTrendValues = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "fitDispersionTrendValues",
                double[].class,
                double[].class);
        fitDispersionTrendValues.setAccessible(true);

        final double[] means = new double[] { 10d, 10d, 10d, 10d };
        final double[] alphaRaw = new double[] { 0.20d, 0.40d, 0.60d, 0.80d };

        assertThrows(AssertionError.class, () -> invokeStaticObject(fitDispersionTrend, means, alphaRaw));

        final double[] fit = (double[]) invokeStaticObject(fitDispersionTrendValues, means, alphaRaw);
        for (double value : fit) {
            assertTrue(Double.isFinite(value));
            assertTrue(value > 0.0d);
        }
        assertEquals(fit[0], fit[1], 1.0e-12d);
        assertEquals(fit[1], fit[2], 1.0e-12d);
        assertEquals(fit[2], fit[3], 1.0e-12d);
    }

    @Test
    void fitLocalDispersionTrend_matchesWeightedLocalQuadraticReferenceCase() throws Exception {
        final Method fitLocalDispersionTrend = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "fitLocalDispersionTrend",
                double[].class,
                double[].class);
        fitLocalDispersionTrend.setAccessible(true);

        final double[] means = new double[7];
        final double[] alphaRaw = new double[7];
        final double[] expected = new double[7];
        final double[] logMeans = new double[] { -3.0d, -2.0d, -1.0d, 0.0d, 1.0d, 2.0d, 3.0d };
        for (int i = 0; i < logMeans.length; i++) {
            means[i] = Math.exp(logMeans[i]);
            final double logDispersion = 0.4d + (0.25d * logMeans[i]) - (0.08d * logMeans[i] * logMeans[i]);
            expected[i] = Math.exp(logDispersion);
            alphaRaw[i] = expected[i];
        }
        alphaRaw[3] = Double.NaN;

        final double[] fit = (double[]) invokeStaticObject(fitLocalDispersionTrend, means, alphaRaw);
        for (int i = 0; i < fit.length; i++) {
            assertEquals(expected[i], fit[i], 1.0e-10d);
        }
    }

    @Test
    void smoothRejectionCurve_matchesRLowessReferenceCase() throws Exception {
        final Method smoothRejectionCurve = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "smoothRejectionCurve",
                double[].class,
                double[].class);
        smoothRejectionCurve.setAccessible(true);

        final double[] theta = new double[] {
                0.00d, 0.05d, 0.10d, 0.15d, 0.20d,
                0.25d, 0.30d, 0.35d, 0.40d, 0.45d,
                0.50d, 0.55d, 0.60d, 0.65d, 0.70d,
                0.75d, 0.80d, 0.85d, 0.90d, 0.95d
        };
        final double[] numRej = new double[] {
                0.0d, 1.0d, 3.0d, 4.0d, 9.0d,
                8.0d, 12.0d, 15.0d, 14.0d, 17.0d,
                21.0d, 20.0d, 19.0d, 23.0d, 28.0d,
                26.0d, 27.0d, 29.0d, 25.0d, 24.0d
        };
        final double[] expected = new double[] {
                -0.11916366647295054d,
                1.2866923935931063d,
                2.7203737437002946d,
                5.117114098623135d,
                7.168113775247277d,
                9.444588192257768d,
                11.732355891978377d,
                13.827188329736737d,
                15.172825530883392d,
                17.265714569269974d,
                19.43797815779426d,
                19.999999999999993d,
                20.562355535732408d,
                23.237080238464884d,
                25.572517690013974d,
                26.760957147016963d,
                27.260925017489036d,
                27.05839366241564d,
                25.809658181231036d,
                23.69188548819945d
        };

        final double[] smooth = (double[]) invokeStaticObject(smoothRejectionCurve, theta, numRej);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], smooth[i], 1.0e-12d);
        }
    }

    @Test
    void independentFilteringResidualMetric_usesRmseNotSampleSd() throws Exception {
        final Method rmse = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "rmse",
                double[].class);
        rmse.setAccessible(true);

        final double[] residual = new double[] { 1.0d, 2.0d, 3.0d };
        final double expected = Math.sqrt((1.0d + 4.0d + 9.0d) / 3.0d);

        assertEquals(expected, invokeStaticDouble(rmse, (Object) residual), 1.0e-12d);
    }

    @Test
    void twoSidedPValueFromZ_preservesExtremeTailProbability() throws Exception {
        final Method twoSidedPValueFromZ = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "twoSidedPValueFromZ",
                double.class);
        twoSidedPValueFromZ.setAccessible(true);

        final double actual = invokeStaticDouble(twoSidedPValueFromZ, 10.0d);

        assertTrue(actual > 0.0d);
        assertEquals(1.523970604832094E-23d, actual, 1.0e-30d);
    }

    @Test
    void robustMadSd_usesTrueMedianForEvenLengthInput() throws Exception {
        final Method robustMadSd = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "robustMadSd",
                double[].class,
                int.class);
        robustMadSd.setAccessible(true);

        final double[] values = new double[] { 1.0d, 2.0d, 3.0d, 100.0d };
        final double expected = referenceRobustMadSd(values);

        assertEquals(expected, invokeStaticDouble(robustMadSd, values, values.length), 1.0e-12d);
    }

    @Test
    void estimateDispersionPriorStats_keepsObservedVarianceUnfloored() throws Exception {
        final Method estimateDispersionPriorStats = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "estimateDispersionPriorStats",
                int.class,
                double[].class,
                double[].class,
                double[].class);
        estimateDispersionPriorStats.setAccessible(true);

        final double[] means = new double[] { 5.0d, 10.0d, 20.0d, 40.0d };
        final double[] trendParams = new double[] { 0.5d, 1.0d };
        final double[] trendValues = new double[means.length];
        final double[] dispersionRaw = new double[means.length];
        for (int i = 0; i < means.length; i++) {
            trendValues[i] = trendParams[0] + (trendParams[1] / means[i]);
            dispersionRaw[i] = trendValues[i];
        }

        final double[] stats = (double[]) invokeStaticObject(
                estimateDispersionPriorStats,
                6,
                means,
                dispersionRaw,
                trendValues);

        assertEquals(0.25d, stats[0], 1.0e-12d);
        assertEquals(0.0d, stats[1], 1.0e-12d);
    }

    @Test
    void estimateDispersionPriorStats_matchesSmallDfReference() throws Exception {
        final Method estimateDispersionPriorStats = Deseq2LikeRegressionZModel.class.getDeclaredMethod(
                "estimateDispersionPriorStats",
                int.class,
                double[].class,
                double[].class,
                double[].class);
        estimateDispersionPriorStats.setAccessible(true);

        final double[] means = new double[] { 4.0d, 6.0d, 8.0d, 12.0d, 18.0d, 30.0d, 50.0d, 80.0d };
        final double[] trendParams = new double[] { 0.7d, 4.0d };
        final double[] trendValues = new double[means.length];
        final double[] residuals = new double[] { -1.1d, -0.6d, -0.2d, 0.1d, 0.45d, 0.7d, 1.0d, 1.3d };
        final double[] dispersionRaw = new double[means.length];
        for (int i = 0; i < means.length; i++) {
            trendValues[i] = referenceTrendDispersion(means[i], trendParams);
            dispersionRaw[i] = trendValues[i] * Math.exp(residuals[i]);
        }

        final double[] expected = referenceDispersionPriorStats(5, means, dispersionRaw, trendValues);
        final double[] actual = (double[]) invokeStaticObject(
                estimateDispersionPriorStats,
                5,
                means,
                dispersionRaw,
                trendValues);

        assertEquals(expected[0], actual[0], 1.0e-12d);
        assertEquals(expected[1], actual[1], 1.0e-12d);
    }

    private static Dataset createDataset(final float[][] values, final List<String> rowNames) {
        return createDataset(values, rowNames, Arrays.asList("A1", "A2", "A3", "B1", "B2", "B3"));
        }

    private static Dataset createDataset(final float[][] values,
                         final List<String> rowNames,
                         final List<String> columnNames) {
        final Matrix matrix = new Matrix(values.length, values[0].length);
        for (int rowIndex = 0; rowIndex < values.length; rowIndex++) {
            for (int columnIndex = 0; columnIndex < values[rowIndex].length; columnIndex++) {
                matrix.setElement(rowIndex, columnIndex, values[rowIndex][columnIndex]);
            }
        }

        return new DefaultDataset(
                "test-dataset",
                matrix,
                rowNames,
            columnNames,
                null);
    }

    private static Template createTemplate(final Dataset dataset) {
        return createTemplate(dataset, "A", new String[] { "A1", "A2", "A3" }, "B", new String[] { "B1", "B2", "B3" });
    }

    private static Template createTemplate(final Dataset dataset,
                                           final String referenceName,
                                           final String[] referenceSamples,
                                           final String interestName,
                                           final String[] interestSamples) {
        return new TemplateImplFromSampleNames(
                "two-class",
                referenceName,
                referenceSamples,
                interestName,
                interestSamples)
                .createTemplate(dataset);
    }

    private static Map<String, TwoClassMarkerStats> createMarkerScores(final Dataset dataset) {
        final Map<String, TwoClassMarkerStats> markerScores = new HashMap<String, TwoClassMarkerStats>();
        for (String rowName : dataset.getRowNames()) {
            markerScores.put(rowName, new TwoClassMarkerStats());
        }
        return markerScores;
    }

    private static Deseq2LikeRegressionZModel.MainStat findRow(
            final List<Deseq2LikeRegressionZModel.MainStat> rows,
            final String feature) {
        for (Deseq2LikeRegressionZModel.MainStat row : rows) {
            if (feature.equals(row.feature)) {
                return row;
            }
        }
        throw new AssertionError("Missing feature: " + feature);
    }

    private static double invokeStaticDouble(final Method method, final Object... args) {
        try {
            return ((Double) method.invoke(null, args)).doubleValue();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed invoking " + method.getName(), e);
        }
    }

    private static Object invokeStaticObject(final Method method, final Object... args) {
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed invoking " + method.getName(), e);
        }
    }

    private static double readPrivateDoubleField(final Object target, final String fieldName) {
        try {
            final java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getDouble(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed reading field " + fieldName, e);
        }
    }

    private static Object readPrivateObjectField(final Object target, final String fieldName) {
        try {
            final java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed reading field " + fieldName, e);
        }
    }

    /** Match {@code Deseq2LikeRegressionZModel.nbLogPmf} (log-domain) so optimizers agree. */
    private static double nbLogLikelihood(final double y, final double mu, final double alpha) {
        final double minDispEval = Math.exp(-30.0d);
        final double r = 1.0d / Math.max(minDispEval, alpha);
        final double logDenom = Math.log(r + mu);
        final double logP = Math.log(r) - logDenom;
        final double logOneMinusP = Math.log(mu) - logDenom;
        return Gamma.logGamma(y + r) - Gamma.logGamma(r) - Gamma.logGamma(y + 1.0d)
                + r * logP
                + y * logOneMinusP;
    }

    private static double[] referenceDispersionPriorStats(final int sampleCount,
                                                          final double[] meanNormCounts,
                                                          final double[] dispersionRaw,
                                                          final double[] trendValues) {
        final double[] residuals = new double[dispersionRaw.length];
        int count = 0;
        for (int i = 0; i < dispersionRaw.length; i++) {
            if (!Double.isFinite(dispersionRaw[i]) || dispersionRaw[i] < 1.0e-6d || !Double.isFinite(meanNormCounts[i])) {
                continue;
            }
            final double trend = trendValues[i];
            if (!Double.isFinite(trend) || trend <= 0.0d) {
                continue;
            }
            residuals[count++] = Math.log(dispersionRaw[i]) - Math.log(trend);
        }

        final int residualDf = sampleCount - 2;
        if (count == 0) {
            final double priorVar = residualDf > 0 ? 0.25d : 0.0d;
            return new double[] { priorVar, 0.0d };
        }

        final double varLogDispEsts = Math.pow(referenceRobustMadSd(Arrays.copyOf(residuals, count)), 2.0d);

        if (residualDf > 0 && residualDf <= 3) {
            final double histogramMin = -10.0d;
            final double histogramMax = 10.0d;
            final double histogramStep = 0.5d;
            final int histogramBins = (int) ((histogramMax - histogramMin) / histogramStep);
            final int[] observedHistogram = new int[histogramBins];
            int observedCount = 0;
            for (int i = 0; i < count; i++) {
                final double residual = residuals[i];
                if (residual > histogramMin && residual < histogramMax) {
                    final int bin = (int) ((residual - histogramMin) / histogramStep);
                    if (bin >= 0 && bin < histogramBins) {
                        observedHistogram[bin]++;
                        observedCount++;
                    }
                }
            }

            if (observedCount > 0) {
                final double[] observedDensity = new double[histogramBins];
                final double observedScale = 1.0d / (observedCount * histogramStep);
                double smallestObservedDensity = Double.POSITIVE_INFINITY;
                for (int bin = 0; bin < histogramBins; bin++) {
                    observedDensity[bin] = observedHistogram[bin] * observedScale;
                    if (observedDensity[bin] > 0.0d && observedDensity[bin] < smallestObservedDensity) {
                        smallestObservedDensity = observedDensity[bin];
                    }
                }

                final double[] obsVarGrid = new double[200];
                final double[] klDivergences = new double[200];
                final MersenneTwister random = new MersenneTwister(2);
                final ChiSquaredDistribution chiSquared = new ChiSquaredDistribution(random, residualDf);
                final double logResidualDf = Math.log(residualDf);

                for (int gridIndex = 0; gridIndex < obsVarGrid.length; gridIndex++) {
                    final double variance = (8.0d * gridIndex) / (obsVarGrid.length - 1.0d);
                    obsVarGrid[gridIndex] = variance;
                    final double simulatedSd = Math.sqrt(variance);
                    final int[] simulatedHistogram = new int[histogramBins];
                    int simulatedCount = 0;
                    for (int sampleIndex = 0; sampleIndex < 10000; sampleIndex++) {
                        final double simulated = Math.log(chiSquared.sample())
                                + (random.nextGaussian() * simulatedSd)
                                - logResidualDf;
                        if (simulated > histogramMin && simulated < histogramMax) {
                            final int bin = (int) ((simulated - histogramMin) / histogramStep);
                            if (bin >= 0 && bin < histogramBins) {
                                simulatedHistogram[bin]++;
                                simulatedCount++;
                            }
                        }
                    }

                    final double[] simulatedDensity = new double[histogramBins];
                    final double simulatedScale = simulatedCount > 0 ? 1.0d / (simulatedCount * histogramStep) : 0.0d;
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
                    final UnivariateFunction klFit = new LoessInterpolator(0.2d, 0).interpolate(obsVarGrid, klDivergences);
                    double minFit = Double.POSITIVE_INFINITY;
                    for (int gridIndex = 0; gridIndex < 1000; gridIndex++) {
                        final double variance = (8.0d * gridIndex) / 999.0d;
                        final double fitted = klFit.value(variance);
                        if (Double.isFinite(fitted) && fitted < minFit) {
                            minFit = fitted;
                            argminKl = variance;
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Keep the raw-grid minimum if smoothing fails.
                }

                return new double[] { Math.max(argminKl, 0.25d), varLogDispEsts };
            }
        }

        if (residualDf > 0) {
            return new double[] { Math.max(0.25d, varLogDispEsts - Gamma.trigamma(residualDf / 2.0d)), varLogDispEsts };
        }
        return new double[] { varLogDispEsts, varLogDispEsts };
    }

    private static double referenceTrendDispersion(final double mean, final double[] trendParams) {
        return trendParams[0] + (trendParams[1] / mean);
    }

    private static double referenceRobustMadSd(final double[] values) {
        if (values.length == 0) {
            return Double.NaN;
        }
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final double median = referenceMedianOfSorted(sorted);
        final double[] absDev = new double[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
            absDev[i] = Math.abs(sorted[i] - median);
        }
        Arrays.sort(absDev);
        return referenceMedianOfSorted(absDev) / 0.6744897501960817d;
    }

    private static double referenceMedianOfSorted(final double[] sorted) {
        if ((sorted.length & 1) == 1) {
            return sorted[sorted.length / 2];
        }
        final int upper = sorted.length / 2;
        return (sorted[upper - 1] + sorted[upper]) / 2.0d;
    }
}
