/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports.web;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.strucs.Hyperlink;
import edu.mit.broad.genome.objects.strucs.Linked;
import edu.mit.broad.genome.reports.DefaultHyperlink;
import edu.mit.broad.genome.reports.pages.Page;

/**
 * IMP IMP: Al Linked objects here must
 * must be lean and mean as it can be stored in the thousands (like a string)
 * Especially important that the createAllLinks method doesnt whenever possible
 * store the entire collection of links.
 * Instead it has the logic to dynamically create links upon the call
 * Also, DO NOT cache creation
 */
public class LinkedFactory {
    private LinkedFactory() { }

    public static Linked createLinkedGeneSymbol(String symbol) {
        return new LinkedGeneSymbol(symbol);
    }

    public static Linked createLinkedGeneSet(GeneSet gset) {
        return new LinkedGeneSet(gset);
    }

    public static Linked createLinkedSymbol(String symbol) {
        return new LinkedProbe(symbol);
    }

    public static class SimpleLinkedPage implements Linked {
        private String text;
        private String pageName; // @note IMP Dont save the page that doesnt clear memory!!
        private String pageExt; // @note IMP Dont save the page that doesnt clear memory!!

        public SimpleLinkedPage(final String text, final Page page) {
            this.text = text;
            this.pageName = page.getName();
            this.pageExt = page.getExt();
        }

        public String getText() {
            return text;
        }

        public Hyperlink createDefaultLink() {
            return new DefaultHyperlink(text, pageName + "." + pageExt);
        }

        public Hyperlink[] createAllLinks() {
            return new Hyperlink[]{createDefaultLink()};
        }
    }

    /**
     * For a HUGO GeneSymbol
     */
    public static class LinkedGeneSymbol implements Linked {
        private String fSymbol;

        public LinkedGeneSymbol(final String symbol) {
            if (symbol == null) {
                throw new IllegalArgumentException("Parameter symbol cannot be null");
            }
            this.fSymbol = symbol;
        }

        public String getText() {
            return fSymbol;
        }

        public Hyperlink createDefaultLink() {
            return new DefaultHyperlink(WebResources.ENTREZ_GENE_SYMBOL.getName(),
                    WebResources.ENTREZ_GENE_SYMBOL.getUrlPrefix() + fSymbol + "[sym]");
        }

        public Hyperlink[] createAllLinks() {
            return new Hyperlink[]{createDefaultLink(),
                    new DefaultHyperlink(WebResources.STANFORD_SOURCE_GENE.getName(),
                            WebResources.STANFORD_SOURCE_GENE.getUrlPrefix() + fSymbol)};
        }
    }

    /**
     * For a GeneSet
     */
    public static class LinkedGeneSet implements Linked {
        private String fGeneSetName;
        private String url;

        public LinkedGeneSet(final GeneSet gset) {
            if (gset == null) {
                throw new IllegalArgumentException("Parameter gset cannot be null");
            }

            this.fGeneSetName = gset.getName(true);

            if (StringUtils.isNotBlank(gset.getNameEnglish()) && gset.getNameEnglish().toLowerCase().startsWith("http")) {
                this.url = gset.getNameEnglish();
            } else {
                this.url = Constants.NA;
            }
        }

        public String getText() {
            return fGeneSetName;
        }

        public Hyperlink createDefaultLink() {
            return new DefaultHyperlink(fGeneSetName, url);
        }

        public Hyperlink[] createAllLinks() {
            return new Hyperlink[]{createDefaultLink()};
        }

    }

    /**
     * For an affy probe
     */
    public static class LinkedProbe implements Linked {
        private String fProbe;

        public LinkedProbe(String probe) {
            if (probe == null) {
                throw new IllegalArgumentException("Parameter probe cannot be null");
            }
            this.fProbe = probe;
        }

        public String getText() {
            return fProbe;
        }

        // todo fixme
        public String toString() {
            return fProbe;
        }

        public Hyperlink createDefaultLink() {
            return new DefaultHyperlink(WebResources.ENSEMBL.getName(),
                    WebResources.ENSEMBL.getUrlPrefix() + fProbe);
        }

        public Hyperlink[] createAllLinks() {
            return new Hyperlink[]{createDefaultLink(),};
        }
    }

    public static class SimpleLinkedFile implements Linked {
        private String text;
        private String fileName; // @note IMP Dont save the page that doesnt clear memory!!
    
        public SimpleLinkedFile(final String text, final File file) {
            this.text = text;
            this.fileName = file.getName();
        }
    
        public String getText() {
            return text;
        }
    
        public Hyperlink createDefaultLink() {
            return new DefaultHyperlink(text, fileName);
        }
    
        public Hyperlink[] createAllLinks() {
            return new Hyperlink[]{createDefaultLink()};
        }
    }
}
