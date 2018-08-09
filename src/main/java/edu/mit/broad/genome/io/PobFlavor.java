/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import edu.mit.broad.genome.objects.DataType;
import edu.mit.broad.genome.objects.PersistentObject;

import java.awt.datatransfer.DataFlavor;

/**
 * Construct a DataFlavor that represents one PersistentObject
 * or a list of PersistentObject's.
 * Typically used for datatransfer - via clipboard or dnd.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PobFlavor extends DataType {

    private final boolean fInited;

    /**
     * all PersistentObject classes
     */
    public static final PobFlavor ALL_POB = new PobFlavor(PersistentObject.class);
    public static final DataFlavor pobListFlavor = new DataFlavor(javaSerializedObjectMimeType
            + ";class=java.util.List", "PobList");

    /**
     * Class Constructor.
     */
    public PobFlavor(Class pobClass) {

        super(pobClass, "PersistentObject");

        if (!PersistentObject.class.isAssignableFrom(pobClass)) {
            fInited = false;

            throw new IllegalArgumentException("Invalid pobClass: " + pobClass);
        }

        fInited = true;
    }

    /**
     * All public API's must call
     */
    protected void checkInit() {

        if (!fInited) {
            throw new IllegalStateException("Object was not initialized properly");
        }
    }

    public boolean equals(DataFlavor df) {
        return this.equals((Object) df);
    }

    /**
     * This is the trick.
     * This method overrides the parent impl to check for not just for
     * equality but also if the class represented by df is a sub-class
     * (isAssignableFrom() the class represented by this ClassFlavor)
     */
    public boolean equals(PobFlavor pf) {

        checkInit();

        Class cl = pf.getRepresentationClass();

        return this.getRepresentationClass().isAssignableFrom(cl);
    }

    public boolean equals(Object obj) {

        if (obj instanceof PobFlavor) {
            return this.equals((PobFlavor) obj);
        } else {
            return false;
        }
    }
}    // End PobFlavor
