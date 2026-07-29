/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ProgressMonitorInputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.xbench.core.api.Application;

/**
 * Downloads a single HTTP(S) URL, showing a Swing progress dialog (with cancel support, via the
 * standard {@link ProgressMonitorInputStream}) when running in the UI; skipped for headless/CLI
 * use, matching the old FTP download path's UI-detection convention. Replaces the FTP-specific
 * download+progress machinery (formerly {@code FtpSingleUrlTransferCommand}/
 * {@code FtpProgressMonitor}/{@code FtpResultInputStream}) now that all downloads -- MSigDB and
 * otherwise -- are HTTP(S)-only. Unlike that FTP path, this streams the response directly rather
 * than buffering it to a temp file first; that indirection was specific to the old FTP client
 * library and is not needed here.
 *
 * @author David Eby
 */
public class HttpDownloadCommand {
    private static final Logger klog = LoggerFactory.getLogger(HttpDownloadCommand.class);

    // A connect timeout is safe to enforce (fail fast if the host is unreachable); a read
    // timeout is deliberately not set, since a large, legitimately slow-but-working collection
    // download should not be aborted mid-transfer.
    private static final int CONNECT_TIMEOUT_MS = 15000;

    private HttpDownloadCommand() { }

    public static InputStream retrieveAsInputStream(URL url) throws IOException {
        String fileName = FilenameUtils.getName(url.getPath());
        klog.info("File download started.  Retrieving {} from {}...", fileName, url);

        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        InputStream rawStream = connection.getInputStream();

        if (!isUiMode()) {
            return rawStream;
        }

        ProgressMonitorInputStream pis = new ProgressMonitorInputStream(
                Application.getWindowManager().getRootFrame(), "Downloading " + fileName, rawStream);
        // Set a precise maximum from the Content-Length header when the server reports one;
        // otherwise leave ProgressMonitorInputStream's own (less precise) default in place.
        long contentLength = connection.getContentLengthLong();
        if (contentLength > 0 && contentLength <= Integer.MAX_VALUE) {
            pis.getProgressMonitor().setMaximum((int) contentLength);
        }
        // Cancellation is handled by ProgressMonitorInputStream itself: once the user cancels,
        // the next read() throws InterruptedIOException (an IOException), which naturally
        // propagates up through the caller's normal parsing/close logic.
        return pis;
    }

    // Any CLI-based Application will throw an exception on the Application.getWindowManager()
    // call; skip the progress dialog entirely in that case, same convention previously used by
    // FtpSingleUrlTransferCommand.
    private static boolean isUiMode() {
        try {
            Application.getWindowManager();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
