/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.models.NumberedProxyModel;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.core.Widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * Base class for several Viewer.
 * Contains commonly useful initialization and methods.
 *
 * @author Aravind Subramanian
 */
public abstract class AbstractViewer extends JPanel implements Widget {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    private Icon fIcon;
    private String fName;
    private String fTitle;

    public AbstractViewer(final String name, final Icon icon, final String title) {
        if (name == null) { throw new IllegalArgumentException("Param name cannot be null"); }
        if (title == null) { throw new IllegalArgumentException("Param title cannot be null"); }

        this.fIcon = icon;
        this.fName = name;
        this.fTitle = title;
    }

    public AbstractViewer(final String name, final Icon icon, final PersistentObject pob_for_setting_title) {
        this(name, icon, formatTitle(pob_for_setting_title, name));
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
     */
    protected static String formatTitle(PersistentObject pob, String name) {
        return (pob == null) ? "na -- " + name : pob.getName();
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

    protected static JTable createTable(final TableModel model, final boolean addRowNumCol, final boolean boldHeaders) {
        TableModel amodel = (addRowNumCol) ? new NumberedProxyModel(model) : model;

        JTable table = new JTable(amodel);
        if (addRowNumCol) { // has to be done after setting model
            setColumnSize(35, 0, table, true);
        }

        table.setCellSelectionEnabled(true);
        return table;
    }
}
