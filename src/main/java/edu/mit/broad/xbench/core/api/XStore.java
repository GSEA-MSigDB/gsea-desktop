/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.parsers.ParseUtils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class Store
 *
 * @author Aravind Subramanian
 * @note Does nothing if the JVM is not in xomics mode
 */
public class XStore extends AbstractListModel<String> implements ComboBoxModel<String> {
    private static final Logger klog = LoggerFactory.getLogger(XStore.class);

    private File fFile;

    // fLines represents the *sorted contents* of the loaded list, as presented to the UI.
    // TODO: could be better to represent as new TreeSet<String>(new ComparatorFactory.FileExtComparator())
    // Note that this is always sorted by FileExt, even for the DirPathStore.
    private List<String> fLinesByFileExt;
    
    // fLinesByLoadOrder represents the *contents on disk* of the loaded list.  This is the
    // order in which they were loaded.  We add to the end (newest) and remove from the front (oldest).
    private List<String> fLinesByLoadOrder;

    private int fSelIndex = 0;

    private boolean fLoaded;

    private AdditionDecider fDecider;

    public static interface AdditionDecider {

        public String addThis(final String str);

    }

    // Subclasses must call init
    // TODO: Why not just have them override this constructor?
    protected XStore() { }

    protected void init(final File file, final AdditionDecider dec) {
        this.fFile = file;
        this.fLinesByFileExt = new ArrayList<String>();
        this.fLinesByLoadOrder = new ArrayList<String>();
        this.fDecider = dec;
    }

    // lazily loaded
    private void load() {

        if (fLoaded) {
            return;
        }

        try {
            if (!fFile.exists()) {
                fFile.createNewFile();
            }

            final List<String> ffn = ParseUtils.readFfn(fFile);
            this.fLinesByFileExt = new ArrayList<String>();

            for (int i = 0; i < ffn.size(); i++) {
                String s = ffn.get(i);
                String ret = fDecider.addThis(s.trim());
                if (ret != null && ret.length() != 0) {
                    fLinesByFileExt.add(ret);
                    fLinesByLoadOrder.add(ret);
                }
            }

            this.sort();
            fLoaded = true;

            this.fireContentsChanged(this, 0, fLinesByFileExt.size());

        } catch (Throwable t) {
            klog.error(MarkerFactory.getMarker("FATAL"), "Could not init store: {}", fFile);
            klog.error(t.getMessage(), t);
        }
    }

    public void clearAll() {
        fLinesByFileExt.clear();
        fLinesByLoadOrder.clear();
        save();
    }

    // whitespace on either end will be trimmed
    private void add(String text) {

        if (!fLoaded) {
            load();
        }

        if (text == null) {
            return;
        }

        text = text.trim();

        if (text.length() == 0) {
            return;
        }

        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }

        String ret = fDecider.addThis(text);
        // Items are always added to the *end* of the list
        if (ret != null && ret.length() != 0) {
            fLinesByLoadOrder.add(ret);
            fLinesByFileExt.add(ret);
            this.fireIntervalAdded(this, fLinesByFileExt.size() - 1, fLinesByFileExt.size() - 1);
        } else if (fLinesByLoadOrder.contains(text)) {
            // Item already present, so we should "refresh" it in terms of LRU order
            fLinesByLoadOrder.remove(text);
            fLinesByLoadOrder.add(text);
        }
    }

    // If in a loop, better to build and then store
    private void save() {
        try {
            if (!fFile.exists()) {
                fFile.createNewFile();
            }

            if (fFile.canWrite()) {
                FileUtils.writeLines(fFile, fLinesByLoadOrder);
            }
        } catch (Throwable t) {
            klog.error("Trouble saving store", t);
        }
    }

    public void trim(final int maxNumLines) {

        if (!fLoaded) {
            load();
        }

        if (fLinesByLoadOrder.size() > maxNumLines) {
            int size = fLinesByFileExt.size();
            // Always remove from the *front* of the list
            List<String> removedItems = fLinesByLoadOrder.subList(0, size - maxNumLines);
            fLinesByLoadOrder = fLinesByLoadOrder.subList(size - maxNumLines, size);
            
            // Now adjust the extension-sorted list to remove the same items
            fLinesByFileExt.removeAll(removedItems);
            
            save();
            this.fireIntervalRemoved(this, 0, size - maxNumLines);
        }
    }

    public void removeAndSave(final List<String> items) {
        fLinesByLoadOrder.removeAll(items);
        // Need to iterate over the items for fLinesByFileExt to fire the removal events
        for (String item : items) {
            int indexOf = fLinesByFileExt.indexOf(item);
            if (indexOf < 0) continue;
            fLinesByFileExt.remove(item);
            this.fireIntervalRemoved(this, indexOf, indexOf);
        }
        save();
    }

    private void sort() {
        Collections.sort(fLinesByFileExt, new ComparatorFactory.FileExtComparator());
    }

    public void addAndSave(final String text) {
        this.add(text);
        this.save();
        this.sort();
        // The sort() call might reorder everything
        this.fireContentsChanged(this, 0, fLinesByFileExt.size());
    }
    
    public Collection<String> getLines() {
        return fLinesByFileExt;
    }

    /**
     * Refresh the item in the XStore.
     * @param item
     */
    public void refresh(String item) {
        // If present, make the item be the Most Recent.
        // We do nothing if the item is not already in the XStore.
        // NOTE: no need to adjust fLinesByFileExt or re-sort as that list will be
        // unchanged.  Thus, also no need to fireContentsChanged();
        if (fLinesByLoadOrder.contains(item)) {
            fLinesByLoadOrder.remove(item);
            fLinesByLoadOrder.add(item);
            this.save();
        }
    }
    
    /**
     * AbstractList implementation
     * Returns the number of components in this list.
     * Used internally by the ListModel when building ComboBoxes, JLists etc.
     */
    public int getSize() {
        if (!fLoaded) {
            load();
        }

        return fLinesByFileExt.size();
    }

    public String getElementAt(final int index) {
        return fLinesByFileExt.get(index);
    }

    public boolean contains(final String str) {
        return fLinesByFileExt.contains(str);
    }

    public String getSelectedItem() {
        if (fLinesByFileExt.size() == 0) {
            return null;
        }

        return fLinesByFileExt.get(fSelIndex);
    }

    public void setSelectedItem(final Object obj) {
        fSelIndex = fLinesByFileExt.indexOf(obj);
    }
}
