/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.parsers.AuxUtils;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class BitSetDataset extends AbstractObject {

    // @note BitSet does not seem to work like an array - it has no max index etc
    // so cant work with its length/size as with a normal arrays
    // each BitSet is like a ROW
    private BitSet[] fBitSets;

    // these are Lists and not Sets as we want to be able to maintain the order
    // we however check for duplicates in the constructors

    /**
     * aka column names
     */
    private List<String> fcBitNames;

    /**
     * aka row names
     */
    private List<String> frBitSetNames;

    public BitSetDataset(final GeneSetMatrix gm) {

        // the UNION (
    	// TODO: review GeneSetMatrix and related for type safety
        List<String> bitNames = new ArrayList<String>(gm.getAllMemberNamesOnlyOnceS());

        BitSet[] bss = new BitSet[gm.getNumGeneSets()];
        List<String> bitSetNames = new ArrayList<String>(gm.getNumGeneSets());
        for (int i = 0; i < gm.getNumGeneSets(); i++) {
            bss[i] = new BitSet(bitNames.size());
            //System.out.println("AAT>> " + bitNames.size() + " " + (bss[i].size() - 1) + " " + bss[i].length() + " " + bss[i].get(bitNames.size() + 1000));
            bitSetNames.add(AuxUtils.getAuxNameOnlyNoHash(gm.getGeneSetName(i)));
        }

        for (int i = 0; i < bitNames.size(); i++) {
            String name = bitNames.get(i).toString();
            for (int g = 0; g < gm.getNumGeneSets(); g++) {
                GeneSet gset = gm.getGeneSet(g);
                if (gset.isMember(name)) {
                    bss[g].set(i);
                }
            }
        }

        String name = NamingConventions.generateName(gm);
        name = NamingConventions.removeExtension(name);
        this.init(name, bss, bitSetNames, bitNames, true);
    }

    /**
     * assumes that when i am called the data has already been duplicated
     * i.e callers within this method are responsible for passing me
     * already duplicated (or manipulated/subsetted) data.
     * Remeber BitSets are ROWS
     */
    private void init(final String bsname, final BitSet[] bss, final List<String> bitSetNames, final List<String> bitNames,
                      final boolean shareBitSets) {

        super.initialize(bsname);

        if (bss == null) {
            throw new IllegalArgumentException("Param bss cannot be null");
        }

        if (bitSetNames == null) {
            throw new IllegalArgumentException("bitSetNames cannot be null");
        }

        if (bitNames == null) {
            throw new IllegalArgumentException("Param bitNames cannot be null");
        }

        if (bss.length != bitSetNames.size()) {
            throw new IllegalArgumentException("BitSet length: " + bss.length + " and bitSetNames (row names): "
                    + bitSetNames.size() + " do not match in size");
        }

        // ensure all bss are of same size
        // also make sure they match the colsize

        if (bss.length > 0) {
            int size = bss[0].size();

            //Every bit set has a current size, which is the number of bits of space currently in use by the bit set.
            //Note that the size is related to the implementation of a bit set, so it may change with implementation.
            // The length of a bit set relates to logical length of a bit set and is defined independently of implementation.
            /* DONT KNOW why i cant do this size check??
            int length = bss[0].length();

            if (length != bitNames.size()) {
                throw new IllegalArgumentException("BitNames: " + bitNames.size() + " does not match BitSet length: " + length);
            }
            */

            for (int i = 0; i < bss.length; i++) {
                if (bss[i] == null) {
                    throw new IllegalArgumentException("BitSet cannot be null at: " + i);
                }
                if (bss[i].size() != size) {
                    throw new IllegalArgumentException("BitSets are not of equal length, expected: " + size + " but at: " + i + " got size: " + (bss[i].size()));
                }
            }

            /* @note cant do this check see note above for why
            if (bitNames.size() != size - 1) {
                System.out.println(bitNames);
                throw new IllegalArgumentException("Specified bitNames size: " + bitNames.size() + " not of same length as the bitSet: " + (size - 1));
            }
            */

        }

        if (shareBitSets) {
            this.fBitSets = bss;
        } else {
            log.debug("NON-sharing mode, so making deep copy of bitsets: {}", bss.length);
            this.fBitSets = new BitSet[bss.length];
            for (int i = 0; i < bss.length; i++) {
                this.fBitSets[i] = (BitSet) bss[i].clone();
            }
        }

        this.fcBitNames = Collections.unmodifiableList(bitNames);
        this.frBitSetNames = Collections.unmodifiableList(bitSetNames);

        // data integrity checks: no column names must be duplicated
        // ditto for rows but we avoid doing that as its sloooow
        ensureAllUniqueValues(frBitSetNames);
    }

    private static void ensureAllUniqueValues(final List<String> list) {

        if (list == null) {
            throw new IllegalArgumentException("Parameter list cannot be null");
        }

        Set<String> set = new HashSet<String>();
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (set.contains(item)) {
                throw new IllegalArgumentException("Duplicate COL names are NOT allowed in Datasets. The offending entry was: " + item + " at pos: " + i + "\n" + set);
            }
            set.add(item);
        }

        set.clear();
    }

    public Dataset toDataset() {
    	List<String> rowNames = new ArrayList<String>(frBitSetNames);
        List<String> colNames = new ArrayList<String>(fcBitNames);
        return new DefaultDataset(getName(), toMatrix(), rowNames, colNames, null);
    }

    // returns a safe copy
    public BitSet getBitSet(int rown) {
        return (BitSet) fBitSets[rown].clone();
    }

    public int getNumBitSets() {
        return fBitSets.length;
    }

    public int getNumBits() {
        return fcBitNames.size();
        // cant do this see above for why
        //return fBitSets[0].size() - 1;
    }

    public String getQuickInfo() {
        return fBitSets.length + "x" + fcBitNames.size();
    }

    public Matrix orMatrix() {

        Matrix matrix = new Matrix(getNumBitSets(), getNumBitSets());

        // @todo can optimize the loop as symmetrical if needed
        for (int r = 0; r < getNumBitSets(); r++) {
            for (int c = 0; c < getNumBitSets(); c++) {
                BitSet bs = getBitSet(r); // receives a safe copy
                bs.or(fBitSets[c]); // this op modifies bs but not fBitSets[c]
                matrix.setElement(r, c, bs.cardinality());
            }
        }

        return matrix;
    }

    public Matrix andMatrix(boolean fractionalize) {

        Matrix matrix = new Matrix(getNumBitSets(), getNumBitSets());

        for (int r = 0; r < getNumBitSets(); r++) {
            for (int c = 0; c < getNumBitSets(); c++) {
                BitSet bs = getBitSet(r); // receives a safe copy
                float size = bs.cardinality();
                bs.and(fBitSets[c]);

                float num = bs.cardinality();
                if (fractionalize) {
                    num = num / size;
                }
                matrix.setElement(r, c, num);
            }
        }

        return matrix;
    }


    public Dataset and_by_or() {
        log.debug("Doing and_by_or");
        return and_by_or(getName() + "AND_BY_OR");
    }

    public Dataset and_by_or(String name) {
        return new DefaultDataset(name, and_by_orMatrix(), frBitSetNames, frBitSetNames, null);
    }

    public Matrix and_by_orMatrix() {
        Matrix mOr = orMatrix();
        Matrix mAnd = new Matrix(andMatrix(false));
        mAnd.divide(mOr, true);
        return mAnd;
    }

    private Matrix toMatrix() {
        int numCols = getNumBits();
        final Matrix m = new Matrix(getNumBitSets(), numCols);

        for (int r = 0; r < getNumBitSets(); r++) {
            for (int c = 0; c < getNumBits(); c++) {
                if (fBitSets[r].get(c)) {
                    m.setElement(r, c, 1);
                } else {
                    m.setElement(r, c, 0);
                }
            }
        }

        // dont set as immutable
        return m;
    }
}
