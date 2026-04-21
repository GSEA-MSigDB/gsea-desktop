/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.alg.DatasetStatsCore.TwoClassMarkerStats;
import edu.mit.broad.genome.alg.Metrics;
import edu.mit.broad.genome.math.AddressedVector;
import edu.mit.broad.genome.math.DoubleElement;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.ScoredDatasetImpl;
import edu.mit.broad.genome.objects.Template;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wald z-statistic ranking for pre-normalized (non-count) expression: per-gene ordinary least
 * squares with {@code y = β₀ + β₁·condition}, reporting {@code β₁ / SE(β₁)} as the gene score.
 *
 * <p>This is the fallback when {@link Metrics.Wald} is selected but
 * {@link Deseq2LikeRegressionZModel} cannot run (e.g. non-integer or non-count data). It uses the
 * same {@link #createConditionVector condition coding} as the DESeq2-like path so ranks are
 * comparable in sign (class of interest vs reference). There is no median-of-ratios normalization
 * or NB dispersion—those apply only to raw counts.
 *
 * <p>Unlike {@link Deseq2LikeRegressionZModel}, this path does not apply DESeq2-style independent
 * filtering on mean normalized counts; {@link TwoClassMarkerStats#lowInformation} is left false
 * after {@link #scoreForTemplate} (with {@code lowInformationChecked} set) so enrichment uses all
 * non-omitted genes unless that logic is extended for normalized inputs.
 */
class PrenormalizedRegressionZModel {
    private final Logger log = LoggerFactory.getLogger(PrenormalizedRegressionZModel.class);

    private static final double EPS = 1e-10;

    private final Dataset ds;

    private PrenormalizedRegressionZModel(final Dataset ds) {
        this.ds = ds;
    }

    /**
     * Construct a model for {@link Metrics#Wald} scoring on already-normalized values.
     * Template shape is validated here; per-gene fits occur in {@link #scoreForTemplate}.
     */
    public static PrenormalizedRegressionZModel fit(final Dataset ds,
                                                  final Template realTemplate,
                                                  final Map<String, TwoClassMarkerStats> markerScores) {
        if (markerScores == null) {
            throw new IllegalArgumentException("markerScores cannot be null for " + Metrics.Wald.NAME + " scoring on normalized data");
        }
        if (realTemplate.getNumClasses() != 2) {
            throw new IllegalArgumentException(Metrics.Wald.NAME + " on normalized data requires a 2-class template");
        }

        final int rowCount = ds.getNumRow();
        final int colCount = ds.getNumCol();

        if (rowCount == 0 || colCount < 2) {
            throw new IllegalArgumentException("Dataset too small for " + Metrics.Wald.NAME + " scoring: " + rowCount + " rows, " + colCount + " cols");
        }

        return new PrenormalizedRegressionZModel(ds);
    }

    /**
     * Score genes for a given template using simple linear regression.
     * Returns z-scores for each gene.
     */
    public ScoredDataset scoreForTemplate(final Template template,
                                          final SortMode sort,
                                          final Order order,
                                          final Map<String, TwoClassMarkerStats> markerScores) {
        if (markerScores == null) {
            throw new IllegalArgumentException("markerScores cannot be null for " + Metrics.Wald.NAME + " scoring on normalized data");
        }
        if (template.getNumClasses() != 2) {
            throw new IllegalArgumentException(Metrics.Wald.NAME + " on normalized data requires a 2-class template");
        }

        final int rowCount = ds.getNumRow();
        final int colCount = ds.getNumCol();
        final double[] condition = createConditionVector(template, colCount);

        final DoubleElement[] sorted = new DoubleElement[rowCount];
        int invalidFitCount = 0;
        int neutralizedRows = 0;

        for (int r = 0; r < rowCount; r++) {
            final String rowName = ds.getRowName(r);
            final TwoClassMarkerStats markerScore = markerScores.get(rowName);
            if (markerScore != null) {
                markerScore.lowInformationChecked = true;
                markerScore.lowInformationThreshold = Double.NaN;
                // No DESeq2 mean-normalized-count filter on this path; see class Javadoc.
                markerScore.lowInformation = false;
            }
            if (markerScore != null && markerScore.omit) {
                sorted[r] = new DoubleElement(r, Double.NaN);
                continue;
            }

            final Vector row = ds.getRow(r);
            final double[] values = new double[colCount];
            int finiteCount = 0;
            for (int c = 0; c < colCount; c++) {
                final double v = row.getElement(c);
                values[c] = v;
                if (Double.isFinite(v)) {
                    finiteCount++;
                }
            }

            if (finiteCount < 2) {
                sorted[r] = new DoubleElement(r, Double.NaN);
                continue; // Not enough data for regression
            }

            // Fit linear regression: y = b0 + b1*condition
            final LinearFit fit = fitLinearRegression(values, condition);
            double z = (fit != null && Double.isFinite(fit.zScore)) ? fit.zScore : Double.NaN;
            if (!Double.isFinite(z)) {
                invalidFitCount++;
                z = 0.0d;
            }
            if (z == 0.0d) {
                neutralizedRows++;
            }

            sorted[r] = new DoubleElement(r, z);
        }

        // Sort according to user spec
        final DoubleElement.DoubleElementComparator baseComparator =
                new DoubleElement.DoubleElementComparator(sort, order.isAscending());
        Arrays.parallelSort(sorted, (element1, element2) -> {
            final int cmp = baseComparator.compare(element1, element2);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(element1.fIndex, element2.fIndex);
        });

        if (invalidFitCount > 0) {
            final double invalidFraction = (double) invalidFitCount / (double) rowCount;
            if (invalidFraction >= 0.25d) {
                log.warn("{} (normalized): {} of {} rows ({}) produced non-finite Wald z-scores and were set to 0.0",
                        Metrics.Wald.NAME, invalidFitCount, rowCount, invalidFraction);
            } else if (log.isDebugEnabled()) {
                log.debug("{} (normalized): {} of {} rows ({}) produced non-finite Wald z-scores and were set to 0.0",
                        Metrics.Wald.NAME, invalidFitCount, rowCount, invalidFraction);
            }
        }
        if (neutralizedRows > 0 && log.isDebugEnabled()) {
            log.debug("{} (normalized): neutralized {} row(s) with score 0.0 (invalid fit or exact zero)", Metrics.Wald.NAME, neutralizedRows);
        }

        final List<DoubleElement> dels = Arrays.asList(sorted);
        return new ScoredDatasetImpl(new AddressedVector(dels), ds);
    }

    /**
     * Fit linear model: y = β₀ + β₁*x, return β₁/SE(β₁) as z-score.
     * Simple least-squares regression using standard formulas.
     */
    private static LinearFit fitLinearRegression(final double[] y, final double[] x) {
        final int n = y.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int finiteCount = 0;

        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(y[i]) || !Double.isFinite(x[i])) {
                continue;
            }
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            finiteCount++;
        }

        if (finiteCount < 2) {
            return null;
        }

        final double meanX = sumX / finiteCount;
        final double meanY = sumY / finiteCount;
        final double varX = (sumX2 - finiteCount * meanX * meanX) / finiteCount;
        if (varX < EPS) {
            return null; // No variation in x
        }

        // Slope and intercept
        final double b1 = (sumXY - finiteCount * meanX * meanY) / (finiteCount * varX);
        final double b0 = meanY - b1 * meanX;

        // Residual variance
        double residualSumSq = 0;
        int residualDf = finiteCount - 2;
        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(y[i]) || !Double.isFinite(x[i])) {
                continue;
            }
            final double predicted = b0 + b1 * x[i];
            final double resid = y[i] - predicted;
            residualSumSq += resid * resid;
        }

        if (residualDf <= 0) {
            return null;
        }

        // Floor residual variance so SE(β₁) stays finite when RSS/df is ~0 (exact or near-exact fit).
        final double residualVar = Math.max(residualSumSq / residualDf, EPS);
        final double seB1 = Math.sqrt(residualVar / (finiteCount * varX));
        final double zScore = b1 / seB1;

        return new LinearFit(b1, zScore);
    }

    private static double[] createConditionVector(final Template template, final int numCols) {
        final double[] condition = new double[numCols];
        Arrays.fill(condition, Double.NaN);

        // Match {@link Deseq2LikeRegressionZModel} condition coding: class of interest = 1,
        // reference = 0, so β₁ > 0 means higher expression in the interest class.
        final int coiIndex = template.getClassOfInterestIndex();
        final int interestClass = (coiIndex == 0) ? 0 : 1;
        final int referenceClass = (interestClass == 0) ? 1 : 0;

        final Template.Class classRef = template.getClass(referenceClass);
        for (Template.Item item : classRef.getItemsOrderedByProfilePos()) {
            final int pos = item.getProfilePosition();
            if (pos >= 0 && pos < numCols) {
                condition[pos] = 0.0d;
            }
        }

        final Template.Class classCoi = template.getClass(interestClass);
        for (Template.Item item : classCoi.getItemsOrderedByProfilePos()) {
            final int pos = item.getProfilePosition();
            if (pos >= 0 && pos < numCols) {
                condition[pos] = 1.0d;
            }
        }

        return condition;
    }

    private static class LinearFit {
        final double coefficient;
        final double zScore;

        LinearFit(final double coefficient, final double zScore) {
            this.coefficient = coefficient;
            this.zScore = zScore;
        }
    }
}
