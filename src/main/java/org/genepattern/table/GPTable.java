/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.table;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
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