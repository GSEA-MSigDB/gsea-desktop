/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


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
        Boolean dark = FxOsAppearance.detect();
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
}
