/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.xchoosers;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

/**
 * Inner class for a bag of templates possibly from seperate source files
 */
public class TemplateSelectionMultiSource extends TemplateSelection {

    protected Logger log = XLogger.getLogger(TemplateSelectionMultiSource.class);

    // IMP IMP IMP
    // Override sub-class as we want the full paths for ALL templates
    // format is:
    // fullpath2mainTemplate#foo,fullpath2mainTemplate#bar ...
    // OVA and ALL_PAIRS are magic strings that expand
    public String formatForUI() {

        if (fTemplateNamesOrPaths == null) {
            return null;
        }

        //klog.debug("TemplatePaths: " + fTemplatePaths);

        String[] vals = (String[]) fTemplateNamesOrPaths.toArray(new String[fTemplateNamesOrPaths.size()]);

        if (vals == null || vals.length == 0) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                continue;
            }

            buf.append(vals[i]);

            if (i != vals.length - 1) {
                buf.append(',');
            }
        }

        log.debug("Got combo string: " + buf.toString());
        return buf.toString();
    }

} // End inner class MultiSource
