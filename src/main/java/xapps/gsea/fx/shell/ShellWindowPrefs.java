/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Restores, tracks, and persists the main window's normal (non-maximized) bounds.
 */
final class ShellWindowPrefs {

    private static final Logger klog = LoggerFactory.getLogger(ShellWindowPrefs.class);

    private double normalX = Double.NaN;
    private double normalY = Double.NaN;
    private double normalWidth = Double.NaN;
    private double normalHeight = Double.NaN;

    void restore(Stage stage) {
        try {
            double x = XPreferencesFactory.kAppXPosition.getInt();
            double y = XPreferencesFactory.kAppYPosition.getInt();
            double w = Math.max(1, XPreferencesFactory.kAppWidth.getInt());
            double h = Math.max(1, XPreferencesFactory.kAppHeight.getInt());

            Screen screen = screenForPoint(x, y);
            if (screen == null) {
                screen = Screen.getPrimary();
            }
            Rectangle2D vis = screen.getVisualBounds();

            if (w > vis.getWidth() || h > vis.getHeight()) {
                w = Math.max(1, vis.getWidth() * 3.0 / 4.0);
                h = Math.max(1, vis.getHeight() * 3.0 / 4.0);
            }
            stage.setWidth(w);
            stage.setHeight(h);

            Rectangle2D proposed = new Rectangle2D(x, y, w, h);
            if (!intersectsAnyScreen(proposed)) {
                x = vis.getMinX() + (vis.getWidth() - w) / 2.0;
                y = vis.getMinY() + (vis.getHeight() - h) / 2.0;
            }
            stage.setX(x);
            stage.setY(y);

            normalX = x;
            normalY = y;
            normalWidth = w;
            normalHeight = h;

            if (XPreferencesFactory.kAppMaximized.getBoolean()) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            klog.debug("Window restore: {}", e.toString());
            stage.centerOnScreen();
        }
    }

    void installTracking(Stage stage) {
        Runnable capture = () -> captureNormalBounds(stage);
        stage.xProperty().addListener((o, a, b) -> capture.run());
        stage.yProperty().addListener((o, a, b) -> capture.run());
        stage.widthProperty().addListener((o, a, b) -> capture.run());
        stage.heightProperty().addListener((o, a, b) -> capture.run());
        stage.iconifiedProperty().addListener((o, a, b) -> capture.run());
        stage.maximizedProperty().addListener((o, wasMax, isMax) -> {
            if (wasMax && !isMax) {
                Platform.runLater(() -> captureNormalBounds(stage));
            }
        });
    }

    void persist(Stage stage) throws Exception {
        boolean maximized = stage.isMaximized();
        XPreferencesFactory.kAppMaximized.setValue(maximized);
        if (!maximized && !stage.isIconified()) {
            captureNormalBounds(stage);
        }
        if (!Double.isNaN(normalWidth) && !Double.isNaN(normalHeight)
                && normalWidth > 1 && normalHeight > 1) {
            XPreferencesFactory.kAppWidth.setValue((int) Math.round(normalWidth));
            XPreferencesFactory.kAppHeight.setValue((int) Math.round(normalHeight));
        }
        if (!Double.isNaN(normalX) && !Double.isNaN(normalY)) {
            XPreferencesFactory.kAppXPosition.setValue((int) Math.round(normalX));
            XPreferencesFactory.kAppYPosition.setValue((int) Math.round(normalY));
        }
    }

    private void captureNormalBounds(Stage stage) {
        if (stage.isMaximized() || stage.isIconified()) {
            return;
        }
        double w = stage.getWidth();
        double h = stage.getHeight();
        double x = stage.getX();
        double y = stage.getY();
        if (Double.isNaN(w) || Double.isNaN(h) || w <= 1 || h <= 1
                || Double.isNaN(x) || Double.isNaN(y)) {
            return;
        }
        normalX = x;
        normalY = y;
        normalWidth = w;
        normalHeight = h;
    }

    private static Screen screenForPoint(double x, double y) {
        for (Screen screen : Screen.getScreens()) {
            if (screen.getVisualBounds().contains(x, y)) {
                return screen;
            }
        }
        return null;
    }

    private static boolean intersectsAnyScreen(Rectangle2D window) {
        for (Screen screen : Screen.getScreens()) {
            if (screen.getVisualBounds().intersects(window)) {
                return true;
            }
        }
        return false;
    }
}
