/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.annotation;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.parsers.MiscParsers;
import edu.mit.broad.genome.parsers.ParserFactory;

import org.apache.commons.io.FilenameUtils;
import org.genepattern.data.matrix.ClassVector;
import org.genepattern.module.VisualizerUtil;
import org.genepattern.uiutil.FileChooser;
import org.genepattern.uiutil.UIUtil;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Annotates sets (lists) in a table
 *
 * @author jgould
 */
// TODO: check all of the member privacy access levels.
// Doubtful that package-level visibility is required.
public class SetAnnotator {
    Frame parent;

    SparseClassVector classVector = new SparseClassVector();

    int classNumberCounter = 0;

    final static Color[] colors = {Color.red, Color.yellow, Color.blue,
            Color.GREEN, Color.ORANGE, Color.magenta, Color.CYAN, Color.PINK,
            Color.GRAY};

    private boolean annotateRow = true;

    SetAnnotatorModel model;

    FeatureRenderer featureRenderer;

    SampleClassEditor sampleClassEditor;

    FeatureClassEditor featureClassEditor;

    private JMenuItem openFeaturesMenuItem;

    private JMenuItem viewFeatureListsMenuItem;

    private JTable table;

    int widthPerClass = 6;

    public SparseClassVector getClassVector() {
        return classVector;
    }

    public void slice(int[] order) {
        classVector.slice(order);
        classVector.notifyListeners();
    }

