/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Functionally extends java.lang.Math with additional math related methods.
 *
 * @author Michael Angelo (CMath in GeneCluster)
 * @author Aravind Subramanian
 * @author David Eby
 */
public class XMath {

    private static final Logger klog = Logger.getLogger(XMath.class);

    private XMath() { }

    public static boolean isAscending(int[] ints) {
        for (int i = 0; i < ints.length - 1; i++) {
            int thisValue = ints[i];
            int nextValue = ints[i + 1];
            if (nextValue < thisValue) {
                return false;
            }
        }

        return true;
    }

    public static float getFWERTwoTailed(final float realScore, final Matrix rndScores) {
        if (isPositive(realScore)) {
            return getFWER(realScore, rndScores, true);
        } else {
            return getFWERLessThan(realScore, rndScores);
        }
    }

    public static float getFWER(final float realScore, final Matrix rndScores, final boolean pos) {
        Vector best_of_each_perm;
        if (pos) {
            best_of_each_perm = rndScores.getColumnMaxes();
        } else {
            best_of_each_perm = rndScores.getColumnMins();
        }

        double fwer;
        if (pos) {
            fwer = XMath.getPValue(realScore, best_of_each_perm);
        } else {
            fwer = XMath.getPValueLessThan(realScore, best_of_each_perm);
        }

        return (float) fwer;
    }

    public static float getFWERLessThan(final float realScore, final Matrix rndScores) {
        Vector lowest_of_each_perm = new Vector(rndScores.getNumCol());
        for (int c = 0; c < rndScores.getNumCol(); c++) {
            lowest_of_each_perm.setElement(c, rndScores.getColumnV(c).min());
        }

        return (float) XMath.getPValueLessThan(realScore, lowest_of_each_perm);
    }

    /**
     * @see http://www.cs.utsa.edu/~wagner/laws/ALogs.html
     *      Java supplies a function to calculate natural logs,
     *      base e = 2.718281828459045. To calculate logs to other bases,
     *      you need to multiply by a fixed constant: for a log base b multiply by 1/logeb
     */
    public static double log2(final double d) {
        return Math.log(d) / Math.log(2.0);
    }

    public static boolean isSameSign(final float a, final float b) {
        if (a < 0 && b < 0) {
            return true;
        } else if (a > 0 && b > 0) {
            return true;
        } else if (a == 0 && b == 0) {
            return true;
        }
        return false;
    }

    public static boolean isPositive(final float x) {
        return x >= 0;
    }

    public static boolean isNegative(final float x) {
        return x <= 0;
    }

    private static int[] toIndices(final int maxIndex) {
        if (maxIndex <= 0) {
            throw new IllegalArgumentException("Specified max for indices must be more than 0, got: " + maxIndex);
        }

        int[] inds = new int[maxIndex];
        for (int i = 0; i < maxIndex; i++) {
            inds[i] = i;
        }
        return inds;
    }

    /**
     * uses the common xmath random seed
     *
     * IMP num elements are returned AND the random numbers fall b/w 0 and num-1
     *
     * @see sampleWithoutReplacement for a diff way
     *      Creates an array whose elements are randomly arranged between
     *      0 and num - 1
     *      Example: randomize(5) could yield: 0, 3, 1, 2, 4
     *      <p/>
     *      The same random number generator is employed (in this instance of the jvm)
     *      so safe to call multiple times.
     *      But as it uses the same seed each time, the rnd stays the same from
     *      jvm invoc to jvm invoc. See link below for more.
     * @see http://mindprod.com/gotchas.html#RANDOM
     */
    public static int[] randomizeWithoutReplacement(final int num, final RandomSeedGenerator rsgen) {
        final Random rnd = rsgen.getRandom();

        // TODO: evaluate performance of using a Set
        List<Integer> seen = new ArrayList<Integer>(num);
        int[] inds = new int[num];
        int cnt = 0;

        for (int i = 0; i < num;) {
            int r = rnd.nextInt(num);

            if (seen.contains(r)) {
                continue;
            }

            seen.add(r);

            inds[cnt++] = r;

            if (cnt == num) {
                break;
            }
        }

        return inds;
    }

