/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.menu;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.uiutil.CenteredDialog;
import org.genepattern.uiutil.UIUtil;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * @author Joshua Gould
 */
public class FindAction extends AbstractAction {
    private JTable table;
    private SearchDialog searchDialog;
    private int highlightColumn;
    private Frame parent;
    private FindModel findModel;

    public FindAction(Frame parent, JTable table, IExpressionData data, int highlightColumn) {
        this(parent, table);
        findModel = new IExpressionDataModel(data);
        this.highlightColumn = highlightColumn;
    }

    private FindAction(Frame parent, JTable table) {
        super("Find...");
        this.parent = parent;
        this.table = table;

        KeyStroke ks = KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx());
        this.putValue(AbstractAction.ACCELERATOR_KEY, ks);
    }

    public void actionPerformed(ActionEvent e) {
        if (searchDialog == null) { searchDialog = new SearchDialog(); }
        searchDialog.setVisible(true);
    }

    private static interface FindModel {
        public Object getValue(int row);
    }

    private static class IExpressionDataModel implements FindModel {
        private IExpressionData data;
        public IExpressionDataModel(IExpressionData data) {
            this.data = data;
        }

        public Object getValue(int row) {
            return data.getRowName(row);
        }
    }

    /**
     * @author Joshua Gould
     */
    class SearchDialog extends CenteredDialog {
        public SearchDialog() {
            super(parent);
            setTitle("Find");
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            JPanel searchPanel = new JPanel(new BorderLayout());
            JLabel searchTermLabel = new JLabel("Search features for:");
            final JTextField searchField = new JTextField(20);

            searchPanel.add(searchTermLabel, BorderLayout.NORTH);
            searchPanel.add(searchField, BorderLayout.CENTER);

            final JCheckBox caseCheckBox = new JCheckBox("Match case");
            JPanel optionsPanel = new JPanel();
            optionsPanel.add(caseCheckBox);

            JButton findButton = new JButton("Find Next");
            getRootPane().setDefaultButton(findButton);
            findButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean caseSensitive = caseCheckBox.isSelected();
                    String searchTerm = searchField.getText().trim();

                    if (searchTerm == null || searchTerm.equals("")) {
                        notFound();
                        return;
                    }
                    if (!caseSensitive) {
                        searchTerm = searchTerm.toLowerCase();
                    }

                    boolean found = false;
                    int row = -1;
                    int startRow = table.getSelectedRow() + 1;

                    for (int i = startRow, rows = table.getRowCount(); i < rows
                            && !found; i++) {
                        String value = String.valueOf(findModel.getValue(i));
                        if (!caseSensitive) {
                            value = value.toLowerCase();
                        }
                        if (value.indexOf(searchTerm) >= 0) {
                            row = i;
                            found = true;
                        }
                    }
                    if (!found) {
                        for (int i = 0; i < startRow && !found; i++) { // wrap
                            // search if not found
                            String value = String.valueOf(findModel.getValue(i));
                            if (!caseSensitive) {
                                value = value.toLowerCase();
                            }
                            if (value.indexOf(searchTerm) >= 0) {
                                row = i;
                                found = true;
                            }
                        }
                    }

                    if (!found) {
                        notFound();
                    } else {
                        table.changeSelection(row, highlightColumn, false, false);
                    }
                }
            });

            JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
            buttonPanel.add(findButton);
            buttonPanel.add(closeButton);

            Container c = getContentPane();
            c.setLayout(new BorderLayout());
            JPanel temp = new JPanel();

            temp.add(searchPanel);
            temp.add(buttonPanel);
            c.add(temp, BorderLayout.CENTER);
            c.add(optionsPanel, BorderLayout.SOUTH);
            pack();
            searchField.requestFocus();
            setResizable(false);
        }

        private void notFound() {
            UIUtil.showMessageDialog(this, "The search term you entered was not found.");
        }
    }
}
