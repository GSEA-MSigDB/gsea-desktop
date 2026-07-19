/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.io;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterprisedt.net.ftp.EventAdapter;
import com.enterprisedt.net.ftp.EventListener;
import com.enterprisedt.net.ftp.FTPException;

import javafx.stage.Window;
import xapps.gsea.fx.FxFtpProgressDialog;
import xapps.gsea.fx.shell.FxWorkspaceWindowManager;
import xapps.gsea.fx.shell.GseaFxShell;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.WindowManager;

/**
 * FTP transfer progress listener with optional JavaFX progress/cancel dialog
 * (Swing {@code ProgressMonitor} parity for the OpenJFX shell).
 */
public class FtpProgressMonitor implements EventListener {
    private static final Logger klog = LoggerFactory.getLogger(FtpProgressMonitor.class);

    private final EventAdapter eventDelegate = new EventAdapter();
    private final FtpSingleUrlTransferCommand ftpCommand;
    private long size = 1L;
    private volatile boolean canceled;
    private FxFtpProgressDialog fxDialog;

    public FtpProgressMonitor(FtpSingleUrlTransferCommand ftpCommand) {
        this.ftpCommand = ftpCommand;
        tryAttachFxDialog();
    }

    private void tryAttachFxDialog() {
        try {
            WindowManager wm = Application.getWindowManager();
            Window owner = null;
            if (wm instanceof FxWorkspaceWindowManager) {
                GseaFxShell shell = ((FxWorkspaceWindowManager) wm).getShell();
                if (shell != null) {
                    owner = shell.getStage();
                }
            }
            fxDialog = new FxFtpProgressDialog(ftpCommand, this, owner);
            fxDialog.show();
        } catch (Throwable t) {
            klog.debug("No FX FTP progress UI: {}", t.toString());
            fxDialog = null;
        }
    }

    public void initSize(long size) {
        this.size = size > 0 ? size : 1L;
    }

    public void setNote(String note) {
        klog.debug("FTP: {}", note);
        if (fxDialog != null) {
            fxDialog.setProgress(-1, note);
        }
    }

    public void close() {
        if (fxDialog != null) {
            fxDialog.close();
            fxDialog = null;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    @Override
    public void bytesTransferred(String connId, String remoteFilename, long count) {
        eventDelegate.bytesTransferred(connId, remoteFilename, count);
        if (this.isCanceled()) {
            klog.info("Cancelling FTP transfer...");
            ftpCommand.getClient().cancelAllTransfers();
            try {
                ftpCommand.getClient().disconnect(true);
            } catch (IOException | FTPException e) {
                klog.error(e.getMessage(), e);
            }
        } else {
            int progressPercent = (int) Math.round(100.0 * count / size);
            if (fxDialog != null) {
                fxDialog.setProgress(progressPercent, progressPercent + "% complete");
            } else if (progressPercent % 10 == 0) {
                klog.debug("FTP {}% complete", progressPercent);
            }
        }
    }

    @Override
    public void commandSent(String connId, String cmd) {
        eventDelegate.commandSent(connId, cmd);
    }

    @Override
    public void downloadCompleted(String connId, String remoteFilename) {
        eventDelegate.downloadCompleted(connId, remoteFilename);
    }

    @Override
    public void downloadStarted(String connId, String remoteFilename) {
        eventDelegate.downloadStarted(connId, remoteFilename);
    }

    @Override
    public void replyReceived(String connId, String reply) {
        eventDelegate.replyReceived(connId, reply);
    }

    @Override
    public void uploadCompleted(String connId, String remoteFilename) {
        eventDelegate.uploadCompleted(connId, remoteFilename);
    }

    @Override
    public void uploadStarted(String connId, String remoteFilename) {
        eventDelegate.uploadStarted(connId, remoteFilename);
    }
}
