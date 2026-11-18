/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.alg.gsea;

import java.util.Arrays;

import org.apache.commons.math3.special.Erf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Independent filtering, LOWESS, and the small numeric helpers used by the
 * DESeq2-like fit. The model class keeps one-line delegates so existing
 * callers, including reflective tests, stay on the same names.
 */
final class Deseq2Statistics {
    private static final Logger log = LoggerFactory.getLogger(Deseq2Statistics.class);

    static double trendDispersion(final double mean, final double a, final double b) {
        return Math.max(Deseq2LikeRegressionZModel.MIN_DISP, a + b / Math.max(Deseq2LikeRegressionZModel.EPS, mean));
    }


    static double selectIndependentFilteringThreshold(final double[] filter, final double[] pValues) {
        final double[] safeFilter = sanitizeFilter(filter);
        final double lowerQuantile = meanZeroFraction(safeFilter);
        final double upperQuantile = lowerQuantile < 0.95d ? 0.95d : 1.0d;
        if (upperQuantile <= lowerQuantile + Deseq2LikeRegressionZModel.EPS) {
            return quantile(safeFilter, lowerQuantile);
        }

        final double[] theta = sequence(lowerQuantile, upperQuantile, Deseq2LikeRegressionZModel.FILTER_GRID_POINTS);
        final double[] numRej = new double[theta.length];
        for (int i = 0; i < theta.length; i++) {
            final double cutoff = quantile(safeFilter, theta[i]);
            final double[] filteredPadj = applyIndependentFilteringAtSafeThreshold(safeFilter, pValues, cutoff);
            numRej[i] = countRejections(filteredPadj, Deseq2LikeRegressionZModel.TARGET_FDR);
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


    static double[] applyIndependentFilteringAtThreshold(final double[] filter,
            final double[] pValues,
            final double threshold) {
        if (!Double.isFinite(threshold)) {
            return benjaminiHochberg(pValues);
        }
        return applyIndependentFilteringAtSafeThreshold(sanitizeFilter(filter), pValues, threshold);
    }


    static double[] applyIndependentFilteringAtSafeThreshold(final double[] safeFilter,
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


    static double[] benjaminiHochberg(final double[] pValues) {
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


    static double[] sanitizeFilter(final double[] filter) {
        final double[] safe = new double[filter.length];
        for (int i = 0; i < filter.length; i++) {
            final double value = filter[i];
            safe[i] = Double.isFinite(value) && value > 0.0d ? value : 0.0d;
        }
        return safe;
    }


    static double meanZeroFraction(final double[] values) {
        int zeros = 0;
        for (double value : values) {
            if (value == 0.0d) {
                zeros++;
            }
        }
        return values.length == 0 ? 0.0d : ((double) zeros / (double) values.length);
    }


    static double[] sequence(final double start, final double end, final int length) {
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


    static double[] smoothRejectionCurve(final double[] theta, final double[] numRej) {
        try {
            if (theta.length != numRej.length || theta.length == 0) {
                return new double[0];
            }
            final double range = theta[theta.length - 1] - theta[0];
            final double delta = range > 0.0d ? Deseq2LikeRegressionZModel.LOWESS_DELTA_FRACTION * range : 0.0d;
            return lowess(theta, numRej, Deseq2LikeRegressionZModel.FILTER_SMOOTHING_SPAN, 3, delta);
        } catch (RuntimeException ex) {
            final double[] copy = new double[numRej.length];
            System.arraycopy(numRej, 0, copy, 0, numRej.length);
            return copy;
        }
    }


    static double[] lowess(final double[] x,
            final double[] y,
            final double f,
            final int nsteps,
            final double delta) {
        return lowess(x, y, f, nsteps, delta, null);
    }


    static double[] lowess(final double[] x,
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

                final Deseq2LikeRegressionZModel.LowessPointFit fit = lowessPoint(x, y, x[i], nleft, nright, weights, iter > 1, robustnessWeights,
                        priorWeights);
                ys[i] = fit.ok ? fit.y : y[i];

                if (last < i - 1) {
                    final double denom = x[i] - x[last];
                    for (int j = last + 1; j < i; j++) {
                        final double alpha = Math.abs(denom) <= Deseq2LikeRegressionZModel.EPS ? 0.0d : (x[j] - x[last]) / denom;
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


    static Deseq2LikeRegressionZModel.LowessPointFit lowessPoint(final double[] x,
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
            return new Deseq2LikeRegressionZModel.LowessPointFit(Double.NaN, false);
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
        return new Deseq2LikeRegressionZModel.LowessPointFit(ys, true);
    }


    static double square(final double value) {
        return value * value;
    }


    static double cube(final double value) {
        return value * value * value;
    }


    static double[] positiveResiduals(final double[] numRej, final double[] smooth) {
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


    static double rmse(final double[] values) {
        if (values.length == 0) {
            return 0.0d;
        }
        double sumSq = 0.0d;
        for (double value : values) {
            sumSq += value * value;
        }
        return Math.sqrt(sumSq / values.length);
    }


    static double countRejections(final double[] adjustedPValues, final double alpha) {
        int count = 0;
        for (double value : adjustedPValues) {
            if (Double.isFinite(value) && value < alpha) {
                count++;
            }
        }
        return count;
    }


    static boolean anyGreaterThan(final double[] values, final double threshold) {
        for (double value : values) {
            if (value > threshold) {
                return true;
            }
        }
        return false;
    }


    static int firstGreaterThan(final double[] values, final double threshold) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] > threshold) {
                return i;
            }
        }
        return 0;
    }


    static double quantile(final double[] values, final double probability) {
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


    static double medianOfSorted(final double[] values, final int length) {
        if (length <= 0) {
            return Double.NaN;
        }
        if ((length & 1) == 1) {
            return values[length / 2];
        }
        return (values[(length / 2) - 1] + values[length / 2]) / 2.0d;
    }


    static double trimmedMean(final double[] values, final int length, final double trim) {
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


    static double trimRatioForSampleCount(final int sampleCount) {
        if (sampleCount <= 3) {
            return 1.0d / 3.0d;
        }
        if (sampleCount <= 23) {
            return 1.0d / 4.0d;
        }
        return 1.0d / 8.0d;
    }


    static double scaleForTrimmedVariance(final int sampleCount) {
        if (sampleCount <= 3) {
            return 2.04d;
        }
        if (sampleCount <= 23) {
            return 1.86d;
        }
        return 1.51d;
    }


    static double sampleVariance(final double[] values) {
        // TODO: evaluate a pre-pass to determine usable samples and lift the min count check above the loop.
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


    static double robustMadSd(final double[] values, final int length) {
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
        final double sd = mad / Deseq2LikeRegressionZModel.ROBUST_NORMAL_SD_QUANTILE;
        return Double.isFinite(sd) && sd >= 0.0d ? sd : 0.0d;
    }


    static double twoSidedPValueFromZ(final double z) {
        if (!Double.isFinite(z)) {
            return Double.NaN;
        }
        final double p = Erf.erfc(Math.abs(z) / Math.sqrt(2.0d));
        return clamp(p, 0.0d, 1.0d);
    }


    static double max(final double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }


    static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

}
