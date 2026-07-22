/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import javafx.application.Platform;

/**
 * FX-thread marshaling helpers. Sync if already on the FX thread; otherwise
 * {@link Platform#runLater}; if the toolkit is absent (headless tests), run on the caller.
 */
public final class FxThreads {

    private FxThreads() {
    }

    public static void runLater(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException noToolkit) {
            action.run();
        }
    }
}
