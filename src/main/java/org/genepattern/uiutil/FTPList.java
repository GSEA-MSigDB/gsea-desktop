/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.uiutil;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class FTPList {
    private FileTransferClient ftpClient;
    public String host;

    public FTPList(final String host, final String username, final String password) throws IOException, FTPException, IllegalArgumentException {
        ftpClient = new FileTransferClient();
        this.host = host;

        // set remote host
        ftpClient.setRemoteHost(host);
        ftpClient.setUserName(username);
        ftpClient.setPassword(password);

        // connect to the server
        ftpClient.connect();

        // set connect mode to Passive
        ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
    }
    
    public String[] getDirectoryListing(final String dir, final Comparator comp) throws IOException, FTPException {
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
