/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.broad.gsea.ui;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;

import org.apache.commons.lang3.SystemUtils;

/**
 * Platform desktop integration (dock icon, About/Quit handlers on macOS).
 */
public class DesktopIntegration {

    public static void setDockIcon(Image image) {
        if (SystemUtils.IS_OS_MAC_OSX && image != null) {
            try {
                Taskbar.getTaskbar().setIconImage(image);
            } catch (UnsupportedOperationException | SecurityException ignored) {
                // Some environments do not expose the taskbar API.
            }
        }
    }

    /**
     * Wire macOS application menu About / Quit to the JavaFX shell callbacks.
     * {@code quitHandler} returns {@code true} when quit should proceed (Swing
     * {@code performQuit} / {@code cancelQuit} parity).
     * Safe to call on any OS (no-ops when handlers are unsupported).
     */
    public static void installHandlers(Runnable aboutHandler, java.util.function.BooleanSupplier quitHandler) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        try {
            if (desktop.isSupported(Desktop.Action.APP_ABOUT) && aboutHandler != null) {
                desktop.setAboutHandler(e -> aboutHandler.run());
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
        try {
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER) && quitHandler != null) {
                desktop.setQuitHandler(new QuitHandler() {
                    @Override
                    public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
                        try {
                            if (quitHandler.getAsBoolean()) {
                                response.performQuit();
                            } else {
                                response.cancelQuit();
                            }
                        } catch (Throwable t) {
                            response.cancelQuit();
                        }
                    }
                });
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
    }
}
