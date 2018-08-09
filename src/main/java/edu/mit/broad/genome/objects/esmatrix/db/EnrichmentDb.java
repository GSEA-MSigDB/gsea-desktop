/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.math.LabelledVectorProcessor;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;

import java.io.File;
import java.util.*;

/**
 * @author Aravind Subramanian
 */
public interface EnrichmentDb extends PersistentObject {

    public EnrichmentDb cloneDeep(final EnrichmentResult[] results);

    public File getEdbDir();

    // @note more eff to make this api part of edb multi
    //public EnrichmentDb cloneDeep(final String[] gsetNames);

    // -------------------------------------------------------------------------------------------- //
    // @todo reconsider: So far seems like an edb from one list is likely the norm rather than the exception
    // YES!! -> an edb is off one ranked list. For multi ranked lists see edb multi
    // so perhaps if a more granualr edb is needed do that as a different interface

    public Template getTemplate();

    public Dataset getDataset();

    public RankedList getRankedList();

    public GeneSet[] getGeneSets();

    public int getNumScores(boolean pos);

    public int getNumNominallySig(float npCutoffInclusive, boolean pos);

    public int getNumFDRSig(float fdrCutoffInclusive, boolean pos);

    public EnrichmentResult getResult(final int i);

    public EnrichmentResult[] getResults();

    public EnrichmentResult[] getResults(final Comparator comp);

    public EnrichmentResult[] getResults(final Comparator comp, final boolean pos);

    public List getResultsList();

    public int getNumResults();

    public EnrichmentResult getResultForGeneSet(final String gsetName);

    public LabelledVector getESS_lv();

    public Vector getNESS();

    public Vector getNPs();

    public Vector getFDRs();

    public LabelledVectorProcessor getRankedListProcessor();

    public Metric getMetric();

    public Map getMetricParams();

    public SortMode getSortMode();

    public Order getOrder();

    public int getNumPerm();

} // End EnrichmentDb
