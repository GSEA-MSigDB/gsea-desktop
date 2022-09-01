/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.ui;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.Versioned;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.actions.ext.BrowserAction;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import xapps.gsea.GseaWebResources;

import org.apache.commons.lang3.StringUtils;
import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xtools.api.param.ChooserHelper;
import xtools.api.param.Param;
import xtools.api.param.Validator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetMatrixChooserWindow {
    private static final Logger klog = LoggerFactory.getLogger(GeneSetMatrixChooserWindow.class);

    public GeneSetMatrixChooserWindow() { }

    public String[] showDirectlyWithModels() {
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel wrapper = new JPanel(new BorderLayout()); // @note needed else the input widget comes up real small in the dd
        wrapper.add(tabbedPane, BorderLayout.CENTER);

        Action helpAction = JarResources.createHelpAction(Param.GMX);
        Action infoAction = new BrowserAction("MSigDB Collections", "MSigDB Collections Info",
                GuiHelper.ICON_HELP16, GseaWebResources.getGseaBaseURL() + "/msigdb/");
        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor("Select a gene set", wrapper, helpAction, infoAction, true);

        final JList<FTPFile> humanFTPFileJList = new JList<FTPFile>();
        final JList<FTPFile> mouseFTPFileJList = new JList<FTPFile>();
        if (XPreferencesFactory.kOnlineMode.getBoolean()) {
            FTPList ftpList = null;
            try {
                ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                        GseaWebResources.getGseaFTPServerUserName(), GseaWebResources.getGseaFTPServerPassword());
                try {
                    FTPFile[] humanFTPFiles = ChooserHelper.retrieveFTPFiles(ftpList, ".symbols.gmt",
                            MSigDBSpecies.Human, GseaWebResources.getGseaFTPServerGeneSetsDir(MSigDBSpecies.Human));
                    FTPFile[] mouseFTPFiles = ChooserHelper.retrieveFTPFiles(ftpList, ".symbols.gmt",
                            MSigDBSpecies.Mouse, GseaWebResources.getGseaFTPServerGeneSetsDir(MSigDBSpecies.Mouse));
                    ChooserHelper.populateFTPModel(humanFTPFiles, humanFTPFileJList, desc,
                            new ComparatorFactory.FTPFileByVersionComparator("h"), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    ChooserHelper.populateFTPModel(mouseFTPFiles, mouseFTPFileJList, desc,
                            new ComparatorFactory.FTPFileByVersionComparator("mh"), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    tabbedPane.addTab("Human Collection (MSigDB)", new JScrollPane(humanFTPFileJList));
                    tabbedPane.addTab("Mouse Collection (MSigDB)", new JScrollPane(mouseFTPFileJList));
                } finally {
                    if (ftpList != null) { ftpList.quit(); }
                }
            } catch (Exception ex) {
                klog.error(ex.getMessage(), ex);
                tabbedPane.addTab("Human Collection (MSigDB)", new JScrollPane(ChooserHelper.createErrorMessageDisplay(ex)));
                tabbedPane.addTab("Mouse Collection (MSigDB)", new JScrollPane(ChooserHelper.createErrorMessageDisplay(ex)));
            }
        } else {
            tabbedPane.addTab("Human Collection (MSigDB)", new JScrollPane(ChooserHelper.createOfflineMessageDisplay()));
            tabbedPane.addTab("Mouse Collection (MSigDB)", new JScrollPane(ChooserHelper.createOfflineMessageDisplay()));
        }

        // TODO: strong typing, should be JList<GeneSetMatrix> (or POB) but need to verify and make changes elsewhere
        // Likewise for the next two.
        DefaultListCellRenderer defaultRenderer = new GeneSetMatrixChooserWindow.NonFTPGeneSetsRenderer();
        final JList<GeneSetMatrix> localGeneSetMatrixJList = new JList<GeneSetMatrix>(ObjectBindery.getModel(GeneSetMatrix.class));
        localGeneSetMatrixJList.setCellRenderer(defaultRenderer);
        localGeneSetMatrixJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabbedPane.addTab("Local GMX/GMT", new JScrollPane(localGeneSetMatrixJList));
        desc.enableDoubleClickableJList(localGeneSetMatrixJList);

        final JList<GeneSet> localGeneSetJList = new JList<GeneSet>(ObjectBindery.getModel(GeneSet.class));
        localGeneSetJList.setCellRenderer(defaultRenderer);
        localGeneSetJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabbedPane.addTab("Local GRP Gene sets", new JScrollPane(localGeneSetJList));
        desc.enableDoubleClickableJList(localGeneSetJList);
        
        final JList<GeneSet> localMatrixSubsetsJList = new JList<GeneSet>(ObjectBindery.getHackAuxGeneSetsBoxModel());
        localMatrixSubsetsJList.setCellRenderer(defaultRenderer);
        localMatrixSubsetsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabbedPane.addTab("Subsets", new JScrollPane(localMatrixSubsetsJList));
        desc.enableDoubleClickableJList(localMatrixSubsetsJList);
        
        JTextArea taGenes = new JTextArea();
        taGenes.setText(""); // @note
        taGenes.setBorder(BorderFactory.createTitledBorder("Make an 'on-the-fly' gene set: Enter features below, one per line"));
        tabbedPane.addTab("Text entry", new JScrollPane(taGenes));
        
        BooleanSupplier warningChecker = () -> {
            List<Versioned> selectedItems = new ArrayList<Versioned>(humanFTPFileJList.getSelectedValuesList());
            selectedItems.addAll(mouseFTPFileJList.getSelectedValuesList());
            selectedItems.addAll(localGeneSetMatrixJList.getSelectedValuesList());
            selectedItems.addAll(localGeneSetJList.getSelectedValuesList());
            selectedItems.addAll(localMatrixSubsetsJList.getSelectedValuesList());
            if (selectedItems.isEmpty()) { return true; }

            MSigDBVersion first = selectedItems.remove(0).getMSigDBVersion();

            // If *all* versions found are unknown then we give no warning.  OTF is considered unknown.
            // We also give a warning if any of the recognized versions don't match.
            boolean allUnknown = first.isUnknownVersion();
            boolean allKnown = !first.isUnknownVersion();
            allUnknown &= StringUtils.isNotBlank(taGenes.getText());
            allKnown &= StringUtils.isBlank(taGenes.getText());

            for (Versioned item : selectedItems) {
                MSigDBVersion currVer = item.getMSigDBVersion();
                if (!currVer.isUnknownVersion()) {
                    if (!first.equals(currVer)) { return false; }
                    allUnknown = false;
                    allKnown &= true;
                } else {
                    allUnknown &= true;
                    allKnown = false;
                }
            }

            // The check passes at this point if either all are unknown or all are known
            return allUnknown || allKnown;
        };
        Supplier<Errors> warningMsgBuilder = () -> {
            Errors warnings = new Errors("Mixed MSigDB versions detected");
            warnings.add("Selecting collections from multiple MSigDB versions");
            warnings.add("may result in omitted genes and is not recommended.\n");
            warnings.add("NOTE: another tab may have a selection.");
            warnings.add(ChooserHelper.DESELECT_INSTRUCTIONS);
            warnings.add("\nClick Cancel to change the selection or OK to keep it.");
            return warnings;
        };
        Validator warningValidator = new Validator(warningChecker, warningMsgBuilder);
        desc.setWarningValidator(warningValidator);
        
        BooleanSupplier errorChecker = () -> {
            List<Versioned> selectedItems = new ArrayList<Versioned>(humanFTPFileJList.getSelectedValuesList());
            selectedItems.addAll(mouseFTPFileJList.getSelectedValuesList());
            selectedItems.addAll(localGeneSetMatrixJList.getSelectedValuesList());
            selectedItems.addAll(localGeneSetJList.getSelectedValuesList());
            selectedItems.addAll(localMatrixSubsetsJList.getSelectedValuesList());
            
            // We ignore all items with Unknown species for the purpose of this check.
            // Note that this is also why we don't bother with the on-the-fly gene set.
            selectedItems.removeIf(new Predicate<Versioned>() {
                public boolean test(Versioned item) { return item.getMSigDBVersion().isUnknownVersion(); }
            });
            if (selectedItems.isEmpty()) { return true; }

            MSigDBVersion first = selectedItems.remove(0).getMSigDBVersion();
            for (Versioned item : selectedItems) {
                if (first.getMsigDBSpecies() != item.getMSigDBVersion().getMsigDBSpecies()) { return false; }
            }
            return true;
        };
        Supplier<Errors> errorMsgBuilder = () -> {
            Errors errors = new Errors("Multiple species selected");
            errors.add("Multiple species selections are not allowed.\n");
            errors.add("Is there a selection on another tab?");
            errors.add(ChooserHelper.DESELECT_INSTRUCTIONS);
            return errors;
        };
        Validator errorValidator = new Validator(errorChecker, errorMsgBuilder);
        desc.setErrorValidator(errorValidator);

        desc.setDisplayWider();
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            List<String> allValues = new ArrayList<String>();
            for (FTPFile ftpFile : humanFTPFileJList.getSelectedValuesList()) {
                allValues.add(ftpFile.getPath());
            }
            for (FTPFile ftpFile : mouseFTPFileJList.getSelectedValuesList()) { 
                allValues.add(ftpFile.getPath());
            }
            for (GeneSetMatrix geneSetMatrix : localGeneSetMatrixJList.getSelectedValuesList()) {
                allValues.add(ParserFactory.getCache().getSourcePath(geneSetMatrix));
            }
            for (GeneSet geneSet : localGeneSetJList.getSelectedValuesList()) {
                allValues.add(ParserFactory.getCache().getSourcePath(geneSet));
            }
            for (GeneSet geneSet : localMatrixSubsetsJList.getSelectedValuesList()) {
                allValues.add(ParserFactory.getCache().getSourcePath(geneSet));
            }

            // add text area stuff as a GRP gene set
            String onTheFlyText = taGenes.getText();
            if (StringUtils.isNotBlank(onTheFlyText)) {
                String[] onTheFlyGenes = ParseUtils.string2strings(onTheFlyText, "\t\n"); // we want things synched
                if (onTheFlyGenes.length != 0) {
                    GeneSet gset = new GeneSet("from_text_entry_", onTheFlyGenes);
                    try {
                        ParserFactory.save(gset, File.createTempFile(gset.getName(), ".grp"));
                        allValues.add(ParserFactory.getCache().getSourcePath(gset));
                    } catch (Throwable t) {
                        klog.error(t.getMessage(), t);
                    }
                }
            }
            return allValues.toArray(new String[allValues.size()]);
        }
    }

    public static class NonFTPGeneSetsRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    
            if (value instanceof PersistentObject) {
                PersistentObject pob = (PersistentObject) value;
    
                if (pob.getQuickInfo() != null) {
                    StringBuffer buf = new StringBuffer("<html><body>").append(pob.getName());
                    buf.append("<font color=#666666> [").append(pob.getQuickInfo()).append(']').append("</font></html></body>");
                    this.setText(buf.toString());
                } else {
                    this.setText(pob.getName());
                }
    
                if (ParserFactory.getCache().isCached(pob)) {
                    File f = ParserFactory.getCache().getSourceFile(pob);
                    this.setToolTipText(f.getAbsolutePath());
                } else {
                    this.setToolTipText("Unknown origins of file");
                }
            } else if (value instanceof File) {
                this.setText(((File) value).getName());
                this.setIcon(DataFormat.getIcon(value));
                this.setToolTipText(((File) value).getAbsolutePath());
            }
    
            return this;
        }
    }
}