    /**
     * @param numRndNeeded       number of random picks nmeeded
     * @param maxRndNumExclusive range -> picked from 0 to highestrandomnum-1
     */
    // TODO: refactor with the above code
    public static int[] randomlySampleWithoutReplacement(final int numRndNeeded, final int highestrandomnumExclusive, final RandomSeedGenerator rsgen) {
        final Random rnd = rsgen.getRandom();
        if (highestrandomnumExclusive == numRndNeeded) { // no random picking needed, we have exactly as many as asked for
            return XMath.toIndices(highestrandomnumExclusive);
        }

        if (numRndNeeded > highestrandomnumExclusive) {
            throw new IllegalArgumentException("Cannot pick more numbers (no replacement) numRndNeeded: " + numRndNeeded + " than max possible number maxRndNumExclusive: " + highestrandomnumExclusive);
        }

        // TODO: evaluate performance of using a Set
        List<Integer> seen = new ArrayList<Integer>(numRndNeeded);
        int[] inds = new int[numRndNeeded];
        int cnt = 0;

        for (int i = 0; i < numRndNeeded;) {
            int r = rnd.nextInt(highestrandomnumExclusive);

            if (seen.contains(r)) {
                continue;
            }

            seen.add(r);

            inds[cnt++] = r;

            if (cnt == numRndNeeded) {
                break;
            }
        }

        return inds;
    }

    public static double getPValue(final float score, final Vector values) {
        final float[] values1 = values.elementData;
        int cntmore = 0;
        for (int i = 0; i < values1.length; i++) {
            if (values1[i] > score) {
                cntmore++;
            }
        }
        return ((double) cntmore) / (double) values1.length;
    }

    public static double getPValueLessThan(final float score, final Vector values) {
        final float[] values1 = values.elementData;
        int cntless = 0;
        for (int i = 0; i < values1.length; i++) {
            if (values1[i] < score) {
                cntless++;
            }
        }
        return ((double) cntless) / (double) values1.length;
    }

    /**
     * If the score is negative look to the left (i.e count how many less)
     * If the score is positive look to the right (i.e count how many are less)
     *
     * @param realEs
     */
    public static float getPValueTwoTailed_pos_neg_seperate(float realEs, Vector rndEs) {
        Vector ex = rndEs.extract(realEs, ScoreMode.POS_AND_NEG_SEPERATELY);
        final float score = realEs;
        final Vector values = ex;
        final float[] values1 = values.elementData;
        int cnt = 0;
        
        if (score >= 0) {
            for (int i = 0; i < values1.length; i++) {
                if (values1[i] > score) {
                    cnt++;
                }
            }
        } else {
            for (int i = 0; i < values1.length; i++) {
                if (values1[i] < score) {
                    cnt++;
                }
            }
        } // @note isnt this redundant
        return (float) (((double) cnt) / (double) values1.length);
    }

    private static void enforceEqualSize(final Vector x, final Vector y) {
        if (x.getSize() != y.getSize()) {
            throw new IllegalArgumentException("Vector lengths not equal x=" + x.getSize()
                    + " and y=" + y.getSize());
        }
    }

    private static void enforceEqualSize(final Vector[] vss) {
        if (vss.length == 0) {
            return;
        }

        int size = vss[0].getSize();
        for (int i = 0; i < vss.length; i++) {
            if (vss[i].getSize() != size) {
                throw new IllegalArgumentException("Vector lengths not equal first=" + size
                        + " and y=" + vss[i].getSize() + " at index " + i);
            }
        }
    }

