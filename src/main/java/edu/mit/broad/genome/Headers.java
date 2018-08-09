/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

/**
 * Commonly used headers in parsing etc
 * to avoid hardcoding all over the place
 */
public interface Headers extends Constants {

    /**
     * Other commonly used Headers
     */
    public static final String RANKED_LIST = "RANKED_LIST";
    public static final String TEMPLATE = "TEMPLATE";
    public static final String GENESET = "GENESET";

    /**
     * affy control probe name prefix
     */
    public static final String AFFX_CONTROL_PREFIX = "AFFX";
    public static final String AFFX_NULL = "---";

    public static final String LV_PROC = "LC_PROC";
    public static final String SORT_MODE = "SORT_MODE";
    public static final String ORDER = "ORDER";
    public static final String METRIC = "METRIC";
    public static final String NUM_PERMS = "NUM_PERMS";

    public static final String USE_MEDIAN = "USE_MEDIAN";
    public static final String USE_BIASED = "USE_BIASED";
    public static final String FIX_LOW = "FIX_LOW";

    public static final String EXCLUDE_NAME = "EXCLUDE_NAME";

    public static final String DMEG = "DMEG";
    public static final String GENE_SYMBOL = "GENE_SYMBOL";
    public static final String GENE_TITLE = "GENE_TITLE";

} // End Headers
