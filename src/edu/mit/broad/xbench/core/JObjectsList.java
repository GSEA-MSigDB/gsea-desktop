/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import edu.mit.broad.genome.io.PobTransferable;
import edu.mit.broad.genome.swing.dnd.DndSource;
import edu.mit.broad.genome.swing.dnd.DragSourceDecorator;
import edu.mit.broad.xbench.RendererFactory2;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;

/**
 * JList that holds objects and implements a little bit extra functionality.
 * <p/>
 * Functionality:
 * <p/>
 * <ul>
 * <li> Dndable out of jlist with arbitrary objects (those that are known can be dnded)</li>
 * <li> NamingConventions icons for known objects</li>
 * </ul>
 * <p/>
 * DND Note:
 * This is a drag source -> Files can be dragged OUT into appropriate receivers
 * This is NOT a drag receiver
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class JObjectsList extends JList implements DndSource {

    private JObjectsList fInstance;
    private Object[] fObjects;

    /**
     * Class Constructor.
     *
     * @param title
     * @param icon
     * @param objects
     */
    public JObjectsList(Object[] objects) {

        if (objects == null) {
            throw new IllegalArgumentException("Param objects cannot be null");
        }

        this.fObjects = objects;
        fInstance = this;

        DefaultListModel model = new DefaultListModel();

        for (int i = 0; i < fObjects.length; i++) {
            model.addElement(fObjects[i]);
        }

        this.setModel(model);

        if (model.getSize() > 0) {
            this.setSelectedIndex(0);
        }

        this.setCellRenderer(new RendererFactory2.CommonLookAndDoubleClickListRenderer(this));
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Enable Dnd
        DragSourceDecorator dec = new DragSourceDecorator(this);
    }

    /**
     * Files and Pobs are transferable
     *
     * @return
     */
    public Transferable getTransferable() {

        //log.debug("getTransferable(): " );
        Object[] objs = fInstance.getSelectedValues();

        return new PobTransferable(objs);
    }

    public Component getDraggableComponent() {
        return this;
    }

    public static void displayInWindow(final String title, final Icon icon, final JObjectsList jol) {

        SimpleWindow wd = new SimpleWindow(title, icon, new JScrollPane(jol));

        wd.open();
    }
}    // End JObjectsList