    private static void enforceEqualSize(final float[] x, final float[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Vector lengths not equal x=" + x.length
                    + " and y=" + y.length);
        }
    }

    /**
     * dist <- sqrt(sum((x-y)*(x-y)))
     * No parameters.
     */
    public static double euclidean(final Vector x, final Vector y) {
        enforceEqualSize(x, y);

        final int size = x.getSize();
        int nonMissingSize = size;
        float sum = 0f;
        for (int i = 0; i < size; i++) {
            float xVal = x.getElement(i);
            float yVal = y.getElement(i);
            if (Float.isNaN(xVal) || Float.isNaN(yVal)) { nonMissingSize--; }
            else {
                float diff = xVal - yVal;
                sum += diff * diff;
            }
        }
        return (nonMissingSize == 0) ? Double.NaN : Math.sqrt(sum);
    }

    /**
     * dist <- sum(abs(x-y))
     * No parameters.
     */
    public static double manhattan(final Vector x, final Vector y) {
        enforceEqualSize(x, y);

        final int size = x.getSize();
        int nonMissingSize = size;
        double sum = 0.0;
        for (int i = 0; i < size; i++) {
            float xVal = x.getElement(i);
            float yVal = y.getElement(i);
            if (Float.isNaN(xVal) || Float.isNaN(yVal)) { nonMissingSize--;}
            else { sum += Math.abs(xVal - yVal); }
        }

        return (nonMissingSize == 0) ? Double.NaN : sum;
    }

    public static double meanOrMedianRatio(final Vector x, final Vector y, final boolean useMean) {
        return x.meanOrMedian(useMean) / y.meanOrMedian(useMean);
    }

    public static double meanOrMedianDiff(final Vector x, final Vector y, final boolean useMean) {
        return x.meanOrMedian(useMean) - y.meanOrMedian(useMean);
    }

    public static Vector medianVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] medians = new float[size];

        for (int i = 0; i < size; i++) {
            float[] v1 = new float[vss.length];
            for (int c = 0; c < vss.length; c++) {
                v1[c] = vss[c].getElement(i);
            }
            medians[i] = median(v1);
        }
        return new Vector(medians);
    }

    public static Vector meanVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] means = new float[size];

        for (int i = 0; i < size; i++) {
            int nonMissingSize = vss.length;
            float runningSum = 0.0f;
            for (int c = 0; c < vss.length; c++) {
                float value = vss[c].getElement(i);
                if (!Float.isNaN(value)) { runningSum += value; }
                else { nonMissingSize--; }
            }
            means[i] = (nonMissingSize == 0) ? Float.NaN : runningSum / nonMissingSize;
        }
        return new Vector(means);
    }

    public static Vector maxVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] maxs = new float[size];

        for (int i = 0; i < size; i++) {
            int nonMissingSize = vss.length;
            float max = Float.NEGATIVE_INFINITY;
            for (int c = 0; c < vss.length; c++) {
                float value = vss[c].getElement(i);
                if (Float.isNaN(value)) { nonMissingSize--; }
                else if (max < value) { max = value; }
            }
            maxs[i] = (nonMissingSize == 0) ? Float.NaN : max;
        }
        return new Vector(maxs);
    }

    public static Vector sumVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] sums = new float[size];

        for (int i = 0; i < size; i++) {
            int nonMissingSize = vss.length;
            float runningSum = 0.0f;
            for (int c = 0; c < vss.length; c++) {
                float value = vss[c].getElement(i);
                if (!Float.isNaN(value)) { runningSum += value; }
                else { nonMissingSize--; }
            }
            sums[i] = (nonMissingSize == 0) ? Float.NaN : runningSum;
        }
        return new Vector(sums);
    }

    public static float max(final float[] values) {
        if (values.length == 0) {
            klog.warn("FIX ME Zero length array");
            throw new IllegalArgumentException("Zero length array not allowed");
        }
    
        int nonMissingSize = values.length;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            if (Float.isNaN(values[i])) { nonMissingSize--; }
            else if (max < values[i]) { max = values[i]; }
        }
        return (nonMissingSize == 0) ? Float.NaN : max;
    }

    public static float median(final float[] x) {
        if (x.length == 0) {
            return Float.NaN;
        }

        final int size = x.length;
        float[] v1 = new float[size];

        // Replace NaN values with POSITIVE_INTEGER so they sort to the end
        int nonMissingCount = size;
        for (int i = 0; i < size; i++) {
            if (!Float.isNaN(x[i])) { v1[i] = x[i]; } 
            else {
                v1[i] = Float.POSITIVE_INFINITY;
                nonMissingCount--;
            } 
        }
        
        if (nonMissingCount == 0) { return Float.NaN; }
        
        // Call out some common cases to skip extra operations when they can be avoided.  This is
        // just using a reasonable guess for a limit that covers the majority of cases (based on 
        // its use in Collapse, not its use for Dataset Metrics).
        // Here, v1 will hold non-missing values in the index range 0 to nonMissingCount-1, so we
        // determine the median over that range only.  The index range above that will hold missing 
        // values - since they've been replaced by POSITIVE_INFINITY - after sorting.
        Arrays.parallelSort(v1);
        switch (nonMissingCount) {
        case 1: return v1[0];
        case 2: return (v1[0] + v1[1]) / 2;
        case 3: return v1[1];
        case 4: return (v1[1] + v1[2]) / 2;
        case 5: return v1[2];

        default:
            // Only do the extra divisions and odd vs. even check for the general case.
            final int in1 = (nonMissingCount - 1) / 2;
            final int in2 = nonMissingCount / 2;
            // This test indicates whether the size is odd or even
            return (in1 == in2) ? v1[in1] : (v1[in1] + v1[in2]) / 2;
        }
    }

    public static float mean(final float[] x) {
        if (x.length == 0) {
            return Float.NaN;
        }

        int nonMissingSize = x.length;
        float runningSum = 0.0f;
        for (int i = 0; i < x.length; i++) {
            if (!Float.isNaN(x[i])) { runningSum += x[i]; }
            else { nonMissingSize--; }
        }
        return (nonMissingSize == 0) ? Float.NaN : runningSum / nonMissingSize;
    }

    public static float sum(final float[] x) {
        if (x.length == 0) {
            return Float.NaN;
        }

        int nonMissingSize = x.length;
        float runningSum = 0.0f;
        for (int i = 0; i < x.length; i++) {
            if (!Float.isNaN(x[i])) { runningSum += x[i]; }
            else { nonMissingSize--; }
        }
        return (nonMissingSize == 0) ? Float.NaN : runningSum;
    }

    /*
     * @see http://www.ruf.rice.edu/~lane/hyperstat/A51911.html
     *      <p/>
     *      will ret Nan if only 1 element in each vector
     */
    public static double pearson(final Vector x, final Vector y) {
        enforceEqualSize(x, y);

        if (x.getSize() <= 1) { return Double.NaN; }
        double N = (double) x.getSize();

        final double xSum = x.sumNaNsafe();
        final double ySum = y.sumNaNsafe();
        double numr = x.sumprod(y) - ((xSum * ySum) / N);
        double denr = ((x.squaresumNaNsafe() - ((xSum * xSum) / N)))
                * ((y.squaresumNaNsafe() - ((ySum * ySum) / N)));

        denr = Math.sqrt(denr);

        return numr / denr;
    }

    /**
     * dist <- sum(x*y) / (sqrt(sum(x^2) * sum(y^2)) );
     *
     * @todo check if this is a dist or simil?
     * i.e is a low number "good" or "bad"
     * Looks like its a "bad", and hence need to do 1-
     * <p/>
     * No parameters
     */
    public static double cosine(final Vector x, final Vector y) {
        final float[] x1 = x.elementData;
        final float[] y1 = y.elementData;
        enforceEqualSize(x1, y1);
        
        double mag_x = 0.0;
        double mag_y = 0.0;
        double sump = 0.0;
        
        int nonMissingSize = x1.length;
        for (int i = 0; i < x1.length; i++) {
            if (Double.isNaN(x1[i]) || Double.isNaN(x1[i])) { nonMissingSize--;}
            else {
                mag_x += x1[i] * x1[i];
                mag_y += y1[i] * y1[i];
                sump += x1[i] * y1[i];
            }
        }
        
        return (nonMissingSize == 0) ? Double.NaN : 1.0d - (sump / Math.sqrt(mag_x * mag_y));
    }

    public static double s2n(final Vector x, final Vector y,
            final boolean usebiased, final boolean usemedian, final boolean fixlow) {
        double s2n;
        if (usemedian) {
            s2n = (x.median() - y.median())
                    / (x.stddev(usebiased, fixlow) + y.stddev(usebiased, fixlow));
        } else {
            s2n = (x.mean() - y.mean())
                    / (x.stddev(usebiased, fixlow) + y.stddev(usebiased, fixlow));
        }

        return s2n;
    }
    
    /**
     * @see http://trochim.human.cornell.edu/kb/stat_t.htm
     */
    public static double tTest(final Vector x, final Vector y, 
            final boolean usebiased, final boolean usemedian, final boolean fixlow) {
        double numr;
        if (usemedian) {
            numr = x.median() - y.median();
        } else {
            numr = x.mean() - y.mean();
        }

        double denr;
        if (usebiased) {
            denr = Math.sqrt((x.var(usebiased, fixlow) / (x.getSize() - 1))
                    + (y.var(usebiased, fixlow) / (y.getSize() - 1)));
        } else {
            denr = Math.sqrt((x.var(usebiased, fixlow) / (x.getSize()))
                    + (y.var(usebiased, fixlow) / (y.getSize())));
        }

        return (numr / denr);
    }

    public static double mannWhitney(final int[] hitIndices, final int totSize) {

        // term 1
        double sr = 0; // sum of ranks of genes in the gene set
        for (int i = 0; i < hitIndices.length; i++) {
            sr += (double) hitIndices[i];
        }

        double N = (double) hitIndices.length; // size of gene set
        double term1 = (sr / N);

        // term 2
        double st = (totSize * (totSize - 1)) / 2; // sum of ranks of ALL genes

        double d = (double) totSize; // number of elements in the dataset

        double term2 = (st - sr) / (d - N);

        double mw = term1 - term2;

        //System.out.println("sr: " + sr + " st: " + st + " term1: " + term1 + " term2: " + term2 + " mw: " + mw);

        // now this is when a low rank sum is GOOD, and hence the mw score a negative number
        // but to put it on the same terms as our ES score, lets negate it

        return -1.0d * mw;
    }

    public static int getMoreThanCount(final float value, final Vector sorted_pos_to_neg) {
        int cnt = 0;
        for (int i = 0; i < sorted_pos_to_neg.getSize(); i++) {
            if (value > sorted_pos_to_neg.getElement(i)) {
                break;
            } else {
                cnt++;
            }
        }

        return cnt;
    }

    public static int getLessThanCount(final float value, final Vector sorted_low_to_high) {
        int cnt = 0;
        for (int i = 0; i < sorted_low_to_high.getSize(); i++) {
            if (value < sorted_low_to_high.getElement(i)) {
                break;
            } else {
                cnt++;
            }
        }

        return cnt;
    }

	public static boolean isNearlyZero(double d) {
	    // See https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/
	    // Since we are only concerned with testing values very close to zero here, we don't deal with ULPs 
	    // as discussed in the above article.  It's enough to use a small epsilon instead.
	    // Since our computations are done in Doubles, we use the equivalent of the C++ DBL_EPSILON
	    // instead of FLT_EPSILON (see http://www.cplusplus.com/reference/cfloat/)
	    return (Math.abs(d) <= 1e-9d);
	}
}