/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.esmatrix.db;

/**
 * @author Aravind Subramanian
 */
public class EnrichmentReportDbImpl {

    private EnrichmentReport[] fReports;

    /**
     * Class constructor
     *
     * @param reports
     */
    public EnrichmentReportDbImpl(final EnrichmentReport[] reports) {
        this.fReports = reports;
    }

} // End class EnrichmentReportDbImpl
