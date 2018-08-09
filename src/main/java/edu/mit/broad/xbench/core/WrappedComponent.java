/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Wrap a JComponent to have associated ornaments (title and icon).
 *
 * @author David Eby
 */
public interface WrappedComponent {

    /**
     * Return the wrapped JComponent.
     */
    public JComponent getWrappedComponent();

    /**
     * Get a title to be associated with the JComponent.
     */
    public String getAssociatedTitle();

    /**
     * Get an Icon to be associated with the the JComponent. This can be null if
     * none is provided.
     * 
     * @return
     */
    public Icon getAssociatedIcon();
}
