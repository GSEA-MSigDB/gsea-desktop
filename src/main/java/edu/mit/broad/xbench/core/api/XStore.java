/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.parsers.ParseUtils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Persistent path/string store (no Swing list models).
 */
public class XStore {
    private static final Logger klog = LoggerFactory.getLogger(XStore.class);

    private File fFile;
    private List<String> fLinesByFileExt;
    private List<String> fLinesByLoadOrder;
    private boolean fLoaded;
    private AdditionDecider fDecider;

    public interface AdditionDecider {
        String addThis(String str);
    }

    protected XStore() {
    }

    protected void init(final File file, final AdditionDecider dec) {
        this.fFile = file;
        this.fLinesByFileExt = new ArrayList<>();
        this.fLinesByLoadOrder = new ArrayList<>();
        this.fDecider = dec;
    }

    private void load() {
        if (fLoaded) {
            return;
        }
        try {
            if (!fFile.exists()) {
                fFile.createNewFile();
            }
            final List<String> ffn = ParseUtils.readFfn(fFile);
            this.fLinesByFileExt = new ArrayList<>();
            this.fLinesByLoadOrder = new ArrayList<>();
            for (int i = 0; i < ffn.size(); i++) {
                String s = ffn.get(i);
                String ret = fDecider.addThis(s.trim());
                if (ret != null && ret.length() != 0) {
                    fLinesByFileExt.add(ret);
                    fLinesByLoadOrder.add(ret);
                }
            }
            sort();
            fLoaded = true;
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
        if (ret != null && ret.length() != 0) {
            fLinesByLoadOrder.add(ret);
            fLinesByFileExt.add(ret);
        } else if (fLinesByLoadOrder.contains(text)) {
            fLinesByLoadOrder.remove(text);
            fLinesByLoadOrder.add(text);
        }
    }

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
            List<String> removedItems = fLinesByLoadOrder.subList(0, size - maxNumLines);
            fLinesByLoadOrder = new ArrayList<>(fLinesByLoadOrder.subList(size - maxNumLines, size));
            fLinesByFileExt.removeAll(removedItems);
            save();
        }
    }

    public void removeAndSave(final List<String> items) {
        fLinesByLoadOrder.removeAll(items);
        fLinesByFileExt.removeAll(items);
        save();
    }

    private void sort() {
        Collections.sort(fLinesByFileExt, new ComparatorFactory.FileExtComparator());
    }

    public void addAndSave(final String text) {
        this.add(text);
        this.save();
        this.sort();
    }

    public Collection<String> getLines() {
        if (!fLoaded) {
            load();
        }
        return fLinesByFileExt;
    }

    public void refresh(String item) {
        if (fLinesByLoadOrder.contains(item)) {
            fLinesByLoadOrder.remove(item);
            fLinesByLoadOrder.add(item);
            this.save();
        }
    }

    public int getSize() {
        if (!fLoaded) {
            load();
        }
        return fLinesByFileExt.size();
    }

    public String getElementAt(final int index) {
        if (!fLoaded) {
            load();
        }
        return fLinesByFileExt.get(index);
    }

    public boolean contains(final String str) {
        // Swing XStore.contains: do not call load() — AdditionDecider.addThis uses contains during load.
        return fLinesByFileExt.contains(str);
    }
}
