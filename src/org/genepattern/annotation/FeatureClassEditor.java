/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.annotation;

import org.genepattern.uiutil.CenteredDialog;
import org.genepattern.uiutil.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class FeatureClassEditor extends CenteredDialog {

    private JComboBox classComboBox = new JComboBox();

    private JPanel colorPanel;

    private JButton deleteButton = new JButton("Delete");

    private JList list = new JList();

    private JScrollPane scrollPane = new JScrollPane(list);

    private MyClass selectedClass;

    private Container featureListContainer;

    private SparseClassVector classVector;

    private Frame parent;

    private SetAnnotatorModel model;

    public FeatureClassEditor(final Frame parent, SetAnnotatorModel model,
                              SparseClassVector cv) {
        super(parent);
        this.parent = parent;
        this.model = model;
        this.classVector = cv;
        setTitle("Feature Annotations");

        colorPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                int descent = g.getFontMetrics().getDescent();
                g.drawString("Click to edit...", 4, getHeight() - descent);
            }
        };

        colorPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                final Integer classNumber = ((MyClass) classComboBox
                        .getSelectedItem()).classNumber;

                Color c = JColorChooser.showDialog(parent, "Select Color",
                        colorPanel.getBackground());
                if (c != null) {
                    colorPanel.setBackground(c);
                    classVector.setClass(classNumber, classVector
                            .getClassName(classNumber), c);
                }
            }
        });

        colorPanel.setPreferredSize(new Dimension(100, 20));
        colorPanel.setMinimumSize(new Dimension(100, 20));
        colorPanel.setBorder(BorderFactory.createEtchedBorder());

        classComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedClass = (MyClass) classComboBox.getSelectedItem();
                if (selectedClass != null) {
                    Color c = classVector.getColor(selectedClass.classNumber);
                    colorPanel.setBackground(c);
                    setMembers(selectedClass.classNumber);
                }
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (UIUtil.showConfirmDialog(parent,
                        "Are you sure you want to delete "
                                + selectedClass.className + "?")) {

                }
                classVector.removeClass(selectedClass.classNumber);
            }
        });
        featureListContainer = new JPanel(new BorderLayout());

        for (Iterator classNumbers = classVector.getClassNumbers(); classNumbers
                .hasNext();) {
            Integer i = (Integer) classNumbers.next();
            String className = classVector.getClassName(i);
            classComboBox.addItem(new MyClass(className, i));
        }

        if (classComboBox.getItemCount() == 0) {
            UIUtil
                    .showMessageDialog(
                            parent,
                            "No feature annotations loaded.\nLoad feature annotations by clicking Open Feature List(s) on the File menu.");
            return;
        }
        classComboBox.setSelectedIndex(0);

        JPanel p = new JPanel();
        p.add(classComboBox);
        p.add(colorPanel);
        p.add(deleteButton);
        JPanel temp = new JPanel(new BorderLayout());
        temp.add(p, BorderLayout.NORTH);
        temp.add(scrollPane, BorderLayout.CENTER);
        featureListContainer.add(temp, BorderLayout.CENTER);
        setContentPane(featureListContainer);
        pack();
        setVisible(true);
    }

    private void setMembers(Integer classNumber) {
        Vector members = new Vector();
        List memberIndices = classVector.getMembers(classNumber);
        for (int i = 0; i < memberIndices.size(); i++) {
            Integer row = (Integer) memberIndices.get(i);
            members.add(model.getName(row.intValue()));
        }
        list.setListData(members);
    }

    private static class MyClass {
        String className;

        Integer classNumber;

        public MyClass(String className, Integer classNumber) {
            this.className = className;
            this.classNumber = classNumber;
        }

        public String toString() {
            return className;
        }
    }

}
