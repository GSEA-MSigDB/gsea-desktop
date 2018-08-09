/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap.image;

import java.util.List;

/**
 * @author Joshua Gould
 */
public interface FeatureAnnotator {
    /**
     * @param feature the feature
     * @param j       the column
     * @return the jth annotation for the feature at the given row
     */
    public String getAnnotation(String feature, int j);

    /**
     * @return the maximum number of annotations to draw
     */
    public int getColumnCount();

    /**
     * @param featureName the feature
     * @param j           the column
     * @return the jth color for the feature at the given row
     */
    public List getColors(String featureName);

}
