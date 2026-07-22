/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import xapps.gsea.fx.FxThreads;

/**
 * Capped append-only log buffer. Listeners are notified on the FX thread
 * ({@code null} chunk means reload full text — clear or post-truncation).
 */
public final class LogBuffer {

    private static final int MAX_CHARS = 512_000;
    private static final String TRUNCATION_MARKER = "…[log truncated]\n";

    private final String initialBanner;
    private final StringBuilder text = new StringBuilder();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public LogBuffer() {
        this(null);
    }

    public LogBuffer(String initialBanner) {
        this.initialBanner = initialBanner != null ? initialBanner : "";
        if (!this.initialBanner.isEmpty()) {
            text.append(this.initialBanner);
        }
    }

    public synchronized void append(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        text.append(chunk);
        boolean truncated = trimIfNeeded();
        // After truncation, force a full reload so live TextAreas stay in sync with getText().
        notifyListeners(truncated ? null : chunk);
    }

    public synchronized void clear() {
        text.setLength(0);
        if (!initialBanner.isEmpty()) {
            text.append(initialBanner);
        }
        notifyListeners(null);
    }

    public synchronized String getText() {
        return text.toString();
    }

    /** {@code null} chunk means reload full text (cleared or truncated). */
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

    /** @return true if the buffer was trimmed */
    private boolean trimIfNeeded() {
        if (text.length() <= MAX_CHARS) {
            return false;
        }
        int excess = text.length() - MAX_CHARS;
        text.delete(0, excess);
        if (!startsWithTruncationMarker()) {
            text.insert(0, TRUNCATION_MARKER);
        }
        return true;
    }

    private boolean startsWithTruncationMarker() {
        int n = TRUNCATION_MARKER.length();
        if (text.length() < n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (text.charAt(i) != TRUNCATION_MARKER.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void notifyListeners(String chunkOrNull) {
        if (listeners.isEmpty()) {
            return;
        }
        FxThreads.runLater(() -> {
            for (Consumer<String> listener : listeners) {
                try {
                    listener.accept(chunkOrNull);
                } catch (Exception ignored) {
                }
            }
        });
    }
}
