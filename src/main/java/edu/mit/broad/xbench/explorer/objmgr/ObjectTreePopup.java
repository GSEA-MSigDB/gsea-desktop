/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.objmgr;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.swing.GPopupChecker;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.core.api.Application;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class ObjectTreePopup extends GPopupChecker {

    private ObjectTree fTree;

    public ObjectTreePopup(final ObjectTree tree) {
        this.fTree = tree;

        // For double clicks
        // IMP to NOT place this piece of code in the popupmenu checker - that causes
        // the widget to launch twice
        fTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showPopup(e);
                }
            }
        });

    }


    protected void maybeShowPopup(MouseEvent e) {

        if (isRootExpandClick(e)) {
            expandAll();
            return;
        }

        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    private void showPopup(MouseEvent e) {

        Object ot = e.getSource();

        if (ot instanceof JTree) {
            PersistentObject pob = getSelectedPob();

            if (pob != null) {
                JPopupMenu menu = Application.getWindowManager().createPopupMenu(pob);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        } else {
            System.out.println(">>>>  showPopup: " + ot);
        }
    }


    /**
     * was the root of the tree shift clicked?
     * This is used to trigger an expland all event
     *
     * @param e
     * @return
     */
    private boolean isRootExpandClick(MouseEvent e) {

        if (e.isShiftDown() == false) {
            return false;
        }

        TreePath[] paths = fTree.getSelectionPaths();

        if (paths == null) {

        } else if (paths.length != 1) {

        } else {
            Object obj = paths[0].getLastPathComponent();
            //log.debug("selected obj: " + obj);
            if (obj == fTree.getModel().getRoot()) {
                return true;
            }
        }

        return false;

    }


    private PersistentObject getSelectedPob() {

        PersistentObject[] pobs = getSelectedPobs();
        if ((pobs == null) || (pobs.length == 0)) {
            return null;
        } else {
            return pobs[0];
        }

    }

    /**
     * @return selected Pob or null if none selected
     */
    private PersistentObject[] getSelectedPobs() {
        TreePath[] paths = fTree.getSelectionPaths();

        if (paths == null) {
            return null;
        }

        List pobs = new ArrayList();
        for (int i = 0; i < paths.length; i++) {
            Object obj = paths[i].getLastPathComponent();

            if (obj instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode dmtr = (DefaultMutableTreeNode) obj;
                if (dmtr.getAllowsChildren()) {
                    // its a "folder" those arent dndable
                } else {
                    Object userobj = dmtr.getUserObject();
                    //log.debug("UserObject is: " + userobj + " class: " + userobj.getClass());
                    if (userobj instanceof PersistentObject) {
                        pobs.add(userobj);
                    }
                }
            }
        }

        return (PersistentObject[]) pobs.toArray(new PersistentObject[pobs.size()]);

    }

    // a convenience when developing with junit -- if objects are already in memory
    // the tree will open up all expanded. No effect for user runtime - as obviously no object
    // in memory at startup
    // the shift-click thing takes care of user expand request (see below)
    private void expandAll() {
        try {
            TreePath path = fTree.getPathForRow(0);
            if (path == null) {
                return;
            }
            //log.info("path = " + path);
            GuiHelper.Tree.expandAll(fTree, path);
        } catch (Throwable t) {
            //klog.warn("develop error -- ignoring", t);
        }

    }

} // End class MyPopupMouseListener
