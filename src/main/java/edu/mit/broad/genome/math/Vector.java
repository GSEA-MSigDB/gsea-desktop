/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.utils.ImmutedException;
import gnu.trove.TFloatArrayList;

import java.util.*;

/**
 * Lots of code and ideas copied form GVector.
 * <p/>
 * vector -> encapsulates an array of flaots
 * <p/>
 * Vector is very very mutable!
 * @maint IMP IMP IMP IMP
 * When adding methods that alter data in this Vector, make sure
 * to do a immutability check
 * <p/>
 * why not use GVectpor dfirecly -> uses double while we can do with float.
 * has some inapp methods.
 * <p/>
 * * A float precision, general, and dynamically resizeable one
 * dimensional vector class. Index numbering begins with zero.
 * <p/>
 * Notes:
 * - before adding methoids you might want to check if already mimpl in GVector
 * - note that the constrictors do a sysarray copy for safety
 *
 * @author Aravind Subramanian
 * @author David Eby
 */
public class Vector {
    // note: only elementCount data in elementData are valid.
    // elementData.length is the allocated size.
    // invariant: elementData.length >= elementCount.
    // @note (as) making these protected for package friendly use (by Matrix etc)

    /**
     * The number of elements in this vector
     */
    private int elementCount;

    /**
     * The raw floats
     */
    protected float elementData[];

    /**
     * Holds computed set of data
     */
    public transient ComputeSet computeset = new ComputeSet();

    /**
     * Flag for whether this Vector can have its values altered or not
     */
    private boolean fImmuted;

    /**
     * Adjust any Infinite or NaN values in the given ranked score vector to (+/-)0.000001f.
     * *NOTE*: this method assumes but does not check that the vector is *rank ordered*.  It
     * is meant to be called after RankedList.getScoresV(boolean), for example. 
     */
    public static final Vector infinityAdjustRankedScoreVector(Vector vec) {
    	if (vec.elementCount == 0) { return vec; }
    	
    	// Since the scores are ranked, if there are any Infinite or NaN scores they must
    	// appear either at the very beginning or the very end.  There may be *other* such
    	// values, but they must at least be in one (or both) of those positions.  If not,
    	// we can skip the adjustments
    	boolean finiteHead = false;
    	float startVal = vec.elementData[0];
        if (Float.isNaN(startVal)) { startVal = 0.000001f; }
        else if (Float.isInfinite(startVal)) { startVal = (startVal < 0) ? -0.000001f : 0.000001f; }
        else finiteHead = true;

        final int end = vec.elementCount - 1;
		float endVal = vec.elementData[end];
        if (Float.isNaN(endVal)) { endVal = 0.000001f; }
        else if (Float.isInfinite(endVal)) { endVal = (endVal < 0) ? -0.000001f : 0.000001f; }
        else if (finiteHead) { return vec; }  // Finite values found in both Head & Tail position; no adjustments made
    	
        if (vec.fImmuted) { vec = new Vector(vec); }
		
        // We don't need to iterate over these two positions since they were already checked above.
        vec.elementData[0] = startVal;
        vec.elementData[end] = endVal;
        // Work from the beginning going up, but break as soon as we find a Finite value.  There can't be
        // any NaN or Infinite values after that until the very end of the list, which will be covered by
        // loop below.  Note that we avoid re-checking array positions 0 & end since they were already
        // adjusted above if necessary.
        for (int fwdPtr = 1; fwdPtr < end; fwdPtr++) {
        	final float score = vec.elementData[fwdPtr];
	        if (Float.isNaN(score)) { vec.elementData[fwdPtr] = 0.000001f; }
	        else if (Float.isInfinite(score)) { vec.elementData[fwdPtr] = (score < 0) ? -0.000001f : 0.000001f; }
	        else break;
        }
        // Likewise, work from the end going down.  Note that this might halt immediately in the pathological
        // case where *all* values were NaN or Infinite.  The above loop would have made that adjustment.
        for (int bkwdPtr = end - 1; bkwdPtr > 1; bkwdPtr--) {
        	final float score = vec.elementData[bkwdPtr];
	        if (Float.isNaN(score)) { vec.elementData[bkwdPtr] = 0.000001f; }
	        else if (Float.isInfinite(score)) { vec.elementData[bkwdPtr] = (score < 0) ? -0.000001f : 0.000001f; }
	        else break;
        }

        return vec;
    }

