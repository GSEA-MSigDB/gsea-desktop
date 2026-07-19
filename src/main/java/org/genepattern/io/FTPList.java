/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.genepattern.io;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Thin edtftpj wrapper for listing remote MSigDB directories.
 */
public class FTPList {
    private final FileTransferClient ftpClient;
    public final String host;

    public FTPList(final String host, final String username, final String password)
            throws IOException, FTPException, IllegalArgumentException {
        ftpClient = new FileTransferClient();
        this.host = host;

        ftpClient.setRemoteHost(host);
        ftpClient.setUserName(username);
        ftpClient.setPassword(password);

        ftpClient.connect();
        ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
    }

    public String[] getDirectoryListing(final String dir, final Comparator<String> comp)
            throws IOException, FTPException {
        ftpClient.changeDirectory(dir);
        String[] fileNames = ftpClient.directoryNameList();
        if (comp != null && fileNames != null && fileNames.length > 0) {
            Arrays.parallelSort(fileNames, comp);
        }
        return fileNames;
    }

    public void quit() throws IOException, FTPException {
        ftpClient.disconnect();
    }
}
