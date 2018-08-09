/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import xapps.gsea.GseaWebResources;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.net.ftp.FileTransferInputStream;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.core.api.Application;

/**
 * Encapsulates the action of downloading a single URL from our FTP servers as a
 * Command object. Instances are not meant to be reused.
 * 
 * @author David Eby
 */
public class FtpSingleUrlTransferCommand {
    private static final Logger klog = XLogger.getLogger(FtpSingleUrlTransferCommand.class);

    private final FileTransferClient client;
    private final URL ftpURL;
    private String fileName;
    private FtpProgressMonitor ftpProgressMonitor = null;

    public FtpSingleUrlTransferCommand(URL ftpURL) throws FTPException, IOException {
        client = new FileTransferClient();
        this.ftpURL = ftpURL;
        initClient();

        // Use a progress monitor if we are running in a UI, but skip it for
        // headless operation (command-line, GP modules).
        try {
            Application.getWindowManager();
            ftpProgressMonitor = new FtpProgressMonitor(this);
            client.setEventListener(ftpProgressMonitor);
        } catch (Exception e) {
            // Any CLI-based Application will throw an exception on the
            // Application.getWindowManager() call.
            // TODO: Improve this. Is there a better way know that we're running
            // in the UI vs. CLI? It seems like checking the java.awt.headless
            // property would do the trick but it fails.
        }
    }

    public FileTransferClient getClient() {
        return client;
    }

    public String getFileName() {
        return fileName;
    }

    private void initClient() throws FTPException, IOException {
        if (!ftpURL.getProtocol().equalsIgnoreCase("ftp")) {
            throw new IllegalArgumentException("Expected ftp url");
        }

        String host = GseaWebResources.getGseaFTPServer();
        if (!ftpURL.getHost().equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Unexpected hostname: " + ftpURL.getHost());
        }
        String fullPath = ftpURL.getPath();
        fileName = FilenameUtils.getName(fullPath);
        String remoteDirectory = FilenameUtils.getFullPath(fullPath);

        client.setRemoteHost(host);
        client.setUserName(GseaWebResources.getGseaFTPServerUserName());
        client.setPassword(GseaWebResources.getGseaFTPServerPassword());

        // connect to the server
        setMonitorNote("Connecting...");
        client.connect();
        setMonitorNote("Connected");

        // set transfer type to binary
        client.setContentType(FTPTransferType.BINARY);

        // set connect mode to Passive
        client.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);

        // change to directory containing msigdb
        client.changeDirectory(remoteDirectory);
    }

    public FtpResultInputStream retrieveAsInputStream() throws FTPException, IOException {
        klog.info("File download started.  Retrieving " + fileName + " from remote server...");
        setMonitorNote("Starting download");
        try {
            try {
                if (ftpProgressMonitor != null) {
                    ftpProgressMonitor.initSize(client.getSize(fileName));
                }

                FileTransferInputStream ftpStream = null;
                try {
                    File dest = File.createTempFile("gsea", "tmp_item");
                    ftpStream = client.downloadStream(fileName);

                    GzipCompressorOutputStream out = null;
                    try {
                        out = new GzipCompressorOutputStream(new BufferedOutputStream(
                                new FileOutputStream(dest)));
                        IOUtils.copy(ftpStream, out);
                    } finally {
                        if (out != null) out.close();
                    }

                    if (monitorCancelled()) {
                        klog.info("File transfer cancelled.");
                        return null;
                    } else {
                        klog.info("Download complete");
                        return new FtpResultInputStream(dest, true);
                    }
                } finally {
                    if (ftpStream != null) ftpStream.close();
                }
            } finally {
                if (client.isConnected()) {
                    client.disconnect();
                }
            }
        } finally {
            if (ftpProgressMonitor != null) ftpProgressMonitor.close();
        }
    }

    private void setMonitorNote(String note) {
        if (ftpProgressMonitor == null) return;
        ftpProgressMonitor.setNote(note);
    }

    private boolean monitorCancelled() {
        return (ftpProgressMonitor != null && ftpProgressMonitor.isCanceled());
    }
}
