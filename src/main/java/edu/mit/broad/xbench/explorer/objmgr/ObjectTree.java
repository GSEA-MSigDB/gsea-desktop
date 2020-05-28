/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.objmgr;

import edu.mit.broad.genome.io.FileTransferable;
import edu.mit.broad.genome.io.PobTransferable;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.dnd.DndSource;
import edu.mit.broad.genome.swing.dnd.DragSourceDecorator;
import edu.mit.broad.xbench.actions.misc_actions.CopyFilesAction;
import edu.mit.broad.xbench.actions.misc_actions.FilesSelectable;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays objects in the (parser) objects in a JTree organized
 * by object type.
 * <p/>
 * Provides drag functionality for PersistentObjects.
 * Shift-click root expand the whole tree
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ObjectTree extends JTree implements DndSource, FilesSelectable {

    private static final Logger klog = Logger.getLogger(ObjectTree.class);

    /**
     * Class Constructor.
     * Creates a new Tree based on Object Cache.
     * Listens to cache for additions, and aut updates itself.
     */
    public ObjectTree() {

        super();

        // Fix for show drop insertion point
        //fInstance.updateUI();
        // from http://groups.google.com/groups?q=tooltip+in+jtree+group:comp.lang.java.vdbgui&hl=en&lr=&ie=UTF-8&safe=off&selm=7rrnk0%24enc%241%40f40-3.zfn.uni-bremen.de&rnum=7
        this.setToolTipText("");    // wierd. Needed before tooltips show up
        this.setModel(ParserFactory.getCache().createTreeModel());
        this.setCellRenderer(new ObjectTreeRenderer());

        Border b = BorderFactory.createTitledBorder(null, "Double click to open, right click for more options",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.CENTER,
                new Font("Helvetica", Font.PLAIN, 10), Color.GRAY);

        this.setBorder(b);

        // For listening and triggering popups
        this.addMouseListener(new ObjectTreePopup(this));

        this.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        this.setRootVisible(true);
        this.setShowsRootHandles(true);

        expandAll();

        // Enable Dnd
        new DragSourceDecorator(this);

        CopyFilesAction ca = new CopyFilesAction(this);
        this.addKeyListener(ca.createCtrlCKeyListener());
    }

    // a convenience when developing with junit -- if objects are already in memory
    // the tree will open up all expanded. No effect for user runtime - as obviously no object
    // in memory at startup
    // the shift-click thing takes care of user expand request (see below)
    private void expandAll() {
        try {
            TreePath path = this.getPathForRow(0);
            if (path == null) {
                return;
            }
            //log.info("path = " + path);
            GuiHelper.Tree.expandAll(this, path);
        } catch (Throwable t) {
            klog.warn("develop error -- ignoring", t);
        }

    }

    public Transferable getTransferable() {
        PersistentObject pobs[] = getSelectedPobs();
        if (pobs != null) {
            return new PobTransferable(pobs);
        } else {
            return null;
        }
    }

    public Component getDraggableComponent() {
        return this;
    }

    /**
     * @return selected Pob or null if none selected
     */
    private PersistentObject[] getSelectedPobs() {
        TreePath[] paths = this.getSelectionPaths();

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

    /**
     * FilesSelectable impl
     *
     * @return
     */
    public FileTransferable getSelectedFiles() {
        return new FileTransferable(getTheSelectedFiles());
    }

    private File[] getTheSelectedFiles() {

        List files = new ArrayList();
        PersistentObject pobs[] = getSelectedPobs();

        for (int i = 0; i < pobs.length; i++) {
            File file = ParserFactory.getCache().getSourceFile(pobs[i]);
            if (file != null) {
                files.add(file);
            }
        }

        return (File[]) files.toArray(new File[files.size()]);

    }


}        // End ObjectCacheTree

/*--- Formatted in Sun Java Convention Style on Fri, Sep 27, '02 ---*/