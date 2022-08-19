/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.ui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.charts.XChart;
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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xtools.api.param.Param;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetMatrixChooserWindow {
    private static final Logger klog = LoggerFactory.getLogger(GeneSetMatrixChooserWindow.class);
    private JList[] jlGenes;
    private final JTextArea taGenes = new JTextArea();
    private NamedModel[] fModels;

    public GeneSetMatrixChooserWindow() { }

    public Object[] showDirectlyWithModels() {
        ListCellRenderer defaultRenderer = new GeneSetMatrixChooserWindow.NonFTPGeneSetsRenderer();
        NamedModel humanModel = new NamedModel("Human Collections (MSigDB)", null, defaultRenderer);
        NamedModel mouseModel = new NamedModel("Mouse Collections (MSigDB)", null, defaultRenderer);
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            DefaultListModel<String> offlineMsgModel = createOfflineMessageModel();
            humanModel.model = offlineMsgModel;
            mouseModel.model = offlineMsgModel;
        } else {
            FTPList ftpList = null;
            try {
                ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                        GseaWebResources.getGseaFTPServerUserName(), GseaWebResources.getGseaFTPServerPassword());
                try {
                    populateFTPModel(humanModel, ftpList, GseaWebResources.getGseaFTPServerGeneSetsDir("Human"),
                            new GeneSetMatrixChooserWindow.GeneSetsFromFTPSiteComparator());
                    populateFTPModel(mouseModel, ftpList, GseaWebResources.getGseaFTPServerGeneSetsDir("Mouse"),
                            new GeneSetMatrixChooserWindow.GeneSetsFromFTPSiteComparator());
//                    populateFTPModel(humanModel, ftpList, GseaWebResources.getGseaFTPServerGeneSetsDir("Human"),
//                            new GeneSetMatrixChooserWindow.GeneSetsFromFTPSiteComparator());
//                    populateFTPModel(mouseModel, ftpList, GseaWebResources.getGseaFTPServerGeneSetsDir("Mouse"),
//                            new GeneSetMatrixChooserWindow.GeneSetsFromFTPSiteComparator());
                } finally {
                    if (ftpList != null) { ftpList.quit(); }
                }
            } catch (Exception ex) {
                DefaultListModel<String> errorMsgModel = createErrorMessageModel(ex);
                humanModel.model = errorMsgModel;
                mouseModel.model = errorMsgModel;
            }
        }
        this.fModels = new NamedModel[] { humanModel, mouseModel, 
                new NamedModel("Local GMX/GMT", ObjectBindery.getModel(GeneSetMatrix.class), defaultRenderer),
                new NamedModel("Local GRP Gene sets", ObjectBindery.getModel(GeneSet.class), defaultRenderer),
                new NamedModel("Subsets", ObjectBindery.getHackAuxGeneSetsBoxModel(), defaultRenderer)
        };
        
        // carefull with rebuild / reset the model here -> that ruins the selection policy
        if (jlGenes == null) {
            jlGenes = new JList[fModels.length];
            for (int i = 0; i < fModels.length; i++) {
                jlGenes[i] = new JList();
                if (fModels[i].renderer != null) { jlGenes[i].setCellRenderer(fModels[i].renderer); }
            }
        }

        for (int i = 0; i < fModels.length; i++) {
            jlGenes[i].setModel(fModels[i].model);
            jlGenes[i].setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }

        return _just_show();
    }

    private JTabbedPane tab;

    // just the showing part, abs no setting data
    private Object[] _just_show() {
        String text = "Select a gene set";
        taGenes.setText(""); // @note
        taGenes.setBorder(BorderFactory.createTitledBorder("Make an 'on-the-fly' gene set: Enter features below, one per line"));

        if (tab == null) {
            tab = new JTabbedPane();
            for (int i = 0; i < jlGenes.length; i++) {
                tab.addTab(fModels[i].name, new JScrollPane(jlGenes[i]));
            }

            tab.addTab("Text entry", new JScrollPane(taGenes));
        }

        JPanel dummy = new JPanel(new BorderLayout()); // @note needed else the input widget comes up real small in the dd
        dummy.add(tab, BorderLayout.CENTER);

        Action helpAction = JarResources.createHelpAction(Param.GMX);
        Action infoAction = new BrowserAction("MSigDB Collections", "MSigDB Collections Info",
                GuiHelper.ICON_HELP16, GseaWebResources.getGseaBaseURL() + "/msigdb/");
        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor(text, dummy, helpAction, infoAction, true);
        for (int i = 0; i < jlGenes.length; i++) {
            desc.enableDoubleClickableJList(jlGenes[i]);
        }
        desc.setDisplayWider();
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            java.util.List allValues = new ArrayList();

            for (int j = 0; j < jlGenes.length; j++) {
                Object[] sels = jlGenes[j].getSelectedValues();
                if (sels != null) {
                    for (int i = 0; i < sels.length; i++) {
                        if (sels[i] != null) {
                            allValues.add(sels[i]);
                        }
                    }
                }
            }

            // add text area stuff as a GRP gene set
            String s = taGenes.getText();

            if (s != null) {
                String[] strs = ParseUtils.string2strings(s, "\t\n"); // we want things synched
                if (strs.length != 0) {
                    GeneSet gset = new GeneSet("from_text_entry_", strs);
                    try {
                        ParserFactory.save(gset, File.createTempFile(gset.getName(), ".grp"));
                    } catch (Throwable t) {
                        klog.error(t.getMessage(), t);
                    }
                    allValues.add(gset);
                }
            }
            return allValues.toArray(new Object[allValues.size()]);
        }
    }

    private void populateFTPModel(NamedModel namedModel, FTPList ftpList,
            String ftpDir, GeneSetMatrixChooserWindow.GeneSetsFromFTPSiteComparator geneSetDBComp) throws Exception {
        String[] ftpFileNames = ftpList.getDirectoryListing(ftpDir, geneSetDBComp);
        DefaultListModel fileModel = new DefaultListModel();
        for (int i = 0, length = ftpFileNames.length; i < length; i++) {
            fileModel.addElement(new FTPFile(ftpList.host, ftpDir, ftpFileNames[i], null));
        }
        namedModel.model = fileModel;
        namedModel.renderer = new GeneSetMatrixChooserWindow.GeneSetsFromFTPSiteRenderer(geneSetDBComp.getHighestVersionId());
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

    public static class GeneSetsFromFTPSiteComparator implements Comparator<String> {
        // Used to track highest version seen by this instance.  Use a fake lowest-possible version
        // for the initial comparison.
        private String highestVersionId;
        private DefaultArtifactVersion highestVersion = new DefaultArtifactVersion("v0.0");
    
        public String getHighestVersionId() { return highestVersionId; }
    
        /**
         * Compare method that yields the following ordering of gmt file names:
         *       versions will be listed in descending order
         *       within a given version, collection types (c1, c2, c3, c4, c5)
         *         will be listed in increasing order
         *       within a given version and collection type, subset types
         *         (e.g., all vs. cp) will be listed in lexicographic order
         */
        public int compare(String pn1, String pn2) {
            MSigDBFilenameParser p1 = new MSigDBFilenameParser(pn1);
            MSigDBFilenameParser p2 = new MSigDBFilenameParser(pn2);
    
            DefaultArtifactVersion version1 = new DefaultArtifactVersion(p1.getCanonicalVersionId());
            DefaultArtifactVersion version2 = new DefaultArtifactVersion(p2.getCanonicalVersionId());
    
            if (!version1.equals(version2)) {
                int compareTo = version2.compareTo(version1);
                if (compareTo < 0) {
                    if (highestVersion.compareTo(version1) < 0) {
                        highestVersion = version1;
                        highestVersionId = p1.getVersionId();
                    }
                } else {
                    if (highestVersion.compareTo(version2) < 0) {
                        highestVersion = version2;
                        highestVersionId = p2.getVersionId();
                    }
                }
                return compareTo;
            } else {
                if (highestVersion.compareTo(version1) < 0) {
                    // Doesn't matter which we use since they are equal
                    highestVersion = version1;
                    highestVersionId = p1.getVersionId();
                }
                int collectionNum1 = p1.getCollectionIdNum();
                int collectionNum2 = p2.getCollectionIdNum();
                if (collectionNum1 != collectionNum2) {
                    return collectionNum1 - collectionNum2;
                } else {
                    String subsetId1 = p1.getSubsetId();
                    String subsetId2 = p2.getSubsetId();
                    return subsetId1.compareTo(subsetId2);
                }
            }
        }
    
        public boolean equals(Object o2) { return false; }
    }

    /**
     * static nested class for parsing names of gmt files retrievable from
     * MSigDB
     */
    public static class MSigDBFilenameParser {
        static String MSIGDB_VERSION_V1 = "v1";
        static String MSIGDB_VERSION_V2 = "v2";
    
        private String versionId = null;
        private String subsetId = null;
        private int collectionIdNum = 0;
    
        public MSigDBFilenameParser(String filename) {
            if (filename == null) { throw new IllegalArgumentException("MSigDB Filename cannot be null"); }
    
            String collectionId = filename.substring(0,2);
            if (filename.startsWith("h.")) {
                collectionIdNum = 0;
            } else if (filename.startsWith("mh")) {
                collectionIdNum = 0;
            } else {
                collectionIdNum = Integer.parseInt(collectionId.substring(1));
            }
            versionId = filename.substring(filename.lastIndexOf("v"), filename.lastIndexOf(".symbols.gmt"));
    
            if (!versionId.equals(MSIGDB_VERSION_V1) && !versionId.equals(MSIGDB_VERSION_V2)) {
                subsetId = filename.substring(3, filename.lastIndexOf(versionId)-1);
            }
        }
    
        public String getVersionId() { return versionId; }
    
        public String getCanonicalVersionId() {
            if (versionId.equals(MSIGDB_VERSION_V1) || versionId.equals(MSIGDB_VERSION_V2)) { return versionId + ".0"; }
            return versionId;
        }
    
        public int getCollectionIdNum() {
            return collectionIdNum;
        }
    
        public String getSubsetId() {
            return subsetId;
        }
    }

    public static class NonFTPGeneSetsRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    
            // order is important
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
            } else if (value instanceof XChart) {
                this.setText(((XChart) value).getName());
                this.setIcon(XChart.ICON);
            }
    
            return this;
        }
    }

    public static class GeneSetsFromFTPSiteRenderer extends DefaultListCellRenderer {
        private final String highestVersionId;
    
        public GeneSetsFromFTPSiteRenderer(String highestVersionId) {
            this.highestVersionId = StringUtils.lowerCase(highestVersionId);
        }
    
        public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    
            if (value != null && value instanceof FTPFile) {
                String s = ((FTPFile) value).getName();
                final String slc = s.toLowerCase();
                if (slc.contains(highestVersionId)) {
                    Font font = this.getFont();
                    String fontName = font.getFontName();
                    int fontSize = font.getSize();
                    this.setFont(new Font(fontName, Font.BOLD, fontSize));
                }
                this.setText(s);
                this.setIcon(RendererFactory2.FTP_FILE_ICON);
            }
    
            return this;
        }
    }
}
