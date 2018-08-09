/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.filemgr;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.Arrays;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class RecentDirsModel implements ComboBoxModel {

    private ComboBoxModel fModel;

    /**
     * This is the no-selection sharing magic
     */
    private Object fSelectedObj;

    private List fObjects;

    /**
     * Class Constructor.
     *
     * @param model
     */
    public RecentDirsModel(final ComboBoxModel model, final Object[] addThese) {
        this(model, Arrays.asList(addThese));
    }

    public RecentDirsModel(final ComboBoxModel model, final List addThese) {
        this.fModel = model;
        this.fObjects = addThese;
    }

    public void addListDataListener(ListDataListener l) {
        fModel.addListDataListener(l);
    }

    public Object getElementAt(int index) {
        if (index < fObjects.size()) {
            return fObjects.get(index);
        } else {
            return fModel.getElementAt(index - fObjects.size());
        }
    }

    public int getSize() {
        return fObjects.size() + fModel.getSize();
    }

    public void removeListDataListener(ListDataListener l) {
        fModel.removeListDataListener(l);
    }

    public Object getSelectedItem() {
        return fSelectedObj;
    }

    public void setSelectedItem(Object obj) {
        this.fSelectedObj = obj;
    }

} // End class RecentDirsModel
