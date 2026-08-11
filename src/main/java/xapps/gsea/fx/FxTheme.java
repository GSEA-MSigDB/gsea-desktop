/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import xapps.gsea.fx.shell.FxWindowsDarkFrame;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/**
 * Shared GSEA FX stylesheets + light/dark appearance (pref + OS system theme).
 */
public final class FxTheme {

    public static final String APPEARANCE_SYSTEM = "System";
    public static final String APPEARANCE_LIGHT = "Light";
    public static final String APPEARANCE_DARK = "Dark";

    private static final String STYLESHEET = "/xapps/gsea/fx/gsea-fx.css";
    private static final String DARK_STYLESHEET = "/xapps/gsea/fx/gsea-fx-dark.css";
    private static final String BENTO_STYLESHEET = "/xapps/gsea/fx/bento.css";

    /** Matches {@code gsea-fx.css} / {@code gsea-fx-dark.css} root backgrounds. */
    private static final Color FILL_LIGHT = Color.web("#e8ecf1");
    private static final Color FILL_DARK = Color.web("#1e1f22");

    private static final List<WeakReference<Scene>> SCENES = new ArrayList<>();
    private static final List<WeakReference<DialogPane>> DIALOGS = new ArrayList<>();
    private static final String PROP_WINDOW_WIRED = "gsea.fx.theme.wired";
    private static final String PROP_DIALOG_WIRED = "gsea.fx.theme.dialog-wired";

    private static volatile Boolean cachedOsDark;
    private static volatile long cachedOsDarkAtMs;
    /** Last successful OS dark probe; retained when a later probe fails. */
    private static volatile Boolean lastKnownOsDark;

    private FxTheme() {
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        // Class tokens must be on the root before stylesheets resolve looked-up colors
        // (e.g. dock tab Text -fx-fill: -color-fg-*), or the first pass sticks on light values.
        if (scene.getRoot() != null) {
            prepareRoot(scene.getRoot());
        }
        syncAppearance(scene);
        track(SCENES, scene);
        scene.windowProperty().addListener((obs, o, w) -> wireWindow(w));
        if (scene.getWindow() != null) {
            wireWindow(scene.getWindow());
        }
    }

    /**
     * Apply {@code gsea-root} / {@code gsea-dark} before the node enters a {@link Scene}
     * so the first CSS pass uses the correct color tokens.
     */
    public static void prepareRoot(Parent root) {
        if (root == null) {
            return;
        }
        ensureRootClass(root);
        setDarkClass(root, isDarkEffective());
    }

