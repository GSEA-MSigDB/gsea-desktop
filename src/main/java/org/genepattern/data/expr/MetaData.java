/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.data.expr;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Joshua Gould
 */
public class MetaData {
    private HashMap<String, Integer> metaDataName2Depth;
    private ArrayList<String[]> metaDataList;
    private int capacity;

    /**
     * Creates a new <tt>MetaData</tt> instance
     *
     * @param capacity the capacity
     */
    public MetaData(int capacity) {
        this.capacity = capacity;
        metaDataList = new ArrayList<>();
        metaDataName2Depth = new HashMap<>();
    }

    /**
     * Constructs and returns a new <tt>MetaData</tt> instance that contains
     * the indicated cells. Indices can be in arbitrary order.
     *
     * @param indices The indices
     * @return the new MetaData
     */
    public MetaData slice(int[] indices) {
        MetaData slicedMetaData = new MetaData(indices.length);
        slicedMetaData.metaDataName2Depth = (HashMap<String, Integer>) this.metaDataName2Depth.clone();
        slicedMetaData.metaDataList = new ArrayList<String[]>();
        for (int i = 0, size = this.metaDataList.size(); i < size; i++) {
            String[] obj = (String[]) this.metaDataList.get(i);
            String[] copy = new String[indices.length];
            for (int j = 0, length = indices.length; j < length; j++) {
                copy[j] = obj[indices[j]];
            }
            slicedMetaData.metaDataList.add(copy);
        }
        return slicedMetaData;
    }

    /**
     * Sets the meta data for the given name
     *
     * @param name   The name of the meta data
     * @param values The values of the meta data
     */
    public void setMetaData(String name, String[] values) {
        if (values.length != capacity) {
            throw new IllegalArgumentException(
                    "Length of values must be equal to capacity (" + capacity
                            + ")");
        }
        metaDataList.set(getDepth(name), values);
    }

    /**
     * Gets the meta data at the given index
     *
     * @param index The index
     * @param name  The name of the meta data at the given index
     * @return The value of the meta data at the given index
     */
    public String getMetaData(int index, String name) {
        return getArray(name)[index];
    }

    /**
     * @param name The name of the meta data
     * @return index in metaDataList list
     */
    protected int getDepth(String name) {
        Integer depth = metaDataName2Depth.get(name);
        if (depth == null) {
            depth = metaDataList.size();
            final int _depth = depth;
            for (int i = metaDataList.size(); i <= _depth; i++) {
                metaDataList.add(new String[capacity]);
            }
            metaDataList.set(_depth, new String[capacity]);
            metaDataName2Depth.put(name, depth);
            return _depth;
        } else {
            return depth;
        }
    }

    /**
     * @param name The name of the meta data
     * @return the array for the given name
     */
    protected String[] getArray(String name) {
        return metaDataList.get(getDepth(name));
    }
}
