/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/

package org.genepattern.annotation;

/**
 * Model for annotating features in a table
 *
 * @author jgould
 */
public interface SetAnnotatorModel {
    public int getIndex(String name);

    public String getName(int index);

    public int getMappedIndex(int row);

    /**
     * @return the number of features in the model
     */
    public int getFeatureCount();

}