/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.pages;

import edu.mit.broad.genome.Printf;
import org.apache.ecs.Element;
import org.apache.ecs.html.*;

/**
 * Simple table with:
 * <p/>
 * key value
 * <p/>
 * For example a header table with a several simple lines = values
 */
public class KeyValTable {

    private org.apache.ecs.html.Table fTable;

    private int fFloatFormatPrecision;

    /**
     * Class constructor
     */
    public KeyValTable() {
        this.fTable = new org.apache.ecs.html.Table(0);
        fTable.setCols(2); // always
        
        // add the col names
        fTable.addElement(HtmlFormat.THs.keyValTable(""));
        fTable.addElement(HtmlFormat.THs.keyValTable(""));
        this.fFloatFormatPrecision = -1; // @note magic number
    }

    public org.apache.ecs.html.Table getTable() {
        return fTable;
    }

    public void addRow(String key, Element el) {
        _addRow(key, el);
    }

    public void addRow(String key, String val) {
        _addRow(key, val);
    }

    public void addRow(String key, float val) {
        Object obj;
        if (fFloatFormatPrecision != -1) {
            obj = Printf.format(val, fFloatFormatPrecision);
        } else {
            obj = new Float(val);
        }

        _addRow(key, obj);
    }

    // does the real stuff
    public void _addRow(String key, Object val) {
        TR tr = new TR();
        tr.addElement(HtmlFormat._td(key));
        tr.addElement(HtmlFormat._td(val));
        fTable.addElement(tr);
    }

} // End inner class Table
