/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform open helpers matching Swing {@code TaskManager.openUrlInBrowser} fallbacks:
 * Desktop → WSL ({@code wslview} / {@code cmd.exe} / PowerShell) → Win {@code cmd} /
 * Mac {@code open} / {@code xdg-open}.
 */
public final class FxDesktopUtil {
    private static final Logger klog = LoggerFactory.getLogger(FxDesktopUtil.class);
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();

    private FxDesktopUtil() {
    }

    public static void openUrl(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is empty");
        }
        openUri(URI.create(url.trim()));
    }

    public static void openUrl(URL url) throws Exception {
        if (url == null) {
            throw new IllegalArgumentException("URL is null");
        }
        openUri(url.toURI());
    }

    public static void openUri(URI uri) throws Exception {
        if (uri == null) {
            throw new IllegalArgumentException("URI is null");
        }
        // Swing FileBrowserAction / OsExplorerAction: empty-authority rewrite so macOS
        // Desktop.browse accepts local file:// URIs (file:/path → file:///path).
        if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
            uri = new URI(uri.getScheme(), "", uri.getPath(), null, null);
        }
        String uriText = uri.toString();

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri);
                    return;
                } catch (Exception e) {
                    klog.debug("Desktop.browse failed, trying fallbacks: {}", e.toString());
                }
            }
        }

        if (isWslEnvironment()) {
            if (tryLaunch("wslview", uriText)) {
                return;
            }
            if (tryLaunch("cmd.exe", "/c", "start", "", uriText)) {
                return;
            }
            if (tryLaunch("powershell.exe", "-NoProfile", "-Command", "Start-Process '" + uriText + "'")) {
                return;
            }
        }

        if (OS_NAME.contains("win")) {
            if (tryLaunch("cmd", "/c", "start", "", uriText)) {
                return;
            }
        } else if (OS_NAME.contains("mac")) {
            if (tryLaunch("open", uriText)) {
                return;
            }
        } else {
            if (tryLaunch("xdg-open", uriText)) {
                return;
            }
        }

        throw new UnsupportedOperationException("Unable to open browser for URL on this platform: " + uriText);
    }

    public static void openFile(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(file);
                return;
            } catch (Exception e) {
                klog.debug("Desktop.open failed, trying fallbacks: {}", e.toString());
            }
        }
        openUri(file.toURI());
    }

    /**
     * Swing {@code OsExplorerAction}: open directory, or parent of a file / missing path
     * ({@code isDirectory()} is false when the path does not exist → {@code getParentFile()}).
     */
    public static void openInOsExplorer(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        File dir = file.isDirectory() ? file : file.getParentFile();
        if (dir == null) {
            throw new IllegalArgumentException("No parent directory for: " + file.getPath());
        }
        openUri(dir.toURI());
    }

    private static boolean isWslEnvironment() {
        return System.getenv("WSL_DISTRO_NAME") != null || System.getenv("WSL_INTEROP") != null;
    }

    private static boolean tryLaunch(String... command) {
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException ioe) {
            klog.debug("Could not launch command: {}", command[0], ioe);
            return false;
        }
    }
}