    public SetAnnotator(final Frame parent, final SetAnnotatorModel model,
                        boolean _annotateRow) {
        this.parent = parent;
        this.model = model;
        this.annotateRow = _annotateRow;
        featureRenderer = new FeatureRenderer();
        if (annotateRow) {
            viewFeatureListsMenuItem = new JMenuItem("Feature Annotations...");
        } else {
            viewFeatureListsMenuItem = new JMenuItem("Sample Annotations...");
        }
        viewFeatureListsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!annotateRow) {
                    if (sampleClassEditor != null
                            && sampleClassEditor.isShowing()) {
                        return;
                    }
                    sampleClassEditor = new SampleClassEditor(parent, model,
                            classVector);
                } else {
                    if (featureClassEditor != null
                            && featureClassEditor.isShowing()) {
                        return;
                    }
                    featureClassEditor = new FeatureClassEditor(parent, model,
                            classVector);
                }
            }
        });

        if (annotateRow) {
            openFeaturesMenuItem = new JMenuItem("Open Feature List(s)...");
        } else {
            openFeaturesMenuItem = new JMenuItem("Open Cls File...");
        }
        openFeaturesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String title = annotateRow ? "Select Feature List(s)"
                        : "Select Cls File";
                File f = FileChooser.showOpenDialog(parent, title);
                if (f != null) {
                    String extension = FilenameUtils.getExtension(f.getName());
                    if (annotateRow) {
                        boolean missingFeatures = false;
                        try {

                            if (extension != null) {
                                extension = extension.toLowerCase();
                                if (extension.equals("gmt")
                                        || extension.equals("gmx")) {
                                    GeneSetMatrix gmt = ParserFactory
                                            .readGeneSetMatrix(f, false);

                                    int sets = gmt.getNumGeneSets();
                                    for (int i = 0; i < sets; i++) {
                                        GeneSet set = gmt.getGeneSet(i);
                                        boolean result = addToFeatureList(set
                                                .getMembers(), set.getName(),
                                                null);
                                        missingFeatures = missingFeatures
                                                || result;
                                    }
                                } else if (extension.equals("grp")) {
                                    GeneSet gset = ParserFactory.readGeneSet(f,
                                            false);
                                    missingFeatures = addToFeatureList(gset
                                            .getMembers(), f.getName(), null);
                                } else {
                                    List featureList = VisualizerUtil
                                            .readFeatureList(parent, f
                                                    .getCanonicalPath());
                                    missingFeatures = addToFeatureList(
                                            featureList, f.getName(), null);
                                }
                            } else {
                                List featureList = VisualizerUtil
                                        .readFeatureList(parent, f
                                                .getCanonicalPath());
                                missingFeatures = addToFeatureList(featureList,
                                        f.getName(), null);
                            }
                            if (missingFeatures) {
                                UIUtil
                                        .showMessageDialog(parent,
                                                "Warning: The file contains features that are not found in the data set.");

                            }
                            if (table != null) {
                                table.invalidate();
                                table.validate();
                                table.repaint();
                            }

                        } catch (Exception x) {
                            UIUtil.showErrorDialog(parent,
                                    "An error occurred while reading "
                                            + f.getName());
                        }

                    } else {
                        try {
                            if (extension != null && extension.equals("xls")) {
                                Map map = MiscParsers.parseColorMapFromExcel(f);
                                for (Iterator it = map.keySet().iterator(); it
                                        .hasNext();) {
                                    String key = (String) it.next();
                                    Color val = (Color) map.get(key);
                                    // FIXME
                                }

                            } else {
                                ClassVector cv = VisualizerUtil.readCls(parent,
                                        f.getCanonicalPath());
                                if (cv == null) {
                                    return;
                                }
                                if (cv.size() != model.getFeatureCount()) {
                                    UIUtil
                                            .showErrorDialog(
                                                    parent,
                                                    "The number of samples in the cls file ("
                                                            + cv.size()
                                                            + ") does not match the number of samples in the dataset ("
                                                            + model
                                                            .getFeatureCount()
                                                            + ").");
                                    return;
                                }
                                List group = new ArrayList();
                                for (int j = 0; j < cv.getClassCount(); j++) {
                                    Color c = null;
                                    int index = j + classNumberCounter;
                                    if (index >= colors.length) {
                                        c = new Color(
                                                (int) (Math.random() * 255),
                                                (int) (Math.random() * 255),
                                                (int) (Math.random() * 255));
                                    } else {
                                        c = colors[index];
                                    }
                                    group.add(new Integer(index));
                                    classVector.setClass(new Integer(index), cv
                                            .getClassName(j), c);

                                }
                                classVector.addClassGroup(group, f.getName());

                                for (int j = 0; j < cv.size(); j++) {
                                    classVector.addClass(j, new Integer(cv
                                            .getAssignment(j)
                                            + classNumberCounter));
                                }
                                classVector.notifyListeners();
                                classNumberCounter += cv.getClassCount();
                            }
                            if (table != null) {
                                table.invalidate();
                                table.validate();
                                table.repaint();
                            }
                        } catch (Exception e1) {
                            UIUtil.showErrorDialog(parent,
                                    "An error occurred while reading "
                                            + f.getName());
                        }
                    }

                    // new EditClassDialog(className, classNumber, c, true);

                }
            }
        });

        // Skip the menu if we're running without a parent Frame (e.g. reporting from the command-line 
        if (annotateRow && parent != null) {
            openFeaturesMenuItem.setAccelerator(KeyStroke.getKeyStroke('O',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
    }

    boolean addToFeatureList(List featureList, String className, Color c) {

        boolean missingFeatures = false;
        Integer classNumber = new Integer(classNumberCounter);
        for (int i = 0; i < featureList.size(); i++) {
            int index = model.getIndex((String) featureList.get(i));
            if (index == -1) {
                missingFeatures = true;
            } else {
                classVector.addClass(index, classNumber);
            }
        }

        if (c == null) {
            if (classNumberCounter >= colors.length) {
                c = new Color((int) (Math.random() * 255),
                        (int) (Math.random() * 255),
                        (int) (Math.random() * 255));
            } else {
                c = colors[classNumberCounter];
            }
        }
        classVector.setClass(classNumber, className, c);
        classNumberCounter++;
        return missingFeatures;
    }

    class FeatureRenderer extends javax.swing.table.DefaultTableCellRenderer {
        List classNumbers;

        MyIcon icon = new MyIcon();

        public void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
        }

        public Component getTableCellRendererComponent(
                javax.swing.JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            if (annotateRow) {
                classNumbers = classVector.getClassNumbers(model
                        .getMappedIndex(row));
            } else {
                classNumbers = classVector.getClassNumbers(model
                        .getMappedIndex(column));

            }
            if (classNumbers != null && classNumbers.size() != 0) {
                icon.labelRows = annotateRow;
                setIcon(icon);
            } else {
                setIcon(null);
            }

            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, false, row, column);
            return c;
        }

        class MyIcon implements javax.swing.Icon {
            boolean labelRows = true;

            int iconSize = 0;

            public void paintIcon(java.awt.Component c, java.awt.Graphics g,
                                  int x, int y) {

                if (classNumbers != null) {
                    iconSize = classNumbers.size() * widthPerClass;
                    int height = getHeight();
                    int xStart = x;
                    for (int i = 0; i < classNumbers.size(); i++) {
                        Integer classNumber = (Integer) classNumbers.get(i);
                        g.setColor(classVector.getColor(classNumber));
                        g.fillRect(xStart, y, widthPerClass, height);
                        xStart += widthPerClass;
                    }

                }
            }

            public int getIconHeight() {
                return labelRows ? FeatureRenderer.this.getHeight() : iconSize;
            }

            public int getIconWidth() {
                return labelRows ? iconSize : FeatureRenderer.this.getWidth();
            }
        }
    }

    public JMenuItem getOpenFeaturesMenuItem() {
        return openFeaturesMenuItem;
    }

    public JMenuItem getViewFeatureListsMenuItem() {
        return viewFeatureListsMenuItem;
    }

}
