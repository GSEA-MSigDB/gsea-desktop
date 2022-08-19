/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.ui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.RendererFactory2;
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

import xtools.api.param.Param;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
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

        DefaultListModel<FTPFile> humanFtpFileModel = new DefaultListModel<FTPFile>();
        DefaultListModel<FTPFile> mouseFtpFileModel = new DefaultListModel<FTPFile>();
        final JList<FTPFile> humanFTPFileJList = new JList<FTPFile>(humanFtpFileModel);
        final JList<FTPFile> mouseFTPFileJList = new JList<FTPFile>(mouseFtpFileModel);
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            // TODO: switch away from JList. Prob disabled TextArea 
            DefaultListModel<String> offlineMsgModel = createOfflineMessageModel();
            JList<String> offlineMsgList = new JList<String>(offlineMsgModel);
            offlineMsgList.setCellRenderer(new DefaultListCellRenderer());
            offlineMsgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabbedPane.addTab("Human Collection (MSigDB)", new JScrollPane(offlineMsgList));
            tabbedPane.addTab("Mouse Collection (MSigDB)", new JScrollPane(offlineMsgList));
        } else {
            FTPList ftpList = null;
            try {
                ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                        GseaWebResources.getGseaFTPServerUserName(), GseaWebResources.getGseaFTPServerPassword());
                try {
                    FTPFile[] humanFTPFiles = retrieveFTPFiles(ftpList, MSigDBSpecies.Human, GseaWebResources.getGseaFTPServerGeneSetsDir("Human"));
                    populateFTPModel(humanFTPFiles, humanFTPFileJList, humanFtpFileModel, new ComparatorFactory.FTPFileByVersionComparator("h"));
                    tabbedPane.addTab("Human Collection (MSigDB)", new JScrollPane(humanFTPFileJList));
                    desc.enableDoubleClickableJList(humanFTPFileJList);
                    FTPFile[] mouseFTPFiles = retrieveFTPFiles(ftpList, MSigDBSpecies.Mouse, GseaWebResources.getGseaFTPServerGeneSetsDir("Mouse"));
                    populateFTPModel(mouseFTPFiles, mouseFTPFileJList, mouseFtpFileModel, new ComparatorFactory.FTPFileByVersionComparator("mh"));
                    tabbedPane.addTab("Mouse Collection (MSigDB)", new JScrollPane(mouseFTPFileJList));
                    desc.enableDoubleClickableJList(mouseFTPFileJList);
                } finally {
                    if (ftpList != null) { ftpList.quit(); }
                }
            } catch (Exception ex) {
                // TODO: switch away from JList. Prob disabled TextArea 
                DefaultListModel<String> errorMsgModel = createErrorMessageModel(ex);
                JList<String> errorMsgList = new JList<String>(errorMsgModel);
                errorMsgList.setCellRenderer(new DefaultListCellRenderer());
                errorMsgList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                tabbedPane.addTab("Human Collection (MSigDB)", new JScrollPane(errorMsgList));
                tabbedPane.addTab("Mouse Collection (MSigDB)", new JScrollPane(errorMsgList));
            }
        }

        // TODO: strong typing, should be JList<GeneSetMatrix> (or POB) but need to verify and make changes elsewhere
        // Likewise for the next two.
        DefaultListCellRenderer defaultRenderer = new GeneSetMatrixChooserWindow.NonFTPGeneSetsRenderer();
        final JList localGeneMatrixJList = new JList(ObjectBindery.getModel(GeneSetMatrix.class));
        localGeneMatrixJList.setCellRenderer(defaultRenderer);
        localGeneMatrixJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabbedPane.addTab("Local GMX/GMT", new JScrollPane(localGeneMatrixJList));
        desc.enableDoubleClickableJList(localGeneMatrixJList);

        final JList localGeneSetJList = new JList(ObjectBindery.getModel(GeneSet.class));
        localGeneSetJList.setCellRenderer(defaultRenderer);
        localGeneSetJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabbedPane.addTab("Local GRP Gene sets", new JScrollPane(localGeneSetJList));
        desc.enableDoubleClickableJList(localGeneSetJList);
        
        final JList localMatrixSubsetsJList = new JList(ObjectBindery.getHackAuxGeneSetsBoxModel());
        localMatrixSubsetsJList.setCellRenderer(defaultRenderer);
        localMatrixSubsetsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabbedPane.addTab("Subsets", new JScrollPane(localMatrixSubsetsJList));
        desc.enableDoubleClickableJList(localMatrixSubsetsJList);
        
        JTextArea taGenes = new JTextArea();
        taGenes.setText(""); // @note
        taGenes.setBorder(BorderFactory.createTitledBorder("Make an 'on-the-fly' gene set: Enter features below, one per line"));
        tabbedPane.addTab("Text entry", new JScrollPane(taGenes));

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
            for (Object thing : localGeneMatrixJList.getSelectedValuesList()) {
                allValues.add(formatUnknownListObject(thing));
            }
            for (Object thing : localGeneSetJList.getSelectedValuesList()) {
                allValues.add(formatUnknownListObject(thing));
            }
            for (Object thing : localMatrixSubsetsJList.getSelectedValuesList()) {
                allValues.add(formatUnknownListObject(thing));
            }

            // add text area stuff as a GRP gene set
            String onTheFlyText = taGenes.getText();
            if (onTheFlyText != null) {
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

    private String formatUnknownListObject(Object thing) {
        if (thing instanceof PersistentObject) {
            return ParserFactory.getCache().getSourcePath(thing);
        } else {
            return thing.toString();
        }
    }
    
    private FTPFile[] retrieveFTPFiles(FTPList ftpList, MSigDBSpecies species, String ftpDir) throws Exception {
        String[] ftpFileNames = ftpList.getDirectoryListing(ftpDir, null);
        FTPFile[] ftpFiles = new FTPFile[ftpFileNames.length];
        for (int i = 0; i < ftpFileNames.length; i++) {
            String ftpFileName = ftpFileNames[i];
            String versionId = NamingConventions.extractVersionFromFileName(ftpFileName, ".symbols.gmt");
            ftpFiles[i] = new FTPFile(ftpList.host, ftpDir, ftpFileName, new MSigDBVersion(species, versionId));
        }
        return ftpFiles;
    }

    private void populateFTPModel(FTPFile[] ftpFiles, JList<FTPFile> ftpFileList, DefaultListModel<FTPFile> ftpFileModel,
            ComparatorFactory.FTPFileByVersionComparator ftpFileComp) throws Exception {
        Arrays.parallelSort(ftpFiles, ftpFileComp);
        for (int i = 0; i < ftpFiles.length; i++) {
            ftpFileModel.addElement(ftpFiles[i]);
        }
        ftpFileList.setCellRenderer(new FTPFileListCellRenderer(ftpFileComp.getHighestVersionId()));
        ftpFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private DefaultListModel<String> createErrorMessageModel(Exception ex) {
        klog.error(ex.getMessage(), ex);
        DefaultListModel<String> model = new DefaultListModel<String>();
        model.addElement("Error listing Broad website");
        model.addElement(ex.getMessage());
        model.addElement("Use 'Load Data' to access local files.");
        model.addElement("Choose gene sets from other tabs.");
        return model;
    }

    private DefaultListModel<String> createOfflineMessageModel() {
        DefaultListModel<String> model = new DefaultListModel<String>();
        model.addElement("Offline mode");
        model.addElement("Change this in Menu=>Preferences");
        model.addElement("Use 'Load Data' to access local files.");
        model.addElement("Choose gene sets from other tabs");
        return model;
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
