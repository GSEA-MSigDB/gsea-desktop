/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import java.awt.*;

/**
 * @author Aravind Subramanian
 */
public interface ColorMap {

    public static interface Rows {
        // rows have NO intrinsic order so dont proivide that in the API

        public Color getColor(final String rowName, final int c);

        public int getNumCol();

        //; if the map contains symbols or probe sets
        public boolean isInSymbols(int col);

    }

    public static interface Columns {

        public Color getColor(final String rowName, final String colName);

        public int getNumRow();

        public String getRowName(final int row);

    }

} // End class ColorMap
