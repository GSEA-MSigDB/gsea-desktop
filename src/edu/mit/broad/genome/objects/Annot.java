/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.sampledb.SampleAnnot;

/**
 * @author Aravind Subramanian
 */
public interface Annot {

    public FeatureAnnot getFeatureAnnot();

    public SampleAnnot getSampleAnnot_global();

    // dont give this AP as it means that the ds is loaded (which can be lazy)
    // public SampleAnnot getSampleAnnot_synched(final Dataset ds);

    public SampleAnnot getSampleAnnot_synched(final String[] names);

    public Chip getChip();

    public void setChip(final Chip chip, final ColorMap.Rows cmr);

    //public Annot cloneDeep(final List featureNames, final List featureDescs, boolean preserveSampleAnnot);

} // End class Annot
