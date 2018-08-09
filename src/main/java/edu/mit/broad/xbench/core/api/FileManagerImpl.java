/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.RendererFactory2;
import edu.mit.broad.xbench.explorer.filemgr.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
public class FileManagerImpl implements FileManager {

    private static final Logger klog = XLogger.getLogger(FileManagerImpl.class);

    // Lots of things made lazilly in an effort to speedup loading process
    // Also some dependencies to Application helped by lazy loading

    // @note dependency on vdb

    private XStore fRecentFilesStore_as_dirs;

    private XStore fRecentFilesStore_as_files;

    private XStore fRecentUrlsStore;

    private XStore fRecentDirsStore;

    public XDirChooser fDirChooser;

    private XFileChooserImpl fFileChooser;

    /**
     * Class constructor
     */
    public FileManagerImpl() {
    }

    private void initFC() {
        if (fFileChooser == null) {
            RecentDirsModel model = new RecentDirsModel(getRecentFilesStore_as_dirs(), new String[]{});
            JList jlRecent = new JList(model);
            jlRecent.setCellRenderer(new ListCellRenderer());

            /* @note changed after webstart
            JList jlStd = new JList(Application.getVdbManager().getDescriptedFiles());
            jlStd.setCellRenderer(new ListCellRenderer());

            //fFileChooser = new XFileChooserImpl(jlStd, jlRecent);
            */

            fFileChooser = new XFileChooserImpl(jlRecent);

            // Listen to the parser for files opened
            ParserFactory.getCache().addPathAdditionsListener(new MyPropertyChangeListener());
        }
    }

    // we need these in addition to the automatic file chooser based mechanism because
    // files can be loaded in in other ways - for example double click of a jlist
    public void registerRecentlyOpenedDir(final File dir) {
        if (dir != null && dir.isDirectory() && dir.exists()) {
            XStore xstore = getRecentDirsStore();
            if (xstore.contains(dir.getPath()) == false) {
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

    public File[] getRecentDirs() {

        getRecentFilesStore();
        getRecentDirsStore();

        Set dirs = new HashSet();
        java.util.List lines = new ArrayList();
        lines.addAll(getRecentDirsStore().fLines);
        lines.addAll(getRecentFilesStore().fLines);

        klog.debug("# of lines: " + lines.size());

        for (int i = 0; i < lines.size(); i++) {
            Object obj = lines.get(i);
            if (obj != null) {
                File file = new File(obj.toString());
                if (file.exists()) {
                    if (file.isFile()) {
                        dirs.add(file.getParentFile());
                    } else {
                        dirs.add(file);
                    }
                }
            }
        }

        return (File[]) dirs.toArray(new File[dirs.size()]);
    }

    /**
     * Might return null if na
     *
     * @return
     */
    public File getLastDirAccesessed() {
        XStore xs = getRecentDirsStore();

        if (xs.getSize() == 0) {
            return null;
        }

        Object obj = xs.getElementAt(xs.getSize() - 1);

        if (obj != null) {
            return new File(obj.toString());
        } else {
            return null;
        }
    }

    public XFileChooser getFileChooser() throws HeadlessException {
        initFC();
        return fFileChooser;
    }

    public XFileChooser getFileChooser(final FileFilter[] filts) throws HeadlessException {
        initFC();
        // @todo filt
        return fFileChooser;
    }

    public XDirChooser getDirChooser(final String approveButtonTxt) {
        if (fDirChooser == null) {
            fDirChooser = new XDirChooserJideImpl();
            File lastDir = getLastDirAccesessed();
            if (lastDir != null && lastDir.exists()) {
                fDirChooser.setCurrentLocation(lastDir.getPath());
            }

        }

        fDirChooser.resetState(); // @note
        fDirChooser.setApproveButtonText(approveButtonTxt);

        return fDirChooser;
    }

    private XStore getRecentFilesStore_as_dirs() {
        // NO saving etc via this
        if (fRecentFilesStore_as_dirs == null) {
            getRecentFilesStore(); // just init

            // files file but still list only the dirs (in the chooser)
            this.fRecentFilesStore_as_dirs = new XStores.DirPathStore(new File(Application.getVdbManager().getRuntimeHomeDir(),
                    "recent_files.txt"));
        }

        return fRecentFilesStore_as_dirs;
    }

    public XStore getRecentFilesStore() {

        if (fRecentFilesStore_as_files == null) {
            this.fRecentFilesStore_as_files = new XStores.FilePathStore(new File(Application.getVdbManager().getRuntimeHomeDir(),
                    "recent_files.txt"));

            try {
                java.util.List rems = new ArrayList();
                for (int i = 0; i < fRecentFilesStore_as_files.getSize(); i++) {
                    File f = new File(fRecentFilesStore_as_files.getElementAt(i).toString());
                    if (!f.exists()) {
                        rems.add(fRecentFilesStore_as_files.getElementAt(i));
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


    public XStore getRecentDirsStore() {
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
     * @version %I%, %G%
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

    /**
     * Class ListCellRenderer
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    // class ListCellRenderer extends DefaultListCellRenderer {
    private static class ListCellRenderer extends RendererFactory2.CommonLookListRenderer {
        private static final Icon ICON = JarResources.getIcon("pinned_ovr.gif");

        public ListCellRenderer() {
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            // doesnt work properly unless called ??
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }

    }


} // End interface FileManagerImpl
