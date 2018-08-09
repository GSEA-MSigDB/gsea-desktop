/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.api;

/**
 * @author Aravind Subramanian
 */
public class ReportIndexState {

    public static ReportIndexState NO_REPORT_INDEX = new ReportIndexState(false, false, false, null);

    private boolean fMakeReportIndexPage;

    // Maintain a html index automatically or not (might choose to disable if custom reporting)
    private boolean fKeepTrackOfPagesInHtmlIndex;

    private String fHeaderOpt;

    // Central add pages or not (might choose to disable if pages are large memory)
    private boolean fKeepTrackOfPages = true;

    public ReportIndexState(boolean makeReportIndexPage,
                            final String headerOpt) {
        this(makeReportIndexPage, true, true, headerOpt);
    }

    public ReportIndexState(boolean makeReportIndexPage,
                            boolean keepTrackOfPagesInHtmlIndex,
                            boolean keepTrackOfPages,
                            final String headerOpt) {

        this.fMakeReportIndexPage = makeReportIndexPage;
        this.fKeepTrackOfPagesInHtmlIndex = keepTrackOfPagesInHtmlIndex;
        this.fKeepTrackOfPages = keepTrackOfPages;
        this.fHeaderOpt = headerOpt;
    }

    public boolean makeReportIndexPage() {
        return fMakeReportIndexPage;
    }

    public boolean keepTrackOfPagesInHtmlIndex() {
        return fKeepTrackOfPagesInHtmlIndex;
    }

    public boolean keepTrackOfPages() {
        return fKeepTrackOfPages;
    }

    public String getHeader() {
        return fHeaderOpt;
    }

    public String toString() {
        return "fMakeReportIndexPage=" + fMakeReportIndexPage + ",fKeepTrackOfPagesInHtmlIndex=" + fKeepTrackOfPagesInHtmlIndex +
                ",fKeepTrackOfPages=" + fKeepTrackOfPages + ",fHeaderOpt=" + fHeaderOpt;
    }

} // End class ReportIndexState
