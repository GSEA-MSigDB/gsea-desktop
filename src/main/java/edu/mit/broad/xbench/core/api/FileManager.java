/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.genome.parsers.ParserFactory;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Recent-file / recent-dir persistence (no Swing dialogs).
 */
public class FileManager {
    private static final Logger klog = LoggerFactory.getLogger(FileManager.class);

    private XStore fRecentFilesStore_as_files;
    private XStore fRecentUrlsStore;
    private XStore fRecentDirsStore;
    /** Disk writes for the recent-file list. Parser threads only enqueue. */
    private final ExecutorService recentWrites = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "gsea-recent-files");
        t.setDaemon(true);
        return t;
    });

    public FileManager() {
        ParserFactory.getCache().addPathAdditionsListener(new MyPropertyChangeListener());
    }

    /**
     * Block until recent-file writes queued so far have hit disk.
     * Called on shutdown so a daemon writer cannot drop the last paths.
     */
    public void flushRecentWrites() {
        try {
            recentWrites.submit(() -> { }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            klog.warn("Could not flush recent-file writes", e);
        }
    }

    public synchronized void registerRecentlyOpenedFile(final File file) {
        if (file == null) {
            return;
        }
        getRecentFilesStore().addAndSave(file.getPath());
        File parent = file.getParentFile();
        if (parent != null) {
            registerRecentlyOpenedDir(parent);
        }
    }

    public synchronized void registerRecentlyOpenedDir(final File dir) {
        if (dir == null) {
            return;
        }
        getRecentDirsStore().addAndSave(dir.getPath());
    }

    public synchronized void registerRecentlyOpenedURL(final String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        getRecentUrlsStore().addAndSave(url);
    }

    public synchronized File getLastDirAccesessed() {
        XStore xs = getRecentDirsStore();
        if (xs.getSize() == 0) {
            return SystemUtils.getUserHome();
        }
        String str = xs.getElementAt(xs.getSize() - 1);
        if (str != null) {
            return new File(str);
        }
        return SystemUtils.getUserHome();
    }

    public synchronized XStore getRecentFilesStore() {
        if (fRecentFilesStore_as_files == null) {
            this.fRecentFilesStore_as_files = new XStores.FilePathStore(new File(
                    Application.getVdbManager().getRuntimeHomeDir(), "recent_files.txt"));
            try {
                List<String> rems = new ArrayList<>();
                for (int i = 0; i < fRecentFilesStore_as_files.getSize(); i++) {
                    String filePath = fRecentFilesStore_as_files.getElementAt(i);
                    if (!new File(filePath).exists()) {
                        rems.add(filePath);
                    }
                }
                fRecentFilesStore_as_files.removeAndSave(rems);
                fRecentFilesStore_as_files.trim(30);
            } catch (Throwable t) {
                klog.error("Recent file list initing error: {}", t.toString());
            }
        }
        return fRecentFilesStore_as_files;
    }

    public synchronized XStore getRecentDirsStore() {
        if (fRecentDirsStore == null) {
            this.fRecentDirsStore = new XStores.DirPathStore(new File(
                    Application.getVdbManager().getRuntimeHomeDir(), "recent_dirs.txt"));
            fRecentDirsStore.trim(50);
        }
        return fRecentDirsStore;
    }

    public synchronized XStore getRecentUrlsStore() {
        if (fRecentUrlsStore == null) {
            this.fRecentUrlsStore = new XStores.StringStore(new File(
                    Application.getVdbManager().getRuntimeHomeDir(), "recent_urls.txt"));
            fRecentUrlsStore.trim(50);
        }
        return fRecentUrlsStore;
    }

    class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (ObjectCache.PROP_PATH_ADDED.equals(evt.getPropertyName())) {
                Object obj = evt.getNewValue();
                if (obj != null) {
                    File file = new File(obj.toString());
                    recentWrites.execute(() -> registerRecentlyOpenedFile(file));
                }
            }
        }
    }
}
