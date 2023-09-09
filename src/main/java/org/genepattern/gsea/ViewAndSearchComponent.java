/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.gsea;

import com.jidesoft.dialog.ButtonPanel;
//import com.jidesoft.grid.SortableTable;
//import com.jidesoft.grid.SortableTableModel;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.swing.GuiHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.jfree.ui.SortButtonRenderer;
import org.jfree.ui.SortableTable;
import org.jfree.ui.SortableTableHeaderListener;
import org.jfree.ui.SortableTableModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jgould
 */
public class ViewAndSearchComponent extends JPanel {
    private SortableTable sortableTable;
    private ControlPanel controlPanel;

    public ViewAndSearchComponent(String runBtnText, final SortableTableModel tableModel, 
            ActionListener runListener, ActionListener reportListener) {
        this.sortableTable = new SortableTable(tableModel) {
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
        };
        
//        sortableTable.setShowSortOrderNumber(false);
        sortableTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane scrollPane = new JScrollPane(sortableTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
        controlPanel = new ControlPanel(runBtnText, runListener, reportListener, sortableTable);
        this.add(controlPanel, BorderLayout.SOUTH);
    }

    static class ControlPanel extends ButtonPanel {
        private JButton bLeadingEdge;
        private JButton buildhtmlReportBtn;
        private TableSelectionListener tableSelectionListener;
        private JLabel label;

        public ControlPanel(String runBtnText, final ActionListener runListener,
                final ActionListener reportListener, final SortableTable table) {
            setBorder(BorderFactory.createEmptyBorder(5, 10, 20, 10));
            JButton bHelp = JarResources.createHelpButton("post_hoc");
            addButton(bHelp, "Help");
            label = new JLabel("For " + table.getSelectedRows().length
                    + " selected gene sets: ");
            table.getSelectionModel().addListSelectionListener(
                    new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            label.setText("For "
                                    + table.getSelectedRows().length
                                    + " selected gene sets: ");
                        }
                    });
            add(label);

            bLeadingEdge = new JButton(runBtnText, GuiHelper.ICON_START16);
            bLeadingEdge.addActionListener(runListener);
            bLeadingEdge.setEnabled(false);
            tableSelectionListener = new TableSelectionListener(table);
            table.getSelectionModel().addListSelectionListener(
                    tableSelectionListener);
            tableSelectionListener.addComponent(bLeadingEdge);
            add(bLeadingEdge);

            buildhtmlReportBtn = new JButton("Build HTML Report",
                    GuiHelper.ICON_START16);
            buildhtmlReportBtn.addActionListener(reportListener);
            buildhtmlReportBtn.setEnabled(false);
            tableSelectionListener.addComponent(buildhtmlReportBtn);
            add(buildhtmlReportBtn);
        }
    }

    public TableSelectionListener getTableSelectionListener() {
        return controlPanel.tableSelectionListener;
    }

    private static class TableSelectionListener implements ListSelectionListener {
        private SortableTable table;

        private List list;

        public TableSelectionListener(SortableTable table) {
            table.getSelectionModel().addListSelectionListener(this);
            this.table = table;
            list = new ArrayList();
        }

        public void addComponent(Component c) {
            list.add(c);
        }

        public void valueChanged(ListSelectionEvent e) {
            boolean enabled = table.getSelectedRows().length >= 2;
            for (int i = 0, size = list.size(); i < size; i++) {
                ((Component) list.get(i)).setEnabled(enabled);
            }

        }
    }

    public String[] getSelectedColumnArray(int columnIndex) {
        final int[] rows = sortableTable.getSelectedRows();
        final String[] names = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            names[i] = sortableTable.getModel()
                    .getValueAt(rows[i], columnIndex).toString();
        }

        return names;
    }

    public SortableTable getTable() {
        return sortableTable;
    }

    public JPanel getButtonPanel() {
        return this.controlPanel;
    }

}
