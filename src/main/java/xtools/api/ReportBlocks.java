/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.HtmlFormat;
import org.apache.ecs.html.Div;
import org.apache.ecs.html.H4;
import org.apache.ecs.html.LI;
import org.apache.ecs.html.UL;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class ReportBlocks {

    private static final Logger klog = XLogger.getLogger(ReportBlocks.class);

    // a report listing section that simply lists the elements in a block
    public static class SimpleBlockListing {

        private ToolReport fReport;

        private Div fDiv;

        private UL fUl;

        private boolean fClosed;

        /**
         * Class constructor
         *
         * @param title
         * @param report
         */
        public SimpleBlockListing(final String title, final ToolReport report) {

            if (report == null) {
                throw new IllegalArgumentException("Param report cannot be null");
            }

            this.fReport = report;

            this.fDiv = new Div();
            H4 h4 = new H4(title);
            this.fDiv.addElement(h4);
            this.fUl = new UL();
        }

        public void add(final String hyperLinkThisTerm, final File file) {
            if (fClosed) {
                klog.warn("Already closed for: " + hyperLinkThisTerm + " " + file);
            }

            this.fUl.addElement(new LI(HtmlFormat.Links.hyper(hyperLinkThisTerm, file, "", fReport.getReportDir())));
        }

        public void close() {
            if (fClosed) {
                return;
            }

            this.fDiv.addElement(fUl);
            this.fReport.getIndexPage().addBlock(fDiv, false);
            fClosed = true;
        }

    } // End class SimpleBlockListing
}
