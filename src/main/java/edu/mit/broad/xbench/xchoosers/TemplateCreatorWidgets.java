/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.xchoosers;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.TemplateImplFromSampleNames;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.searchers.GeneSearchList;

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;

import au.com.pegasustech.demos.layout.SRLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class TemplateCreatorWidgets {
    public static class OnTheFlyFromSampleNames extends JPanel {
        private JTextArea taClassA;
        private JTextField tfClassA;
        private JTextArea taClassB;
        private JTextField tfClassB;
        private JComboBox cbDataset;
        private Template createdTemplate;

        public OnTheFlyFromSampleNames() {
            JPanel pan = new JPanel(new SRLayout(2, 15));

            JPanel sub_a = new JPanel(new BorderLayout());
            sub_a.add(new JLabel("Class A (sample names must match the dataset)"), BorderLayout.NORTH);
            taClassA = new JTextArea(20, 10);
            taClassA.setBorder(BorderFactory.createTitledBorder("Samples for class A (one per line)"));
            sub_a.add(new JScrollPane(taClassA), BorderLayout.CENTER);
            tfClassA = new JTextField("ClassA", 20);
            tfClassA.setBorder(BorderFactory.createTitledBorder("Enter a brief name for class A"));
            sub_a.add(tfClassA, BorderLayout.SOUTH);

            taClassB = new JTextArea(20, 10);
            taClassB.setBorder(BorderFactory.createTitledBorder("Samples for class B (one per line)"));
            JPanel sub_b = new JPanel(new BorderLayout());
            sub_b.add(new JLabel("Class B (and sample names cant be reused)"), BorderLayout.NORTH);
            sub_b.add(new JScrollPane(taClassB), BorderLayout.CENTER);
            tfClassB = new JTextField("ClassB", 20);
            tfClassB.setBorder(BorderFactory.createTitledBorder("Enter a brief name for class B"));
            sub_b.add(tfClassB, BorderLayout.SOUTH);

            pan.add(sub_a);
            pan.add(sub_b);

            this.setLayout(new BorderLayout());
            this.add(pan, BorderLayout.CENTER);

            JPanel val = new JPanel(new BorderLayout(10, 10));
            JButton bValidate = new JButton("Apply to dataset");
            bValidate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    createTemplate();
                }
            });
            val.add(bValidate, BorderLayout.EAST);

            cbDataset = new JComboBox();
            cbDataset.setBorder(BorderFactory.createTitledBorder("Dataset"));
            cbDataset.setEditable(false);
            ObjectBindery.bind(cbDataset, new Class[]{Dataset.class});
            GuiHelper.safeSelect(cbDataset);
            val.add(cbDataset, BorderLayout.CENTER);

            this.add(val, BorderLayout.SOUTH);
        }

        private void createTemplate() {
            try {
                // Preliminary checks
                if (taClassA.getText().trim().length() == 0) {
                    Application.getWindowManager().showMessage("No sample names specified in class A");
                    return;
                }

                if (taClassB.getText().trim().length() == 0) {
                    Application.getWindowManager().showMessage("No sample names specified in class B");
                    return;
                }

                if (tfClassA.getText().trim().length() == 0) {
                    Application.getWindowManager().showMessage("Invalid (empty) name specified for class A");
                    return;
                }

                if (tfClassB.getText().trim().length() == 0) {
                    Application.getWindowManager().showMessage("Invalid (empty) name specified for class B");
                    return;
                }

                Object obj = cbDataset.getSelectedItem();
                if (obj == null) {
                    Application.getWindowManager().showMessage("No dataset available. First import a dataset and then apply this phenotype");
                    return;
                }

                // OK, lets go for it

                File out = Application.getVdbManager().getDefaultOutputDir();
                String classAName = tfClassA.getText();
                String classBName = tfClassB.getText();

                String tn = classAName + "_vs_" + classBName + ".cls";
                File file = NamingConventions.createSafeFile(out, tn);

                // spaces allowed in sample names
                final String[] classASampleNames = ParseUtils.string2strings(taClassA.getText(), "\n\t");
                final String[] classBSampleNames = ParseUtils.string2strings(taClassB.getText(), "\n\t");

                TemplateImplFromSampleNames tsn = new TemplateImplFromSampleNames(tn, classAName, classASampleNames, classBName, classBSampleNames);

                // @note IMP must save the tsn (as it has sample names) to the dataset
                this.createdTemplate = tsn.createTemplate((Dataset) obj);
                //ParserFactory.save(createdTemplate, file); <- dont this removes the sample names
                ParserFactory.save(tsn, file);

                Application.getWindowManager().showMessage("Successfully made template: " + createdTemplate.getName());

            } catch (Throwable t) {
                Application.getWindowManager().showError("Trouble making template", t);
            }
        }

    } // End class OnTheFlyFromSampleNames


    /**
     * @author Aravind Subramanian
     */
    public static class GenePhenotype extends JPanel {
        private GeneSearchList geneSearch;
        private JComboBox<Dataset> cbDataset;
        private Template createdTemplate;

        public GenePhenotype() {
            this.geneSearch = new GeneSearchList();
            this.setLayout(new BorderLayout(10, 10));
            this.add(geneSearch.getComponent(), BorderLayout.CENTER);

            JPanel val = new JPanel(new BorderLayout());
            JButton bValidate = new JButton("Apply to dataset");
            bValidate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    createTemplate();
                }
            });
            val.add(bValidate, BorderLayout.EAST);

            cbDataset = new JComboBox<>();
            cbDataset.setBorder(BorderFactory.createTitledBorder("Dataset"));
            GuiHelper.safeSelect(cbDataset);
            cbDataset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Dataset ds = cbDataset.getModel().getElementAt(cbDataset.getSelectedIndex());
                    if (ds != null) {
                        geneSearch.setFeatures(ds.getRowNames());
                    }
                }
            });
            cbDataset.setEditable(false);
            ObjectBindery.bind(cbDataset, new Class[]{Dataset.class});
            val.add(cbDataset, BorderLayout.CENTER);

            this.add(val, BorderLayout.SOUTH);
        }

        private void createTemplate() {
            try {
                // Preliminary checks
                String selectedItem = geneSearch.getChosenFeature();
                if (StringUtils.isBlank(selectedItem)) {
                    Application.getWindowManager().showMessage("First select a gene, then apply");
                    return;
                }
                if (!geneSearch.isChosenFeatureInList()) {
                    Application.getWindowManager().showMessage("Chosen gene is not in the feature list");
                    return;
                }

                Dataset ds = cbDataset.getModel().getElementAt(cbDataset.getSelectedIndex());
                if (ds == null) {
                    Application.getWindowManager().showMessage("No dataset available. First import a dataset and then apply this phenotype");
                    return;
                }

                // OK, go for it
                File out = Application.getVdbManager().getDefaultOutputDir();
                String geneName = selectedItem.toString();
                String tn = geneName + "_profile_in_" + ds.getName() + ".cls";
                File file = NamingConventions.createSafeFile(out, tn);

                this.createdTemplate = TemplateFactory.createContinuousTemplate(geneName, ds);
                ParserFactory.save(createdTemplate, file);

                Application.getWindowManager().showMessage("Successfully made template: " + createdTemplate.getName());
            } catch (Throwable t) {
                Application.getWindowManager().showError("Trouble making template", t);
            }
        }
    }
}
