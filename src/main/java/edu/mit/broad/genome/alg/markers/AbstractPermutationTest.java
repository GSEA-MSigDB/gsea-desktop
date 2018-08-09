/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.markers;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.Template;
import gnu.trove.TFloatArrayList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public abstract class AbstractPermutationTest implements PermutationTest {

    private final Logger log = XLogger.getLogger(this.getClass());

    // The original dataset, untouched
    private Dataset fDataset;

    private Template fTemplate;
    protected int fNumMarkers;

    protected int fNumPerms;

    protected LabelledVectorProcessor fLabelledVectorProc;
    protected Metric fMetric;
    protected SortMode fSort;
    protected Order fOrder;

    protected Map fMetricParams;
    private String fName;

    public static final float[] DEFAULT_SIG_LEVELS = new float[]{(float) 0.01, (float) 0.05, (float) 0.5};

    private TFloatArrayList fSigLevels;

    private Matrix fUpSignificanceLevelsMatrix;

    private Matrix fDnSignificanceLevelsMatrix;

    // each row is 1 rank and its scores, each col is a score for a perm
    private Matrix fNmarkersUpMatrix;

    private Matrix fNmarkersDnMatrix;


    /**
     * Class constructor
     */
    AbstractPermutationTest() {

    }

    protected void init(final String name,
                        final int numMarkers,
                        final int numPerms,
                        final LabelledVectorProcessor rp,
                        final Metric metric,
                        final SortMode sort,
                        final Order order,
                        final Map metricParams,
                        final Dataset ds,
                        final Template template,
                        final float[] sigLevels,
                        final boolean categorical) {


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
        }

        if (!categorical) {
            if (!metric.isContinuous()) {
                throw new StandardException("As the phenotype choosen was continuous, only continuous class metrics are allowed. Got: " + metric.getName(), 1011);
            }

            if (!template.isContinuous()) {
                throw new IllegalArgumentException("Only continuous templates allowed. Got: " + template.getName());
            }
        }

        this.fName = name;
        this.fNumMarkers = numMarkers;
        this.fNumPerms = numPerms;
        this.fLabelledVectorProc = rp;
        this.fMetric = metric;
        this.fSort = sort;
        this.fOrder = order;
        this.fMetricParams = metricParams;
        this.fDataset = ds;
        this.fTemplate = template;

        this.fSigLevels = new TFloatArrayList();

        if (sigLevels == null || sigLevels.length == 0) {
            this.fSigLevels.add(DEFAULT_SIG_LEVELS);
        } else {
            this.fSigLevels.add(sigLevels);
        }
    }

    public int getNumMarkers() {
        return fNumMarkers;
    }

    public String getName() {
        return fName;
    }

    public int getNumPerms() {
        return fNumPerms;
    }

    public Metric getMetric() {
        return fMetric;
    }

    public Template getTemplate() {
        return fTemplate;
    }

    public Dataset getDataset() {
        return fDataset;
    }

    public float[] getSigLevels() {
        return fSigLevels.toNativeArray();
    }

    // each row is 1 rank and its scores, each col is a score for a SIG level
    public Dataset getSigLevels(final boolean up) {
        checkCalced();
        checkCalced();
        List ranks = new ArrayList();
        for (int i = 0; i < getNumMarkers(); i++) {
            ranks.add("rank_" + i);
        }

        List siglevel = new ArrayList();
        for (int i = 0; i < getSigLevels().length; i++) {
            siglevel.add("sig_" + getSigLevels()[i]);
        }

        if (up) {
            return new DefaultDataset(getName() + "_up_rnd", fUpSignificanceLevelsMatrix, ranks, siglevel, true, null);
        } else {
            return new DefaultDataset(getName() + "_dn_rnd", fDnSignificanceLevelsMatrix, ranks, siglevel, true, null);
        }
    }

    private void checkCalced() {
        if (fUpSignificanceLevelsMatrix == null || fDnSignificanceLevelsMatrix == null) {
            throw new IllegalStateException("calcAndSetSignificanceLevels not yet called");
        }
    }


    protected void calcAndSetSignificanceLevels(Matrix nMarkersTopMatrix, Matrix nMarkersBotMatrix) {

        if (fNmarkersUpMatrix != null) {
            log.warn("Already filled markers -- refilling ...");
        }

        this.fNmarkersUpMatrix = nMarkersTopMatrix;
        this.fNmarkersDnMatrix = nMarkersBotMatrix;

        int size = getSigLevels().length;
        int numMarkers = nMarkersTopMatrix.getNumRow();

        this.fUpSignificanceLevelsMatrix = new Matrix(nMarkersTopMatrix.getNumRow(), size);
        this.fDnSignificanceLevelsMatrix = new Matrix(nMarkersBotMatrix.getNumRow(), size);

        for (int r = 0; r < numMarkers; r++) {
            Vector v = nMarkersTopMatrix.getRowV(r);
            for (int c = 0; c < size; c++) {
                float level = _getSignificanceLevel(v, fSigLevels.get(c), fSort, fOrder);
                this.fUpSignificanceLevelsMatrix.setElement(r, c, level);
            }

            v = nMarkersBotMatrix.getRowV(r);
            for (int c = 0; c < size; c++) {
                float level = _getSignificanceLevel(v, 1.0f - fSigLevels.get(c), fSort, fOrder); // other side
                this.fDnSignificanceLevelsMatrix.setElement(r, c, level);
            }
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
    private static float _getSignificanceLevel(final Vector scores,
                                               final float level,
                                               final SortMode sort,
                                               final Order order) {

        if (scores.getSize() == 0) {
            return Float.NaN;
        }

        // first put the scores in whatever order the markers were in
        scores.sort(sort, order); // can do directly as the v is a clone
        int index = (int) (level * scores.getSize());
        //log.debug("significance level: " + level + " index: " + index);

        return scores.getElement(index);
    }


} // End class AbstractPermutationTest
