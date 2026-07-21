/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;

/**
 * Capped append-only log buffer for a single job. Listeners are notified on the FX thread.
 */
public final class JobLogBuffer {

    private static final int MAX_CHARS = 512_000;

    private final StringBuilder text = new StringBuilder();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private volatile String lastLine = "";

    public synchronized void append(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        text.append(chunk);
        trimIfNeeded();
        updateLastLine(chunk);
        String snapshot = chunk;
        notifyListeners(snapshot);
    }

    public synchronized void clear() {
        text.setLength(0);
        lastLine = "";
        notifyListeners(null);
    }

    public synchronized String getText() {
        return text.toString();
    }

    public String getLastLine() {
        return lastLine;
    }

    /** {@code null} chunk means the buffer was cleared (reload full text). */
    public void addListener(Consumer<String> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<String> listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void trimIfNeeded() {
        if (text.length() <= MAX_CHARS) {
            return;
        }
        int excess = text.length() - MAX_CHARS;
        text.delete(0, excess);
        // Keep a marker so truncated history is obvious.
        if (text.length() > 0 && text.charAt(0) != '…') {
            text.insert(0, "…[log truncated]\n");
        }
    }

    private void updateLastLine(String chunk) {
        int from = chunk.lastIndexOf('\n');
        String candidate;
        if (from >= 0 && from + 1 < chunk.length()) {
            candidate = chunk.substring(from + 1).trim();
        } else if (from >= 0) {
            // Chunk ended with newline — look at prior content in the buffer.
            String all = text.toString();
            int end = all.length();
            while (end > 0 && all.charAt(end - 1) == '\n') {
                end--;
            }
            int start = all.lastIndexOf('\n', end - 1) + 1;
            candidate = all.substring(Math.max(0, start), end).trim();
        } else {
            candidate = chunk.trim();
        }
        if (!candidate.isEmpty()) {
            lastLine = candidate;
        }
    }

    private void notifyListeners(String chunkOrNull) {
        if (listeners.isEmpty()) {
            return;
        }
        Runnable r = () -> {
            for (Consumer<String> listener : listeners) {
                try {
                    listener.accept(chunkOrNull);
                } catch (Exception ignored) {
                }
            }
        };
        try {
            if (Platform.isFxApplicationThread()) {
                r.run();
            } else {
                Platform.runLater(r);
            }
        } catch (IllegalStateException noToolkit) {
            // Unit tests / headless — notify on the calling thread.
            r.run();
        }
    }
}
