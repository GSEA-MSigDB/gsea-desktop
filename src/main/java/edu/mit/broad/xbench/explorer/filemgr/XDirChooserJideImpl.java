/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.filemgr;

import com.jidesoft.swing.FolderChooser;
import edu.mit.broad.xbench.core.api.Application;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class XDirChooserJideImpl implements XDirChooser {

    private boolean fProceed;

    private static final String DEFAULT_APPROVE_BUTTON_TEXT = "Open";

    private final Logger log = Logger.getLogger(this.getClass());

    private FolderChooser fFolderChooser;

    /**
     * Class constructor
     */
    public XDirChooserJideImpl() {
        init();
    }

    // does the GUI building
    // params can be null
    private void init() {

        this.fFolderChooser = new FolderChooser();
        fFolderChooser.setFileHidingEnabled(true);

    }

    public boolean show() {
        return show(Application.getWindowManager().getRootFrame());
    }

    public boolean show(JFrame rootFrame) {

        fProceed = false; // clear previous

        int val = fFolderChooser.showOpenDialog(rootFrame);

        if (val == FolderChooser.APPROVE_OPTION) {
            fProceed = true;
        } else {
            fProceed = false;
        }

        return fProceed;
    }

    public File getSelectedDir() {
        if (fProceed) {
            Application.getFileManager().registerRecentlyOpenedDir(fFolderChooser.getSelectedFile());
            return fFolderChooser.getSelectedFile();
        } else {
            log.warn("asked for dir though proceed was false");
            return null;
        }
    }

    public void setCurrentLocation(String path) {
        fFolderChooser.setCurrentDirectory(new File(path));
    }

    public void setApproveButtonText(String txt) {
        fFolderChooser.setApproveButtonText(txt);
    }

    // reset the original state i.e button labels etc
    public void resetState() {

        File[] files = Application.getFileManager().getRecentDirs();

        List<String> recents = new ArrayList <String>();
        for (int i=0; i < files.length; i++) {
            recents.add(files[i].getPath());
        }

        this.fFolderChooser.setRecentList(recents);
        this.fFolderChooser.setApproveButtonText(DEFAULT_APPROVE_BUTTON_TEXT);
    }

} // End GDirChooser


