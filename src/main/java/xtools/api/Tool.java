/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xtools.api;

import edu.mit.broad.genome.reports.api.Report;
import xtools.api.param.ParamSet;

import java.io.Serializable;

/**
 * Runnable analysis tool.
 */
public interface Tool extends Serializable {

    String getHelpURL();

    String getName();

    String getTitle();

    String getDesc();

    ToolCategory getCategory();

    ParamSet getParamSet();

    void declareParams();

    void execute() throws Exception;

    Report getReport();
}
