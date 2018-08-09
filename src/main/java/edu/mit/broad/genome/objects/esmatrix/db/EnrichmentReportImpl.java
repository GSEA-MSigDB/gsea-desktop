/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class EnrichmentReportImpl implements EnrichmentReport {

    private File fPlotFile;

    private File fHtmlFile;

    public EnrichmentReportImpl(final File htmlFile, final File plotFile) {
        this.fPlotFile = plotFile;
        this.fHtmlFile = htmlFile;
    }


    public File getESPlotFile() {
        return fPlotFile;
    }

    public File getHtmlFile() {
        return fHtmlFile;
    }


} // End class EnrichmentResultReportImpl
