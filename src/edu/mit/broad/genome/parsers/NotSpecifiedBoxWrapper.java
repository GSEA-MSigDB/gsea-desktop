/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.swing.NotSpecified;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListDataListener;

/**
 * dont want to replicate PobBoxModels structire but at same time dont want to add the ns object
 * to pbm
 */
//class NotSpecifiedBoxWrapper implements ComboBoxModel {
public class NotSpecifiedBoxWrapper extends DefaultComboBoxModel {

    private NotSpecified fNotSpecified;

    private boolean fSelected;

    private PobBoxModels fRealModel;

    private Logger log = XLogger.getLogger(NotSpecifiedBoxWrapper.class);


    /**
     * Constructs an empty DefaultComboBoxModel object.
     */
    public NotSpecifiedBoxWrapper(PobBoxModels model) {
        super();
        this.fNotSpecified = new NotSpecified();
        this.fRealModel = model;
    }

    // implements javax.swing.ComboBoxModel
    /**
     * Set the value of the selected item. The selected item may be null.
     * <p/>
     *
     * @param anObject The combo box value or null for no selection.
     */
    public void setSelectedItem(Object anObject) {
        log.debug("setting selected item:" + anObject);
        if (anObject == fNotSpecified) {
            fSelected = true;
        } else {
            fSelected = false;
            fRealModel.setSelectedItem(anObject);
        }

        super.fireContentsChanged(ObjectCache.class, -1, -1);
        super.fireContentsChanged(this, 0, getSize());

    }

    // implements javax.swing.ComboBoxModel
    public Object getSelectedItem() {
        if (fSelected) {
            return fNotSpecified;
        } else {
            return fRealModel.getSelectedItem();
        }
    }

    // implements javax.swing.ListModel
    public int getSize() {
        return fRealModel.getSize() + 1;
    }

    // implements javax.swing.ListModel
    public Object getElementAt(int index) {
        if (index == 0) {
            return fNotSpecified;
        } else {
            return fRealModel.getElementAt(index - 1);
        }
    }

    /**
     * Returns the index-position of the specified object in the list.
     *
     * @param anObject
     * @return an int representing the index position, where 0 is
     *         the first position
     */
    /*
    public int getIndexOf(Object anObject) {
        if (anObject == fNotSpecified) {
            return 0;
        } else {
            return fRealModel.getIndexOf(anObject) + 1;
        }
    }
    */
    public void addListDataListener(ListDataListener l) {
        super.addListDataListener(l);
        fRealModel.addListDataListener(l);
    }

    public void removeListDataListener(ListDataListener l) {
        super.removeListDataListener(l);
        fRealModel.removeListDataListener(l);
    }


} // End NotSpecifiedBoxWrapper
