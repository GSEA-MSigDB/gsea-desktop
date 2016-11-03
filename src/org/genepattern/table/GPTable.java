/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.table;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;

/**
 * @author Joshua Gould
 */
public class GPTable extends JTable {
    public GPTable(TableModel model) {
        super(model);
        this.setShowGrid(true);
        this.setGridColor(java.awt.Color.black);
        this.setColumnSelectionAllowed(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        addMouseListener(new java.awt.event.MouseAdapter() {
            java.awt.Point p = new java.awt.Point();

            public void mouseClicked(java.awt.event.MouseEvent e) {
                p.x = e.getX();
                p.y = e.getY();
                int row = rowAtPoint(p);
                int column = columnAtPoint(p);
                if (row != -1 && column != -1) {
                    Object value = getValueAt(row, column);
                }
            }
        });
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return realIndex >= 0 ? getColumnName(realIndex) : null;
            }
        };
    }

    /**
     * Copies the data in the table to the system clipboard
     */
    public void copy() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        int[] selectedRows = this.getSelectedRows();
        int[] selectedColumns = this.getSelectedColumns();
        StringBuffer buf = getSelectedData(this, selectedRows, selectedColumns);
        StringSelection stringSelection = new StringSelection(buf.toString());
        clipboard.setContents(stringSelection, stringSelection);
    }

    /**
     * Gets the data selected by the user in the this.
     *
     * @param table           Description of the Parameter
     * @param selectedRows    Description of the Parameter
     * @param selectedColumns Description of the Parameter
     * @return the selected data.
     */
    public static StringBuffer getSelectedData(JTable table,
                                               int[] selectedRows, int[] selectedColumns) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < selectedRows.length; i++) {
            for (int j = 0; j < selectedColumns.length; j++) {
                Object value = table.getValueAt(selectedRows[i],
                        selectedColumns[j]); // column converted to model
                if (value != null) {
                    buf.append(value);
                }
                buf.append("\t");
            }
            buf.append("\n");
        }
        return buf;
    }

    public String getToolTipText(MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();
        int hitColumnIndex = columnAtPoint(p);
        int hitRowIndex = rowAtPoint(p);
        if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
            Object obj = getValueAt(hitRowIndex, hitColumnIndex);
            if (obj != null) {
                tip = obj.toString();
            }
        }
        if (tip == null) {
            tip = getToolTipText();
        }
        return tip;
    }
}
