/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import edu.mit.broad.genome.io.FtpProgressMonitor;
import edu.mit.broad.genome.io.FtpSingleUrlTransferCommand;
import javafx.stage.Window;

/**
 * JavaFX progress/cancel UI for FTP downloads, replacing Swing {@code ProgressMonitor}
 * chrome while reusing {@link FtpProgressMonitor}'s cancel/disconnect behavior.
 */
public final class FxFtpProgressDialog {

    private final FxCancellableProgressDialog ui;
    private final FtpProgressMonitor monitor;

    public FxFtpProgressDialog(FtpSingleUrlTransferCommand command, FtpProgressMonitor monitor, Window owner) {
        this.monitor = monitor;
        String title = "Retrieving file "
                + (command != null && command.getFileName() != null ? command.getFileName() : "");
        // Swing ProgressMonitor note (ASCII ellipsis).
        ui = new FxCancellableProgressDialog(title, "Initializing connection...", owner, this::cancelTransfer);
    }

    private void cancelTransfer() {
        if (monitor != null) {
            monitor.cancel();
        }
    }

    public void show() {
        ui.show();
    }

    /**
     * @param percent 0–100, or negative to update note only
     */
    public void setProgress(int percent, String noteText) {
        ui.setProgress(percent, noteText);
    }

    public void close() {
        ui.close();
    }
}
