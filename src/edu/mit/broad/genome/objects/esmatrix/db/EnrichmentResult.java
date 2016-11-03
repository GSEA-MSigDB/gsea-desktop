/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetSignal;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.strucs.FdrStruc;
import edu.mit.broad.vdb.chip.Chip;

/**
 * Object capturing one "cell" of info in the gsea matrix
 * Note that the dataset, template, gset everything can be different b/w
 * er's
 */
public interface EnrichmentResult {

    /// @note dont provide this because its not always clear what a generic  name is:
    // is it the name of the gset, the template or the rl??
    //public String getName();

    public EnrichmentScore getScore();

    public FdrStruc getFDR();

    public Vector getRndESS();

    public int getNumPerms();

    public GeneSetSignal getSignal();

    // DATA RELATED APIs

    // Results are never from datasets. Some are but not all are made from datasets
    //public Dataset getDataset();

    public RankedList getRankedList();

    public Template getTemplate();

    public GeneSet getGeneSet();

    public String getGeneSetName(); // @for convenience

    public Chip getChip();

} // End EnrichmentResult
