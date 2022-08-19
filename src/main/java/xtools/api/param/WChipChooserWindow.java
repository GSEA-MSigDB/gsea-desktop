/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.actions.ext.BrowserAction;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import xapps.gsea.GseaWebResources;
import xtools.api.ui.FTPFileListCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

/**
 * @author Aravind Subramanian, David Eby
 */
public class WChipChooserWindow {
    private static final Logger klog = LoggerFactory.getLogger(WChipChooserWindow.class);

    public WChipChooserWindow() { }

    public String[] showDirectlyWithModels() {
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel wrapper = new JPanel(new BorderLayout()); // @note needed else the input widget comes up real small in the dd
        wrapper.add(tabbedPane, BorderLayout.CENTER);
        
        Action helpAction = JarResources.createHelpAction(Param.CHIP);
        Action infoAction = new BrowserAction("MSigDB Chips", "MSigDB Chips Info",
                GuiHelper.ICON_HELP16, GseaWebResources.getGseaChipInfoHelpURL());
        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor("Select a chip", wrapper, helpAction, infoAction, false);
        
        DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
        DefaultListModel<FTPFile> humanFtpFileModel = new DefaultListModel<FTPFile>();
        DefaultListModel<FTPFile> mouseFtpFileModel = new DefaultListModel<FTPFile>();
        final JList<FTPFile> humanFTPFileJList = new JList<FTPFile>(humanFtpFileModel);
        final JList<FTPFile> mouseFTPFileJList = new JList<FTPFile>(mouseFtpFileModel);
        if (! XPreferencesFactory.kOnlineMode.getBoolean()) {
            // TODO: switch away from JList. Prob disabled TextArea
            DefaultListModel<String> offlineMsgModel = createOfflineMessageModel();
            JList<String> offlineMsgList = new JList<String>(offlineMsgModel);
            offlineMsgList.setCellRenderer(defaultRenderer);
            offlineMsgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(offlineMsgList));
            tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(offlineMsgList));
        } else {
            FTPList ftpList = null;
            try {
                ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                        GseaWebResources.getGseaFTPServerUserName(), GseaWebResources.getGseaFTPServerPassword());
                try {
                    FTPFile[] humanFTPFiles = retrieveFTPFiles(ftpList, MSigDBSpecies.Human, GseaWebResources.getGseaFTPServerChipDir("Human"));
                    populateFTPModel(humanFTPFiles, humanFTPFileJList, humanFtpFileModel, new ComparatorFactory.FTPFileByVersionComparator());
                    tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(humanFTPFileJList));
                    desc.enableDoubleClickableJList(humanFTPFileJList);
                    FTPFile[] mouseFTPFiles = retrieveFTPFiles(ftpList, MSigDBSpecies.Mouse, GseaWebResources.getGseaFTPServerChipDir("Mouse"));
                    populateFTPModel(mouseFTPFiles, mouseFTPFileJList, mouseFtpFileModel, new ComparatorFactory.FTPFileByVersionComparator("Mouse"));
                    tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(mouseFTPFileJList));
                    desc.enableDoubleClickableJList(mouseFTPFileJList);
                } finally {
                    if (ftpList != null) { ftpList.quit(); }
                }
            } catch (Exception ex) {
                // TODO: switch away from JList. Prob disabled TextArea 
                DefaultListModel<String> errorMsgModel = createErrorMessageModel(ex);
                JList<String> errorMsgList = new JList<String>(errorMsgModel);
                errorMsgList.setCellRenderer(defaultRenderer);
                errorMsgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(errorMsgList));
                tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(errorMsgList));
            }
        }

        // TODO: strong typing, should be JList<Chip> (or POB) but need to verify and make changes elsewhere
        final JList localChipJList = new JList(ObjectBindery.getModel(Chip.class));
        localChipJList.setCellRenderer(defaultRenderer);
        localChipJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabbedPane.addTab("Local Chips", new JScrollPane(localChipJList));
        desc.enableDoubleClickableJList(localChipJList);

        BooleanSupplier validator = () -> {
            boolean haveHuman = !humanFTPFileJList.isSelectionEmpty();
            boolean noMouse = mouseFTPFileJList.isSelectionEmpty();
            if (haveHuman) { return noMouse && localChipJList.isSelectionEmpty(); }
            if (!noMouse) { return localChipJList.isSelectionEmpty(); }
            return true;
        };

        desc.setDisplayWider();
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            if (validator.getAsBoolean()) {
                klog.debug("Valid selection");
            } else {
                klog.debug("Invalid selection");
            }
            
            FTPFile selected = humanFTPFileJList.getSelectedValue();
            if (selected != null) { return new String[]{ selected.getPath() }; }

            selected = mouseFTPFileJList.getSelectedValue();
            if (selected != null) { return new String[]{ selected.getPath() }; }
            
            // TODO: always Chip/POB, or refactored to String
            Object selectedObj = localChipJList.getSelectedValue();
            if (selectedObj != null) { return new String[]{ ParserFactory.getCache().getSourcePath(selectedObj) }; }

            return new String[] {};
        }
    }

    private FTPFile[] retrieveFTPFiles(FTPList ftpList, MSigDBSpecies species, String ftpDir) throws Exception {
        String[] ftpFileNames = ftpList.getDirectoryListing(ftpDir, null);
        FTPFile[] ftpFiles = new FTPFile[ftpFileNames.length];
        for (int i = 0; i < ftpFileNames.length; i++) {
            String ftpFileName = ftpFileNames[i];
            String versionId = NamingConventions.extractVersionFromFileName(ftpFileName, ".chip");
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

    static class ChipFTPListRenderer extends DefaultListCellRenderer {
        private String highestVersionId;
        
        public ChipFTPListRenderer(String highestVersionId) {
            this.highestVersionId = highestVersionId;
        }
    
        public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    
            if (value != null && value instanceof FTPFile) {
                String name = ((FTPFile) value).getName();
                this.setText(name);
    
                if (!isSelected) {
                    setForeground(Color.BLACK);
                    setIcon(null);
                }
    
                if (StringUtils.containsIgnoreCase(name, highestVersionId)) {
                    Font font = this.getFont();
                    String fontName = font.getFontName();
                    int fontSize = font.getSize();
                    this.setFont(new Font(fontName, Font.BOLD, fontSize));
                }
            }
            
            return this;
        }
    }
}
