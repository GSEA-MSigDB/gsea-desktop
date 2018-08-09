/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.web;

/**
 * @author Aravind Subramanian
 */
public class WebResources {

    public static final WebResource NETAFFX = new DefaultWebResource("NetAffx",
            "https://www.affymetrix.com/LinkServlet?probeset=");

    public static final WebResource STANFORD_SOURCE_GENE = new DefaultWebResource("Source",
            "http://genome-www5.stanford.edu/cgi-bin/SMD/source/sourceResult?option=Name&choice=Gene&organism=Hs&criteria=");

    public static final WebResource ENTREZ_GENE_SYMBOL = new DefaultWebResource("Entrez",
            "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=search&db=gene&term=");

    public static final WebResource STANFORD_SOURCE_CLONE = new DefaultWebResource("Source",
            "http://source.stanford.edu/cgi-bin/source/sourceResult?option=CloneID&choice=cDNA&criteria=");

    static class DefaultWebResource implements WebResource {
        private String fName;
        private String fPrefix;

        DefaultWebResource(String name, String prefix) {
            this.fName = name;
            this.fPrefix = prefix;
        }

        public String getName() {
            return fName;
        }

        // always of the form: prefix=
        public String getUrlPrefix() {
            return fPrefix;
        }

    }

} // End WebResources
