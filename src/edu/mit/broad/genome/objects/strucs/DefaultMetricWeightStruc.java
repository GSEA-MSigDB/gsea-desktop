/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.strucs;

import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.LabelledVector;
import edu.mit.broad.genome.objects.MetricWeightStruc;
import edu.mit.broad.genome.objects.RankedList;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public class DefaultMetricWeightStruc implements MetricWeightStruc {

    private float fTotalPosWeight;
    private float fTotalNegWeight;

    private int fTotalPosLength;
    private int fTotalNegLength;

    private String fMetricName;

    private static final Logger klog = XLogger.getLogger(DefaultMetricWeightStruc.class);

    /**
     * Class constructor
     *
     * @param rl
     */
    public DefaultMetricWeightStruc(final String metricName, final RankedList rl) {

        if (rl == null) {
            throw new IllegalArgumentException("Param rl cannot be null");
        }

        this.fMetricName = metricName;

        // this.fRankedList = rl;

        for (int r = 0; r < rl.getSize(); r++) {
            float score = rl.getScore(r);
            if (XMath.isNegative(score)) {
                fTotalNegWeight += score;
                fTotalNegLength++;
            } else {
                fTotalPosWeight += score;
                fTotalPosLength++;
            }
        }

    }

    /**
     * Class constructor
     *
     * @param metricName
     * @param lv
     */
    public DefaultMetricWeightStruc(final String metricName, final LabelledVector lv) {

        if (lv == null) {
            throw new IllegalArgumentException("Param lv cannot be null");
        }

        for (int r = 0; r < lv.getSize(); r++) {
            float score = lv.getScore(r);
            if (XMath.isNegative(score)) {
                fTotalNegWeight += score;
                fTotalNegLength++;
            } else {
                fTotalPosWeight += score;
                fTotalPosLength++;
            }
        }

        this.fMetricName = metricName;
    }

    public String getMetricName() {
        return fMetricName;
    }

    public void setMetricName(String name) {
        if (fMetricName != null) {
            klog.warn("Overwriting metric: " + fMetricName + " with: " + name);
            TraceUtils.showTrace();
        }
        this.fMetricName = name;
    }

    public float getTotalPosWeight() {
        return fTotalPosWeight;
    }

    public float getTotalNegWeight() {
        return fTotalNegWeight;
    }

    public int getTotalPosLength() {
        return fTotalPosLength;
    }

    public int getTotalNegLength() {
        return fTotalNegLength;
    }

    public float getTotalWeight() {
        return getTotalPosWeight() + Math.abs(getTotalNegWeight());
    }

    public int getTotalLength() {
        return getTotalPosLength() + getTotalNegLength();
    }

    public float getTotalNegLength_frac() {
        return (float) getTotalNegLength() / (float) getTotalLength();
    }

    public float getTotalPosLength_frac() {
        return (float) getTotalPosLength() / (float) getTotalLength();
    }

    public float getTotalNegWeight_frac() {
        return Math.abs(getTotalNegWeight() / getTotalWeight());
    }

    public float getTotalPosWeight_frac() {
        return getTotalPosWeight() / getTotalWeight();
    }

} // End class DefaultMetricWeightStruc
