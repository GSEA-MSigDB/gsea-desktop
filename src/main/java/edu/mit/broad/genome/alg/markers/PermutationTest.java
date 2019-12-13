/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg.markers;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.alg.DatasetMetrics;
import edu.mit.broad.genome.alg.DatasetStatsCore;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.RankedListGenerators;
import edu.mit.broad.genome.alg.DatasetStatsCore.TwoClassMarkerStats;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;
import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * NamingConventions permutation tests
 *
 * @author Aravind Subramanian
 */
public class PermutationTest {

    private final Logger log = Logger.getLogger(PermutationTest.class);

    // The original dataset, untouched
    private Dataset fDataset;

    private Template fTemplate;
    private int fNumMarkers;

    private int fNumPerms;

    private LabelledVectorProcessor fLabelledVectorProc;
    private Metric fMetric;
    private SortMode fSort;
    private Order fOrder;

    private Map<String, Boolean> fMetricParams;
    private String fName;

    public static final float[] DEFAULT_SIG_LEVELS = new float[]{0.01f, 0.05f, 0.5f};

    // TODO: get rid of trove
    private TFloatArrayList fSigLevels;

    private Matrix fUpSignificanceLevelsMatrix;
    private Matrix fDnSignificanceLevelsMatrix;
    
    private Template[] fRndTemplates;

    private ScoredDataset fRealScoredDataset;

    // TODO: get rid of trove
    // holds points at which the metric changes sign
    private TIntArrayList fMetricCrossOverRanks;

    private MetricWeightStruc[] fRndMetricWeightStrucs;

    private Matrix nMarkersUpMatrix;
    private Matrix nMarkersDnMatrix;

