/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.tui;

import java.awt.*;
import java.awt.event.MouseMotionListener;

/**
 * @author Aravind Subramanian
 */
public interface ParamSetDisplay {

    public void addMouseMotionListener(MouseMotionListener l);

    public Component getAsComponent();


    /**
     * restores defaults as got from the current ParamSet
     */
    public void reset();

}
