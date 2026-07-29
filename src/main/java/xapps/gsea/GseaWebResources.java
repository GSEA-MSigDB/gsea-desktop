/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GseaWebResources {
    public static String getGseaBaseURL() {
        return "https://www.gsea-msigdb.org/gsea";
    }

    public static String getGseaURLDisplayName() {
        return "www.gsea-msigdb.org/gsea";
    }

    public static String getGseaHelpURL() {
        return "https://docs.gsea-msigdb.org/";
    }

    public static String getGseaContactURL() {
        return getGseaHelpURL() + "Contact/";
    }

    public static String getGseaDataFormatsHelpURL() {
        return getGseaHelpURL() + "GSEA/Data_Formats/";
    }

   public static String getGseaChipInfoHelpURL() {
        return getGseaHelpURL() + "GSEA/GSEA_User_Guide/#appendix-b";
    }

    public static String getHumanArrayAnnotationsURL() {
        return "https://data.broadinstitute.org/gsea-msigdb/msigdb/annotations/human/";
    }

    public static String getMouseArrayAnnotationsURL() {
        return "https://data.broadinstitute.org/gsea-msigdb/msigdb/annotations/mouse/";
    }

    public static String getGseaExamplesURL() {
        return getGseaBaseURL() + "/datasets.jsp";
    }

    public final static String DEFAULT_MSIGDB_CATALOG_URL =
            "https://data.broadinstitute.org/gsea-msigdb/msigdb/release/msigdb_releases.json";
}
