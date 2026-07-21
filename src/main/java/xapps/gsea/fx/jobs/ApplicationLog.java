/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * Unattributed application messages (not tied to a job). Owned by {@link JobRuntime}.
 */
public final class ApplicationLog {

    private static final int MAX_CHARS = 512_000;
    private static final String BANNER = "< Application messages will appear below >\n\n";

    private final StringBuilder history = new StringBuilder(BANNER);
    private volatile TextArea activeArea;

    public void setActiveArea(TextArea area) {
        this.activeArea = area;
        if (area != null) {
            area.setText(history.toString());
        }
    }

    public String getHistoryText() {
        return history.toString();
    }

    public void append(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        runOnFx(() -> {
            history.append(chunk);
            if (history.length() > MAX_CHARS) {
                history.delete(0, history.length() - MAX_CHARS);
            }
            TextArea area = activeArea;
            if (area != null) {
                area.appendText(chunk);
                area.positionCaret(area.getLength());
            }
        });
    }

    public void clear() {
        runOnFx(() -> {
            history.setLength(0);
            history.append(BANNER);
            TextArea area = activeArea;
            if (area != null) {
                area.setText(history.toString());
            }
        });
    }

    private void runOnFx(Runnable action) {
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
