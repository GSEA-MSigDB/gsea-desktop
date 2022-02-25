/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.viewers;

import com.jidesoft.grid.SortableTable;

import edu.mit.broad.genome.models.NumberedProxyModel;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.core.Widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * Base class for several Viewer.
 * Contains commonly useful initialization and methods.
 *
 * @author Aravind Subramanian
 */
public abstract class AbstractViewer extends JPanel implements Widget {
    protected Logger log;

    private Icon fIcon;

    private String fName;

    private String fTitle;


    /**
     * Class constructor
     *
     * @param name
     * @param icon
     */
    /*
    public AbstractViewer(final String name, final Icon icon) {
        this(name, icon, (String) null);
    }
    */

    // Users of this method must call init
    protected AbstractViewer() {
        this.log = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Class constructor
     *
     * @param name
     * @param icon
     * @param title
     */
    public AbstractViewer(final String name, final Icon icon, final String title) {
        init(name, icon, title);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param icon
     * @param pob_for_setting_title
     */
    public AbstractViewer(final String name, final Icon icon, final PersistentObject pob_for_setting_title) {
        this(name, icon, formatTitle(pob_for_setting_title, name));
    }

    protected void init(final String name, final Icon icon, final String title) {

        if (name == null) {
            throw new IllegalArgumentException("Param name cannot be null");
        }

        if (title == null) {
            throw new IllegalArgumentException("Param title cannot be null");
        }

        this.fIcon = icon;
        this.fName = name;
        this.fTitle = title;
        if (log == null) { this.log = LoggerFactory.getLogger(this.getClass()); }

    }

    public JComponent getWrappedComponent() {
        _checkInit();
        return this;
    }

    public Icon getAssociatedIcon() {
        _checkInit();
        return fIcon;
    }

    public String getName() {
        _checkInit();
        return fName;
    }

    private void _checkInit() {
        if (fName == null) {
            throw new IllegalStateException("Viewer likely not init'ed name: " + fName);
        }

        if (fTitle == null) {
            throw new IllegalStateException("Viewer likely not init'ed title: " + fTitle);
        }
    }

    public String getAssociatedTitle() {
        return fTitle;
    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

    /**
     * useful to give windows a title that includes the pob's
     * name.
     * Null pob is ok - just returns the prefix
     *
     * @param pob
     * @return
     */
    protected static String formatTitle(PersistentObject pob, String name) {

        if (pob == null) {
            return "na -- " + name;
        } else {
            return pob.getName();
        }
    }

    protected static void setColumnSize(int size, int col, JTable table, boolean alsoMax) {
        GuiHelper.Table.setColumnSize(size, col, table, alsoMax);
    }

    protected static JScrollPane createAlwaysScrollPane(JTable table) {

        JScrollPane sp = new JScrollPane(table);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        return sp;

    }

    protected static SortableTable createTable(final TableModel model,
                                               final boolean addRowNumCol,
                                               final boolean boldHeaders) {


        TableModel amodel = model;

        if (addRowNumCol) {
            amodel = new NumberedProxyModel(model);
        }

        SortableTable table = new SortableTable(amodel); // @note changed for jide

        // @note comm out renderers Dec 2005 .. the move to jgoodies lnf makes the headers look not so good

        if (addRowNumCol) { // has to be done after setting model
            setColumnSize(35, 0, table, true);
        }

        table.setCellSelectionEnabled(true);
        return table;
    }
}
