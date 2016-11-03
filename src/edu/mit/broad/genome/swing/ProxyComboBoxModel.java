/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import javax.swing.*;
import javax.swing.event.ListDataListener;

/**
 * We encapsulate the real model
 * Purpose - so that can manage the currently selected item differently
 * we want to share the data across boxes, but NOT the selections!
 * Also prevents client code from adding to the boxes (i.e the defaultmodel
 * which is mutable is not exposed)
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ProxyComboBoxModel implements ComboBoxModel {

    private DefaultComboBoxModel fModel;

    /**
     * Thios is the no-selection sharing magic
     */
    private int fSelIndex = 0;

    /**
     * Does nothing
     */
    protected ProxyComboBoxModel() {
    }

    /**
     * Class Constructor.
     *
     * @param model
     */
    public ProxyComboBoxModel(DefaultComboBoxModel model) {
        this.fModel = model;
    }

    public void addListDataListener(ListDataListener l) {
        fModel.addListDataListener(l);
    }

    public Object getElementAt(int index) {
        return fModel.getElementAt(index);
    }

    public int getSize() {
        return fModel.getSize();
    }

    public void removeListDataListener(ListDataListener l) {
        fModel.removeListDataListener(l);
    }

    /**
     * ComboBoxModel impl.
     *
     * @return
     */
    public Object getSelectedItem() {
        return fModel.getElementAt(fSelIndex);
    }

    /**
     * ComboBoxModel implementation
     */
    public void setSelectedItem(Object obj) {
        this.fSelIndex = fModel.getIndexOf(obj);
    }

} // End ProxyComboBoxModel
