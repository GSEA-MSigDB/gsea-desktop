/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import javafx.scene.control.TextArea;

/**
 * Unattributed application messages (not tied to a job). Owned by {@link JobRuntime}.
 * Thin façade over {@link LogBuffer} with optional live {@link TextArea} mirroring.
 */
public final class ApplicationLog {

    private static final String BANNER = "< Application messages will appear below >\n\n";

    private final LogBuffer buffer = new LogBuffer(BANNER);
    private volatile TextArea activeArea;

    public ApplicationLog() {
        buffer.addListener(this::onBufferChunk);
    }

    public void setActiveArea(TextArea area) {
        this.activeArea = area;
        if (area != null) {
            area.setText(buffer.getText());
        }
    }

    public String getHistoryText() {
        return buffer.getText();
    }

    public void append(String chunk) {
        buffer.append(chunk);
    }

    public void clear() {
        buffer.clear();
    }

    private void onBufferChunk(String chunkOrNull) {
        TextArea area = activeArea;
        if (area == null) {
            return;
        }
        if (chunkOrNull == null) {
            area.setText(buffer.getText());
            return;
        }
        area.appendText(chunkOrNull);
        area.positionCaret(area.getLength());
    }
}
