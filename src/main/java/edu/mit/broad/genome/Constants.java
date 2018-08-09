/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

/**
 * Commonly used, application wide constants. In addition application wide
 * "convention" methods.
 * <p/>
 * Typically Strings, but other (simple data types) are allowed too.
 * <p/>
 * IMP: dont place headers in here -- theres a seperate locationin
 * parsers.Headers
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Constants {

    public static final String SEQ_ACCESSION = "SEQ_ACCESSION";
    public static final String GENE_SYMBOL = "GENE_SYMBOL";
    public static final String UNIGENE = "UNIGENE";
    public static final String STANFORD_SOURCE_ACCESSIONS = "STANFORD_SOURCE_ACCESSIONS";

    public static final char INTRA_FIELD_DELIM = '';
    public static final String INTRA_FIELD_DELIM_S = "";

    // DO NOT place GUI constants here. That goes in GuiHelper.
    public static final String NA = "na";
    public static final String HYPHEN = "-";
    public static final String AFFYMETRIX = "AFFYMETRIX";

    public static final String NOT_SPECIFIED = "<not specified>";

    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";

    // imp to keep upperspace so that we can distinguish
    public static final String NULL = "NULL";

    /* File extensions etc */
    public static final String RES = "res";
    public static final String SDF = "sdf";
    public static final String DF = "df";
    public static final String GCT = "gct";
    public static final String PCL = "pcl";
    public static final String MAT = "mat";
    public static final String RNK = "rnk";
    public static final String DTGDS = "dtgds";
    public static final String CLS = "cls";
    public static final String GRP = "grp";
    public static final String DEF = "def";
    public static final String GMX = "gmx";
    public static final String GMT = "gmt";
    public static final String DFR = "dfr";
    public static final String EDB = "edb";
    public static final String CDT = "cdt";
    public static final String ATR = "atr";
    public static final String GTR = "gtr";
    public static final String MAP = "map";
    public static final String SIN = "sin";
    public static final String RPT = "rpt";
    public static final String CHIP = "chip";
    public static final String GIN = "gin";
    public static final String DCHIP = "dchip";
    public static final String XLS = "xls";
    public static final String PDF = "pdf";
    public static final String TXT = "txt";
    public static final String XML = "xml";
    public static final String HTML = "html";
    public static final String CSV = "csv";

    /**
     * Vacillated a bit about whether to use # or to use # preferred as its the
     * format for java property files 2) one problme is that cls file already
     * uses # as a special line indicator
     */
    public static final String COMMENT_CHAR = "#";
    public static final String ALL_PAIRS = "ALL_PAIRS";
    public static final String ONE_VERSUS_ALL = "OVA";
    public static final String ONE_VERSUS_ALL_ONLY_FORWARD = "FOVA";

    public static final String REST = "REST";

    /**
     * -D system property that sets the user/debug mode of the application
     *
     * @maint var replicated in reportdisplay
     */
    public static final String DEBUG_MODE_KEY = "debug";

    public static final String MAKE_REPORT_DIR_KEY = "mkdir";

    public static final String GENE_SYMBOL_CHIP = GENE_SYMBOL + "." + CHIP;
    
    public static final String SEQ_ACCESSION_CHIP = SEQ_ACCESSION + "." + CHIP;
}