    /**
     * Class Constructor.
     * Constructs a new generalized mathematic Vector with zero value (0.0)
     * elements; length represents the number of elements in the
     * vector.
     *
     * @param length number of elements in this vector.
     */
    public Vector(final int length) {
        elementCount = length;
        elementData = new float[length];    // will be initialized to 0.0
        if (computeset == null) {
            computeset = new ComputeSet();
        }
    }

    public Vector(final float values[], final boolean share) {
        this.elementCount = values.length;
        if (share) {
            this.elementData = values;
        } else {
            this.elementData = new float[elementCount];
            System.arraycopy(values, 0, elementData, 0, elementCount);
        }
        if (computeset == null) {
            computeset = new ComputeSet();
        }
    }

    /**
     * Class Constructor.
     * Vector is constructed by appending elements of vectors in order.
     * So, length of this Vector is the sum of lengths of individual vectors.
     * Resulting vector  is NOT immutable even if the specified Vector was immutable
     * (permits manipulation)
     *
     * @param vectors
     */
    public Vector(final Vector[] vectors) {

        if (vectors == null) {
            throw new IllegalArgumentException("Param vectors cannot be null");
        }

        int length = 0;

        for (int i = 0; i < vectors.length; i++) {
            length += vectors[i].getSize();
        }

        this.elementCount = length;
        this.elementData = new float[length];    // will be initialized to 0.0

        int pos = 0;

        for (int i = 0; i < vectors.length; i++) {
            System.arraycopy(vectors[i].elementData, 0, elementData, pos, vectors[i].elementCount);
            pos += vectors[i].elementCount;
        }
        if (computeset == null) {
            computeset = new ComputeSet();
        }
    }

    /**
     * Class Constructor.
     * Data IS copied - not shared.
     * Constructs a new generalized mathematic Vector with zero
     * elements; length represents the number of elements in the
     * vector. !! this comment is a bug in Sun's API !!
     *
     * @param values the values for the new vector.
     */
    public Vector(final float values[]) {
        this(values, false);
    }

    public Vector(final List<DoubleElement> dels) {
        this(DoubleElement.toFloats(dels));
    }

    public Vector(final Vector v, final boolean share) {
        this(v.elementData, share);
    }

    /**
     * Class Constructor.
     * Data IS copied - not shared.
     *
     * @param values
     */
    public Vector(final double[] values) {

        this(values.length);

        // http://groups.google.com/groups?hl=en&lr=&ie=UTF-8&threadm=f11f7eb5.0207310307.16d8597a%40posting.google.com&rnum=3&prev=/groups%3Fq%3Dken%2Bstickler%26hl%3Den%26lr%3D%26ie%3DUTF-8%26selm%3Df11f7eb5.0207310307.16d8597a%2540posting.google.com%26rnum%3D3
        // arraycopy is fast because it can just blat bits around; but bit blatting
        // only works when the types of the source and destination array slices are
        // the *same*. I bet you think that's a blot.]
        // so lop based copy seems the only way + is fast enough
        for (int i = 0; i < values.length; i++) {
            elementData[i] = (float) values[i];
        }

        // doesnt work
        //System.arraycopy(values, 0, elementData, 0, elementCount);
    }

    /**
     * Class Constructor.
     * Data IS copied - no sharing
     *
     * @param f
     */
    // TODO: switch to List<Float>
    public Vector(final TFloatArrayList f) {
        this(f.toNativeArray());
    }

    /**
     * Class Constructor.
     * Data IS copied - no sharing
     * Constructs a new Vector and copies the initial values from
     * the parameter vector.
     * Resulting vector  is NOT immutable even if the specified Vector was immutable
     * (permits manipulation)
     *
     * @param vector the source for the new Vector's initial values
     */
    public Vector(Vector vector) {
        this(vector.elementCount, vector);    // al elements
    }

    /**
     * Class Constructor.
     * Data IS copied - no sharing
     * Constructs a new Vector and copies the initial values from
     * the parameter vector.
     * Resulting vector  is NOT immutable even if the specified Vector was immutable
     * (permits manipulation)
     *
     * @param vector the source for the new Vector's initial values
     */
    public Vector(final int numelementstouse, final Vector vector) {

        this(numelementstouse);

        System.arraycopy(vector.elementData, 0, elementData, 0, numelementstouse);

        //this.fImmutable = vector.fImmutable; // NO DONT -- see note above
    }

