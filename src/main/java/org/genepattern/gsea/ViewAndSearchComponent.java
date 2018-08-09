/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import com.jidesoft.dialog.ButtonNames;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.grid.*;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideTitledBorder;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.swing.GuiHelper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jgould
 */
public class ViewAndSearchComponent extends JPanel {

    private SortableTable sortableTable;

    private QuickFilterPane quickFilterPane;

    private QuickTableFilterField filterField;

    private int numberOfGeneSets;

    private JLabel filteredLabel;

    private ControlPanel controlPanel;

    public void setTableModel(final TableModel tableModel) {
        numberOfGeneSets = tableModel.getRowCount();
        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                filteredLabel.setText(tableModel.getRowCount() + " out of "
                        + numberOfGeneSets + " gene sets");
            }
        });

        quickFilterPane.setTableModel(new SortableTableModel(tableModel));
        filterField.setTableModel(quickFilterPane.getDisplayTableModel());
        this.sortableTable.setModel(filterField.getDisplayTableModel());

        filterField.getDisplayTableModel().addTableModelListener(
                new TableModelListener() {
                    public void tableChanged(TableModelEvent e) {
                        if (e.getSource() instanceof FilterableTableModel) {
                            int count = ((TableModel) e.getSource())
                                    .getRowCount();

                            filteredLabel.setText(count + " out of "
                                    + numberOfGeneSets + " gene sets");
                        }
                    }
                });
    }

    public ViewAndSearchComponent(String runBtnText, int[] searchColumnIndices,
                                  ActionListener runListener, ActionListener reportListener, 
                                  boolean includeRunPanel, JPanel topPanel) {
        quickFilterPane = new QuickFilterPane();
        quickFilterPane.setColumnIndices(searchColumnIndices);
        JPanel quickSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        filterField = new QuickTableFilterField();
        //filterField.setColumnIndices(searchColumnIndices);
        quickSearchPanel.add(filterField);

        quickSearchPanel.setBorder(new JideTitledBorder(
                new PartialEtchedBorder(PartialEtchedBorder.LOWERED,
                        PartialSide.NORTH), "Filter Gene Sets",
                JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));

        filteredLabel = new JLabel("   ");
        filteredLabel.setHorizontalAlignment(SwingConstants.CENTER);
        filteredLabel.setForeground(GuiHelper.COLOR_DARK_GREEN);
        filteredLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        this.sortableTable = new SortableTable() {
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

        sortableTable.setShowSortOrderNumber(false);
        sortableTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        filterField.setTable(sortableTable);
        JideScrollPane scrollPane = new JideScrollPane(sortableTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        quickSearchPanel.add(filteredLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        if (topPanel != null) {
            headerPanel.add(topPanel, BorderLayout.NORTH);
        }
        headerPanel.add(quickSearchPanel, BorderLayout.CENTER);

        setLayout(new BorderLayout());

        this.add(scrollPane, BorderLayout.CENTER);
        this.add(headerPanel, BorderLayout.NORTH);
        if (includeRunPanel) {
            controlPanel = new ControlPanel(runBtnText, runListener, reportListener, sortableTable);
            this.add(controlPanel, BorderLayout.SOUTH);
        }

    }

    static class ControlPanel extends ButtonPanel {
        private JButton bLeadingEdge;
        private JButton buildhtmlReportBtn;

        private TableSelectionListener tableSelectionListener;

        private JLabel label;

        public ControlPanel(String runBtnText,
                            final ActionListener runListener, final ActionListener reportListener, 
                            final SortableTable table) {

            setBorder(BorderFactory.createEmptyBorder(5, 10, 20, 10));

            JButton bHelp = JarResources.createHelpButton("post_hoc");
            addButton(bHelp, ButtonNames.HELP);

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
