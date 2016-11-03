/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

import java.io.File;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         Defines things that make an enrichment result report
 */
public interface EnrichmentReport {

    public File getESPlotFile();

    public File getHtmlFile();

    // @todo add more stuff??


} // End class EnrichmentResultReport