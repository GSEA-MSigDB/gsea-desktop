/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.markers;

import edu.mit.broad.genome.alg.DatasetMetrics;
import edu.mit.broad.genome.alg.DatasetStatsCore;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.RankedListGenerators;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;
import gnu.trove.TIntArrayList;

import java.util.Map;

/**
 * NamingConventions permutation tests
 *
 * @author Aravind Subramanian
 */
public class PermutationTestBuilder extends AbstractPermutationTest {

    private Template[] fRndTemplates;

    private ScoredDataset fRealScoredDataset;

    // holds points at which the metric changes sign
    private TIntArrayList fMetricCrossOverRanks;

    private MetricWeightStruc[] fRndMetricWeightStrucs;

    private Matrix nMarkersUpMatrix;
    private Matrix nMarkersDnMatrix;

    /**
     * Class constructor
     *
     * @param numMarkers
     * @param numPerms
     * @param lvp
     * @param metric
     * @param sort
     * @param order
     * @param metricParams
     * @param ds
     * @param template
     * @param siglevels
     * @param rst
     * @param rndType
     * @param saveFullRndMetricsMatrix
     * @throws Exception
     */
    public PermutationTestBuilder(final String name,
                                  final int numMarkers,
                                  final int nperm,
                                  final LabelledVectorProcessor lvp,
                                  final Metric metric,
                                  final SortMode sort,
                                  final Order order,
                                  final Map metricParams,
                                  final Dataset ds,
                                  final Template template,
                                  final float[] siglevels,
                                  final boolean categorical) {

        super.init(name, numMarkers, nperm, lvp, metric, sort, order, metricParams, ds, template, siglevels, categorical);

        final DatasetMetrics dm = new DatasetMetrics();
        // calc real metric levels
        DatasetMetrics.ScoredStruc realss = dm.scoreDatasetStruc(fMetric, fSort, fOrder, fMetricParams, fLabelledVectorProc,
                getDataset(), getTemplate());
        this.fRealScoredDataset = realss.sds;

        // calc mean/median and stdev for each class for each marker
        if (categorical) {
            // Also does some error checking.  Actually, we do not use the return value here at all,
            // so in essence *the only thing* we care about is the error checking.
            // TODO: refactor the called method to separate the error checks from the rest.
            Map fMarkerScores = new DatasetStatsCore().calc2ClassCategoricalMetricMarkerScores(getDataset(), getTemplate(), fMetric, fMetricParams);
            //DatasetStatsCore.check2ClassCategoricalDS(getDataset(), getTemplate(), fMetric);
        }

        // Init arrays and matrices
        // each row is a rank, each col is a score for a perm
        this.nMarkersUpMatrix = new Matrix(fNumMarkers, fNumPerms);
        this.nMarkersDnMatrix = new Matrix(fNumMarkers, fNumPerms);
        // Init arrays
        this.fRndTemplates = new Template[fNumPerms];
        this.fMetricCrossOverRanks = new TIntArrayList(fNumPerms);
        this.fRndMetricWeightStrucs = new MetricWeightStruc[fNumPerms];
    }

    private int fNumSetCalls;

    public void doCalc() {
        checkIfDone();
        calcAndSetSignificanceLevels(nMarkersUpMatrix, nMarkersDnMatrix);
    }

    public void addRnd(final Template rndTemplate, final RankedList rndRl) {
        if (fNumSetCalls == getNumPerms()) {
            throw new IllegalStateException("Already done fNumSetCalls: " + fNumSetCalls + " getNumPerms: " + getNumPerms());
        }

        this.fRndTemplates[fNumSetCalls] = rndTemplate;

        for (int r = 0; r < fNumMarkers; r++) {
            nMarkersUpMatrix.setElement(r, fNumSetCalls, rndRl.getScore(r)); // Pth ranked random score
        }

        int p = getDataset().getNumRow() - 1;
        for (int cnt = 0; cnt < fNumMarkers; cnt++, p--) {
            nMarkersDnMatrix.setElement(cnt, fNumSetCalls, rndRl.getScore(p));
        }

        for (int r = 0; r < rndRl.getSize(); r++) {
            if (XMath.isNegative(rndRl.getScore(r))) {
                fMetricCrossOverRanks.add(r + 1);
                break;
            }
        }

        this.fRndMetricWeightStrucs[fNumSetCalls] = rndRl.getMetricWeightStruc();
        this.fNumSetCalls++;
    }

    private void checkIfDone() {
        if (fNumSetCalls != getNumPerms()) {
            throw new IllegalStateException("Not yet done: fNumSetCalls: " + fNumSetCalls + " nperm: " + getNumPerms());
        }
    }

    // @note made lazilly
    private RankedList fRealRankedList;

    /**
     * Eventhough the sds is a ranked list, I dont fully understand if it ranks properly yet
     *
     * @return
     */
    public RankedList getRankedList() {
        if (fRealRankedList == null) {
            this.fRealRankedList = RankedListGenerators.createBySorting(fRealScoredDataset, fSort, fOrder);
        }

        return fRealRankedList;
    }

} // End PermutationTestBuilder
