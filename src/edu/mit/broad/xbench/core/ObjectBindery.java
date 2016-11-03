/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.RendererFactory2;

import javax.swing.*;

/**
 * Factory of binders and xchoosers for objects.
 * <p/>
 * Binder -> something that attaches objects to a control such as a JComboBox
 * Chooser -> a UI that allows interactive choice of some object(s)
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ObjectBindery {

    /**
     * Privatized class constructor
     * Only static methods.
     */
    private ObjectBindery() {
    }

    public static void bind(JComboBox cb, Class[] classes, boolean addNos) {

        if (cb == null) {
            throw new IllegalArgumentException("param cb cannot be null");
        }

        ComboBoxModel model = ParserFactory.getCache().createBoxModel(classes, addNos);

        cb.setModel(model);
        cb.setRenderer(new RendererFactory2.CommonLookListRenderer());

    }

    public static ComboBoxModel getModel(Class c) {
        return ParserFactory.getCache().createBoxModel(c);
    }

    public static ComboBoxModel getHackAuxGeneSetsBoxModel() {
        return ParserFactory.getCache().hackCreateAuxGeneSetsBoxModel();
    }
}    // End ObjectBindery


