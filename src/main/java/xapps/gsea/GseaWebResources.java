/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import edu.mit.broad.genome.parsers.AuxUtils;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GseaWebResources {

    // The duplication here is a leftover from when there used to be two FTP servers.
    // It's just temporary; we plan to switch to HTTP(S) in the future.
    private final static String BROAD_FTP_SERVER = "ftp.broadinstitute.org";
    private final static String GSEA_FTP_SERVER = "ftp.broadinstitute.org";
    private final static String GSEA_FTP_SERVER_USERNAME = "anonymous";
    private final static String GSEA_FTP_SERVER_PASSWORD = "gsea@broadinstitute.org";
    private final static String GSEA_FTP_SERVER_BASE_DIR = "/pub/gsea";
    private final static String GSEA_FTP_SERVER_CHIPFILES_SUB_DIR = "annotations_versioned";
    private final static String GSEA_FTP_SERVER_GENESETS_SUB_DIR = "gene_sets";

    public static String getGseaFTPServer() {
        return GSEA_FTP_SERVER;
    }

    public static String getGseaFTPServerUserName() {
        return GSEA_FTP_SERVER_USERNAME;
    }

    public static String getGseaFTPServerPassword() {
        return GSEA_FTP_SERVER_PASSWORD;
    }

    public static String getBroadFTPBase() {
        return "ftp://" + BROAD_FTP_SERVER + GSEA_FTP_SERVER_BASE_DIR;
    }
    
    public static String getGseaFTPServerChipDir() {
        return GSEA_FTP_SERVER_BASE_DIR + "/" + GSEA_FTP_SERVER_CHIPFILES_SUB_DIR;
    }

    public static String getGseaFTPServerGeneSetsDir() {
        return GSEA_FTP_SERVER_BASE_DIR + "/" + GSEA_FTP_SERVER_GENESETS_SUB_DIR;
    }

    public static String getGseaBaseURL() {
        return "http://www.gsea-msigdb.org/gsea";
    }

    public static String getGseaURLDisplayName() {
        return "www.gsea-msigdb.org/gsea";
    }

    public static String getGseaHelpURL() {
        return getGseaBaseURL() + "/wiki";
    }

    public static String getGseaContactURL() {
        return getGseaBaseURL() + "/contact.jsp";
    }

    public static String getGseaDataFormatsHelpURL() {
        return getGseaBaseURL() + "/wiki/index.php/Data_formats";
    }

    public static String getArrayAnnotationsURL() {
        return "https://data.broadinstitute.org/gsea-msigdb/msigdb/annotations_versioned/";
    }

    public static String getGseaExamplesURL() {
        return getGseaBaseURL() + "/datasets.jsp";
    }

    public static String getGeneSetURL(String gsetName) {
        gsetName = AuxUtils.getAuxNameOnlyNoHash(gsetName);
        return getGseaBaseURL() + "/msigdb/cards/" + gsetName + ".html";
    }
}
