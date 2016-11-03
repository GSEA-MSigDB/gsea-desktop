/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.dnd;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;

/**
 * Target for drag and drop gestures.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface DndTarget {

    public Component getDroppableIntoComponent();

    public void setDropData(Object obj);

    public DataFlavor[] getDroppableFlavors();
}
