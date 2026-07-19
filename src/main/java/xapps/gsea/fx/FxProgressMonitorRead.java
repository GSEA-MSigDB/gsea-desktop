/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.application.Platform;

/**
 * JavaFX stand-in for Swing {@code ProgressMonitorInputStream}: modal progress dialog with Cancel
 * while parsing a single file on a background thread.
 */
public final class FxProgressMonitorRead {

    private FxProgressMonitorRead() {
    }

    /**
     * Parse {@code file} with a cancelable progress dialog. Must not be called on the FX
     * application thread (would deadlock waiting for the dialog to show).
     */
    public static PersistentObject read(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (Platform.isFxApplicationThread()) {
            throw new IllegalStateException("FxProgressMonitorRead.read must run off the FX thread");
        }

        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicReference<FxCancellableProgressDialog> dialogRef = new AtomicReference<>();

        Platform.runLater(() -> {
            FxCancellableProgressDialog dialog = FxCancellableProgressDialog.withCancelFlag(
                    "Loading: " + file.getName(),
                    file.getAbsolutePath(),
                    FxCancellableProgressDialog.firstShowingOwner(),
                    cancelled);
            dialogRef.set(dialog);
            dialog.show();
            ready.countDown();
        });

        if (!ready.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for load progress dialog");
        }

        final long fileLen = Math.max(1L, file.length());
        try (java.io.InputStream in = new FilterInputStream(new FileInputStream(file)) {
            private long bytesRead;

            private void checkCancelled() throws IOException {
                if (cancelled.get()) {
                    throw new InterruptedIOException("Load cancelled");
                }
            }

            private void note(int n) {
                if (n <= 0) {
                    return;
                }
                bytesRead += n;
                final double frac = Math.min(1.0, bytesRead / (double) fileLen);
                FxCancellableProgressDialog d = dialogRef.get();
                if (d != null) {
                    d.setFraction(frac);
                }
            }

            @Override
            public int read() throws IOException {
                checkCancelled();
                int b = super.read();
                if (b >= 0) {
                    note(1);
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                checkCancelled();
                int n = super.read(b, off, len);
                note(n);
                return n;
            }
        }) {
            return ParserFactory.read(file.getPath(), in);
        } finally {
            FxCancellableProgressDialog d = dialogRef.get();
            if (d != null) {
                d.close();
            }
        }
    }
}
