/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * Functionally extends java.lang.Math with additional math related methods.
 *
 * @author Michael Angelo (CMath in GeneCluster)
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class XMath {

    private static final Logger klog = Logger.getLogger(XMath.class);

    /**
     * Privatized class constructor.
     */
    private XMath() {
    }

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
     * @param d
     * @return
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
        } else if (a == 0 && b == 0) {
            return true;
        } else if (a > 0 && b > 0) {
            return true;
        }

        return false;
    }

    public static boolean isEven(final int x) {

        if ((float) (x / 2) == x / (float) 2.0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPositive(final float x) {
        return x >= 0;
    }

    public static boolean isPositive(final double x) {
        return x >= 0;
    }

    public static boolean isNegative(final float x) {
        return x <= 0;
    }

    public static int[] toIndices(final int maxIndex, final boolean inclusive) {
        if (maxIndex <= 0) {
            throw new IllegalArgumentException("Specified max for indices must be more than 0, got: " + maxIndex);
        }

        int till;
        if (inclusive) {
            till = maxIndex + 1;
        } else {
            till = maxIndex;
        }

        int[] inds = new int[till];
        for (int i = 0; i < till; i++) {
            inds[i] = i;
        }

        return inds;
    }

    /**
     * uses the common xmath random seed
     *
     * @param num
     * @return
     */
    public static int[] randomizeWithoutReplacement(final int num, final RandomSeedGenerator rsgen) {
        return randomizeWithoutReplacement(num, rsgen.getRandom());
    }

    /**
     * IMP num elements are returned AND the random numbers fall b/w 0 and num-1
     *
     * @see sampleWithoutReplacement for a diff way
     *      Creates an array whose elements are randomly arranged between
     *      0 and num - 1
     *      Example: randomize(5) could yeild: 0, 3, 1, 2, 4
     *      <p/>
     *      The same random number generator is employed (in this instance of the jvm)
     *      so safe to call multiple times.
     *      But as it uses the same seed each time, the rnd stays the same from
     *      jvm inoc to jvm invoc. See link below for more.
     * @see http://mindprod.com/gotchas.html#RANDOM
     */
    public static int[] randomizeWithoutReplacement(final int num, final Random rnd) {

        List seen = new ArrayList(num);
        int[] inds = new int[num];
        int cnt = 0;

        for (int i = 0; i < num;) {
            int r = rnd.nextInt(num);

            if (seen.contains(new Integer(r))) {
                continue;
            }

            seen.add(new Integer(r));

            inds[cnt++] = r;

            if (cnt == num) {
                break;
            }
        }

        return inds;
    }

    public static int[] randomlySampleWithoutReplacement(final int numRndNeeded, final int highestrandomnumExclusive, final RandomSeedGenerator rsgen) {
        return randomlySampleWithoutReplacement(numRndNeeded, highestrandomnumExclusive, rsgen.getRandom());
    }

    /**
     * @param numRndNeeded       number of random picks nmeeded
     * @param maxRndNumExclusive range -> picked from 0 to highestrandomnum-1
     * @param rnd
     * @return
     */
    public static int[] randomlySampleWithoutReplacement(final int numRndNeeded, final int maxRndNumExclusive, final Random rnd) {

        if (maxRndNumExclusive == numRndNeeded) { // no random picking needed, we have exactly as many as asked for
            return XMath.toIndices(maxRndNumExclusive, false);
        }

        if (numRndNeeded > maxRndNumExclusive) {
            throw new IllegalArgumentException("Cannot pick more numbers (no replacement) numRndNeeded: " + numRndNeeded + " than max possible number maxRndNumExclusive: " + maxRndNumExclusive);
        }

        List seen = new ArrayList(numRndNeeded);
        int[] inds = new int[numRndNeeded];
        int cnt = 0;

        for (int i = 0; i < numRndNeeded;) {
            int r = rnd.nextInt(maxRndNumExclusive);

            if (seen.contains(new Integer(r))) {
                continue;
            }

            seen.add(new Integer(r));

            inds[cnt++] = r;

            if (cnt == numRndNeeded) {
                break;
            }
        }

        return inds;
    }

    public static double getPValue(final float score, final float[] values) {

        int cntmore = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] > score) {
                cntmore++;
            }
        }

        //log.debug("Number more than score=" + score + " is=" + cntmore);

        return ((double) cntmore) / (double) values.length;
    }

    public static double getPValueLessThan(final float score, final float[] values) {

        int cntless = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] < score) {
                cntless++;
            }
        }

        return ((double) cntless) / (double) values.length;
    }

    public static double getPValue(final float score, final Vector values) {
        return getPValue(score, values.elementData);
    }

    public static double getPValueLessThan(final float score, final Vector values) {
        return getPValueLessThan(score, values.elementData);
    }

    /**
     * If the score is negative look to the left (i.e count how many less)
     * If the score is positive look to the right (i.e count how many are less)
     *
     * @param score
     * @param values
     * @return
     */
    public static double getPValueTwoTailed(final float score, final float[] values) {

        int cnt = 0;

        if (score >= 0) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] > score) {
                    cnt++;
                }
            }
        } else {
            for (int i = 0; i < values.length; i++) {
                if (values[i] < score) {
                    cnt++;
                }
            }
        }

        //log.debug("Number less/more than score=" + score + " is=" + cnt);

        return ((double) cnt) / (double) values.length;
    }

    public static double getPValueTwoTailed(final float score, final Vector values) {
        return getPValueTwoTailed(score, values.elementData);
    }

    public static float getPValueTwoTailed_pos_neg_seperate(float realEs, Vector rndEs) {
        Vector ex = rndEs.extract(realEs, ScoreMode.POS_AND_NEG_SEPERATELY); // @note isnt this redundant
        return (float) XMath.getPValueTwoTailed(realEs, ex);
    }


    public static float max(final float[] values) {
        if (values.length == 0) {
            klog.warn("FIX ME Zero length array");

            //return -999f;

            throw new IllegalArgumentException("Zero length array not allowed");
        }

        float max = values[0];
        for (int i = 0; i < values.length; i++) {
            if (max < values[i]) {
                max = values[i];
            }
        }

        return max;
    }

    public static int min(final int a, final int b) {
        return min(new int[]{a, b});
    }

    public static int min(final int[] values) {

        if (values.length == 0) {
            throw new IllegalArgumentException("Zero length array not allowed");
        }

        int min = values[0];
        for (int i = 0; i < values.length; i++) {
            if (min > values[i]) {
                min = values[i];
            }
        }

        return min;
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

        float sum = 0;

        for (int i = 0; i < x.getSize(); i++) {
            float diff = x.getElement(i) - y.getElement(i);

            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * dist <- sum(abs(x-y))
     * No parameters.
     */
    public static double manhatten(final Vector x, final Vector y) {

        enforceEqualSize(x, y);

        double sum = 0.0;

        for (int i = 0; i < x.getSize(); i++) {
            sum += Math.abs(x.getElement(i) - y.getElement(i));
        }

        return sum;
    }

    public static double meansdiff(final Vector x, final Vector y) {
        return x.mean() - y.mean();
    }

    public static double meansratio(final Vector x, final Vector y) {
        return x.mean() / y.mean();
    }

    public static double mediansratio(final Vector x, final Vector y) {
        return x.median() / y.median();
    }

    public static double meanOrMedianRatio(final Vector x, final Vector y, final boolean useMean) {
        return x.meanOrMedian(useMean) / y.meanOrMedian(useMean);
    }

    public static double meanOrMedianDiff(final Vector x, final Vector y, final boolean useMean) {
        return x.meanOrMedian(useMean) - y.meanOrMedian(useMean);
    }

    public static double mediansdiff(final Vector x, final Vector y) {
        return x.median() - y.median();
    }

    public static Vector medianVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] medians = new float[size];

        for (int i = 0; i < size; i++) {
            Vector v = new Vector(vss.length);
            for (int c = 0; c < vss.length; c++) {
                v.setElement(c, vss[c].getElement(i));
            }

            medians[i] = (float) v.median();
        }

        return new Vector(medians);
    }

    public static Vector meanVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] means = new float[size];

        for (int i = 0; i < size; i++) {
            Vector v = new Vector(vss.length);
            for (int c = 0; c < vss.length; c++) {
                v.setElement(c, vss[c].getElement(i));
            }

            means[i] = (float) v.mean();
        }

        return new Vector(means);
    }

    public static Vector maxVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] maxs = new float[size];

        for (int i = 0; i < size; i++) {
            Vector v = new Vector(vss.length);
            for (int c = 0; c < vss.length; c++) {
                v.setElement(c, vss[c].getElement(i));
            }

            maxs[i] = v.max();
        }

        return new Vector(maxs);
    }

    public static Vector sumVector(final Vector[] vss) {
        enforceEqualSize(vss);
        int size = vss[0].getSize();
        float[] sums = new float[size];

        for (int i = 0; i < size; i++) {
            Vector v = new Vector(vss.length);
            for (int c = 0; c < vss.length; c++) {
                v.setElement(c, vss[c].getElement(i));
            }

            sums[i] = (float) v.sum();
        }

        return new Vector(sums);
    }

    public static float median(final float[] x) {

        if (x.length == 0) {
            return Float.NaN;
        }

        // mangelos
        int aLen = x.length;
        float[] v1 = new float[aLen];

        System.arraycopy(x, 0, v1, 0, aLen);
        Arrays.sort(v1);

        int ind = (aLen - 1) / 2;

        if (XMath.isEven(aLen)) {
            return (v1[ind] + v1[aLen / 2]) / 2;
        } else {
            return v1[ind];
        }
    }

    public static float mean(final float[] x) {

        if (x.length == 0) {
            return Float.NaN;
        }

        return sum(x) / x.length;
    }

    public static float sum(final float[] x) {

        float sum = 0f;
        for (float f : x) {
            sum += f;
        }
        
        return sum;
    }

    /**
     * No parameters
     *
     * @see http://www.ruf.rice.edu/~lane/hyperstat/A51911.html
     *      <p/>
     *      will ret Nan if only 1 element in each vector
     */
    public static double pearson(final Vector x, final Vector y) {

        enforceEqualSize(x, y);

        double N = (double) x.getSize();

        if (N == 0 || N == 1) {
            return Float.NaN;
        }

        //double numr = (sumprod(x, y) - x.sum()*y.sum()) / N;
        double numr = x.sumprod(y) - ((x.sum() * y.sum()) / N);
        double denr = ((x.squaresum() - ((x.sum() * x.sum()) / N)))
                * ((y.squaresum() - ((y.sum() * y.sum()) / N)));

        denr = Math.sqrt(denr);

        //double denr = ((x.squaresum() - (x.sum()*x.sum())) / N) * ((y.squaresum() - (y.sum()*y.sum())) / N);
        //denr = Math.sqrt(denr);

        /*
        log.debug("numr=" + numr + " denr=" + denr);
        log.debug("x>>" + x.squaresum());
        log.debug("x>>" + (x.sum()*x.sum()));
        log.debug("x>>" + ((x.squaresum() - ((x.sum()*x.sum()) / N) )));
        log.debug("y>>" + y.squaresum());
        log.debug("y>>" + (y.sum()*y.sum()));
        log.debug("y>>" + ((y.squaresum() - ((y.sum()*y.sum()) / N) )));
        */

        return numr / denr;
    }

    /**
     * see kens doc for details
     *
     * @param x
     * @param yTemplate
     * @param z
     * @param usebiased
     * @param usemedian
     * @param fixlow
     * @return
     */
    public static double regressionSlope(final Vector x,
                                         final Vector yTemplate,
                                         final Vector[] splits,
                                         final boolean biased,
                                         final boolean fixlow) {

        enforceEqualSize(x, yTemplate);

        double xsum = x.sum(); // cache to avoid recalc

        double N = (double) x.getSize();
        double numrA = N * x.sumprod(yTemplate);
        double numrB = xsum * yTemplate.sum();

        double denrA = N * x.squaresum();
        double denrB = xsum * xsum;

        double C = (numrA - numrB) / (denrA - denrB);

        double var = 0;
        for (int i = 0; i < splits.length; i++) {
            var += splits[i].stddev(biased, fixlow);
        }

        //System.out.println("C\t" + C + "\tvarsum\t" + var);

        if (var == 0) {
            return Float.NaN;
        } else {
            return C / var;
        }

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
        return cosine(x.elementData, y.elementData);
    }

    public static double cosine(final float[] x, final float[] y) {

        enforceEqualSize(x, y);

        double mag_x = 0.0;
        double mag_y = 0.0;
        double sump = 0.0;

        for (int i = 0; i < x.length; i++) {
            mag_x += x[i] * x[i];
            mag_y += y[i] * y[i];
            sump += x[i] * y[i];
        }

        return 1.0d - (sump / Math.sqrt(mag_x * mag_y));
        //return (sump / Math.sqrt(mag_x * mag_y));
    }

    /**
     * s2n = mean1-mean2/root(var1+var2)
     * no root!!!
     */
    public static double s2n(final Vector x,
                             final Vector y,
                             final boolean usebiased,
                             final boolean usemedian,
                             final boolean fixlow) {

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
    
    public static final BiFunction<Vector, Vector, Double> getS2n(final boolean usebiased,
            final boolean usemedian,
            final boolean fixlow) {
        if (usemedian) {
            if (usebiased) {
                if (fixlow) return (x, y) -> s2nMedianBiasedFixLow(x, y);
                else return (x, y) -> s2nMedianBiasedNotFixLow(x, y);
            } else {
                if (fixlow) return (x, y) -> s2nMedianUnBiasedFixLow(x, y);
                else return (x, y) -> s2nMedianUnBiasedNotFixLow(x, y);
            }
        } else {
            if (usebiased) {
                if (fixlow) return (x, y) -> s2nMeanBiasedFixLow(x, y);
                else return (x, y) -> s2nMeanBiasedNotFixLow(x, y);
            } else {
                if (fixlow) return (x, y) -> s2nMeanUnBiasedFixLow(x, y);
                else return (x, y) -> s2nMeanUnBiasedNotFixLow(x, y);
            }
        }
    }
    
    // Specialized versions
    public static double s2nMeanBiasedFixLow(final Vector x, final Vector y) {

        return (x.mean() - y.mean()) / (x.stddevBiasedFixLow() + y.stddevBiasedFixLow());
    }

    public static double s2nMeanUnBiasedFixLow(final Vector x, final Vector y) {

        return (x.mean() - y.mean()) / (x.stddevUnBiasedFixLow() + y.stddevUnBiasedFixLow());
    }

    public static double s2nMeanBiasedNotFixLow(final Vector x, final Vector y) {

        return (x.mean() - y.mean()) / (x.stddevBiasedNotFixLow() + y.stddevBiasedNotFixLow());
    }

    public static double s2nMeanUnBiasedNotFixLow(final Vector x, final Vector y) {

        return (x.mean() - y.mean()) / (x.stddevUnBiasedNotFixLow() + y.stddevUnBiasedNotFixLow());
    }

    public static double s2nMedianBiasedFixLow(final Vector x, final Vector y) {

        return (x.median() - y.median()) / (x.stddevBiasedFixLow() + y.stddevBiasedFixLow());
    }

    public static double s2nMedianUnBiasedFixLow(final Vector x, final Vector y) {

        return (x.median() - y.median()) / (x.stddevUnBiasedFixLow() + y.stddevUnBiasedFixLow());
    }

    public static double s2nMedianBiasedNotFixLow(final Vector x, final Vector y) {

        return (x.median() - y.median()) / (x.stddevBiasedNotFixLow() + y.stddevBiasedNotFixLow());
    }

    public static double s2nMedianUnBiasedNotFixLow(final Vector x, final Vector y) {

        return (x.median() - y.median()) / (x.stddevUnBiasedNotFixLow() + y.stddevUnBiasedNotFixLow());
    }

    
    /**
     * @see http://trochim.human.cornell.edu/kb/stat_t.htm
     */
    public static double tTest(final Vector x,
                               final Vector y,
                               final boolean usebiased,
                               final boolean usemedian,
                               final boolean fixlow) {

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

    /**
     * @todo confirm formula with pt
     * @see http://trochim.human.cornell.edu/kb/stat_t.htm
     */
    public static double bhat(final Vector v1, final Vector v2, final boolean usebiased,
                              final boolean fixlow) {

        double firstterm = (v2.mean() - v1.mean())
                * (0.5 / (v1.var(usebiased, fixlow) + v2.var(usebiased, fixlow)))
                * (v2.mean() - v1.mean());
        double secterm = 0.5
                * Math.log(((v1.var(usebiased, fixlow) + v2.var(usebiased, fixlow)) / 2)
                / Math.sqrt(v1.var(usebiased, fixlow)
                * v2.var(usebiased, fixlow)));

        return (firstterm + secterm);
    }

    /**
     * Pearson will only work if the the 2 classes are of equal size. Because of the Exy term in the formula.
     */
    public static double pearsonD(final Vector minusplusvec, final Vector y) {

        // lengths still have to be equal
        enforceEqualSize(minusplusvec, y);

        // now x is the minusplus vector -- paranoid check
        for (int i = 0; i < minusplusvec.getSize(); i++) {
            if ((minusplusvec.getElement(i) != -1) || (minusplusvec.getElement(i) != +1)) {
                throw new IllegalArgumentException("MinusPlus vector has invalid element: "
                        + minusplusvec.getElement(i)
                        + " only -1 and +1 allowed");
            }
        }

        // normalize the vector
        Vector v = new Vector(y);

        v.pnormalize();

        // @note the 1-x: this is to abide by the Metric contract
        return 1 - pearson(minusplusvec, v);
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
	    return (d == +0.0d || d == -0.0d || Math.ulp(d) <= Double.MIN_VALUE);
	}

}    // End XMath
