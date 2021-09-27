/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports.pages;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.strucs.Hyperlink;
import edu.mit.broad.genome.objects.strucs.Linked;
import org.apache.ecs.Doctype;
import org.apache.ecs.Document;
import org.apache.ecs.Element;
import org.apache.ecs.StringElement;
import org.apache.ecs.html.*;
import org.apache.log4j.Logger;
import xapps.gsea.GseaWebResources;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class HtmlFormat {
    private static final Logger klog = Logger.getLogger(HtmlFormat.class);

    private static final String CENTER = "center";

    private static final String image = "image";
    private static final String error = "error";
    private static final String richTable = "richTable";
    private static final String dataTable = "dataTable";
    private static final String keyValTable = "keyValTable";
    private static final String table = "table";
    private static final String lessen = "lessen";

    public static final Link CSS_XTOOLS_CANNED_REPORTS = new Link();
    public static final Link ICON_CANNED_REPORTS = new Link();

    public static final Script GP_S3_BUG_WORKAROUND = new Script();
    
    static {
        CSS_XTOOLS_CANNED_REPORTS.setRel("stylesheet");
        CSS_XTOOLS_CANNED_REPORTS.setHref("xtools.css");
        ICON_CANNED_REPORTS.setRel("shortcut icon");
        ICON_CANNED_REPORTS.setHref(GseaWebResources.getGseaBaseURL() + "/images/icon_16x16.png");
        
        // Embedded JavaScript workaround for an issue with reports on GenePattern saving to AWS S3, introduced July 2021.
        // The bug is that, on S3, GP links to files through a redirect and this breaks any relative URLs in the document. 
        // The idea here is to set a <base> element on the report page make all URLs absolute.  However, we have to do it
        // dynamically since we won't know the correct base URL when the reports are generated (not without a lot of extra
        // config & bother, anyway).
        // Fix is due to Ted Liefeld
        GP_S3_BUG_WORKAROUND.setType("text/javascript");
        StringElement scriptString = new StringElement("document.write(\"<base href='\" + window.location.href.substring(0, window.location.href.lastIndexOf('/')+1) + \"' />\");");
        GP_S3_BUG_WORKAROUND.addElement(scriptString);
    }

    public static void setCommonDocThings(final String title, final Document doc) {
        // we want the HEAD to look like:
        // <head>
        // <title>Ensembl Genome Browser</title>
        // <link rel="stylesheet" href="/xtools.css">
        // can later add meta tags ala ensembl if needed
        // <meta name="keywords" content="Ensembl, genome, automated annotation, Human Genome Project, Human genetics, DNA sequencing, genome browser">
        //<meta name="description" content="Ensembl is a joint project between EMBL-EBI and the Sanger Institute to develop a software system which produces and maintains automatic annotation on eukaryotic genomes.">
        // <meta name="author" content="webmaster@ensembl.org">
        // </head>

        // set the global html properties needed
        // doctype. I prefer html to xhtml as not really going to be parsed by anything
        // also, the xhtml ecs api uses lower case java classes and thats annoying and not-very-java-like
        doc.setDoctype(new Doctype.Html401Transitional());

        // the title - shows up in the browser title bar and NOT in the content of the page
        doc.setTitle(new Title(title));

        doc.appendHead(GP_S3_BUG_WORKAROUND);
        doc.appendHead(CSS_XTOOLS_CANNED_REPORTS);
        doc.appendHead(ICON_CANNED_REPORTS);
    }

    /**
     * Class Links
     */
    public static class Links {
        public static StringElement hyper(final String hyperLinkThisTerm, final File file,
                                          final String postTerm, final File baseDir) {
            A link = new A().addElement(hyperLinkThisTerm);
            setHref(link, file, baseDir);
            StringElement sel = new StringElement();
            sel.addElement(link);
            sel.addElement(" " + postTerm);
            return sel;
        }

        public static StringElement hyper(final String hyperLinkThisTerm, final String url, final String postTerm_opt) {
            A link = new A(url).addElement(hyperLinkThisTerm);
            StringElement sel = new StringElement();
            sel.addElement(link);
            if (postTerm_opt != null) {
                sel.addElement(" " + postTerm_opt);
            }
            return sel;
        }

        public static StringElement hyperDir(final String hyperLinkThisTerm, final File dir, final String postTerm) {
            A link = new A().addElement(hyperLinkThisTerm);
            setHrefToDir(link, dir);
            StringElement sel = new StringElement();
            sel.addElement(link);
            sel.addElement(" " + postTerm);
            return sel;
        }

        public static void setHref(final A link, final File linkThisFile, final File baseDir) {
            if (link == null || linkThisFile == null || baseDir == null) {
                return;
            }

            try {
                File fooBaseDir;
                if (linkThisFile.isDirectory()) {
                    fooBaseDir = linkThisFile;
                } else {
                    fooBaseDir = linkThisFile.getParentFile();
                }

                if (fooBaseDir.equals(baseDir)) {
                    link.setHref(linkThisFile.getName());
                } else { // makes an assumption that only 1 folder down
                    link.setHref(fooBaseDir.getName() + "/" + linkThisFile.getName());
                }
            } catch (Throwable t) {
                t.printStackTrace();
                link.setHref("there was an error: " + t.getMessage());
            }
        }

        public static void setHrefToDir(final A link, final File linkThisDir) {
            if (link == null || linkThisDir == null) {
                return;
            }

            if (!linkThisDir.isDirectory()) {
                klog.warn("Not a directory: " + linkThisDir);
            }

            try {
                link.setHref(linkThisDir.toURI().toString());
            } catch (Throwable t) {
                t.printStackTrace();
                link.setHref("there was an error: " + t.getMessage());
            }
        }

        public static void setHref(final A link, final File linkThisFile, final String reltoThisBase) {
            if (linkThisFile != null) {
                link.setHref(reltoThisBase + "/" + linkThisFile.getName());
            }
        }

        public static StringElement hyper(final String preTerm, final String hyperLinkThisTern,
                                          final File file, final String postTerm, final File baseDir) {
            A link = new A().addElement(hyperLinkThisTern);
            setHref(link, file, baseDir);
            StringElement sel = new StringElement();
            sel.addElement(preTerm + " ");
            sel.addElement(link);
            sel.addElement(" " + postTerm);
            return sel;
        }
    } // End class Links

    /**
     * Class Divs
     */
    public static class Divs {
        public static Div image() {
            return new MyDiv(image);
        }

        public static Div error() {
            return new MyDiv(error);
        }

        public static Div richTable() {
            return new MyDiv(richTable);
        }

        public static Div dataTable() {
            return new MyDiv(dataTable);
        }

        public static Div keyValTable() {
            return new MyDiv(keyValTable);
        }
    } // End inner class Divs

    public static class Titles {
        public static Caption table(String text) {
            return new MyTitle(table, "Table: " + text);
        }
    }

    /**
     * Class Table Headers
     */
    public static class THs {
        public static TH richTable(String text) {
            return new MyTH(richTable, text);
        }

        public static TH keyValTable(String text) {
            return new MyTH(keyValTable, text);
        }
    }

    public static class TDs {
        public static TD lessen(String text) {
            return new MyTD(lessen, text);
        }
    }

    // to make the set class easier
    static class MyDiv extends Div {
        MyDiv(String className) {
            super();
            super.setClass(className);
        }
    }

    static class MyTH extends TH {
        MyTH(String className, String text) {
            super(text);
            super.setClass(className);

        }
    }

    static class MyTD extends TD {
        MyTD(String className, String text) {
            super(text);
            super.setClass(className);

        }
    }

    static class MyTitle extends Caption { // Really a caption but in my lingo its a Title
        MyTitle(String className) {
            super();
            super.setClass(className);
        }

        MyTitle(String className, String text) {
            this(className);
            this.addElement(text);
        }
    }

    private HtmlFormat() { }

    public static Caption caption(String s) {
        Caption c = new Caption();
        c.addElement(s);
        return c;
    }

    // key here is to add the text to the font and then add the font to the td
    public static TD _td(String s) {
        if (s != null) {
            s = s.replace('', ' ');
        }

        TD td = new TD(s);
        if (s == null || s.equals(Constants.HYPHEN)) {
            td.setAlign(CENTER);
        }
        return td;
    }

    private static boolean isNullOrNA(String s) {
        if (s == null || s.length() == 0 || s.equalsIgnoreCase(Constants.NA) || s.equalsIgnoreCase(Constants.NULL)) {
            return true;
        }

        return false;
    }

    public static TD _td(final String term, final String bgColor, final Linked linked) {
        TD td = _td(term);

        if (linked != null) {
            Hyperlink[] links = linked.createAllLinks(); // @note we are creating them just in time - no caching etc till here

            if (links.length > 1) { // place the links seperate from the term
                td.addElement(new BR());
                for (int i = 0; i < links.length; i++) {
                    if (isNullOrNA(links[i].getURL())) {
                        td.addElement(new StringElement(links[i].getDisplayName()));
                    } else {
                        A a = new A(links[i].getURL(), links[i].getDisplayName());
                        td.addElement(a);
                    }
                    if (i != links.length - 1) {
                        td.addElement(", &nbsp");
                    }
                }
            } else if (links.length == 1) { // underline the term directly
                if (isNullOrNA(links[0].getURL())) {
                    td = new TD(new StringElement(term));
                } else {
                    A a = new A(links[0].getURL(), term);
                    td = new TD(a);
                }
            }
        }

        if (bgColor != null) {
            td = td.setBgColor(bgColor);
        }

        return td;
    }

    protected static TD _td(Element s) {
        return _td(s.toString());
    }

    protected static TD _td(final Linked l) {
        if (isNullOrNA(l.createDefaultLink().getURL())) {
            return _td(new StringElement(l.getText()));
        } else {
            A a = new A(l.createDefaultLink().getURL(), l.getText());
            return _td(a);
        }
    }

    protected static TD _td(Hyperlink l) {
        if (isNullOrNA(l.getURL())) {
            return _td(new StringElement(l.getDisplayName()));
        } else {
            A a = new A(l.getURL(), l.getDisplayName());
            return _td(a);
        }
    }

    protected static TD _td(Object obj) {
        if (obj instanceof Linked) {
            return _td((Linked) obj);
        } else if (obj instanceof Hyperlink) {
            return _td((Hyperlink) obj);
        } else if (obj instanceof Element) {
            return _td((Element) obj);
        } else if (obj == null) {
            TD td = _td(Constants.HYPHEN);
            td.setAlign(CENTER);
            return td;
        } else {
            return _td(obj.toString());
        }
    }
}