    /**
     * @return sum of all elements in the Vector.  NOTE: this assumes that a "NaN-safe" computation is not necessary, for example 
     * because this is being called on a Vector that is the output of toVectorNaNless().  This should be slightly
     * more performant than sumNaNsafe().  Summing an empty Vector, however, still returns an NaN result.
     */
    public double sum() {
    	if (elementCount == 0) { return Double.NaN; }
        double sum = 0;
        for (int i = 0; i < elementCount; i++) {
            sum += elementData[i];
        }
        return sum;
    }

    /**
     * Does a "NaN-safe" sum calculation, where any NaN values are ignored for the purpose of the computation.
     * Summing an empty Vector, however, still returns an NaN result.
     */
    public double sumNaNsafe() {
        return XMath.sum(elementData);
    }

    public double sum(int startIndexInclusive, int stopIndexEXclusive) {
        double sum = 0;
        for (int i = startIndexInclusive; i < stopIndexEXclusive; i++) {
            sum += elementData[i];
        }

        return sum;
    }

    /**
     * Sum of the squares of every element.  NOTE: this assumes that a "NaN-safe" computation is not necessary, for example 
     * because this is being called on a Vector that is the output of toVectorNaNless().  
     * Calling on an empty Vector returns an NaN result.
     */
    // IMP that return doubles!!
    public double squaresum() {
        if (elementCount == 0) { return Double.NaN; }
        double squaresum = 0;
        for (int i = 0; i < elementCount; i++) {
            final float value = elementData[i];
            squaresum += value * value;
        }
        return squaresum;
    }

    /**
     * @return arithmetic mean.  NOTE: this assumes that a "NaN-safe" computation is not necessary, for example 
     * because this is being called on a Vector that is the output of toVectorNaNless().  This should be slightly
     * more performant than meanNaNsafe().  Calling on an empty Vector, however, still returns an NaN result.
     */
    public double mean() {
        computeset.mean = sum() / elementCount;
        return computeset.mean;
    }
    
    /**
     * Does a "NaN-safe" mean calculation, where any NaN values are ignored for the purpose of the computation.
     * Calling on an empty Vector, however, still returns an NaN result.
     */
    public double meanNaNsafe() {
        return XMath.mean(elementData);
    }

    public double meanOrMedian(final boolean useMean) {
        if (useMean) {
            return mean();
        } else {
            return median();
        }
    }

    public double mean(final int startIndexInclusive, final int stopIndexEXclusive) {
        return sum(startIndexInclusive, stopIndexEXclusive) / (stopIndexEXclusive - startIndexInclusive);
    }

    /**
     * Calculates the median.  This is NaN-safe.
     */
    public double median() {
        return XMath.median(elementData);
    }
    
    private Vector fNaNless;

    public Vector toVectorNaNless() {
        if (fNaNless == null) {
            int size = getSize();
            float[] nanlessArr = new float[size];
            int pos = 0;
            for (int i = 0; i < size; i++) {
                final float val = elementData[i];
                if (!Float.isNaN(val)) { nanlessArr[pos++] = val; }
            }

            if (pos == size) {
                fNaNless = this; // i.e no nans in the data
            } else {
                // Truncate the nanlessArr to just the values (since pos < size)
                fNaNless = new Vector(Arrays.copyOf(nanlessArr, pos));
                fNaNless.setImmutable();
            }
        }

        return fNaNless;
    }

    public Vector synchVectorNaNless(Vector other) {
        if (elementCount != other.elementCount) { 
            throw new IllegalArgumentException("Other vector must be equal length to synch NaNless values");
        }
        // Force population of fNaNless on this Vector since we need it anyway and it allows a fast short-circuit comparison:
        // if there were no NaN values then just return the original 'other' Vector.
        toVectorNaNless();
        if (this == fNaNless) { return other; }
        
        final float[] otherNaNless = new float[fNaNless.elementCount];
        for (int i = 0, pos = 0; i < other.elementCount; i++) {
            if (!Float.isNaN(elementData[i])) { otherNaNless[pos++] = other.elementData[i]; }
        }
        return new Vector(otherNaNless, true);
    }
    
