/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.utils.FileUtils;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class Store
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note Does nothing if the JVM is not in xomics mode
 */
public class XStore extends AbstractListModel implements ComboBoxModel {

    private static final Logger klog = XLogger.getLogger(XStore.class);

    private File fFile;

    protected List fLines;

    private int fSelIndex = 0;

    private boolean fLoaded;

    private AdditionDecider fDecider;

    public static interface AdditionDecider {

        public String addThis(final String str);

    }


    // must call init
    protected XStore() {

    }

    protected void init(final File file, final AdditionDecider dec) {
        this.fFile = file;
        this.fLines = new ArrayList();
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

            final List ffn = ParseUtils.readFfn(fFile);
            this.fLines = new ArrayList();

            for (int i = 0; i < ffn.size(); i++) {
                String s = ffn.get(i).toString();

                String ret = fDecider.addThis(s);

                if (ret != null && ret.length() != 0) {
                    fLines.add(ret);
                }

            }

            this.sort();
            fLoaded = true;

            this.fireContentsChanged(this, fLines.size() - 1, fLines.size());

        } catch (Throwable t) {
            klog.fatal("Could not init store: " + fFile, t);
        }
    }

    public void clearAll() {
        fLines.clear();
        save();
    }

    /**
     * whitespace on either end not allowed (i.e trimmed)
     *
     * @param text
     */
    public void add(String text) {

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
            fLines.add(ret);
            this.fireContentsChanged(this, fLines.size() - 1, fLines.size());
        }

    }

    /**
     * If ain a loop, better to build and then store
     *
     * @param text
     */

    // do in thread so that we dont need to wait
    public void save() {
        Thread thread = new Thread(append());
        thread.start();
    }

    // This trimming is messed up because we sort and hence the order can be anything (and hence those that are trimmed can be anything)
    public void trim(final int maxNumLines) {

        if (!fLoaded) {
            load();
        }

        //System.out.println(">>>> TRIMMING " + fLines.size());

        if (fLines.size() > maxNumLines) {
            int size = fLines.size();
            fLines = fLines.subList(size - maxNumLines, size);
        }

        save();
        this.fireContentsChanged(this, fLines.size() - 1, fLines.size());
    }

    public void removeAndSave(final List objects) {

        for (int i = 0; i < objects.size(); i++) {
            fLines.remove(objects.get(i));
        }

        save();
    }

    public void sort() {
        Collections.sort(fLines, new ComparatorFactory.FileExtComparator());
    }

    public void addAndSave(final String text) {

        if (!fLoaded) {
            load();
        }

        if (text == null || text.length() == 0) {
            return;
        }

        this.add(text);
        this.save();
        this.sort();
    }

    private Runnable append() {

        return new Runnable() {

            public void run() {

                try {

                    if (fFile.exists() == false) {
                        fFile.createNewFile();
                    }

                    if (fFile.canWrite()) {
                        FileUtils.write(fLines, fFile);
                    }

                } catch (Throwable t) {
                    klog.error("Trouble saving store", t);
                }
            }
        };
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

        return fLines.size();
    }

    /**
     * AbstractList implementation
     * Returns the component at the specified index.
     * Used internally by the ListModel when building ComboBoxes, JLists etc.
     */
    public Object getElementAt(final int index) {
        return fLines.get(index);
    }

    public boolean contains(final String str) {
        return fLines.contains(str);
    }

    /**
     * ComboBoxModel implementation
     * <p/>
     * If there are no project in the cache, null is returned.
     * If there are projects in the cache, guaranteed that a non-null
     * project will be returned.
     */
    public Object getSelectedItem() {

        if (fLines.size() == 0) {
            return null;
        }

        return fLines.get(fSelIndex);
    }

    /**
     * ComboBoxModel implementation
     */
    public void setSelectedItem(final Object obj) {
        fSelIndex = fLines.indexOf(obj);
    }

}    // End inner class Store
