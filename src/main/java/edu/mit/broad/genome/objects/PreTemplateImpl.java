/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Aravind Subramanian
 */
public class PreTemplateImpl extends AbstractPreTemplate {
    private Pair[] fPairs;
    private Map fSampleNamePairMap;

    private boolean fContinuous;

    private int fNumClasses;

    /**
     * Class constructor
     *
     * @param justPhenoName
     * @param pairs
     */
    public PreTemplateImpl(final String sourcefileName,
                           final String justPhenoName,
                           final Pair[] pairs, boolean continuous) {
        super(sourcefileName + "#" + justPhenoName);

        // checks
        for (int i = 0; i < pairs.length; i++) {
            if (pairs[i] instanceof NumPair && !continuous) {
                throw new IllegalArgumentException("NumPair but not continuous");
            }

            if (pairs[i] instanceof StringPair && continuous) {
                throw new IllegalArgumentException("StringPair but continuous");
            }

        }

        this.fPairs = pairs;
        this.fContinuous = continuous;

        this.fSampleNamePairMap = new HashMap();
        Set uniqValues = new HashSet();
        for (int p = 0; p < pairs.length; p++) {
            if (pairs[p] == null) {
                throw new IllegalArgumentException("Param pairs cannot be null at index: " + p);
            }
            if (fSampleNamePairMap.containsKey(pairs[p].getSampleName())) {
                throw new IllegalArgumentException("Duplicate sample names: " + pairs[p].getSampleName());
            }

            this.fSampleNamePairMap.put(pairs[p].getSampleName(), pairs[p]);
            if (!fContinuous) {
                uniqValues.add(pairs[p].getValue());
            }
        }

        if (fContinuous) {
            this.fNumClasses = fSampleNamePairMap.size();
        } else {
            this.fNumClasses = uniqValues.size();
        }
    }

    public int getNumClasses() {
        return fNumClasses;
    }

    public boolean isContinuous() {
        return fContinuous;
    }

    public boolean isCategorical() {
        return !fContinuous;
    }

    public boolean isAux() {
        if (fContinuous) {
            return true;
        } else {
            return false; // @note is always aux
        }
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer();
        if (isContinuous()) {
            buf.append(fPairs.length).append("=>C");
        } else {
            buf.append(fPairs.length).append("=>").append(getNumClasses());
        }

        return buf.toString();
    }

    public static interface Pair {
        public String getSampleName();

        public Object getValue();

    }

    public static class StringPair implements Pair {
        String sampleName;
        String className;

        public StringPair(final String sampleName, final String className) {
            this.sampleName = sampleName;
            this.className = className;
        }

        public String getSampleName() {
            return sampleName;
        }

        public Object getValue() {
            return className;
        }
    }

    public static class NumPair implements Pair {
        String sampleName;
        float classValue;

        public NumPair(final String sampleName, final String classValue) {
            this.sampleName = sampleName;
            this.classValue = Float.parseFloat(classValue);
        }

        public String getSampleName() {
            return sampleName;
        }

        public Object getValue() {
            return classValue;
        }
    }
}
