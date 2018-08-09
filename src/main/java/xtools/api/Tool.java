/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.reports.api.Report;
import xtools.api.param.ParamSet;

import javax.swing.*;
import java.io.Serializable;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Tool extends Serializable {

    public static final Icon ICON = JarResources.getIcon("Tool16.gif");

    public String getHelpURL();

    /**
     * @return The name of this Tool
     */
    public String getName();

    public String getTitle();

    /**
     * @return A short description of this tool
     */
    public String getDesc();

    /**
     * @return The category that this tool belongs to
     */
    public ToolCategory getCategory();

    /**
     * @return ParamSet that this tools accepts/needs to execute
     */
    public ParamSet getParamSet();

    /**
     * State the Param (reqd and opt) that the Tool uses.
     */
    public void declareParams();

    /**
     * Actually run the Tool
     */
    public void execute() throws Exception;

    /**
     * Retrieve reports of results of execution of the tool.
     * The Report may have nothing in it but canNOT be null
     */
    public Report getReport();

}    // End Tool