    public PermutationTest(final String name, final int numMarkers, final int nperm, final LabelledVectorProcessor lvp,
    		final Metric metric, final SortMode sort, final Order order, final Map<String, Boolean> metricParams, 
    		final Dataset ds, final Template template, final float[] siglevels, final boolean categorical) {
        if (numMarkers > ds.getNumRow()) {
		    throw new IllegalArgumentException("numMarkers: " + numMarkers + " cannot be larger than dataset size: " + ds.getNumRow());
		}
		if (metric == null) {
		    throw new IllegalArgumentException("Param metric cannot be null");
		}
		if (template == null) {
		    throw new IllegalArgumentException("Param template cannot be null");
		}
		if (categorical) {
		    if (!metric.isCategorical()) {
		        throw new StandardException("As the phenotype choosen was categorical, only categorical metrics are allowed. Got: " + metric.getName(), 1010);
		    }
		    if (!template.isCategorical()) {
		        throw new IllegalArgumentException("Only categorical templates allowed. Got: " + template.getName());
		    }
		} else {
		    if (!metric.isContinuous()) {
		        throw new StandardException("As the phenotype choosen was continuous, only continuous class metrics are allowed. Got: " + metric.getName(), 1011);
		    }
		    if (!template.isContinuous()) {
		        throw new IllegalArgumentException("Only continuous templates allowed. Got: " + template.getName());
		    }
		}
		
		this.fName = name;
		this.fNumMarkers = numMarkers;
		this.fNumPerms = nperm;
		this.fLabelledVectorProc = lvp;
		this.fMetric = metric;
		this.fSort = sort;
		this.fOrder = order;
		this.fMetricParams = metricParams;
		this.fDataset = ds;
		this.fTemplate = template;
		
		this.fSigLevels = new TFloatArrayList();
		if (siglevels == null || siglevels.length == 0) {
		    this.fSigLevels.add(DEFAULT_SIG_LEVELS);
		} else {
		    this.fSigLevels.add(siglevels);
		}

        final DatasetMetrics dm = new DatasetMetrics();
        // calc real metric levels
        this.fRealScoredDataset = dm.scoreDataset(fMetric, fSort, fOrder, fMetricParams, fLabelledVectorProc, fDataset, fTemplate);

        // calc mean/median and stdev for each class for each marker
        if (categorical) {
            // Also does some error checking.  Actually, we do not use the return value here at all,
            // so in essence *the only thing* we care about is the error checking.
            // TODO: refactor the called method to separate the error checks from the rest.
            Map<String, TwoClassMarkerStats> fMarkerScores = new DatasetStatsCore().calc2ClassCategoricalMetricMarkerScores(fDataset, fTemplate, fMetric, fMetricParams);
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

    public int getNumMarkers() {
        return fNumMarkers;
    }

    public String getName() {
        return fName;
    }

    public Metric getMetric() {
        return fMetric;
    }

    public Template getTemplate() {
        return fTemplate;
    }

    public float[] getSigLevels() {
        return fSigLevels.toNativeArray();
    }

    // each row is 1 rank and its scores, each col is a score for a SIG level
    public Dataset getSigLevels(final boolean up) {
        if (fUpSignificanceLevelsMatrix == null || fDnSignificanceLevelsMatrix == null) {
		    throw new IllegalStateException("calcAndSetSignificanceLevels not yet called");
		}
        List<String> ranks = new ArrayList<String>();
        for (int i = 0; i < fNumMarkers; i++) {
            ranks.add("rank_" + i);
        }

        List<String> siglevel = new ArrayList<String>();
		for (int i = 0; i < fSigLevels.size(); i++) {
            siglevel.add("sig_" + fSigLevels.get(i));
        }

		if (up) {
		    return new DefaultDataset(fName + "_up_rnd", fUpSignificanceLevelsMatrix, ranks, siglevel, null);
		} else {
		    return new DefaultDataset(fName + "_dn_rnd", fDnSignificanceLevelsMatrix, ranks, siglevel, null);
		}
    }

    /**
     * significance level -> have a vector of n scores. have the level of significance.
     * the score at index in sorted vector -> n * level
     *
     * @param scores
     * @param level
     * @return
     */
    private static float _getSignificanceLevel(final Vector scores, final float level, final SortMode sort, 
    		final Order order) {
    	// TODO: we can avoid this if we can prove something about the size elsewhere
    	// That's important as we might be able to avoid the test on every row.  I believe we can just
    	// check fNumPerms > 0 for this as the matrix size is fixed on construction.  The point is *not*
    	// to make this test fast, but rather to avoid it altogether inside the loop.  Can possibly do a
    	// a single test and then use Matrix.fill to implement, or use an alternate constructor with
    	// org.ujmp.core.Matrix.Factory.nans().   It's also possible / likely that this is
    	// an invalid case anyway and ca just be short-circuited.
    	// Also, should be able to precalc the index values below (hold in a small array) instead and
    	// pass in as arg.
        if (scores.getSize() == 0) {
            return Float.NaN;
        }

        // first put the scores in whatever order the markers were in
        scores.sort(sort, order); // can do directly as the v is a clone
        int index = (int) (level * scores.getSize());

        return scores.getElement(index);
    }

    private int fNumSetCalls;

    public void doCalc() {
    	// TODO: possible performance tweaks here; must evaluate carefully as this might be a sensitive section.
    	// See note above in _getSignificanceLevel().
        if (fNumSetCalls != fNumPerms) {
		    throw new IllegalStateException("Not yet done: fNumSetCalls: " + fNumSetCalls + " nperm: " + fNumPerms);
		}
        
        if (fUpSignificanceLevelsMatrix != null) {
		    log.warn("Already filled markers -- refilling ...");
		}
		
		int numSigLevels = fSigLevels.size();
		int numMarkers = nMarkersUpMatrix.getNumRow();
		
		this.fUpSignificanceLevelsMatrix = new Matrix(nMarkersUpMatrix.getNumRow(), numSigLevels);
		this.fDnSignificanceLevelsMatrix = new Matrix(nMarkersDnMatrix.getNumRow(), numSigLevels);
		
		for (int r = 0; r < numMarkers; r++) {
		    Vector v1 = nMarkersUpMatrix.getRowV(r);
		    for (int c = 0; c < numSigLevels; c++) {
		        float level = _getSignificanceLevel(v1, fSigLevels.get(c), fSort, fOrder);
		        this.fUpSignificanceLevelsMatrix.setElement(r, c, level);
		    }
		
		    Vector v2 = nMarkersDnMatrix.getRowV(r);
		    for (int c = 0; c < numSigLevels; c++) {
		        float level = _getSignificanceLevel(v2, 1.0f - fSigLevels.get(c), fSort, fOrder); // other side
		        this.fDnSignificanceLevelsMatrix.setElement(r, c, level);
		    }
		}
    }

    public void addRnd(final Template rndTemplate, final RankedList rndRl) {
        if (fNumSetCalls == fNumPerms) {
            throw new IllegalStateException("Already done fNumSetCalls: " + fNumSetCalls + " getNumPerms: " + fNumPerms);
        }

        this.fRndTemplates[fNumSetCalls] = rndTemplate;

        for (int r = 0; r < fNumMarkers; r++) {
            nMarkersUpMatrix.setElement(r, fNumSetCalls, rndRl.getScore(r)); // Pth ranked random score
        }

        int p = fDataset.getNumRow() - 1;
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

    // @note made lazily
    private RankedList fRealRankedList;

    /*
     * Even though the sds is a ranked list, I dont fully understand if it ranks properly yet
     */
    public RankedList getRankedList() {
        if (fRealRankedList == null) {
            this.fRealRankedList = RankedListGenerators.createBySorting(fRealScoredDataset, fSort, fOrder);
        }

        return fRealRankedList;
    }
}