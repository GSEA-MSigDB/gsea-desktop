/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions.misc_actions;

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
public class GeneSetMatrix2GeneSetsAction extends FileObjectAction {

    private Object fFileOrObject;

    /**
     * Class constructor
     *
     * @param name
     * @param id
     * @param desc
     */
    public GeneSetMatrix2GeneSetsAction() {
        super("GeneSetMatrix2GeneSetsAction", "=> Extract GeneSets from the GeneSetMatrix", 
                "Extract the GeneSets from the GeneSetMatrix", null);
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
     * Only works on Template objects
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

                ParserFactory.extractGeneSets(gm);

                Application.getWindowManager().showMessage("Successfully created " + gm.getNumGeneSets()
                        + " GeneSets from the GeneSetMatrix " + gm.getName());

            } else {
                Application.getWindowManager().showMessage("No file or object to work on was specified");
            }

        } catch (Throwable t) {
            Application.getWindowManager().showError("Error creating GeneSets from GeneSetMatrix", t);
        }

        return widget;
    }
}    // End GeneSetMatrix2GeneSetsAction
