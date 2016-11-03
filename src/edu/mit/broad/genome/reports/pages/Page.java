/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.pages;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * @author Aravind Subramanian
 */
public interface Page extends Serializable {

    public String getName();

    public String getExt();

    public void write(final OutputStream os) throws Exception;

} // End class Page
