/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import java.io.IOException;

import javax.swing.ProgressMonitor;

import org.apache.log4j.Logger;

import com.enterprisedt.net.ftp.EventAdapter;
import com.enterprisedt.net.ftp.EventListener;
import com.enterprisedt.net.ftp.FTPException;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.core.api.Application;

public class FtpProgressMonitor extends ProgressMonitor implements EventListener {
    private static final Logger klog = XLogger.getLogger(FtpProgressMonitor.class);

    // Default handler for EventListener events (other than those we care about)
    private final EventAdapter eventDelegate = new EventAdapter();

    private final FtpSingleUrlTransferCommand ftpCommand;
    private long size = 1l;

    public FtpProgressMonitor(FtpSingleUrlTransferCommand ftpCommand) {
        super(Application.getWindowManager().getRootFrame(), "Retrieving file "
                + ftpCommand.getFileName(), "Initializing connection...", 0, 100);
        this.ftpCommand = ftpCommand;
    }

    public void initSize(long size) {
        this.size = size;
    }

    // This is the only EventListener method which seems to have a direct
    // bearing on our operations. Most of them are never invoked at all, even
    // the ones that you would expect to be (downloadStarted/downloadCompleted).
    // Thus, our workflow around the use of this component is structured
    // explicitly around the way it actually behaves:
    // - Set up the client for download
    // - Create this ProgressMonitor
    // - Start the download; this PM will watch the xfer and update to reflect
    // the progress.
    @Override
    public void bytesTransferred(String connId, String remoteFilename, long count) {
        eventDelegate.bytesTransferred(connId, remoteFilename, count);
        if (this.isCanceled()) {
            klog.info("Cancelling...");
            ftpCommand.getClient().cancelAllTransfers();
            // The above cancellation does not seem to be enough to actually
            // stop the transfer. Totally disconnect instead.
            try {
                ftpCommand.getClient().disconnect(true);
            } catch (IOException ie) {
                // Log and suppress this exception. We'll allow the client to
                // become completely undone in the main workflow and handle it
                // there.
                klog.error(ie);
            } catch (FTPException fe) {
                // Ditto.
                klog.error(fe);
            }
        } else {
            int progressPercent = Math.round(100 * count / size);
            this.setProgress(progressPercent);
            this.setNote(progressPercent + "% complete");
        }
    }

    // None of the following EventListener methods have any impact on our
    // workflow, as stated above. Simply pass these on to the delegate adapter.

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