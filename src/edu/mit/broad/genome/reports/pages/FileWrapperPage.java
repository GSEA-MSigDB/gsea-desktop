/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.pages;

import edu.mit.broad.genome.NamingConventions;
import org.apache.ecs.html.A;

import java.io.File;
import java.io.OutputStream;

public class FileWrapperPage implements Page {

    private String fDesc;
    private String fName;
    private File fFile;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public FileWrapperPage(File file, String desc) {
        this.fDesc = desc;
        this.fName = file.getName();
        this.fFile = file;
    }

    public File getFile() {
        return fFile;
    }

    public String getName() {
        return fName;
    }

    public String getExt() {
        return NamingConventions.getExtension(fFile);
    }

    public String getDesc() {
        return fDesc;
    }

    public A createLink(final File rptDir, final String optStandardBase) {
        A link = new A().addElement(getName());

        if (optStandardBase != null) {
            HtmlFormat.Links.setHref(link, getFile(), optStandardBase + "/" + rptDir.getName());
        } else {
            //log.debug("report dir is: " + fReportDir);
            HtmlFormat.Links.setHref(link, getFile(), rptDir);
        }
        return link;
    }

    public void write(OutputStream os) throws Exception {
        // does nothing
    }
} // End class FileWrapperPage
