/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.sampledb;

import edu.mit.broad.genome.objects.ColorMap;
import edu.mit.broad.genome.objects.PersistentObject;

/**
 * @author Aravind Subramanian
 */
public interface SampleAnnot extends PersistentObject {

    public SampleAnnot cloneDeep(final String[] useOnlyTheseSamples);

    public int getNumSamples();

    public ColorMap.Columns getColorMap();

} // End interface SampleAnnot