    public static void apply(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        apply(dialog.getDialogPane());
        DialogPane pane = dialog.getDialogPane();
        if (pane.getProperties().containsKey(PROP_DIALOG_WIRED)) {
            return;
        }
        pane.getProperties().put(PROP_DIALOG_WIRED, Boolean.TRUE);
        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN, e -> {
            Window w = pane.getScene() != null ? pane.getScene().getWindow() : null;
            wireWindow(w);
        });
    }

    public static void apply(DialogPane pane) {
        if (pane == null) {
            return;
        }
        prepareRoot(pane);
        syncAppearance(pane);
        track(DIALOGS, pane);
    }

    /** Re-apply light/dark to all tracked scenes/dialogs after a preference change. */
    public static void refreshAll() {
        cachedOsDark = null;
        Runnable run = () -> {
            pruneAndSyncScenes();
            pruneAndSyncDialogs();
        };
        if (Platform.isFxApplicationThread()) {
            run.run();
        } else {
            Platform.runLater(run);
        }
    }

    public static boolean isDarkEffective() {
        String pref = appearancePref();
        if (APPEARANCE_DARK.equalsIgnoreCase(pref)) {
            return true;
        }
        if (APPEARANCE_LIGHT.equalsIgnoreCase(pref)) {
            return false;
        }
        return isOsDark();
    }

    public static String appearancePref() {
        try {
            String v = XPreferencesFactory.kUiAppearance.getString();
            if (v == null || v.isBlank()) {
                return APPEARANCE_SYSTEM;
            }
            if (APPEARANCE_LIGHT.equalsIgnoreCase(v)) {
                return APPEARANCE_LIGHT;
            }
            if (APPEARANCE_DARK.equalsIgnoreCase(v)) {
                return APPEARANCE_DARK;
            }
            return APPEARANCE_SYSTEM;
        } catch (Throwable t) {
            return APPEARANCE_SYSTEM;
        }
    }

    static boolean isOsDark() {
        long now = System.currentTimeMillis();
        Boolean cached = cachedOsDark;
        if (cached != null && now - cachedOsDarkAtMs < 5_000L) {
            return cached;
        }
        Boolean dark = detectOsDark();
        if (dark == null) {
            Boolean known = lastKnownOsDark;
            dark = known != null ? known : Boolean.FALSE;
        } else {
            lastKnownOsDark = dark;
        }
        cachedOsDark = dark;
        cachedOsDarkAtMs = now;
        return dark;
    }

    private static void syncAppearance(Scene scene) {
        boolean dark = isDarkEffective();
        if (scene.getRoot() != null) {
            // Toggle class before stylesheet list changes so looked-up colors rebind correctly.
            setDarkClass(scene.getRoot(), dark);
        }
        syncStylesheets(scene.getStylesheets(), dark);
        scene.setFill(dark ? FILL_DARK : FILL_LIGHT);
        if (scene.getRoot() != null) {
            scene.getRoot().applyCss();
        }
        syncNativeChrome(scene.getWindow(), dark);
    }

    private static void syncAppearance(DialogPane pane) {
        boolean dark = isDarkEffective();
        setDarkClass(pane, dark);
        syncStylesheets(pane.getStylesheets(), dark);
        pane.applyCss();
        if (pane.getScene() != null) {
            pane.getScene().setFill(dark ? FILL_DARK : FILL_LIGHT);
            syncNativeChrome(pane.getScene().getWindow(), dark);
        }
    }

    /** Windows dark title bar; no-op off Windows or before the window is showing. */
    private static void syncNativeChrome(Window window, boolean dark) {
        if (window == null || !FxWindowsDarkFrame.isWindows() || !window.isShowing()) {
            return;
        }
        Runnable apply = () -> {
            if (!window.isShowing()) {
                return;
            }
            if (FxWindowsDarkFrame.apply(window, dark)) {
                return;
            }
            // HWND not ready yet — one deferred retry, then give up silently.
            Platform.runLater(() -> {
                if (window.isShowing()) {
                    FxWindowsDarkFrame.apply(window, dark);
                }
            });
        };
        if (Platform.isFxApplicationThread()) {
            apply.run();
        } else {
            Platform.runLater(apply);
        }
    }

    private static void wireWindow(Window w) {
        if (w == null || w.getProperties().containsKey(PROP_WINDOW_WIRED)) {
            return;
        }
        w.getProperties().put(PROP_WINDOW_WIRED, Boolean.TRUE);
        w.focusedProperty().addListener((obs, was, is) -> {
            if (Boolean.TRUE.equals(is) && APPEARANCE_SYSTEM.equalsIgnoreCase(appearancePref())) {
                cachedOsDark = null;
                refreshAll();
            }
        });
        w.showingProperty().addListener((obs, was, showing) -> {
            if (Boolean.TRUE.equals(showing)) {
                syncNativeChrome(w, isDarkEffective());
            }
        });
        if (w.isShowing()) {
            syncNativeChrome(w, isDarkEffective());
        }
    }

    private static void syncStylesheets(List<String> sheets, boolean dark) {
        String bento = url(BENTO_STYLESHEET);
        String base = url(STYLESHEET);
        String darkUrl = url(DARK_STYLESHEET);
        // Bento first, then GSEA chrome, then dark overrides.
        if (!sheets.contains(bento)) {
            sheets.add(0, bento);
        }
        if (!sheets.contains(base)) {
            int idx = sheets.indexOf(bento);
            sheets.add(idx + 1, base);
        }
        if (dark) {
            if (!sheets.contains(darkUrl)) {
                sheets.add(darkUrl);
            }
        } else {
            sheets.remove(darkUrl);
        }
    }

    private static void setDarkClass(Parent node, boolean dark) {
        if (dark) {
            if (!node.getStyleClass().contains("gsea-dark")) {
                node.getStyleClass().add("gsea-dark");
            }
        } else {
            node.getStyleClass().remove("gsea-dark");
        }
    }

    private static String url(String resource) {
        return FxTheme.class.getResource(resource).toExternalForm();
    }

    private static void ensureRootClass(Parent node) {
        if (!node.getStyleClass().contains("gsea-root")) {
            node.getStyleClass().add("gsea-root");
        }
    }

    private static <T> void track(List<WeakReference<T>> list, T item) {
        synchronized (list) {
            prune(list);
            for (WeakReference<T> ref : list) {
                if (ref.get() == item) {
                    return;
                }
            }
            list.add(new WeakReference<>(item));
        }
    }

    private static void pruneAndSyncScenes() {
        synchronized (SCENES) {
            Iterator<WeakReference<Scene>> it = SCENES.iterator();
            while (it.hasNext()) {
                Scene s = it.next().get();
                if (s == null) {
                    it.remove();
                } else {
                    syncAppearance(s);
                }
            }
        }
    }

    private static void pruneAndSyncDialogs() {
        synchronized (DIALOGS) {
            Iterator<WeakReference<DialogPane>> it = DIALOGS.iterator();
            while (it.hasNext()) {
                DialogPane p = it.next().get();
                if (p == null) {
                    it.remove();
                } else {
                    syncAppearance(p);
                }
            }
        }
    }

    private static <T> void prune(List<WeakReference<T>> list) {
        list.removeIf(ref -> ref.get() == null);
    }

    /** @return dark, light, or null if the probe failed */
    private static Boolean detectOsDark() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                return detectWindowsDark();
            }
            if (os.contains("mac")) {
                return detectMacDark();
            }
            return detectLinuxDark();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean detectWindowsDark() {
        Boolean viaJna = detectWindowsDarkViaRegistry();
        if (viaJna != null) {
            return viaJna;
        }
        try {
            Process p = new ProcessBuilder(
                    "reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme")
                    .redirectErrorStream(true)
                    .start();
            String out = readProcess(p, 1);
            for (String line : out.split("\\R")) {
                String t = line.trim().toLowerCase(Locale.ROOT);
                if (!t.contains("appsuselighttheme")) {
                    continue;
                }
                if (t.contains("0x0")) {
                    return Boolean.TRUE;
                }
                if (t.contains("0x1")) {
                    return Boolean.FALSE;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Fast Advapi32 read of AppsUseLightTheme (0 = dark apps). Avoids spawning reg.exe on the FX thread.
     */
    private static Boolean detectWindowsDarkViaRegistry() {
        try {
            PointerByReference phkResult = new PointerByReference();
            int open = Advapi32.INSTANCE.RegOpenKeyEx(
                    Advapi32.HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    0,
                    Advapi32.KEY_READ,
                    phkResult);
            if (open != 0) {
                return null;
            }
            Pointer hKey = phkResult.getValue();
            try {
                IntByReference type = new IntByReference();
                Memory data = new Memory(4);
                IntByReference size = new IntByReference(4);
                int q = Advapi32.INSTANCE.RegQueryValueEx(
                        hKey,
                        "AppsUseLightTheme",
                        null,
                        type,
                        data,
                        size);
                if (q != 0 || type.getValue() != Advapi32.REG_DWORD) {
                    return null;
                }
                // 0 = dark, 1 = light
                return data.getInt(0) == 0;
            } finally {
                Advapi32.INSTANCE.RegCloseKey(hKey);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean detectMacDark() throws Exception {
        Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                .redirectErrorStream(true)
                .start();
        String out = readProcess(p, 1).trim();
        // Missing AppleInterfaceStyle means light appearance.
        return out.equalsIgnoreCase("Dark");
    }

    private static Boolean detectLinuxDark() throws Exception {
        Process p = new ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
                .redirectErrorStream(true)
                .start();
        String out = readProcess(p, 1).toLowerCase(Locale.ROOT);
        if (out.contains("prefer-dark")) {
            return Boolean.TRUE;
        }
        if (out.contains("prefer-light")) {
            return Boolean.FALSE;
        }
        Process p2 = new ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
                .redirectErrorStream(true)
                .start();
        String theme = readProcess(p2, 1).toLowerCase(Locale.ROOT);
        if (theme.isBlank()) {
            return null;
        }
        return theme.contains("-dark");
    }

    private static String readProcess(Process p, int timeoutSec) throws Exception {
        boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /** Minimal Advapi32 for reading the Windows apps theme DWORD. */
    private interface Advapi32 extends StdCallLibrary {
        Advapi32 INSTANCE = Native.load("advapi32", Advapi32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer HKEY_CURRENT_USER = Pointer.createConstant(0x80000001L);
        int KEY_READ = 0x20019;
        int REG_DWORD = 4;

        int RegOpenKeyEx(Pointer hKey, String lpSubKey, int ulOptions, int samDesired,
                PointerByReference phkResult);

        int RegQueryValueEx(Pointer hKey, String lpValueName, IntByReference lpReserved, IntByReference lpType,
                Pointer lpData, IntByReference lpcbData);

        int RegCloseKey(Pointer hKey);
    }
}
