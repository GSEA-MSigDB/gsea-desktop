/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.uiutil;

import java.awt.*;
import java.io.File;

import javax.swing.JOptionPane;

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
        fc.setModal(true);
        fc.setMultipleMode(false);
        fc.setVisible(true);
        File[] files = fc.getFiles();
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    // TODO: probably should inline this.
    public static File showSaveDialog(Frame parent) {
        FileDialog fc = new FileDialog(parent, "GSEA", FileDialog.SAVE);
        fc.setFile("*.gct;*.res;*.txt");
        fc.setFilenameFilter(new GseaFileFilter(new String[] {"gct", "res", "txt"}, "Expression Files"));
        fc.setModal(true);
        fc.setMultipleMode(false);
        fc.setVisible(true);
        File[] files = fc.getFiles();
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
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