/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;

import java.io.File;
import java.util.Date;

/**
 * @author Aravind Subramanian
 */
public class ReportStub {

    private String fName;
    private long fTimestamp;
    private transient Date fDate;

    private File fReportFile;

    private Report fReport;

    private String fTsLessName;

    /**
     * @param rptFile
     */
    public ReportStub(final File rptFile) {
        this.fReportFile = rptFile;
        this.fName = fReportFile.getName();

        final Object[] rets = NamingConventions.parseReportTimestampFromName(fName);
        this.fTimestamp = ((Long) rets[1]).longValue();

        this.fTsLessName = rets[0].toString();
    }

    public String getName() {
        return fName;
    }

    public String getName_without_ts() {
        return fTsLessName;
    }

    public long getTimestamp() {
        return fTimestamp;
    }
    
    public Date getDate() {
        if (fDate == null) {
            fDate = new Date(fTimestamp);
        }
        return fDate;
    }

    public File getReportFile() {
        return fReportFile;
    }

    public Report getReport(boolean useCache) throws Exception {
        if (fReport == null) {
            fReport = ParserFactory.readReport(fReportFile, useCache);
        }

        return fReport;
    }
} // End class ReportStub
