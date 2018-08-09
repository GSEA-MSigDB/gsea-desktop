/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.ext;

import edu.mit.broad.xbench.actions.XAction;
import edu.mit.broad.xbench.core.api.Application;

import javax.swing.Icon;

import java.awt.event.ActionEvent;
import java.awt.Desktop;
import java.net.URI;

/**
 * Action to launch a web browser on a URL String
 * Note that doesn't work if the URL is file path; use FileBrowserAction instead.  
 *
 * @author Aravind Subramanian
 * @author David Eby
 */
public class BrowserAction extends XAction {

    private String urlString;
    
    public BrowserAction(final String name, final String desc, final Icon icon, final String urlString) {
        super("BrowserAction", name, desc, icon);
        setUrlString(urlString);
    }
    
    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            String path = getUrlString();
            if (path == null)
                throw new NullPointerException("null URL associated with BrowserAction");

            URI uri = new URI(path);
            Desktop.getDesktop().browse(uri);

            // TODO: track down whether this registry has any meaning or use.
            Application.getFileManager().registerRecentlyOpenedURL(getUrlString());
        } catch (Exception e) {
            Application.getWindowManager().showError("Could not launch browser", e);
        }
    }
}    // End BrowserAction
