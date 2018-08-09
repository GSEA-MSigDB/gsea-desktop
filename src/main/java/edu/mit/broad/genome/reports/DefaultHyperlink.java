/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.objects.strucs.Hyperlink;

/**
 * @author Aravind Subramanian
 */
public class DefaultHyperlink implements Hyperlink {

    private String fDispName;

    private String fUrl;

    /**
     * Class constructor
     *
     * @param dispName
     * @param url
     */
    public DefaultHyperlink(final String dispName, final String url) {
        this.fDispName = dispName;
        this.fUrl = url;
    }

    public String getURL() {
        return fUrl;
    }

    public String getDisplayName() {
        return fDispName;
    }

} // End class DefaultHyperlink
