/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads a single HTTP(S) URL as an {@link InputStream}. Replaces the FTP-specific
 * download+progress machinery (formerly {@code FtpSingleUrlTransferCommand}/
 * {@code FtpProgressMonitor}/{@code FtpResultInputStream}) now that all downloads -- MSigDB and
 * otherwise -- are HTTP(S)-only. Streams the response directly rather than buffering to a temp
 * file first.
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
        return connection.getInputStream();
    }
}
