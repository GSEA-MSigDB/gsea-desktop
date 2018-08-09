/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.apache.commons.lang3.ObjectUtils;

import edu.mit.broad.genome.objects.PersistentObject;

/**
 * The default model for combo boxes.
 * <p/>
 * Extending the DefaultComboBoxModel with our own features. Adding sorting
 * ability. Store objects in an ArrayList.
 *
 * @author Aravind Subramanian
 * @author David Eby
 */
public class PobBoxModel extends DefaultComboBoxModel<PersistentObject> {

    private List<PersistentObject> persistentObjects = new ArrayList<PersistentObject>();
    private Object selected = null;

    private Comparator<PersistentObject> comp;

    /**
     * Constructs an empty instance.
     */
    public PobBoxModel() {
    }

    @Override
    public void addElement(PersistentObject persistentObject) {
        persistentObjects.add(persistentObject);
        final int endIndex = persistentObjects.size() - 1;
        fireIntervalAdded(this, endIndex, endIndex);
        if (selected == null && persistentObject != null && persistentObjects.size() == 1) {
            setSelectedItem(persistentObject);
        }
    }

    @Override
    public void removeElement(Object anObjectToRemove) {
        final int index = persistentObjects.indexOf(anObjectToRemove);
        if (index != -1) {
            removeElementAt(index);
        }
    }

    @Override
    public void removeElementAt(int index) {
        if (getElementAt(index) == selected) {
            if (index > 0) {
                setSelectedItem(getElementAt(index - 1));
            } else if (persistentObjects.isEmpty()) {
                setSelectedItem(null);
            } else {
                setSelectedItem(getElementAt(index + 1));
            }
        }

        persistentObjects.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    @Override
    public void removeAllElements() {
        if (persistentObjects.isEmpty()) return;

        final int endIndex = persistentObjects.size() - 1;
        persistentObjects.clear();
        selected = null;
        fireIntervalRemoved(this, 0, endIndex);
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    @Override
    public void setSelectedItem(Object objectToSelect) {
        if ((selected == null && objectToSelect != null)
                || (selected != null && !selected.equals(objectToSelect))) {
            selected = objectToSelect;
            fireContentsChanged(ObjectCache.class, -1, -1);
        }
    }

    @Override
    public int getSize() {
        return persistentObjects.size();
    }

    @Override
    public int getIndexOf(Object anObject) {
        return persistentObjects.indexOf(anObject);
    }

    @Override
    public PersistentObject getElementAt(int index) {
        if (index >= 0 && index < persistentObjects.size()) {
            return persistentObjects.get(index);
        }
        return null;
    }

    public void sort() {
        if (comp == null) {
            comp = new PobComparator();
        }

        // log.debug("Started sorting ");
        Collections.sort(persistentObjects, comp);
        // log.debug("done sorting: " + objects.size());
    }

    static class PobComparator implements Comparator<PersistentObject> {

        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is
         * greater than o2.
         */
        public int compare(PersistentObject pn1, PersistentObject pn2) {
            String s1 = pn1.getName();
            String s2 = pn2.getName();
            return ObjectUtils.compare(s1, s2);
        }

        /**
         * Return true if this equals o2.
         */
        public boolean equals(Object o2) {
            return false;
        }
    } // End PobComparator
} // End class PobModel