/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.uiutil;

import java.awt.*;
import java.io.File;

import org.apache.commons.lang3.SystemUtils;

import xapps.gsea.GseaFileFilter;

// TODO: this class is superfluous, should just inline these uses.
// Either that, or centralize ALL uses here.  However, we already have
// the FileManager class that tries to serve that role.
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
}