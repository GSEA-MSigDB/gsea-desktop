/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;

/**
 * Transferable/Clipboard object tp represent one or more Files.
 * Usually for use via Clipboard or dnd.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class FileTransferable implements Transferable, ClipboardOwner {

    private final java.util.List fFiles;
    private final String fPaths;
    private final String fNames;

    /**
     * Class Constructor.
     *
     * @param files
     */
    public FileTransferable(File[] files) {

        if (files != null) {
            fFiles = new ArrayList(files.length);

            for (int i = 0; i < files.length; i++) {
                fFiles.add(files[i]);
            }

            StringBuffer buf = new StringBuffer();
            StringBuffer buf2 = new StringBuffer();

            for (int i = 0; i < fFiles.size(); i++) {
                buf.append(((File) fFiles.get(i)).getAbsolutePath()).append('\n');
                buf2.append(((File) fFiles.get(i)).getName()).append('\n');
            }

            fPaths = buf.toString();
            fNames = buf2.toString();
        } else {
            fFiles = new ArrayList();
            fPaths = "";
            fNames = "";
        }
    }

    public Object getTransferData(DataFlavor flavor) {

        if (flavor.equals(DataFlavor.javaFileListFlavor)) {
            return fFiles;
        } else if (flavor.equals(DataFlavor.stringFlavor)) {
            return fPaths;
        } else {
            return null;
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {

        if ((flavor.equals(DataFlavor.javaFileListFlavor))
                || (flavor.equals(DataFlavor.stringFlavor))) {
            return true;
        }

        return false;
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {

        //log.debug("Lost ownership");
    }

}    // End class FileTransferable
