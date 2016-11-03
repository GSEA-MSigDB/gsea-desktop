/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.misc_actions;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.actions.FileObjectAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;

import java.io.File;

/**
 * Extract the row names of a dataset as a grp file
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetMatrix2GeneSetAction extends FileObjectAction {

    private Object fFileOrObject;

    /**
     * Class constructor
     *
     * @param name
     * @param id
     * @param desc
     */
    public GeneSetMatrix2GeneSetAction() {
        super("GeneSetMatrix2GeneSetAction", "=> Convert the GeneSetMatrix into a single GeneSet", 
              "Convert the GeneSetMatrix into a single GeneSet (eliminates duplicates)", null);
    }

    /**
     * Only works on Template files
     *
     * @param file
     */
    public void setFile(File file) {
        this.fFileOrObject = file;
    }

    /**
     * Only works on GeneSetMatrix objects
     *
     * @param obj
     */
    public void setObject(Object obj) {
        this.fFileOrObject = obj;

    }

    public Widget getWidget() {

        // the load is non-blocking so better to use a null widget mechanism
        Widget widget = null;

        try {

            if (fFileOrObject != null) {
                GeneSetMatrix gm;
                if (fFileOrObject instanceof File) {
                    gm = (GeneSetMatrix) ParserFactory.read((File) fFileOrObject);
                } else if (fFileOrObject instanceof GeneSetMatrix) {
                    gm = (GeneSetMatrix) fFileOrObject;
                } else {
                    throw new IllegalArgumentException("Only GeneSetMatrix or File Objects allowed. Got: " + fFileOrObject);
                }

                final GeneSet gset = ParserFactory.combineIntoOne(gm);
                final File tmp = new File(Application.getVdbManager().getTmpDir(), gset.getName(true));
                ParserFactory.save(gset, tmp);
                Application.getWindowManager().showMessage("Successfully created a GeneSet from the GeneSetMatrix " + gm.getName() + " into: " + tmp.getPath());

            } else {
                Application.getWindowManager().showMessage("No file or object to work on was specified");
            }

        } catch (Throwable t) {
            Application.getWindowManager().showError("Error creating a GeneSet from GeneSetMatrix", t);
        }

        return widget;
    }
}    // End GeneSetMatrix2GeneSetAction
