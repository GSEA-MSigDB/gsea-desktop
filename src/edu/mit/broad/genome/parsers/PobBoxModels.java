/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListDataListener;

/**
 * We encapsulate the real model
 * Ditto to ProxyComboBoxModel except for > 1 model.
 * <p/>
 * IMP IKP: Unexpected behavior if models represnet the same data!
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PobBoxModels implements ComboBoxModel {

    private PobBoxModel[] fModels;

    private Logger log = XLogger.getLogger(PobBoxModels.class);

    /**
     * This is the comboination of model / no-selection sharing magic
     */
    private ModelItsIndex fSelModelIndex;

    /**
     * Class Constructor.
     *
     * @param model
     */
    public PobBoxModels(PobBoxModel[] models) {
        this.fModels = models;
    }

    public void addListDataListener(ListDataListener l) {
        for (int i = 0; i < fModels.length; i++) {
            fModels[i].addListDataListener(l);
        }
    }

    public Object getElementAt(int index) {
        ModelItsIndex mii = getModelAtElementIndex(index);
        if (mii == null) {
            return null;
        } else {
            return mii.getElement();
        }
    }

    private ModelItsIndex getModelAtElementIndex(int index) {

        int cnt = 0;

        for (int i = 0; i < fModels.length; i++) {
            for (int t = 0; t < fModels[i].getSize(); t++) {
                if (cnt == index) {
                    return new ModelItsIndex(fModels[i].getElementAt(t), fModels[i], t, index);
                }
                cnt++;
            }
        }

        log.fatal("No model found for index: " + index);
        return null;
    }

    private ModelItsIndex getModelForObject(Object obj) {

        int cnt = 0;

        for (int i = 0; i < fModels.length; i++) {
            for (int t = 0; t < fModels[i].getSize(); t++) {
                if (fModels[i].getElementAt(t) == obj) {
                    return new ModelItsIndex(obj, fModels[i], t, cnt);
                }
                cnt++;
            }
        }

        StringBuffer buf = new StringBuffer("No model found for object: " + obj).append('\n');
        buf.append("# of models: " + fModels.length).append('\n');
        for (int i = 0; i < fModels.length; i++) {
            buf.append(fModels[i].getElementAt(0)).append('\n');
        }

        log.fatal(buf.toString());
        return null;
    }

    public int getSize() {
        int size = 0;
        for (int i = 0; i < fModels.length; i++) {
            size += fModels[i].getSize();
        }

        return size;
    }

    public void removeListDataListener(ListDataListener l) {
        for (int i = 0; i < fModels.length; i++) {
            fModels[i].removeListDataListener(l);
        }
    }

    /**
     * ComboBoxModel impl.
     *
     * @return
     */
    public Object getSelectedItem() {
        if (fSelModelIndex == null) {
            return null;
        } else {
            return fSelModelIndex.element;
        }
    }

    /**
     * ComboBoxModel implementation
     */
    public void setSelectedItem(Object obj) {
        this.fSelModelIndex = getModelForObject(obj);
    }

    /**
     * not sure how to synch eleemnt and getElement
     */
    class ModelItsIndex {
        DefaultComboBoxModel model;
        int itsElementIndex;
        int globalModelIndex;
        Object element;

        ModelItsIndex(Object element, DefaultComboBoxModel model, int elementIndex, int globalModelIndex) {
            this.element = element;
            this.model = model;
            this.itsElementIndex = elementIndex;
            this.globalModelIndex = globalModelIndex;
        }

        Object getElement() {
            return model.getElementAt(itsElementIndex);
        }
    } // End inner class ModelItsIndex

} // End ProxyComboBoxModels
