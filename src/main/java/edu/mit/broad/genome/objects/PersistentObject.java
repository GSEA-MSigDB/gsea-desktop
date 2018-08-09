/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import java.io.Serializable;

/**
 * An object that can be saved and loaded (i.e is persistent)
 * <p/>
 * Guidelines on creating objects:
 * 1) A pob is essentially a well thought data structure
 * 2) Pobs shoulds not have any user interface gunk (nor swing models)
 * 3) Pobs should be immutable once made
 * 4) Use MutableFoo classes for a mutable / generative version of a pob
 * 5) xxxBuilder classes to incrementally build pobs
 * 6) It is ok to provide simple operational methods in a pob
 * i.e pob.doSomeCalc() is ok
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface PersistentObject extends Serializable {

    /**
     * Invariant after construction. Also as unique as possible.
     *
     * @return Object id
     */
    public Id getId();

    /**
     * The display name of this object
     * Invariant after construction. Not neccesarily unique.
     */
    public String getName();


    /**
     * A more user friendly version of the name
     * (optional - can have some default - not used for anything other than display)
     *
     * @return
     */
    public String getNameEnglish();

    /**
     * @return The comment, if any, associated with the Dataset
     */
    public String getComment();

    /**
     * Add a comment with the pob
     *
     * @param comment
     */
    public void addComment(String comment);

    /**
     * Info on the object that can be quickly and efficiently retrieved
     * Used in UI contexts such as tooltips and in combo boxes
     *
     * @return
     */
    public String getQuickInfo();

}    // End PersistentObject
