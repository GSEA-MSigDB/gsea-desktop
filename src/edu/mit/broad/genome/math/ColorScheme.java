/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import java.awt.*;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface ColorScheme {

    public Color getMinColor();

    public int getNumColors();

    public Color getColor(int c);

    public String getValue(int pos);

}    // End ColorScheme