    /**
     * Trouble is the fixing low thing. Know how to do that
     * for stddevs but nto vars. So, get var, getsttdev,m fix and then get var!
     * <p/>
     * var = sum(x_i-mean)**2 / N
     *
     * @return the variance of the vector unbiased (div by n-1) or biased(divide by n)
     * @see http://davidmlane.com/hyperstat/A16252.html
     *      unbiased variance -> n-1
     *      biased variance -> using n
     *      Most commonly used is unbiased.
     *      MIT generally does NOT use biased -> FALSE
     */
    public double var(boolean biased, boolean fixlow) {
        if (fixlow) {
            stddev(biased, fixlow);
            double var = computeset.stddev * computeset.stddev;
            computeset.var = var;
            return var;
        } else {
            return _var(biased);
        }
    }

    /**
     * the real var calcs are here
     *
     * @param biased
     * @return
     */
    private double _var(boolean biased) {
        double oldvar = 0.0;
        int len = elementCount;

        if (!biased) {
            len--;
        }

        // Variance of 1 point is 0 (we are returning the biased variance in this case)
        if (len <= 0) {
            return oldvar;
        }

        double mean = mean();

        computeset.mean = mean;

        for (int i = 0; i < elementCount; i++) {
            double tmp = elementData[i] - mean;
            oldvar += tmp * tmp;
        }

        // DONT set computeset!
        return oldvar / len;
    }

    /**
     * Some heuristics for adjusting variance based on data from affy chips
     * NOTE: problem occurs when we threshold to a value, then that artificially
     * reduces the variance in the data
     * <p>        
     * First, we make the variance at least a fixed percent of the mean
     * If the mean is too small, then we use an absolute variance
     * <p>       
     * However, we don't want to bias our algs for affy data, e.g. we may
     * get data in 0..1 and in that case it is not appropriate to use
     * an absolute standard deviation of 10 - will kill the signal.
     * @return The std aa_indev of vector
     */
    public double stddev(boolean biased, boolean fixlow) {
        double stddev = Math.sqrt(_var(biased));    // @note call to _var and not var
        double mean = computeset.mean;                    // avoid recalc

        if (fixlow) {
            double minallowed = XMath.isNearlyZero(mean) ? 0.20 : (0.20 * Math.abs(mean));
            stddev = Math.max(stddev, minallowed);
        }
        computeset.stddev = stddev;

        return computeset.stddev;
    }

    /**
     * @return the highest value element of this Vector
     */
    public float max() {
        float max = Float.NEGATIVE_INFINITY;
        int maxat = -1;

        for (int i = 0; i < elementCount; i++) {
            if (elementData[i] > max) {
                max = elementData[i];
                maxat = i;
            }
        }

        if (computeset == null) {
            computeset = new ComputeSet();
        }
        computeset.max = max;
        computeset.maxindex = maxat;

        if (maxat == -1) {
            System.out.println("WARNING: could not find max for: " + toString(','));
            max = Float.NaN;
        }

        return max;
    }

    public int maxAtIndex() {
        float max = Float.NEGATIVE_INFINITY;
        int maxat = -1;

        for (int i = 0; i < elementCount; i++) {
            if (elementData[i] > max) {
                max = elementData[i];
                maxat = i;
            }
        }

        if (maxat == -1) {
            throw new IllegalStateException("No max found. Values are:\n" + Printf.outs(elementData));
        }

        computeset.max = max;
        computeset.maxindex = maxat;

        return maxat;
    }

    // not that max can be max +ve or max -ves
    public float maxDevFrom0() {
        float maxPositive = max();
        float minNegative = min();

        if (Math.abs(minNegative) > maxPositive) {
            return minNegative;
        } else {
            return maxPositive;
        }
    }

    public int maxDevFrom0Index() {
        float maxPositive = max();
        float minNegative = min();

        if (Math.abs(minNegative) > maxPositive) {
            return minAtIndex();
        } else {
            return maxAtIndex();
        }
    }

    /**
     * @return the least value element of this Vector
     */
    public float min() {
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < elementCount; i++) {
            if (elementData[i] < min) {
                min = elementData[i];
            }
        }

        computeset.min = min;

