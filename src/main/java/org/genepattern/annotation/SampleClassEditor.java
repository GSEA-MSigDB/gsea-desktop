/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.annotation;

import org.genepattern.uiutil.CenteredDialog;
import org.genepattern.uiutil.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

public class SampleClassEditor extends CenteredDialog {
    private JComboBox classComboBox = new JComboBox();
    private JPanel colorPanel;
    private JButton deleteButton = new JButton("Delete");
    private javax.swing.JList list = new javax.swing.JList();
    private JScrollPane scrollPane = new JScrollPane(list);
    private Container featureListContainer;
    private JComboBox groupsComboBox;
    private SparseClassVector classVector;
    private SetAnnotatorModel model;

    public SampleClassEditor(final Frame parent, SetAnnotatorModel model,
                             SparseClassVector cv) {
        super(parent);
        this.model = model;
        this.classVector = cv;
        setTitle("Sample Annotations");

        classComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MyClass selectedClass = (MyClass) classComboBox
                        .getSelectedItem();
                if (selectedClass != null) {
                    Color c = classVector.getColor(selectedClass.classNumber);
                    colorPanel.setBackground(c);
                    setMembers(selectedClass.classNumber);
                } else {
                    colorPanel.setBackground(Color.BLACK);
                    setMembers(-1);
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ClassGroup group = (ClassGroup) groupsComboBox
                        .getSelectedItem();
                if (group == null) {
                    return;
                }
                if (UIUtil.showConfirmDialog(parent,
                        "Are you sure you want to delete " + group.toString()
                                + "?")) {

                    for (int j = 0; j < group.group.size(); j++) {
                        classVector.removeClass((Integer) group.group.get(j));
                    }
                    classVector.removeClassGroup(group.group);
                    groupsComboBox.removeItem(group);
                    classComboBox.removeAllItems();
                    if (groupsComboBox.getItemCount() > 0) {
                        groupsComboBox.setSelectedIndex(0);
                    }
                }
            }
        });
        featureListContainer = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        groupsComboBox = new JComboBox();
        List classGroups = classVector.getClassGroups();
        if (classGroups == null || classGroups.size() == 0) {
            UIUtil.showMessageDialog(parent,
                    "No sample annotations loaded.\nLoad sample annotations by clicking Open Cls File on the File menu.");
            return;
        }
        for (int i = 0; i < classGroups.size(); i++) {
            List group = (List) classGroups.get(i);
            ClassGroup classGroup = new ClassGroup(group, classVector);
            groupsComboBox.addItem(classGroup);
        }

        groupsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ClassGroup classGroup = (ClassGroup) groupsComboBox
                        .getSelectedItem();
                classComboBox.removeAllItems();
                if (classGroup != null) {
                    for (int i = 0; i < classGroup.group.size(); i++) {
                        Integer classNumber = (Integer) classGroup.group.get(i);
                        classComboBox.addItem(new MyClass(classVector
                                .getClassName(classNumber), classNumber));
                    }
                    classComboBox.setSelectedIndex(0);
                }
            }
        });

        topPanel.add(groupsComboBox);
        topPanel.add(deleteButton);

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

        groupsComboBox.setSelectedIndex(0);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(classComboBox);
        bottomPanel.add(colorPanel);

        JPanel temp = new JPanel(new BorderLayout());
        temp.add(topPanel, BorderLayout.NORTH);
        temp.add(bottomPanel, BorderLayout.CENTER);
        featureListContainer.add(temp, BorderLayout.NORTH);
        featureListContainer.add(scrollPane, BorderLayout.CENTER);
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

    static class ClassGroup {
        List group;
        String name;
        SparseClassVector classVector;

        public ClassGroup(List group, SparseClassVector classVector) {
            this.group = group;
            this.classVector = classVector;
            update();
        }

        public void update() {
            name = classVector.getClassGroupName(group);
        }

        public String toString() {
            return name;
        }
    }
}
