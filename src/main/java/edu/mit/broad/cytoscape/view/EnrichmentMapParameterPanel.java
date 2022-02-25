/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.cytoscape.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.mit.broad.cytoscape.CytoscapeCyrest;
import edu.mit.broad.cytoscape.EnrichmentMapParameters;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import xapps.api.vtools.ParamSetFormForAFew;
import xtools.munge.CollapseDataset;

//Panel used to access Enrichment map.  Can either access it through the Results table(bottom left of GSEA frame)
// or from the main protocol at the top left of GSEA frame(similar to the leading edge analysis)

// TODO: clean-up misc type safety issues on Swing components; refactor getExpressionFile() and fix file handling issues
public class EnrichmentMapParameterPanel extends JPanel {
    public static final String LAUNCH_MSG = "Please launch Cytoscape 3.3+ with the Enrichment Map plug-in before continuing.";

    private static final Logger klog = LoggerFactory.getLogger(EnrichmentMapParameterPanel.class);

    private DecimalFormat decFormat; // used in the formatted text fields

    // user specified cut offs
    private JFormattedTextField pvalueTextField;
    private JFormattedTextField qvalueTextField;
    private JFormattedTextField coeffecientTextField;
    private JFormattedTextField combinedConstantTextField;

    // chooser for selecting expression file
    private JFormattedTextField GCTFileName1TextField;
    private JFormattedTextField GCTFileName2TextField;

    // flags
    private JRadioButton overlap;
    private JRadioButton jaccard;
    private JRadioButton combined;
    private boolean similarityCutOffChanged = false;

    public static final ImageIcon similarityLogo = JarResources.getImageIcon("GSEA_similarityLogo.png");

    // used so that we can hide the combined constant when combined similarity metric is not selected
    private JPanel similarityPanel;
    private JLabel combinedCutoff;

    // Enrichment map parameters
    private EnrichmentMapParameters params;

    /*
     * constructor for creating window specific to one or two analysis - reached from clicking on the EM icon at top left of GSEA window.
     * Takes an array of datasets. (currenlty only handles two datasets at a time.
     */
    public EnrichmentMapParameterPanel(String[] datasets) {
        super();

        params = new EnrichmentMapParameters("", "");
        setLayout(new BorderLayout());
        JPanel top_panel = new JPanel();

        JPanel dataset_panel = createDatasetPanel(datasets);
        JPanel param_panel = createParametersPanel();
        JPanel button_panel = createBottomPanel();

        StringBuffer colStr = _createColStr();
        // increase frame for dataset data if there are more than one dataset.
        if (datasets.length > 1) top_panel.setLayout(new FormLayout(colStr.toString(), "175dlu,4dlu,260dlu,4dlu"));
        else top_panel.setLayout(new FormLayout(colStr.toString(), "100dlu,4dlu,260dlu,4dlu"));
        CellConstraints cc = new CellConstraints();

        top_panel.add(dataset_panel, cc.xywh(1, 1, 5, 1));
        top_panel.add(param_panel, cc.xy(3, 3));
        // add(button_panel, cc.xy(3, 5));
        add(top_panel, BorderLayout.NORTH);
        add(button_panel, BorderLayout.SOUTH);

        // add the file to the completed interface
        GCTFileName1TextField.setFont(new java.awt.Font("Dialog", 1, 10));
        GCTFileName1TextField.setText(params.getExpressionFilePath());
        GCTFileName1TextField.setToolTipText(params.getExpressionFilePath());

    }

