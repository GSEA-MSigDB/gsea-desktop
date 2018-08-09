/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap.image;

import java.awt.*;
import java.util.List;

public interface SampleAnnotator {
    /**
     * @param sampleName the sampleName
     * @return the phenotype color
     */
    public Color getPhenotypeColor(String sampleName);

    /**
     * @return <tt>true</tt> if phenotype colors should be drawn,
     *         <tt>false</tt> otherwise
     */
    public boolean hasPhenotypeColors();

    /**
     * @param i the row
     * @return the ith label
     */
    public String getLabel(int i);

    /**
     * @param sampleName the sample name
     * @return a list of colors for the sample at the given column
     */
    public List getColors(String sampleName);

}
