/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.api;

import edu.mit.broad.genome.XLogger;
import org.apache.ecs.html.LI;
import org.apache.ecs.html.UL;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
// A simple container
public class ToolComments {

    private List fComments;

    private Logger log = XLogger.getLogger(ToolComments.class);

    public ToolComments() {
        this.fComments = new ArrayList();
    }

    public void add(String comment) {
        if (comment != null && comment.length() > 0) {
            comment = comment.trim();
            if (comment.endsWith("\n")) {
                comment = comment.substring(0, comment.length() - 1);
            }
            log.info(comment);
            fComments.add(comment);
        }
    }

    public boolean isEmpty() {
        return fComments.isEmpty();
    }

    public UL toHTML() {
        UL ul = new UL();

        for (int i = 0; i < fComments.size(); i++) {
            ul.addElement(new LI(fComments.get(i).toString()));
        }

        return ul;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < fComments.size(); i++) {
            buf.append(fComments).append('\n');
        }

        return buf.toString();
    }

} // End class ToolComments
