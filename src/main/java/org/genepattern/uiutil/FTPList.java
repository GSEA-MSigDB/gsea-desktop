/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.uiutil;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;

import javax.swing.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FTPList extends JList {

    private FileTransferClient ftpClient;
    private String[] fFileNames;

    public FTPList(final String host, final String username, final String password, final String dir, final Comparator comp_opt) throws IOException, FTPException, IllegalArgumentException {

        ftpClient = new FileTransferClient();

        // set remote host
        ftpClient.setRemoteHost(host);
        ftpClient.setUserName(username);
        ftpClient.setPassword(password);

        // connect to the server
        ftpClient.connect();

        // set connect mode to Passive
        ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);

        ftpClient.changeDirectory(dir);

        this.fFileNames = ftpClient.directoryNameList();

        DefaultListModel model = new DefaultListModel();
        if (fFileNames != null) {

            if (comp_opt != null) {
                List all = Arrays.asList(fFileNames);
                Collections.sort(all, comp_opt);
                this.fFileNames = (String[]) all.toArray(new String[all.size()]);
            }
            for (int i = 0, length = fFileNames.length; i < length; i++) {
                model.addElement(new FTPFile(host, dir, fFileNames[i]));
            }
        }
        setModel(model);
    }

    public void quit() throws IOException, FTPException {
        ftpClient.disconnect();
    }
}