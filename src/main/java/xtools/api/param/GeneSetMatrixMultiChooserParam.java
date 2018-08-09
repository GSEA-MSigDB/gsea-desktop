/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

/**
 * choose 1 or more string
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note diff from StringsInputParam -> the chpices are presented in a JListWindow and not
 * in a text area
 */
// TODO: collapse object hierarchy.  This is the only implementation of the abstract superclass.
public class GeneSetMatrixMultiChooserParam extends GeneSetMatrixChooserAbstractParam {

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param desc
     * @param reqd
     */
    public GeneSetMatrixMultiChooserParam(String name, String nameEnglish, String desc, boolean reqd) {
        super(name, nameEnglish, desc, reqd, true, true);
    }

    public GeneSetMatrixMultiChooserParam(boolean reqd) {
        this(GMX, GMX_ENGLISH, GMX_DESC_MULTI, reqd);
    }

}    // End class GeneSetMatrixCombiner
