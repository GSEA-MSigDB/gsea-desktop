/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * Seeds File/Directory choosers from {@link AppServices#files()} and registers selections.
 */
public final class FxFileChooserUtil {
    private FxFileChooserUtil() {
    }

    private static File lastDirOrNull() {
        try {
            if ((AppServices.current() != null)) {
                return AppServices.require().files().getLastDirAccesessed();
            }
        } catch (Throwable ignored) {
            // App handler / prefs may not be ready yet
        }
        return null;
    }

    public static void seedInitialDirectory(FileChooser chooser) {
        seedInitialDirectory(chooser, (String) null);
    }

    /** Prefer existing directory or parent of {@code preferredPath}; else last-accessed dir. */
    public static void seedInitialDirectory(FileChooser chooser, String preferredPath) {
        File seed = resolveSeedDir(preferredPath);
        if (seed != null) {
            chooser.setInitialDirectory(seed);
        }
    }

    public static void seedInitialDirectory(DirectoryChooser chooser) {
        seedInitialDirectory(chooser, null);
    }

    /**
     * prefer {@code preferredPath} (JFileChooser walks parents when the path is not an existing directory); blank → last-accessed dir.
     */
    public static void seedInitialDirectory(DirectoryChooser chooser, String preferredPath) {
        File seed = resolveSeedDir(preferredPath);
        if (seed != null) {
            chooser.setInitialDirectory(seed);
        }
    }

    private static File resolveSeedDir(String preferredPath) {
        if (preferredPath != null && !preferredPath.isBlank()) {
            File preferred = new File(preferredPath.trim());
            File seed = preferred;
            while (seed != null && !seed.isDirectory()) {
                seed = seed.getParentFile();
            }
            if (seed != null && seed.isDirectory()) {
                return seed;
            }
        }
        File dir = lastDirOrNull();
        if (dir != null && dir.isDirectory()) {
            return dir;
        }
        return null;
    }

    public static void registerOpened(File file) {
        if (file == null) {
            return;
        }
        try {
            if ((AppServices.current() != null)) {
                AppServices.require().files().registerRecentlyOpenedFile(file);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void registerOpenedDir(File dir) {
        if (dir == null) {
            return;
        }
        try {
            if ((AppServices.current() != null)) {
                AppServices.require().files().registerRecentlyOpenedDir(dir);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Window windowOf(javafx.scene.Node node) {
        if (node != null && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return null;
    }
}