    /**
     * Creates a collapsible panel that holds parameter inputs
     *
     * @return panel containing the parameter specification interface
     */
    private JPanel createParametersPanel() {
        JPanel panel = new JPanel();
        CellConstraints CC = new CellConstraints();

        panel.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.TitledBorder(new javax.swing.border.LineBorder(java.awt.Color.black), "Enrichment Map Parameters",
                        javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP,
                        new java.awt.Font("Dialog", java.awt.Font.BOLD, 12), java.awt.Color.black),
                getBorder()));
        addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                if ("border".equals(e.getPropertyName())) throw new RuntimeException();
            }
        });

        panel.setLayout(new FormLayout("4dlu, 4dlu, 65dlu, 4dlu, 30dlu, 90dlu, 4dlu, 30dlu, 4dlu, 4dlu,15dlu,80dlu,4dlu",
                "15dlu, 4dlu, 15dlu, 4dlu, 200dlu"));
        ((FormLayout) panel.getLayout()).setRowGroups(new int[][] { { 1, 3 } });

        // ---- label2 ----
        JLabel emptyLabel = new JLabel(" ");

        // pvalue cutoff input
        JLabel pvalueCutOffLabel = new JLabel("P-value Cutoff");
        pvalueTextField = new JFormattedTextField(decFormat);
        pvalueTextField.setColumns(3);
        pvalueTextField.addPropertyChangeListener("value", new EnrichmentMapParameterPanel.FormattedTextFieldAction());
        String pvalueCutOffTip = "Sets the p-value cutoff \n" + "only genesets with a p-value less than \n"
                + "the cutoff will be included.";
        pvalueTextField.setToolTipText(pvalueCutOffTip);
        pvalueCutOffLabel.setToolTipText(pvalueCutOffTip);
        pvalueTextField.setText(Double.toString(params.getPvalue()));
        pvalueTextField.setValue(params.getPvalue());

        JPanel pvalueCutOffPanel = new JPanel();
        pvalueCutOffPanel.setLayout(new BorderLayout());
        pvalueCutOffPanel.setToolTipText(pvalueCutOffTip);

        panel.add(pvalueCutOffLabel, CC.xy(3, 1));
        panel.add(pvalueTextField, CC.xy(5, 1));
        panel.add(emptyLabel, CC.xy(11, 1));

        // ---- label3 ----
        // qvalue cutoff input
        JLabel qvalueCutOffLabel = new JLabel("FDR Q-value Cutoff");
        qvalueTextField = new JFormattedTextField(decFormat);
        qvalueTextField.setColumns(3);
        qvalueTextField.addPropertyChangeListener("value", new EnrichmentMapParameterPanel.FormattedTextFieldAction());
        String qvalueCutOffTip = "Sets the FDR q-value cutoff \n" + "only genesets with a FDR q-value less than \n"
                + "the cutoff will be included.";
        qvalueTextField.setToolTipText(qvalueCutOffTip);
        qvalueCutOffLabel.setToolTipText(qvalueCutOffTip);
        qvalueTextField.setText(Double.toString(params.getQvalue()));
        qvalueTextField.setValue(params.getQvalue());

        JPanel qvalueCutOffPanel = new JPanel();
        qvalueCutOffPanel.setLayout(new BorderLayout());
        qvalueCutOffPanel.setToolTipText(qvalueCutOffTip);

        panel.add(qvalueCutOffLabel, CC.xy(3, 3));
        panel.add(qvalueTextField, CC.xy(5, 3));
        panel.add(emptyLabel, CC.xy(11, 3));

        // add a label to the similarity coeffecient section
        JLabel similarityCutoff = new JLabel("Similarity Cutoff:");

        // Coeffecient cutoff input

        ButtonGroup jaccardOrOverlap;

        jaccard = new JRadioButton("Jaccard Coefficient");
        jaccard.setActionCommand("jaccard");
        jaccard.setSelected(true);
        jaccard.setToolTipText("Jaccard Coefficient = [size of (A intersect B)] / [size of (A union B)]");
        overlap = new JRadioButton("Overlap Coefficient");
        overlap.setActionCommand("overlap");
        overlap.setToolTipText("Overlap Coefficient = [size of (A intersect B)] / [size of (minimum( A , B))]");
        combined = new JRadioButton("Jaccard+Overlap Combined");
        combined.setActionCommand("combined");
        combined.setToolTipText("Combined Constant = k; Combined Coefficient = (k * Overlap) + ((1-k) * Jaccard)");
        if (params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_JACCARD)) {
            jaccard.setSelected(true);
            overlap.setSelected(false);
            combined.setSelected(false);
        } else if (params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_OVERLAP)) {
            jaccard.setSelected(false);
            overlap.setSelected(true);
            combined.setSelected(false);
        } else if (params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_COMBINED)) {
            jaccard.setSelected(false);
            overlap.setSelected(false);
            combined.setSelected(true);
        }
        jaccardOrOverlap = new javax.swing.ButtonGroup();
        jaccardOrOverlap.add(jaccard);
        jaccardOrOverlap.add(overlap);
        jaccardOrOverlap.add(combined);

        jaccard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectJaccardOrOverlapActionPerformed(evt);
            }
        });

        overlap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectJaccardOrOverlapActionPerformed(evt);
            }
        });
        combined.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectJaccardOrOverlapActionPerformed(evt);
            }
        });

        coeffecientTextField = new JFormattedTextField(decFormat);
        coeffecientTextField.setColumns(3);
        coeffecientTextField.addPropertyChangeListener("value", new EnrichmentMapParameterPanel.FormattedTextFieldAction());
        String coeffecientCutOffTip = "Sets the Jaccard or Overlap coefficient cutoff \n"
                + "only edges with a Jaccard or Overlap coefficient less than \n" + "the cutoff will be added.";
        coeffecientTextField.setToolTipText(coeffecientCutOffTip);
        coeffecientTextField.setValue(params.getSimilarityCutOff());
        similarityCutOffChanged = false; // reset for new Panel after .setValue(...) wrongly changed it to "true"

        // Add a box to specify the constant used in created the combined value
        combinedCutoff = new JLabel("Combined Constant");
        combinedConstantTextField = new JFormattedTextField(decFormat);
        combinedConstantTextField.setColumns(3);
        combinedConstantTextField.addPropertyChangeListener("value", new FormattedTextFieldAction());
        combinedConstantTextField.setValue(0.5);
        combinedCutoff.setForeground(Color.GRAY);

        // create a similarityPanel that is a collapsible panel
        CollapsiblePanel collapsible_similarityPanel = new CollapsiblePanel("Advanced Options");

        JPanel similarityPanel = collapsible_similarityPanel.getContentPane();
        CellConstraints CC_sp = new CellConstraints();
        similarityPanel.setLayout(new FormLayout(
                /* "4dlu, 4dlu, 65dlu, 4dlu, 30dlu, 90dlu, 4dlu, 30dlu", */
                "1dlu, 1dlu, 30dlu,35dlu, 1dlu, 30dlu, 90dlu, 1dlu, 30dlu,120dlu", "15dlu, 15dlu,15dlu,15dlu,15dlu,5dlu,100dlu"));

        // ---- label4 ----
        similarityPanel.add(similarityCutoff, CC_sp.xywh(3, 1, 5, 1));

        // ---- radioButton1 ----
        similarityPanel.add(jaccard, CC_sp.xywh(4, 2, 4, 1));
        similarityPanel.add(coeffecientTextField, CC_sp.xywh(9, 2, 1, 3));

        // ---- radioButton2 ----
        similarityPanel.add(overlap, CC_sp.xywh(4, 3, 4, 1));

        // ---- radioButton3 ----
        similarityPanel.add(combined, CC_sp.xywh(4, 4, 4, 1));

        // ---- label5 ----
        // only make combined constant visible if combined is selected
        similarityPanel.add(combinedCutoff, CC_sp.xy(7, 5, CellConstraints.CENTER, CellConstraints.DEFAULT));
        similarityPanel.add(combinedConstantTextField, CC_sp.xy(9, 5));
        combinedConstantTextField.setEnabled(false);
        similarityPanel.add(new JLabel("", similarityLogo, JLabel.CENTER), CC_sp.xywh(2, 7, 9, 1));

        panel.add(collapsible_similarityPanel, CC.xywh(3, 5, 11, 1));

        collapsible_similarityPanel.setCollapsed(false);
        collapsible_similarityPanel.setCollapsed(true);

        return panel;
    }

    private void createExpression1FilePanel(PanelBuilder builder, CellConstraints cc, int rowcnt) {

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(1000, 60));
        panel.setLayout(new BorderLayout());

        // add GCT file
        JLabel GCTLabel = new JLabel("*Expression (Dataset 1):");
        GCTLabel.setToolTipText("File with gene expression values.\n" + "Format: gene <tab> description <tab> expression value <tab> ...");

        JButton selectGCTFileButton = new JButton();
        GCTFileName1TextField = new JFormattedTextField();
        GCTFileName1TextField.setColumns(15);
        GCTFileName1TextField.setText(params.getExpressionFilePath());

        selectGCTFileButton.setText("...");
        selectGCTFileButton.setMargin(new Insets(0, 0, 0, 0));
        selectGCTFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectGCTFileButtonActionPerformed(evt);
            }
        });

        JPanel GCTPanel = new JPanel();
        GCTPanel.setLayout(new BorderLayout());

        GCTPanel.add(GCTFileName1TextField, BorderLayout.CENTER);
        GCTPanel.add(selectGCTFileButton, BorderLayout.EAST);

        /*
         * if(this.popOutWindow) panel.add(createInstruction(), BorderLayout.CENTER);
         */
        builder.add(GCTLabel, cc.xy(1, rowcnt));
        builder.add(GCTPanel, cc.xy(3, rowcnt));

    }

    private void createExpression2FilePanel(PanelBuilder builder, CellConstraints cc, int rowcnt) {

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(1000, 60));
        panel.setLayout(new BorderLayout());

        // add GCT file
        JLabel GCTLabel = new JLabel("*Expression (Dataset 2):");
        GCTLabel.setToolTipText("File with gene expression values.\n" + "Format: gene <tab> description <tab> expression value <tab> ...");

        JButton selectGCTFile2Button = new JButton();
        GCTFileName2TextField = new JFormattedTextField();
        GCTFileName2TextField.setColumns(15);
        GCTFileName2TextField.setText(params.getExpression2FilePath());

        selectGCTFile2Button.setText("...");
        selectGCTFile2Button.setMargin(new Insets(0, 0, 0, 0));
        selectGCTFile2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectGCTFile2ButtonActionPerformed(evt);
            }
        });

        JPanel GCT2Panel = new JPanel();
        GCT2Panel.setLayout(new BorderLayout());

        GCT2Panel.add(GCTFileName2TextField, BorderLayout.CENTER);
        GCT2Panel.add(selectGCTFile2Button, BorderLayout.EAST);

        builder.add(GCTLabel, cc.xy(1, rowcnt));
        builder.add(GCT2Panel, cc.xy(3, rowcnt));

    }

    private JTextArea createInstruction() {
        // Create a label explaining when you would want to change the expression file setting
        String text = "*If you are using GSEAPreranked and you would like to see the expression values in the heat map instead of the ranks change this default setting.";
        JTextArea instruction = new JTextArea(text);
        instruction.setRows(2);
        instruction.setEditable(false);
        instruction.setLineWrap(true);
        instruction.setWrapStyleWord(true);
        instruction.setPreferredSize(new Dimension(600, 20));

        return instruction;
    }

    /**
     * jaccard or overlap radio button action listener
     *
     * @param evt
     */
    private void selectJaccardOrOverlapActionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getActionCommand().equalsIgnoreCase("jaccard")) {
            combinedCutoff.setForeground(Color.GRAY);
            combinedConstantTextField.setEnabled(false);
            params.setSimilarityMetric(EnrichmentMapParameters.SM_JACCARD);
            if (!similarityCutOffChanged) {
                params.setSimilarityCutOff(params.getDefaultJaccardCutOff());
                coeffecientTextField.setValue(params.getSimilarityCutOff());
                similarityCutOffChanged = false;
                ; // reset after .setValue(...) wrongly changed it to "true"
            }
        } else if (evt.getActionCommand().equalsIgnoreCase("overlap")) {
            combinedCutoff.setForeground(Color.GRAY);
            combinedConstantTextField.setEnabled(false);
            params.setSimilarityMetric(EnrichmentMapParameters.SM_OVERLAP);
            if (!similarityCutOffChanged) {
                params.setSimilarityCutOff(params.getDefaultOverlapCutOff());
                coeffecientTextField.setValue(params.getSimilarityCutOff());
                similarityCutOffChanged = false;
                ; // reset after .setValue(...) wrongly changed it to "true"
            }
        } else if (evt.getActionCommand().equalsIgnoreCase("combined")) {
            // make the combined constant visible
            combinedCutoff.setForeground(Color.BLACK);
            combinedConstantTextField.setEnabled(true);
            params.setSimilarityMetric(EnrichmentMapParameters.SM_COMBINED);
            if (!similarityCutOffChanged) {
                params.setSimilarityCutOff((params.getDefaultOverlapCutOff() * params.getCombinedConstant())
                        + ((1 - params.getCombinedConstant()) * params.getDefaultJaccardCutOff()));
                coeffecientTextField.setValue(params.getSimilarityCutOff());
                similarityCutOffChanged = false;
                ; // reset after .setValue(...) wrongly changed it to "true"
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Jaccard Radio Button action command");
        }
        similarityPanel.revalidate();
    }

    // gct/expression 1 file selector action listener
    private void selectGCTFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
        FileDialog fileDialog = Application.getFileManager().getEnrichmentMapFileDialog();
        fileDialog.setDirectory(params.getEdbdir());
        fileDialog.setVisible(true);
        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            File file = files[0];
            params.setExpressionFilePath(file.getAbsolutePath());
            GCTFileName1TextField.setText(file.getAbsolutePath());
            GCTFileName1TextField.setToolTipText(file.getAbsolutePath());
        }
    }

    // gct/expression 2 file selector action listener
    private void selectGCTFile2ButtonActionPerformed(java.awt.event.ActionEvent evt) {
        FileDialog fileDialog = Application.getFileManager().getEnrichmentMapFileDialog();
        fileDialog.setDirectory(params.getEdbdir());
        fileDialog.setVisible(true);
        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            File file = files[0];
            params.setExpression2FilePath(file.getAbsolutePath());
            GCTFileName2TextField.setText(file.getAbsolutePath());
            GCTFileName2TextField.setToolTipText(file.getAbsolutePath());
        }
    }

    /**
     * Utility method that creates a panel for buttons at the bottom of the Enrichment Map Panel
     *
     * @return a flow layout panel containing the build map and cancel buttons
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JButton importButton = new JButton();

        importButton.setText("Build Enrichment Map");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BuildActionPerformed(evt);
            }
        });
        importButton.setEnabled(true);

        panel.add(importButton);

        return panel;
    }

    private void BuildActionPerformed(java.awt.event.ActionEvent evt) {
        CytoscapeCyrest cyto = new CytoscapeCyrest(params);

        try {
            // make sure cytoscape rest is running and em is available
            if (!cyto.CytoscapeRestActive()) {
                klog.info(LAUNCH_MSG);
                Application.getWindowManager().showConfirm(LAUNCH_MSG);
            }

            if (cyto.CytoscapeRestActive() && cyto.CytoscapeRestCommandEM()) {
                if (cyto.createEM_get()) {
                    Application.getWindowManager().showMessage(
                            "An Enrichment map was successfully loaded and created in cytoscape.  Please navigate to cytoscape to view results");

                }
            }

        } catch (IOException e) {
            klog.error("Unable to communicate with cytoscape: {}", e.getMessage());
            klog.error(LAUNCH_MSG);
            Application.getWindowManager().showConfirm(LAUNCH_MSG);

        } catch (URISyntaxException e) {
            klog.error("Issue with cytoscape rest command: {}", e.getMessage());

        }
    }

    /**
     * Create a Panel to contain a dropdown box of the datasets
     */
    private JPanel createDatasetPanel(String[] datasets) {

        // sort datasets alphabetically
        Arrays.sort(datasets);
        int num_rows = datasets.length * 5;

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        StringBuffer colStr = _createColStr();
        StringBuffer rowStr = _createRowStr(num_rows + 2);

        PanelBuilder builder = ParamSetFormForAFew.createPanelBuilder(colStr, rowStr);
        CellConstraints cc = new CellConstraints();

        JLabel ds1Label = new JLabel("Dataset 1:");

        JComboBox ds1combobox = new JComboBox(datasets);

        ds1combobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JComboBox cb = (JComboBox) evt.getSource();

                String edb = (String) cb.getSelectedItem();
                params.setEdbdir(edb);

                // check to see if we can find rpt file and expression file
                String expFile = getExpressionFile(edb);

                if (expFile != null && !expFile.equalsIgnoreCase("")) {
                    params.setExpressionFilePath(expFile);
                    if (GCTFileName1TextField != null) GCTFileName1TextField.setText(expFile);
                }
            }
        });

        ds1combobox.setSelectedIndex(0);
        int rowcnt = 3;
        builder.addSeparator("Dataset 1", cc.xyw(1, 1, 4));
        builder.add(ds1Label, cc.xy(1, rowcnt));
        builder.add(ds1combobox, cc.xy(3, rowcnt));
        rowcnt += 2;
        // add expression file
        this.createExpression1FilePanel(builder, cc, rowcnt);
        rowcnt += 2;

        if (datasets.length > 1) {

            JLabel ds2Label = new JLabel("Dataset 2:");
            JComboBox ds2combobox = new JComboBox(datasets);

            ds2combobox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    JComboBox cb = (JComboBox) evt.getSource();
                    String edb = (String) cb.getSelectedItem();
                    params.setEdbdir2(edb);

                    // check to see if we can find rpt file and expression file
                    String expFile = getExpressionFile(edb);

                    if (expFile != null && !expFile.equalsIgnoreCase("")) {
                        params.setExpression2FilePath(expFile);
                        if (GCTFileName2TextField != null) GCTFileName2TextField.setText(expFile);
                    }

                }
            });

            ds2combobox.setSelectedIndex(1);
            builder.addSeparator("Dataset 2", cc.xyw(1, rowcnt, 4));
            rowcnt += 2;
            builder.add(ds2Label, cc.xy(1, rowcnt));
            builder.add(ds2combobox, cc.xy(3, rowcnt));
            rowcnt += 2;
            // add expression file
            this.createExpression2FilePanel(builder, cc, rowcnt);
            rowcnt += 2;
        }

        // Add the instruction panel
        builder.add(createInstruction(), cc.xy(3, rowcnt));

        return builder.getPanel();

    }

    /**
     * Handles setting for the text field parameters that are numbers. Makes sure that the numbers make sense.
     */
    private class FormattedTextFieldAction implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            JFormattedTextField source = (JFormattedTextField) e.getSource();

            String message = "The value you have entered is invalid.\n";
            boolean invalid = false;

            if (source == pvalueTextField) {
                Number value = (Number) pvalueTextField.getValue();
                if ((value != null) && (value.doubleValue() > 0.0) && (value.doubleValue() <= 1)) {
                    params.setPvalue(value.doubleValue());
                } else {
                    source.setValue(params.getPvalue());
                    message += "The pvalue cutoff must be greater than or equal 0 and less than or equal to 1.";
                    invalid = true;
                }
            } else if (source == qvalueTextField) {
                Number value = (Number) qvalueTextField.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 100.0)) {
                    params.setQvalue(value.doubleValue());
                } else {
                    source.setValue(params.getQvalue());
                    message += "The FDR q-value cutoff must be between 0 and 100.";
                    invalid = true;
                }
            } else if (source == coeffecientTextField) {
                Number value = (Number) coeffecientTextField.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 1.0)) {
                    params.setSimilarityCutOff(value.doubleValue());
                    similarityCutOffChanged = true;
                } else {
                    source.setValue(params.getSimilarityCutOff());
                    message += "The Overlap/Jaccard Coefficient cutoff must be between 0 and 1.";
                    invalid = true;
                }
            } else if (source == combinedConstantTextField) {
                Number value = (Number) combinedConstantTextField.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 1.0)) {
                    params.setCombinedConstant(value.doubleValue());

                    // if the similarity cutoff is equal to the default then updated it to reflect what it should be given the value of k
                    if (!similarityCutOffChanged && params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_COMBINED))
                        params.setSimilarityCutOff((params.getDefaultOverlapCutOff() * value.doubleValue())
                                + ((1 - value.doubleValue()) * params.getDefaultJaccardCutOff()));

                    // params.setCombinedConstantCutOffChanged(true);
                } else {
                    source.setValue(0.5);
                    message += "The combined Overlap/Jaccard Coefficient constant must be between 0 and 1.";
                    invalid = true;
                }
            }
            if (invalid) {
                // JOptionPane.showMessageDialog(this, message, "Parameter out of bounds", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /*
     * Given a GSEA results directory check to see if we can find an rpt file that lists all the parameters used for the analysis. If we can
     * find an rpt file, check for expression file
     */
    private String getExpressionFile(String edb) {

        String expressionFile = null;
        File edbdir = new File(edb);
        // check to see that this is the edb directory
        if (!edbdir.getName().equalsIgnoreCase("edb")) edbdir = new File(edb + System.getProperty("file.separator") + "edb");

        // check it is a directory
        if (edbdir.exists() && edbdir.isDirectory()) {
            // get parent directory
            File parentDir = edbdir.getParentFile();
            if (parentDir.exists() && parentDir.isDirectory()) {
                // check to see if there is an rpt file
                File[] files = parentDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".rpt");
                    }
                });
                // Can only use the rpt file if there is only one in the current directory
                if (files.length == 1) {
                    String rptFile = files[0].getAbsolutePath();
                    try {
                        // load the rpt file
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rptFile)));

                        // Create a hashmap to contain all the values in the rpt file.
                        HashMap<String, String> rpt = new HashMap<String, String>();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] tokens = line.split("\t");
                            // there should be two values on each line of the rpt file.
                            if (tokens.length == 2) rpt.put(tokens[0], tokens[1]);
                            else if (tokens.length == 3) rpt.put(tokens[0] + " " + tokens[1], tokens[2]);
                        }

                        // get the GCT or res property
                        // if the dataset was collapsed then use the rnk file.
                        String collapse = "";
                        if (rpt.containsKey("param collapse")) collapse = rpt.get("param collapse");

                        if (rpt.containsKey("param rnk")) expressionFile = (String) rpt.get("param rnk");
                        else if (rpt.containsKey("param res")) {

                            // check to see if the result file exists
                            String res = (String) rpt.get("param res");
                            if (!(new File(res).exists()))
                                Application.getWindowManager().showConfirm("Unable to find expression file: " + res);
                            if (collapse.equalsIgnoreCase("false") || collapse.equalsIgnoreCase("No_Collapse")) expressionFile = res;
                            // if the dataset is collapsed and the original analysis was done on the
                            // non-collpased data then we have to either:
                            // 1. try and collapse the expression data
                            // 2. use the rank files
                            //
                            else {
                                // java -Xmx512m xtools.munge.CollapseDataset -res res -chip chip -mode mode
                                // -rpt_label rpt_label -include_only_symbols true/false -out out -gui true/false
                                // call the collapse tool from code

                                // create the set of properties we need
                                Properties props = new Properties();
                                props.put("res", res);
                                String rpt_label = (rpt.containsKey("param rpt_label")) ? (String) rpt.get("param rpt_label") : "";
                                props.put("rpt_label", rpt_label);
                                String mode = (rpt.containsKey("param mode")) ? (String) rpt.get("param mode") : "";
                                props.put("mode", mode);
                                String chip = (rpt.containsKey("param chip")) ? (String) rpt.get("param chip") : "";
                                props.put("chip", chip);
                                String include = (rpt.containsKey("param include_only_symbols"))
                                        ? (String) rpt.get("param include_only_symbols")
                                        : "";
                                props.put("include_only_symbols", include);
                                String out = (rpt.containsKey("param out")) ? (String) rpt.get("param out") : "";
                                props.put("out", out);
                                props.put("gui", "false");

                                // only try and collapse the dataset if we have all the required parameters from the rpt file.
                                if (!rpt_label.equalsIgnoreCase("") && !mode.equalsIgnoreCase("") && !chip.equalsIgnoreCase("")
                                        && !include.equalsIgnoreCase("") && !out.equalsIgnoreCase("")) {
                                    CollapseDataset tool = new CollapseDataset(props, "");
                                    try {
                                        tool.execute();
                                        // the expression file is created and put into a new directory with a new timestamp
                                        // build the path to the collapsed data set.
                                        File report_dir = tool.getReport().getReportDir();
                                        File res_file = new File(res);
                                        String tempFile = res_file.getName();
                                        String simplename = "";

                                        String extendedName = ("Remap_only".equals(mode)) ? "_remapped_to_symbols.gct" : "_collapsed_to_symbols.gct";
                                        simplename = tempFile.replace(".gct", extendedName);

                                        expressionFile = report_dir + System.getProperty("file.separator") + simplename;
                                    } catch (Throwable t) {
                                        System.out.println(t.getMessage());
                                    }
                                }
                                // can't collapse the dataset - use the rank file from the original edb directory.
                                else {

                                    if (edbdir.exists() && edbdir.isDirectory()) {
                                        // check to see if there is an rpt file
                                        File[] edbfiles = edbdir.listFiles(new FilenameFilter() {
                                            public boolean accept(File dir, String name) {
                                                return name.toLowerCase().endsWith(".rnk");
                                            }
                                        });
                                        if (files.length == 1) expressionFile = files[0].getAbsolutePath();
                                    } else expressionFile = "Unable to get expresion File";
                                }
                            }
                        } else expressionFile = "Unable to get expresion File";
                    } catch (IOException e) {
                        System.out.println("Unable to open rpt file:" + rptFile);
                    }
                }
            }
        }
        return expressionFile;
    }

    // Methods needed by the FormLayout - trying to keep EM interfaces the same as the rest in GSEA
    private static StringBuffer _createColStr() {
        return new StringBuffer("120dlu,      4dlu,        380dlu,   4dlu,  10dlu"); // columns
    }

    private static StringBuffer _createRowStr(int num_params) {
        StringBuffer rowStr = new StringBuffer();
        rowStr.append("pref, 10dlu,"); // for the spacer
        for (int i = 0; i < num_params + 1; i++) { // +1 for the button
            rowStr.append("pref, 5dlu");
            if (num_params != i - 1) {
                rowStr.append(",");
            }
        }
        return rowStr;
    }
}
