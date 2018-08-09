/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * We encapsulate the real model
 * Prevents client code from adding to the model (i.e the defaulttreemodel
 * which is mutable is not exposed)
 * <p/>
 * IMP -> ?? multi-instance selection safe??
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ProxyTreeModel implements TreeModel {

    private final DefaultTreeModel fRealModel;

    /**
     * Class Constructor.
     *
     * @param model
     */
    public ProxyTreeModel(DefaultTreeModel model) {
        this.fRealModel = model;
    }

    public void addTreeModelListener(TreeModelListener l) {
        fRealModel.addTreeModelListener(l);
    }

    public Object getChild(Object parent, int index) {
        return fRealModel.getChild(parent, index);
    }

    public int getChildCount(Object parent) {
        return fRealModel.getChildCount(parent);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return fRealModel.getIndexOfChild(parent, child);
    }

    public Object getRoot() {
        return fRealModel.getRoot();
    }

    public boolean isLeaf(Object node) {
        return fRealModel.isLeaf(node);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        fRealModel.removeTreeModelListener(l);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        fRealModel.valueForPathChanged(path, newValue);
    }

} // End ProxyTreeModel

