/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A PersistentObject represented as a Transferable/ClipboardOwner.
 * Typically used via Clipboard and dnd.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PobTransferable implements Transferable, ClipboardOwner {

    private final List fPobs;
    private final java.util.List fFiles;
    private final String fPaths;

    /**
     * Class Constructor.
     *
     * @param pobs
     */
    public PobTransferable(PersistentObject[] pobs) {

        if (pobs != null) {
            fPobs = new ArrayList(pobs.length);
            fFiles = new ArrayList(pobs.length);

            for (int i = 0; i < pobs.length; i++) {
                fPobs.add(pobs[i]);

                File file = ParserFactory.getCache().getSourceFile(pobs[i]);

                fFiles.add(file);
            }

            fPaths = toPathString();
        } else {
            fPobs = new ArrayList();
            fFiles = new ArrayList();
            fPaths = "";
        }
    }

    /**
     * Mix of files, pobs and other objects
     * files -> stored
     * pobs -> stored
     * others -> ignored
     *
     * @param objs
     */
    public PobTransferable(Object[] objs) {

        if (objs != null) {
            fPobs = new ArrayList();
            fFiles = new ArrayList();

            for (int i = 0; i < objs.length; i++) {
                if (objs[i] instanceof File) {
                    fFiles.add(objs[i]);
                    // Hmm not sure if i should parse at init?
                    //if (ParserFactory.getC
                } else if (objs[i] instanceof PersistentObject) {
                    PersistentObject pob = (PersistentObject) objs[i];
                    fPobs.add(pob);
                    File file = ParserFactory.getCache().getSourceFile(pob);
                    fFiles.add(file);
                } else {
                    ;    // ignore
                }
            }

            fPaths = toPathString();
        } else {
            fPobs = new ArrayList();
            fFiles = new ArrayList();
            fPaths = "";
        }
    }

    public Object getTransferData(DataFlavor flavor) {

        // favor pob flavor first -- less chance of error -- as pobs are already in
        if (flavor.equals(PobFlavor.pobListFlavor)) {
            return fPobs;
        } else if (flavor.equals(DataFlavor.javaFileListFlavor)) {
            return fFiles;
        } else if (flavor.equals(DataFlavor.stringFlavor)) {
            return fPaths;
        } else {
            return null;
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor,
                PobFlavor.pobListFlavor};
    }

    /**
     * supports lists of Pobs, Files and a String
     *
     * @param flavor
     * @return
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {

        if ((flavor.equals(DataFlavor.javaFileListFlavor))
                || (flavor.equals(DataFlavor.stringFlavor))
                || (flavor.equals(PobFlavor.pobListFlavor))) {
            return true;
        }

        return false;
    }

    /**
     * Does nothing
     *
     * @param clipboard
     * @param contents
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

        //log.debug("Lost ownership");
    }

    private String toPathString() {

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < fFiles.size(); i++) {

            //PersistentObject pob = (PersistentObject)fPobs.get(i);
            //buf.append(pob.getName()).append('\t');
            File file = (File) fFiles.get(i);

            buf.append(file.getAbsolutePath()).append('\n');
        }

        return buf.toString();
    }
}    // End FileSelectionFlavor
