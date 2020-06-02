/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.uiutil;

import java.awt.*;
import java.io.File;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.SystemUtils;

import xapps.gsea.GseaFileFilter;

public class FileChooser {
    private FileChooser() {
    }

    // TODO: Evaluate if this is unused.
    // Only caller is SetAnnotator, which seems to be dead code.
    public static File showOpenDialog(Frame parent, String title) {
        if (title == null) {
            title = "GSEA";
        }
        FileDialog fc = new FileDialog(parent, title, FileDialog.LOAD);
        fc.setVisible(true);
        File[] files = fc.getFiles();
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    // TODO: should inline this to the only caller.
    public static File showSaveDialog(Frame parent) {
        FileDialog fc = new FileDialog(parent, "Save as expression file", FileDialog.SAVE);
        // Filtering doesn't work on Windows.
        if (SystemUtils.IS_OS_WINDOWS) {
        	fc.setFile("*.gct;*.res;*.txt");
        } else {
        	fc.setFile("dataset.gct");
        }
        fc.setFilenameFilter(new GseaFileFilter(new String[] {"gct", "res", "txt"}, "Expression Files"));
        fc.setVisible(true);
        File[] files = fc.getFiles();
        if (files != null && files.length > 0) {
        	return files[0];
        }
        return null;
    }

    // TODO: check if needed for Windows
    // Both Linux and Mac already have a widget that prompts
    public static boolean overwriteFile(Component parent, File f) {
        if (!f.exists()) {
            return true;
        }
        String message = "An item named "
                + f.getName()
                + " already exists in this location.\nDo you want to replace it with the one that you are saving?";
        return (JOptionPane.showOptionDialog(parent, message, null,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Replace", "Cancel"}, "Cancel") == JOptionPane.YES_OPTION);
    }
}