/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import gnu.trove.TFloatIntHashMap;
import gnu.trove.TObjectIntHashMap;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class GeneSetStats {

    private final Logger log = XLogger.getLogger(GeneSetStats.class);

    public static TObjectIntHashMap featureFreq(String[] feats) {
    
        // IMP needed as returns 0 and not -1 on no hits!!
        TObjectIntHashMap map = new TObjectIntHashMap();
    
        for (int i = 0; i < feats.length; i++) {
            int curr = 0;
            if (map.containsKey(feats[i])) {
                curr = map.get(feats[i]);
            }
            curr++;
            map.put(feats[i], curr);
        }
    
        return map;
    }

    /**
     * Class Constructor.
     */
    public GeneSetStats() {
    }

    // rec = number of unique 2mers / total number of possible 2 mers
    // NOT same as co-occurrence
    public RedStruc calcRedundancy(final GeneSet[] gsets, final boolean do2mersalso) {

        RedStruc rs = new RedStruc();
        if (do2mersalso) {
            rs.allFoundTwoMers = new HashSet();
            for (int i = 0; i < gsets.length; i++) {
                int n = gsets[i].getNumMembers();
                int num = n * (n - 1) / 2;
                Set s = hashTwoMers(gsets[i]);
                if (s.size() != num) {
                    log.warn("Bad: " + s.size() + " " + num);
                    //throw new IllegalStateException("Bad: " + s.size() + " " + num);
                }
                rs.allFoundTwoMers.addAll(s);
                rs.totNumPairsPossible += num;
            }
        }

        //      * Sets as rows, genes as columns. Aligned
        BitSetDataset bsd = new BitSetDataset(new DefaultGeneSetMatrix("foo", gsets));
        Dataset ds = bsd.and_by_or();

        LabelledVector lv = getFeatureFrequency_lv(gsets);
        rs.featureFreq = lv.sort(SortMode.REAL, Order.DESCENDING);

        float sum = 0;
        // @note IMP intentionally using hash set so that we dont have to worry about nums
        rs.jaccardDistrib = new TFloatIntHashMap();
        for (int r = 0; r < ds.getNumRow(); r++) {
            for (int c = 0; c < ds.getNumCol(); c++) {
                sum += ds.getElement(r, c);
                if (r < c) {
                    int curr = rs.jaccardDistrib.get(ds.getElement(r, c));
                    curr++;
                    rs.jaccardDistrib.put(ds.getElement(r, c), curr);
                }
            }
        }

        rs.jaccardMean = sum / (ds.getNumRow() * ds.getNumRow());

        //log.info("Total possible: " + rs.totNumPairsPossible + " from sets: " + gsets.length + " total unique seen: " + rs.allFoundTwoMers.size());

        return rs;
    }

    // @todo optimize
    public Set hashTwoMers(final GeneSet gset) {
        Set set = new HashSet();
        for (int i = 0; i < gset.getNumMembers(); i++) {
            String a = gset.getMember(i);
            for (int j = 0; j < gset.getNumMembers(); j++) {
                if (j != i) {
                    set.add(new TwoMer(a, gset.getMember(j)));
                }
            }
        }

        return set;
    }

    public static class RedStruc {

        public int totNumPairsPossible;
        public Set allFoundTwoMers;

        public float jaccardMean;
        public TFloatIntHashMap jaccardDistrib;

        public RankedList featureFreq;
    }

    // more efficient to make the call with all gsets as we do the gmann qualification

    public TObjectIntHashMap getFeatureFrequency(final GeneSetMatrix gm) {
        final String[] feats = gm.getAllMemberNames();
        return GeneSetStats.featureFreq(feats);
    }

    public LabelledVector getFeatureFrequency_lv(final GeneSet[] gsets) {
        return getFeatureFrequency_lv(new DefaultGeneSetMatrix("tmp", gsets));
    }

    public LabelledVector getFeatureFrequency_lv(final GeneSetMatrix gm) {
        TObjectIntHashMap map = getFeatureFrequency(gm);

        String[] uniqFeats = gm.getAllMemberNamesOnlyOnce();
        Vector v = new Vector(uniqFeats.length);
        for (int i = 0; i < uniqFeats.length; i++) {
            int cnt = map.get(uniqFeats[i]);
            v.setElement(i, cnt);
        }

        return new LabelledVector(uniqFeats, v);
    }


} // End class GeneSetMatrixStats

