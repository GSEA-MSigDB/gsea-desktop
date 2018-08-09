/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.actions;

import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.xbench.core.Widget;

import java.io.File;

/**
 * Functions:
 * 1) Acts as a proxy for the real FileAction.
 * <p/>
 * 2) If the Widget action is an ObjectAction, and a File were specified, this class parses the File and gives the
 * resulting object to the ObjectAction.
 * Doesnt parse the File until the popup option is actually selected.
 * <p/>
 * Note that typically actions are used via ActionFactory, so client code may not need to explicitly ever use
 * this class
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ProxyFileAction extends WidgetAction {
    private static final File[] wrapAsFileArray(File file) {
        return new File[]{AuxUtils.getBaseFileFromAuxFile(file)};
    }
    
    private FileAction fAction;
    private File[] fFiles;

    /**
     * Class Constructor.
     *
     * @param action
     */
    public ProxyFileAction(final FileAction action, File file) {
        this(action, wrapAsFileArray(file));
    }

    /**
     * Class constructor
     *
     * @param action
     * @param files
     */
    public ProxyFileAction(FileAction action, File[] files) {
        super(getActionId(action), getActionName(action), 
                getActionDescription(action), getActionIcon(action));

        if (files == null) {
            throw new IllegalArgumentException("Param files cannot be null");
        }

        this.fAction = action;
        this.fFiles = files;
    }

    public Widget getWidget() throws Exception {

        if (fAction instanceof FilesAction) {
            ((FilesAction) fAction).setFiles(fFiles);
        } else {
            fAction.setFile(fFiles[0]);
        }

        return fAction.getWidget();
    }
}    // End ProxyFileAction

