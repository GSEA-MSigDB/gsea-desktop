/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.vdb.sampledb.SampleAnnot;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class SampleAnnotImpl extends AbstractObject implements SampleAnnot {

    private List fSampleNames;

    private ColorMap.Columns fColorMap;

    /**
     * Class constructor
     *
     * @param sampleNames
     */
    public SampleAnnotImpl(final String name, final List sampleNames, final ColorMap.Columns cm_opt) {
        initHere(name, sampleNames, cm_opt);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param sampleNames
     * @param cm_opt
     */
    public SampleAnnotImpl(final String name, final String[] sampleNames, final ColorMap.Columns cm_opt) {
        if (sampleNames == null) {
            throw new IllegalArgumentException("Param sampleNames cannot be null");
        }

        List names = new ArrayList();
        for (int i = 0; i < sampleNames.length; i++) {
            names.add(sampleNames[i]);
        }

        initHere(name, names, cm_opt);
    }

    // does the core init
    private void initHere(final String name, final List sampleNames, final ColorMap.Columns cm_opt) {
        super.initialize(name);
        if (sampleNames == null) {
            throw new IllegalArgumentException("Param sampleNames cannot be null");
        }

        this.fSampleNames = sampleNames;

        if (cm_opt == null) {
            this.fColorMap = new ColumnsNull();
        } else {
            this.fColorMap = cm_opt;
        }
    }

    public SampleAnnot cloneDeep(final String newName, final String[] useOnlyTheseSamples) {
        return new SampleAnnotImpl(newName, useOnlyTheseSamples, new ColumnsImpl(useOnlyTheseSamples));
    }

    public SampleAnnot cloneDeep(final String[] useOnlyTheseSamples) {
        return cloneDeep(getName() + "resr_" + useOnlyTheseSamples.length, useOnlyTheseSamples);
    }

    public String getQuickInfo() {
        return null;
    }

    public ColorMap.Columns getColorMap() {
        return fColorMap;
    }

    public int getNumSamples() {
        return fSampleNames.size();
    }

    class ColumnsNull implements ColorMap.Columns {

        public Color getColor(final String rowName, final String colName) {
            return Color.WHITE;
        }

        public int getNumRow() {
            return 1;
        }

        public String getRowName(final int row) {
            return "SampleName";
        }

    } // End class ColumnsNull

} // End class SampleAnnotImpl
