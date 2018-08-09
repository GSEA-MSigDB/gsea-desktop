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
public class StringMultiChooserParam extends StringChooserAbstractParam {

    public StringMultiChooserParam(String name, String nameEnglish, String desc, String[] defs, String[] hints, boolean reqd) {
        super(name, nameEnglish, desc, defs, hints, reqd);
    }

}    // End class StringMultiChooserParam
