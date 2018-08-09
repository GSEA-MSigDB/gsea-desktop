/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.objects.strucs.NamedColor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public class ColumnsImpl implements ColorMap.Columns {

    private Map fName_nc_Map;

    /**
     * Class constructor
     *
     * @param sampleNames
     */
    public ColumnsImpl(final String[] sampleNames) {
        NamedColor[] ncs = new NamedColor[sampleNames.length];
        for (int i = 0; i < sampleNames.length; i++) {
            ncs[i] = new NamedColor(sampleNames[i], Color.WHITE);
        }

        this.init(ncs);
    }

    private void init(final NamedColor[] ncs) {

        if (ncs == null) {
            throw new IllegalArgumentException("Param ncs cannot be null");
        }

        this.fName_nc_Map = new HashMap();
        for (int i = 0; i < ncs.length; i++) {
            fName_nc_Map.put(ncs[i].getName(), ncs[i]);
        }
    }

    public Color getColor(final String rowName, final String colName) {
        Object obj = fName_nc_Map.get(colName);
        if (obj == null) {
            return Color.WHITE;
        } else {
            return ((NamedColor) obj).getColor();
        }
    }

    public int getNumRow() {
        return 1;
    }

    public String getRowName(final int row) {
        return "SAMPLE";
    }

} // End inner class WhiteColumns
