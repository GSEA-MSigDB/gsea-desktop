/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.searchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GeneSearchList {
    private JPanel fMainPanel;
    private List<String> fFeatureNames;
    private JList<String> fSearchList;
    private JTextField field = new JTextField(25);
    private DefaultListModel<String> listModel;
    private Logger log = LoggerFactory.getLogger(GeneSearchList.class);

    public GeneSearchList() {
        this.fFeatureNames = new ArrayList<>();
    }

    public JComponent getComponent() {
        if (fMainPanel == null) { jbInit(); }
        return fMainPanel;
    }

    public void setFeatures(final List<String> featureNames) {
        log.debug("setFeatures: {}", featureNames.size());
        this.fFeatureNames = featureNames;
        listModel.removeAllElements();
        for (int i = 0; i < fFeatureNames.size(); i++) {
            listModel.addElement(fFeatureNames.get(i));
        }

        fSearchList.revalidate();
        fMainPanel.revalidate();
    }
    
    public String getChosenFeature() {
        return field.getText();
    }

    public boolean isChosenFeatureInList() {
        return fFeatureNames != null && fFeatureNames.contains(field.getText());
    }
    
    public void jbInit() {
        listModel = new DefaultListModel<>();
        for (int i = 0; i < fFeatureNames.size(); i++) {
            listModel.addElement(fFeatureNames.get(i));
        }

        this.fMainPanel = new JPanel(new BorderLayout(6, 6));

        JPanel quickSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        quickSelectPanel.add(field);
        quickSelectPanel.setBorder(BorderFactory.createTitledBorder("Selected Gene"));

        this.fSearchList = new JList<String>(listModel);
        this.fSearchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.fSearchList.setVisibleRowCount(50);

        JPanel listPanel = new JPanel(new BorderLayout(2, 2));
        listPanel.setBorder(BorderFactory.createTitledBorder("Feature List"));

        fSearchList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) { return; }
                field.setText(fSearchList.getSelectedValue());
            }
        });
        listPanel.add(new JScrollPane(fSearchList));

        fMainPanel.add(quickSelectPanel, BorderLayout.BEFORE_FIRST_LINE);
        fMainPanel.add(listPanel, BorderLayout.CENTER);
    }
}
