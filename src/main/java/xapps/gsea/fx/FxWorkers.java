/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import javafx.concurrent.Task;

/**
 * Starts JavaFX {@link Task}s on daemon worker threads (FX thread callbacks stay on FX).
 */
public final class FxWorkers {

    private FxWorkers() {
    }

    public static <V> void start(Task<V> task, String threadName) {
        if (task == null) {
            return;
        }
        Thread t = new Thread(task, threadName != null ? threadName : "gsea-fx-worker");
        t.setDaemon(true);
        t.start();
    }
}
