/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.IDataframe;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.objects.strucs.Linked;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

/**
 * Dataframe that holds rich (i.e fancy formatting) info for all its elements
 * This is NOT a very efficient implementation; the goal is simplicity of use.
 * Shouldnt be a problem in practice - rich table outputs are not generally very verbose.
 * <p/>
 * RDF aspects: per element/row/col
 * <p/>
 * 1) color
 * 2) font-wight (normal, bold)
 * 3) font-type: (normal, italic, underlined)
 * 4) link (url)
 * <p/>
 * To optimize on memory usage, these are made as follows:
 * <p/>
 * none
 * per column/row
 * per element
 */
// kept it here as it uses objects (such as Linked) that arent linkage safe
public class RichDataframe extends AbstractObject implements IDataframe {

    // holds the real un-rich data
    private IDataframe fIdf;

    // Hoilds meta format data = formatting that is DATAFRaME-WIDE
    private MetaData fMetaData;

    // not always available
    // contains String objects as keys
    private Rich fRichColors;

    // not always available
    // contains Linked objects as values
    private Rich fRichLinks;

    static class Rich {
        private TIntObjectHashMap cell_id;
    }

    /**
     * Class constructor
     *
     * @param sdf
     * @param cell_id_colorMap
     * @param cell_id_linkMap
     */
    public RichDataframe(final StringDataframe sdf,
                         final MetaData metaData,
                         final TIntObjectHashMap cell_id_colorMap,
                         final TIntObjectHashMap cell_id_linkMap) {
        if (sdf == null) {
            throw new IllegalArgumentException("Parameter sdf cannot be null");
        }

        super.initialize(sdf.getName());
        this.fIdf = sdf;
        this.fMetaData = metaData;

        if (cell_id_colorMap != null && !cell_id_colorMap.isEmpty()) {
            fRichColors = new Rich();
            //log.debug("Setting colorMap to: " + cell_id_colorMap + " " + cell_id_colorMap.size());
            fRichColors.cell_id = cell_id_colorMap;
        }

        if (cell_id_linkMap != null && !cell_id_linkMap.isEmpty()) {
            fRichLinks = new Rich();
            fRichLinks.cell_id = cell_id_linkMap;
        }

    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(fIdf.getNumRow()).append('x').append(fIdf.getNumCol());
        return buf.toString();
    }

    public IDataframe getDataframe() {
        return fIdf;
    }

    // May return null
    public MetaData getMetaData() {
        return fMetaData;
    }

    public String getElementColor(int row, int col) {
        if (fRichColors != null && fRichColors.cell_id != null) {
            Object obj = fRichColors.cell_id.get(_cell_id(row, col));
            if (obj != null) {
                return obj.toString();
            }
        }

        return null;
    }

    public Linked getElementLink(int row, int col) {
        if (fRichLinks != null && fRichLinks.cell_id != null) {
            Object obj = fRichLinks.cell_id.get(_cell_id(row, col));
            if (obj != null) {
                //System.out.println(">> " + obj.getClass());
                return (Linked) obj;
            }
        }

        return null;
    }

    private int _cell_id(int row, int col) {
        return row * fIdf.getNumCol() + col;
    }

    public Object getElementObj(int rown, int coln) {
        return fIdf.getElementObj(rown, coln);
    }

    public String getRowName(int rown) {
        return fIdf.getRowName(rown);
    }

    public String getColumnName(int coln) {
        return fIdf.getColumnName(coln);
    }

    public int getNumRow() {
        return fIdf.getNumRow();
    }

    public int getNumCol() {
        return fIdf.getNumCol();
    }

    /**
     * Internal class representing dataframe wide formating data
     */
    public static class MetaData {

        private TIntIntHashMap fColIndexFloatPrecisionMap;

        private String fTitle;

        /**
         * Class constructor
         *
         * @param title
         */
        public MetaData(final String title,
                        final TIntIntHashMap colIndexFloatPrecisionMap) {
            this.fTitle = title;
            this.fColIndexFloatPrecisionMap = colIndexFloatPrecisionMap;
        }

        public String getTitle() {
            return fTitle;
        }

        boolean gotNfe = false; // opt so that on one error we stop

        public Object adjustPrecision(final Object val, final int coln) {
            if (val != null && val.toString().length() > 0 && !gotNfe && val != null && fColIndexFloatPrecisionMap != null && fColIndexFloatPrecisionMap.containsKey(coln)) {

                try {
                    Float f = new Float(val.toString());
                    int precision = fColIndexFloatPrecisionMap.get(coln);
                    return Printf.format(f.floatValue(), precision);
                } catch (Throwable t) {
                    t.printStackTrace();
                    gotNfe = true;

                }

            }

            return val; // no adjustment
        }

    }

} // End class RichDataframe
