/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.pages;

import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.genome.utils.SystemUtils;
import org.apache.ecs.html.BR;
import org.apache.ecs.html.Div;
import org.apache.ecs.html.LI;
import org.apache.ecs.html.UL;
import xapps.gsea.GseaWebResources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * HtmlPage that is tweaked to serve as an index page for a collection of results
 * <p/>
 * Essentially contains report summary info,  links to the detailed report
 * pages generated.
 * <p/>
 * So, always:
 * <p/>
 * Header
 * Table of Pages made (anme, desc & link)
 * Footer
 * <p/>
 * Optionally the Tool can tap directly into the HtmlIndexPage to add additional stuff
 *
 * @todo 1) add more footer info
 * 2) make this aware of any errors in the tool
 * 3) add xtools version info to the report someplace (so that bug/error reports can contain that)
 * 4) make the footer page a link to the specific help on the web page for that tool
 */

// prefer to not extend as dont want the panoply of methods available in HtmlPage. Better to encapsulate
// ACTUALLY reverted as its just too useful to have access to the htmlpage methods as sometimes
// the indexpage has much more than just the bare bones index
public class HtmlReportIndexPage extends HtmlPage {

    private ToolReport fToolReport;

    private List fErrors;

    /**
     * Class constructor
     *
     * @param name
     * @param title
     */
    public HtmlReportIndexPage(final ToolReport toolReport, final String headerOpt) {
        this(toolReport, null, headerOpt);
    }

    public HtmlReportIndexPage(final ToolReport toolReport, final String specialSuffix, final String headerOpt) {
        super("index", _makeTitle(toolReport, specialSuffix));

        this.fToolReport = toolReport;
        addHeader(headerOpt);
    }

    private static String _makeTitle(ToolReport toolReport, String specialSuffix) {

        String title = "Index for " + toolReport.getProducerName() + " " + toolReport.getReportDir().getName();
        if (specialSuffix != null && specialSuffix.length() > 0) {
            title = title + " " + specialSuffix;
        }

        return title;
    }

    public void addError(String error) {
        if (fErrors == null) {
            fErrors = new ArrayList();
        }
        fErrors.add(error);
    }

    private void addHeader(final String headerOpt) {
        if (headerOpt == null) {
            return;
        }

        getDoc().appendBody(headerOpt);
    }


    /* Example of more stuff
    <hr align="center">
    Report produced by xtools.Gsea on April 24, 2004 8:09PM
    <br>For further usage reference and developer documentation,
    see <link>http://some.com/foo</link>.
    Submit a bug or feature to indev@test.mail
    */
    // as this is mostly canned, dont bother making this in an OOP fashion
    // simply make as a string and set
    private boolean fFooterAdded;

    private boolean browseFooter = true;

    public void setAddBrowseFooter(boolean addBrowseFolder) {
        this.browseFooter = addBrowseFolder;
    }

    // @todo think about dependency to xapps pkg
    private void addFooter() {
        if (!fFooterAdded) {

            // First the standard tail of a report
            Div div = new Div();
            div.addElement(new BR());
            //div.addElement(new HR());
            if (browseFooter) {
                UL ul = new UL();
                ul.addElement(new LI(HtmlFormat.Links.hyperDir("Browse result folder", fToolReport.getReportDir(), "")));
                ul.addElement(new LI(HtmlFormat.Links.hyper("View parameters used for this report", fToolReport.getParamsFile(), "", fToolReport.getReportDir())));
                div.addElement(ul);
            }
            addBlock(div, false);

            // Then the yellow footer
            String date = DateUtils.formatAsDayMonthYear(fToolReport.getDate()) + " " + DateUtils.formatAsHourMin(fToolReport.getDate());
            StringBuffer buf = new StringBuffer("<hr class=\"solid_line\"/>\n");
            buf.append("<div id=\"footer\">\n");
            buf.append("Report: ").append(fToolReport.getName()).append("&nbsp&nbsp ");
            buf.append("by user: ").append(SystemUtils.getUserName()).append("&nbsp&nbsp");
            buf.append("<div class=\"date\">").append(fToolReport.getProducerName()).append("&nbsp[").append(date).append(']').append("</div>\n");

            buf.append("<div class=\"contact\">Website: <a href=\"").append(GseaWebResources.getGseaBaseURL()).append("\">").append(GseaWebResources.getGseaURLDisplayName()).append("</a>\n");
            buf.append("Questions & Suggestions: <a href=\"").append(GseaWebResources.getGseaContactURL()).append("\">Contact page</a></div>\n");

            buf.append("<div class=\"spacer\">&nbsp;</div>\n");
            buf.append("</div>\n");
            getDoc().appendBody(buf.toString());
            this.fFooterAdded = true;
        }
    }

    public void write(OutputStream os) throws IOException {
        addFooter();
        super.write(os);
    }
} // End class ToolIndexPage

