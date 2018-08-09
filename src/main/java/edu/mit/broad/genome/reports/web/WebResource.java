/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.web;

/**
 * @author Aravind Subramanian
 */
public interface WebResource {

    public String getName();

    // always of the form: prefix=
    public String getUrlPrefix();

} // End interface WebCgiResource
