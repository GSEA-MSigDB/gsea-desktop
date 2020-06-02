/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.genome.parsers.ParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import javax.swing.JFileChooser;

import java.awt.FileDialog;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * File / dir choosers are extensions of the swing widgets.
 * Extensions do things like:
 * <p/>
 * 1) presenting additional panel with recently accessed files / dirs
 * 2) standard dirs like data , examples etc built in
 * <p/>
 *
 * @author Aravind Subramanian
 */
public class FileManager {

    private static final Logger klog = Logger.getLogger(FileManager.class);

    // @note dependency on vdb

    private XStore fRecentFilesStore_as_files;

    private XStore fRecentUrlsStore;

    private XStore fRecentDirsStore;

    private FileDialog fMacDirFileDialog;
    
    private JFileChooser fDirFileChooser;

    private FileDialog fFileDialog;

    public FileManager() {
        if (SystemUtils.IS_OS_MAC_OSX) {
            // For macOS, use the native AWT dialogs to better support notarization.
        	fMacDirFileDialog = new FileDialog(Application.getWindowManager().getRootFrame(), "Open", FileDialog.LOAD);
        } else {
            // For non-Mac we use the Swing dialog
            fDirFileChooser = new JFileChooser();
            fDirFileChooser.setApproveButtonText("Select");
            fDirFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fDirFileChooser.setMultiSelectionEnabled(false);
            fDirFileChooser.setFileHidingEnabled(true);
        }
        
        fFileDialog = new FileDialog(Application.getWindowManager().getRootFrame(), "Open", FileDialog.LOAD);
        
        // TODO: clean up RecentDirsModel, XFileChooserImpl, XFileChooser, GseaAppConf, FileChooser and related
        // HeatMapComponent

        // Listen to the parser for files opened
        ParserFactory.getCache().addPathAdditionsListener(new MyPropertyChangeListener());
    }

    // we need these in addition to the automatic file chooser based mechanism because
    // files can be loaded in in other ways - for example double click of a jlist
    public void registerRecentlyOpenedDir(final File dir) {
        if (dir != null && dir.isDirectory() && dir.exists()) {
            XStore xstore = getRecentDirsStore();
            if (!xstore.contains(dir.getPath())) {
                xstore.addAndSave(dir.getPath());
            }
        }
    }

    public void registerRecentlyOpenedFile(final File file) {
        getRecentFilesStore().addAndSave(file.getPath());
    }

    public void registerRecentlyOpenedURL(final String url) {
        getRecentUrlsStore().addAndSave(url);
    }

    // Might return null if na
    private File getLastDirAccesessed() {
        XStore xs = getRecentDirsStore();

        if (xs.getSize() == 0) {
            return null;
        }

        String str = xs.getElementAt(xs.getSize() - 1);

        if (str != null) {
            return new File(str);
        } else {
            return null;
        }
    }

    // TODO: maybe just move this to the call site
    // There's just one caller.  The idea of having it here is to pre-initialize the FD, but we
    // don't do that with any other FDs and it's not clear it's needed on modern computers.  OTOH,
    // maybe we move all those FDs here as well.
    public FileDialog getFileChooser() throws HeadlessException {
        return fFileDialog;
    }
    
    public File chooseDirByDialog(String selectedDir) {
        // Use the most recent Dir if none is selected
        if (StringUtils.isBlank(selectedDir)) {
            selectedDir = getLastDirAccesessed().getAbsolutePath();
        }
        
        if (SystemUtils.IS_OS_MAC_OSX) {
            // Set a property to tell macOS to create a chooser for Directories instead of Files.
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fMacDirFileDialog.setDirectory(selectedDir);
            fMacDirFileDialog.setVisible(true);
            // We reset the property directly after returning so it doesn't affect other FileDialogs
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            File[] selection = fMacDirFileDialog.getFiles();
            if (selection != null && selection.length > 0) {
                // Always only one since multipleMode is false
                registerRecentlyOpenedDir(selection[0]);
                return selection[0];
            }
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
        } else {
            // For non-Mac we use the Swing JFileChooser as the AWT FileDialog does not allow
            // directory choosing on the other platforms.
            fDirFileChooser.setCurrentDirectory(new File(selectedDir));
            if (fDirFileChooser.showOpenDialog(Application.getWindowManager().getRootFrame()) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fDirFileChooser.getSelectedFile();
                registerRecentlyOpenedDir(selectedFile);
                return selectedFile;
            }
        }
        
        // Otherwise return null to indicate no selection
        return null;
    }

    public XStore getRecentFilesStore() {

        if (fRecentFilesStore_as_files == null) {
            this.fRecentFilesStore_as_files = new XStores.FilePathStore(new File(Application.getVdbManager().getRuntimeHomeDir(),
                    "recent_files.txt"));

            try {
                List<String> rems = new ArrayList<String>();
                for (int i = 0; i < fRecentFilesStore_as_files.getSize(); i++) {
                    String filePath = fRecentFilesStore_as_files.getElementAt(i);
                    File f = new File(filePath);
                    if (!f.exists()) {
                        rems.add(filePath);
                    }
                }

                fRecentFilesStore_as_files.removeAndSave(rems);
                fRecentFilesStore_as_files.trim(30);    // only upto 30 recent files

            } catch (Throwable t) {
                t.printStackTrace();
                klog.error("Recent file list initing error: " + t);
            }
        }

        return fRecentFilesStore_as_files;
    }

    private XStore getRecentDirsStore() {
        if (fRecentDirsStore == null) {
            this.fRecentDirsStore = new XStores.DirPathStore(new File(Application.getVdbManager().getRuntimeHomeDir(),
                    "recent_dirs.txt"));
            fRecentDirsStore.trim(50);
        }

        return fRecentDirsStore;
    }

    public XStore getRecentUrlsStore() {
        if (fRecentUrlsStore == null) {
            this.fRecentUrlsStore = new XStores.StringStore(new File(Application.getVdbManager().getRuntimeHomeDir(),
                    "recent_urls.txt"));
            fRecentUrlsStore.trim(50);
        }

        return fRecentUrlsStore;
    }

    /**
     * for Listening to cache events and adding to recent files mechanism
     *
     * @author Aravind Subramanian
     */
    class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ObjectCache.PROP_PATH_ADDED)) {
                Object obj = evt.getNewValue();
                if (obj != null) {
                    File f = new File(obj.toString());
                    Application.getFileManager().registerRecentlyOpenedFile(f);
                }
            }
        }
    }
}