        return min;
    }

    public int minAtIndex() {
        float min = Float.POSITIVE_INFINITY;
        int minat = -1;

        for (int i = 0; i < elementCount; i++) {
            if (elementData[i] < min) {
                min = elementData[i];
                minat = i;
            }
        }

        if (minat == -1) {
            throw new IllegalStateException("No max found. Values are:\n" + Printf.outs(elementData));
        }

        computeset.min = min;
        computeset.minindex = minat;
        return minat;
    }

    /**
     * sum of products of elements of 2 vectors
     * Sum xi * yi
     * NOTE: this assumes that a "NaN-safe" computation is not necessary, for example 
     * because this is being called on a Vector that is the output of toVectorNaNless().
     */
    public double sumprod(Vector y) {
        // TODO: eval for performance since it's only used in the context of Pearson.
        // Possibly just move this to XMath since that's the only user.
        // - No need to check size since we did that earlier.
        // - use elementData array refs directly
        final int xSize = elementCount;
        if (xSize != y.getSize()) {
            throw new RuntimeException("Unequal vector sizes x: " + xSize + " y: " + y.getSize());
        }

        double prodsum = 0;
        for (int i = 0; i < xSize; i++) {
            float xVal = getElement(i);
            float yVal = y.getElement(i);
            prodsum += xVal * yVal;
        }

        return prodsum;
    }

    /**
     * Vector gets sorted internally
     * from min to max.
     * (the real value of every element is used)
     * Vector data order DOES change
     */
    // There's an issue here: with Arrays.sort(), NaN is sorted as *greater than* all other elements.
    // We want a different order (at least in some cases; or is it all?).  It might be enough to 
    // instead store the elementData as Float[] and use Null instead of NaN as those (should) simply
    // sort to the least value naturally.  Will need to be careful what other effects might come of
    // that, though.  Need to check for Null everywhere?
    public void sort() {
        checkImmutable();
        // Possible optimization: Arrays.parallelSort()
        Arrays.sort(elementData);
    }

    /**
     * Vector gets sorted internally from max to min
     * (the real value of every element is used)
     * Vector elements DO change
     */
    // Note: same issue as above.  Same possible optimization, too.
    public void revsort() {
        checkImmutable();
        Arrays.sort(elementData);
        reverse(); // maybe use a comparator to sort elements into descending order directly
    }

    public void abs() {
        checkImmutable();
        for (int i = 0; i < elementData.length; i++) {
            elementData[i] = Math.abs(elementData[i]);
        }
    }

    /**
     * @param mode
     * @param order
     */
    public void sort(SortMode mode, Order order) {
        checkImmutable();

        if (mode.isAbsolute()) {
            abs();
        }

        if (order.isAscending()) {
            sort();
        } else {
            revsort();
        }
    }

    /**
     * The vector's elements are reversed.
     * Vector elements DO change.
     */
    public void reverse() {
        checkImmutable();

        float tmp[] = new float[elementData.length];
        int start = elementCount - 1;

        for (int i = 0; i < elementCount; i++) {
            tmp[start - i] = elementData[i];
        }

        elementData = tmp;
    }

    /**
     * Returns the number of elements in this vector.
     *
     * @return number of elements in this vector
     */
    public int getSize() {
        return elementCount;
    }

    public int getSize(ScoreMode smode) {
        if (smode.isPostiveAndNegTogether()) {
            return getSize();
        } else if (smode.isPostiveAndNegSeperately()) {
            throw new IllegalArgumentException("Not a valid score mode");
        } else if (smode.isPostiveOnly()) {
            int cnt = 0;
            for (int i = 0; i < getSize(); i++) {
                if (XMath.isPositive(getElement(i))) {
                    cnt++;
                }
            }

            return cnt;

        } else if (smode.isNegativeOnly()) {
            int cnt = 0;
            for (int i = 0; i < getSize(); i++) {
                if (XMath.isNegative(getElement(i))) {
                    cnt++;
                }
            }

            return cnt;
        } else {
            throw new IllegalArgumentException("Unknown score mode: " + smode);
        }
    }

    /**
     * Retrieves the value at the specified index value of this
     * vector.
     *
     * @param index the index of the element to retrieve (zero indexed)
     * @return the value at the indexed element
     */
    public float getElement(int index) {
        try {
            return elementData[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("index:" + index + " must be in [0, "
                    + (elementCount - 1) + "]");
        }
    }

    /**
     * Modifies the value at the specified index of this vector.
     *
     * @param index the index if the element to modify (zero indexed)
     * @param value the new vector element value
     */
    public void setElement(int index, float value) {
        checkImmutable();

        try {
            elementData[index] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("index:" + index + " must be in [0, "
                    + (elementCount - 1) + "]");
        }
    }

    public void setElement(int index, double value) {
        setElement(index, (float) value);
    }

    public String toString() {
        if (getSize() == 0) {
            return "";
        }

        StringBuilder buf = new StringBuilder();

        buf.append("(");

        for (int i = 0; i < elementCount - 1; i++) {
            buf.append(elementData[i]);
            buf.append(",");
        }

        buf.append(elementData[elementCount - 1]);
        buf.append(")");

        return buf.toString();
    }

    public String toString(char delim) {
        if (getSize() == 0) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < elementCount - 1; i++) {
            if (!Float.isNaN(elementData[i])) {
                buf.append(elementData[i]);
            }
            buf.append(delim);
        }

        if (!Float.isNaN(elementData[elementCount - 1])) {
            buf.append(elementData[elementCount - 1]);
        } else {
            buf.append(delim);
        }

        return buf.toString();
    }

    public double[] toArrayDouble() {
        double[] dest = new double[elementData.length];
        for (int i = 0; i < elementData.length; i++) {
            dest[i] = elementData[i];
        }

        return dest;
    }

    /**
     * Returns a hash number based on the data values in this
     * object.  Two different Vector objects with identical data values
     * (ie, returns true for equals(Vector) ) will return the same hash
     * number.  Two objects with different data members may return the
     * same hash value, although this is not likely.
     *
     * @return the integer hash value
     */
    public int hashCode() {
        int hash = 0;

        for (int i = 0; i < elementCount; i++) {
            long bits = Double.doubleToLongBits(elementData[i]);

            hash ^= (int) (bits ^ (bits >> 32));
        }

        return hash;
    }

    /**
     * Returns true if all of the data members of Vector vector1
     * are equal to the corresponding data members in this Vector.
     *
     * @param vector1 The vector with which the comparison is made.
     * @return true or false
     */
    public boolean equals(Vector vector1) {
        if (vector1 == null) {
            return false;
        }

        if (elementCount != vector1.elementCount) {
            return false;
        }

        float[] v1data = vector1.elementData;

        for (int i = 0; i < elementCount; i++) {
            if (elementData[i] != v1data[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the Object o1 is of type Vector and all of the data
     * members of t1 are equal to the corresponding data members in this
     * Vector.
     *
     * @param o1 the object with which the comparison is made.
     */
    public boolean equals(Object o1) {
        return (o1 != null) && (o1 instanceof Vector) && equals((Vector) o1);
    }

    /**
     * Make this Vector immutable.
     */
    public void setImmutable() {
        this.fImmuted = true;
    }

    private void checkImmutable() {
        if (fImmuted) {
            throw new ImmutedException();
        }
    }

    public Vector extract(float someScoreThatDecidesMode, ScoreMode smode) {
        if (smode.isPostiveAndNegSeperately()) {
            if (XMath.isPositive(someScoreThatDecidesMode)) {
                return extract(ScoreMode.POS_ONLY);
            } else {
                return extract(ScoreMode.NEG_ONLY);
            }
        } else {
            return extract(smode);
        }
    }

    public Vector extract(ScoreMode smode) {
        TFloatArrayList floats;
        if (smode.isPostiveAndNegTogether()) {
            return this; // no change
        } else if (smode.isPostiveOnly()) {
            floats = new TFloatArrayList();
            for (int i = 0; i < getSize(); i++) {
                float elem = getElement(i);
                if (XMath.isPositive(elem)) {
                    floats.add(elem);
                }
            }
        } else if (smode.isNegativeOnly()) {
            floats = new TFloatArrayList();
            for (int i = 0; i < getSize(); i++) {
                float elem = getElement(i);
                if (XMath.isNegative(elem)) {
                    floats.add(elem);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown smode: " + smode.getName());
        }

        return new Vector(floats.toNativeArray(), true);
    }

    /**
     * These are set each time a mean, media etc call is made.
     * If the vector changes, they do NOT auto change! Only change after the compute call is made again.
     * see metrics and the nnalg for whys this is a useful concept.
     */
    public class ComputeSet {
        public double mean = Float.NaN;
        public final double median = Float.NaN;
        public double stddev = Float.NaN;
        public double max = Float.NaN;
        public double min = Float.NaN;
        public double var = Float.NaN;
        public int maxindex = -1;
        public int minindex = -1;
    }
}
