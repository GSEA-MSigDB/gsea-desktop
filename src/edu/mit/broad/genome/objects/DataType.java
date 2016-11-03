/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import java.awt.datatransfer.DataFlavor;

/**
 * Extends the concept of a java.awt.datatransfer.DataFlavor. Just as in a DataFlavor,
 * each instance of a DataType represents the opaque concept of a data format
 * as would appear on a clipboard, during drag and drop, a Class and its subclasses,
 * an interface and its impementors, data from a single database column name,
 * a file extension etc.
 * <p/>
 * DataType objects are constant and never change once instantiated.
 * <p/>
 * Once i'm confident this works, might deprecate DataType and make
 * subclasses directly extend DatFlavor.
 * <p/>
 * IMP: see equals -> its important to check if need to override its behaviour when
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @done consider adding mroe constructors?
 * -> added to subclasses
 * @done evaluate effect of the other DataFlavor classes
 * -> seems to be ok.
 */
public class DataType extends DataFlavor {

    /**
     * Class Constructor.
     */
    public DataType(Class representationClass, String humanPrsnName) {
        super(representationClass, humanPrsnName);
    }

    /**
     * Constructs a new DataType.  This constructor is
     * provided only for the purpose of supporting the
     * Externalizable interface.  It is not
     * intended for public (client) use.
     * <p/>
     * Subclasses must call one of the other constructors once done.
     */
    protected DataType() {
        super();
    }
}    // End DataType
