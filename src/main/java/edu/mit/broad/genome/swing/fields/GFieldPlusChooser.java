/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;

/**
 * Interface for a GFieldPlusChooser.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface GFieldPlusChooser {

    /**
     * @return An object containing the value choosen.
     */
    public Object getValue();

    /**
     * @return The Component that contains the GFieldPlusChooser.
     *         Often, the GFieldPlusChooser itself.
     *         <p/>
     *         Alternative impl would have this interface subclass Component, buts that is
     *         not possible in the java language specification.
     */
    public JComponent getComponent();

    /**
     * Change the choosen value programmatically
     *
     * @param obj
     */
    public void setValue(Object obj);


}

/*--- Formatted in Sun Java Convention Style on Fri, Sep 27, '02 ---*/