/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GseaWebResources {
    public final static String GSEA_FTP_SERVER = "ftp.broadinstitute.org";
    private final static String GSEA_FTP_SERVER_USERNAME = "anonymous";
    private final static String GSEA_FTP_SERVER_PASSWORD = "gsea@broadinstitute.org";
    public final static String GSEA_FTP_SERVER_BASE_DIR = "/pub/gsea/.hide";
    private final static String GSEA_FTP_SERVER_CHIPFILES_SUB_DIR = "annotations_versioned";
    private final static String GSEA_FTP_SERVER_HUMAN_CHIPFILES_DIR =
            GSEA_FTP_SERVER_BASE_DIR + "/human/" + GSEA_FTP_SERVER_CHIPFILES_SUB_DIR;
    private final static String GSEA_FTP_SERVER_MOUSE_CHIPFILES_DIR =
            GSEA_FTP_SERVER_BASE_DIR + "/mouse/" + GSEA_FTP_SERVER_CHIPFILES_SUB_DIR;
    private final static String GSEA_FTP_SERVER_GENESETS_SUB_DIR = "gene_sets";
    private final static String GSEA_FTP_SERVER_HUMAN_GENESETS_DIR =
            GSEA_FTP_SERVER_BASE_DIR + "/human/" + GSEA_FTP_SERVER_GENESETS_SUB_DIR;

    private final static String GSEA_FTP_SERVER_MOUSE_GENESETS_DIR =
            GSEA_FTP_SERVER_BASE_DIR + "/mouse/" + GSEA_FTP_SERVER_GENESETS_SUB_DIR;
    
    public static String getGseaFTPServer() {
        return GSEA_FTP_SERVER;
    }

    public static String getGseaFTPServerUserName() {
        return GSEA_FTP_SERVER_USERNAME;
    }

    public static String getGseaFTPServerPassword() {
        return GSEA_FTP_SERVER_PASSWORD;
    }
    
    public static String getGseaFTPServerChipDir(String targetSpecies) {
        if ("human".equalsIgnoreCase(targetSpecies)) { return GSEA_FTP_SERVER_HUMAN_CHIPFILES_DIR; }
        return GSEA_FTP_SERVER_MOUSE_CHIPFILES_DIR;
    }

    public static String getGseaFTPServerGeneSetsDir(String targetSpecies) {
        if ("human".equalsIgnoreCase(targetSpecies)) { return GSEA_FTP_SERVER_HUMAN_GENESETS_DIR; }
        return GSEA_FTP_SERVER_MOUSE_GENESETS_DIR;
    }

    public static String getGseaBaseURL() {
        return "https://www.gsea-msigdb.org/gsea";
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

    public static String getGseaChipInfoHelpURL() {
        return getGseaBaseURL() + "/wiki/index.php/CHIP_File_Selection_Help";
    }

    public static String getHumanArrayAnnotationsURL() {
        return "https://data.broadinstitute.org/gsea-msigdb/msigdb/annotations_versioned/";
    }

    public static String getMouseArrayAnnotationsURL() {
        return "https://data.broadinstitute.org/gsea-msigdb/msigdb/annotations_versioned/";
    }

    public static String getGseaExamplesURL() {
        return getGseaBaseURL() + "/datasets.jsp";
    }
}
