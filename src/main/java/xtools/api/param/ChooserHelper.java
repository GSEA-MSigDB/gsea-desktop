/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import java.awt.Color;
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextArea;

import org.apache.commons.lang3.SystemUtils;
import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import xtools.api.ui.FTPFileListCellRenderer;

/**
 * @author Aravind Subramanian, David Eby
 */
public class ChooserHelper {
    public static FTPFile[] retrieveFTPFiles(FTPList ftpList, String extension, MSigDBSpecies species, String ftpDir) throws Exception {
        String[] ftpFileNames = ftpList.getDirectoryListing(ftpDir, null);
        FTPFile[] ftpFiles = new FTPFile[ftpFileNames.length];
        for (int i = 0; i < ftpFileNames.length; i++) {
            String ftpFileName = ftpFileNames[i];
            String versionId = NamingConventions.extractVersionFromFileName(ftpFileName, extension);
            ftpFiles[i] = new FTPFile(ftpList.host, ftpDir, ftpFileName, new MSigDBVersion(species, versionId));
        }
        return ftpFiles;
    }

    public static void populateFTPModel(FTPFile[] ftpFiles, JList<FTPFile> ftpFileList, DialogDescriptor desc,
            ComparatorFactory.FTPFileByVersionComparator ftpFileComp, int selectionModel) {
        Arrays.parallelSort(ftpFiles, ftpFileComp);
        DefaultListModel<FTPFile> ftpFileModel = new DefaultListModel<FTPFile>();
        for (int i = 0; i < ftpFiles.length; i++) {
            ftpFileModel.addElement(ftpFiles[i]);
        }
        ftpFileList.setModel(ftpFileModel);
        ftpFileList.setCellRenderer(new FTPFileListCellRenderer(ftpFileComp.getHighestVersionId()));
        ftpFileList.setSelectionMode(selectionModel);
        desc.enableDoubleClickableJList(ftpFileList);
    }

    public static JTextArea createOfflineMessageDisplay() {
        String message = "Offline mode" + SystemUtils.LINE_SEPARATOR +
          "Change this in Menu=>Preferences" + SystemUtils.LINE_SEPARATOR +
          "Use 'Load Data' to access local files." + SystemUtils.LINE_SEPARATOR +
          "Choose gene sets from other tabs.";
        JTextArea offlineMsgDisplay = new JTextArea();
        offlineMsgDisplay.setText(message);
        offlineMsgDisplay.setEditable(false);
        offlineMsgDisplay.setBackground(Color.WHITE);
        return offlineMsgDisplay;
    }

    public static JTextArea createErrorMessageDisplay(Exception e) {
        String message = "Error listing Broad website" + SystemUtils.LINE_SEPARATOR +
          e.getMessage() + SystemUtils.LINE_SEPARATOR +
          "Use 'Load Data' to access local files." + SystemUtils.LINE_SEPARATOR +
          "Choose gene sets from other tabs.";
        JTextArea errorMsgDisplay = new JTextArea();
        errorMsgDisplay.setText(message);
        errorMsgDisplay.setEditable(false);
        errorMsgDisplay.setBackground(Color.WHITE);
        return errorMsgDisplay;
    }
}
