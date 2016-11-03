/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.misc_actions;

import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.actions.FileAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

import java.io.File;

/**
 * Extract the row names of a dataset as a grp file
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class GeneSetRemoveDuplicatesAction extends FileAction {

    private File fFile;

    /**
     * Class constructor
     *
     * @param name
     * @param id
     * @param desc
     */
    public GeneSetRemoveDuplicatesAction() {
        super("GeneSetRemoveDuplicatesAction", "=> Remove duplicates from the GeneSet", 
                "Remove duplicate members from the gene set. The file will be overwritten with the new data.", 
              null);
    }

    /**
     * Only works on Template files
     *
     * @param file
     */
    public void setFile(final File file) {
        this.fFile = file;
    }

    public Widget getWidget() {

        // the load is non-blocking so better to use a null widget mechanism
        Widget widget = null;

        try {

            if (fFile != null) {

                if (!fFile.getName().endsWith(".grp")) {
                    Application.getWindowManager().showError("Only .grp files allowed - cannot perform this action on file: " + fFile);
                    return null;
                }

                int before = ParseUtils.countLines(fFile, true);
                GeneSet gset = (GeneSet) ParserFactory.read(fFile);
                // save it into the same file -- auto removes the duplicates
                ParserFactory.save(gset, fFile);
                Application.getWindowManager().showMessage("Successfully removed duplicates from the GeneSet. Before: " + before + " after: " + gset.getNumMembers());

            } else {
                TraceUtils.showTrace();
                Application.getWindowManager().showMessage("No file or object to work on was specified: " + fFile);
            }

        } catch (Throwable t) {
            Application.getWindowManager().showError("Error removing duplicates from GeneSet", t);
        }

        return widget;
    }
}    // End GeneSetRemoveDuplicatesAction
