/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.uiutil;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FileChooser {
    static JFileChooser fileChooser;

    public static boolean RUNNING_ON_MAC = System.getProperty("mrj.version") != null
            && javax.swing.UIManager.getSystemLookAndFeelClassName()
            .equals(
                    javax.swing.UIManager.getLookAndFeel().getClass()
                            .getName());

    private FileChooser() {
    }

    private static File showFileDialog(Frame parent, int mode,
                                       File selectedFile, String title) {
        if (title == null) {
            title = "GenePattern";
        } else {
            title = "GenePattern - " + title;
        }
        FileDialog fc = new FileDialog(parent, title, mode);
        if (selectedFile != null) {
            fc.setDirectory(selectedFile.getPath());
            fc.setFile(selectedFile.getName());
        }
        fc.setModal(true);
        fc.setVisible(true);
        String f = fc.getFile();
        String directory = fc.getDirectory();
        if (f != null) {
            File file = new File(directory, f);
            return file; // mac os x file chooser asks chooser whether to
            // replace file
        }
        return null;
    }

    public static File showOpenDialog(Frame parent, String title) {
        if (RUNNING_ON_MAC) {
            return showFileDialog(parent, FileDialog.LOAD, null, title);
        } else {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }
            fileChooser.setDialogTitle(title);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile();
            }
            return null;
        }
    }

    public static File showSaveDialog(Frame parent) {
        return showSaveDialog(parent, null);
    }

    public static File showSaveDialog(Frame parent, File selectedFile) {
        if (RUNNING_ON_MAC) {
            return showFileDialog(parent, FileDialog.SAVE, selectedFile,
                    "GenePattern");
        } else {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }
            fileChooser.setSelectedFile(selectedFile);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                final File outputFile = fileChooser.getSelectedFile();
                if (!overwriteFile(parent, outputFile)) {
                    return null;
                } else {
                    return outputFile;
                }
            }
            return null;
        }
    }

    public static boolean overwriteFile(Component parent, File f) {
        if (!f.exists()) {
            return true;
        }
        String message = "An item named "
                + f.getName()
                + " already exists in this location.\nDo you want to replace it with the one that you are saving?";
        if (JOptionPane.showOptionDialog(parent, message, null,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Replace", "Cancel"}, "Cancel") != JOptionPane.YES_OPTION) {
            return false;
        }
        return true;
    }
}
