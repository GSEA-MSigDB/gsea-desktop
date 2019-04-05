/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.annotation;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
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
import java.util.List;

/**
 * Annotates sets (lists) in a table
 *
 * @author jgould
 */
public class SetAnnotator {
    private SparseClassVector classVector = new SparseClassVector();

    private int classNumberCounter = 0;

    private final static Color[] colors = {Color.red, Color.yellow, Color.blue,
            Color.GREEN, Color.ORANGE, Color.magenta, Color.CYAN, Color.PINK,
            Color.GRAY};

    private boolean annotateRow = true;

    private SetAnnotatorModel model;

    private SampleClassEditor sampleClassEditor;

    private FeatureClassEditor featureClassEditor;

    private JMenuItem openFeaturesMenuItem;

    private JMenuItem viewFeatureListsMenuItem;

    private JTable table;

    private int widthPerClass = 6;

    public SparseClassVector getClassVector() {
        return classVector;
    }

    public SetAnnotator(final Frame parent, final SetAnnotatorModel model,
                        boolean _annotateRow) {
        this.model = model;
        this.annotateRow = _annotateRow;
        if (annotateRow) {
            viewFeatureListsMenuItem = new JMenuItem("Feature Annotations...");
        } else {
            viewFeatureListsMenuItem = new JMenuItem("Sample Annotations...");
        }
        viewFeatureListsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (annotateRow) {
                    if (featureClassEditor == null || !featureClassEditor.isShowing()) {
                        featureClassEditor = new FeatureClassEditor(parent, model, classVector);
                    }
                } else {
                    if (sampleClassEditor == null || !sampleClassEditor.isShowing()) {
                        sampleClassEditor = new SampleClassEditor(parent, model, classVector);
                    }
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

                String title = annotateRow ? "Select Feature List(s)" : "Select Cls File";
                File f = FileChooser.showOpenDialog(parent, title);
                if (f == null) return;
                
                String extension = FilenameUtils.getExtension(f.getName());
                try {
                    if (annotateRow) {
                        annotateRowsFromFile(parent, f, extension);
                    } else {
                        annotateSamplesFromFile(parent, model, f, extension);
                    }
                    if (table != null) {
                        table.invalidate();
                        table.validate();
                        table.repaint();
                    }
                } catch (Exception x) {
                    UIUtil.showErrorDialog(parent, "An error occurred while reading " + f.getName());
                }
            }

            private void annotateRowsFromFile(final Frame parent, File f, String extension) throws Exception {
                boolean missingFeatures = false;
                if (extension != null) {
                    extension = extension.toLowerCase();
                    if (extension.equals("gmt") || extension.equals("gmx")) {
                        GeneSetMatrix gmt = ParserFactory .readGeneSetMatrix(f, false);
        
                        int sets = gmt.getNumGeneSets();
                        for (int i = 0; i < sets; i++) {
                            GeneSet set = gmt.getGeneSet(i);
                            boolean result = addToFeatureList(set.getMembers(), set.getName());
                            missingFeatures |= result;
                        }
                    } else if (extension.equals("grp")) {
                        GeneSet gset = ParserFactory.readGeneSet(f, false);
                        missingFeatures = addToFeatureList(gset .getMembers(), f.getName());
                    } else {
                        List<String> featureList = VisualizerUtil.readFeatureList(parent, f.getCanonicalPath());
                        missingFeatures = addToFeatureList(featureList, f.getName());
                    }
                } else {
                    List<String> featureList = VisualizerUtil.readFeatureList(parent, f.getCanonicalPath());
                    missingFeatures = addToFeatureList(featureList, f.getName());
                }
                if (missingFeatures) {
                    UIUtil.showMessageDialog(parent,
                            "Warning: The file contains features that are not found in the data set.");
                }
            }

            private void annotateSamplesFromFile(final Frame parent, final SetAnnotatorModel model, File f, String extension)
                    throws Exception {
                ClassVector cv = VisualizerUtil.readCls(parent, f.getCanonicalPath());
                if (cv == null) {
                    return;
                }
                if (cv.size() != model.getFeatureCount()) {
                    UIUtil.showErrorDialog(parent,
                            "The number of samples in the cls file (" + cv.size()
                            + ") does not match the number of samples in the dataset ("
                            + model.getFeatureCount() + ").");
                    return;
                }
                List<Integer> group = new ArrayList<>();
                for (int j = 0; j < cv.getClassCount(); j++) {
                    int index = j + classNumberCounter;
                    group.add(index);
                    classVector.setClass(new Integer(index), cv.getClassName(j), getNextClassColor(index));
                }
                classVector.addClassGroup(group, f.getName());

                for (int j = 0; j < cv.size(); j++) {
                    classVector.addClass(j, cv.getAssignment(j) + classNumberCounter);
                }
                classVector.notifyListeners();
                classNumberCounter += cv.getClassCount();
            }
        });

        // Skip the menu if we're running without a parent Frame (e.g. reporting from the command-line 
        if (annotateRow && parent != null) {
            openFeaturesMenuItem.setAccelerator(KeyStroke.getKeyStroke('O',
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
    }

    private boolean addToFeatureList(List<String> featureList, String className) {

        boolean missingFeatures = false;
        for (String feature : featureList) {
            int index = model.getIndex(feature);
            if (index < 0) {
                missingFeatures = true;
            } else {
                classVector.addClass(index, classNumberCounter);
            }
        }

        classVector.setClass(classNumberCounter, className, getNextClassColor(classNumberCounter));
        classNumberCounter++;
        return missingFeatures;
    }

    private Color getNextClassColor(int index) {
        if (index >= colors.length) {
            return new Color((int) (Math.random() * 255),
                    (int) (Math.random() * 255),
                    (int) (Math.random() * 255));
        } else {
            return colors[classNumberCounter];
        }
    }

    public JMenuItem getOpenFeaturesMenuItem() {
        return openFeaturesMenuItem;
    }

    public JMenuItem getViewFeatureListsMenuItem() {
        return viewFeatureListsMenuItem;
    }
}