/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

/**
 * 4 kinds of parameters:
 * <p/>
 * 1) required and NO default provided. Example res file
 * 2) required and a default provided. Example metric
 * 3) optional and no default provided
 * 4) optional and a default provided
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Param {

    public static final Param.Type REQUIRED = new Param.Type("required");
    public static final Param.Type PSEUDO_REQUIRED = new Param.Type("pseudo_required");
    public static final Param.Type BASIC = new Param.Type("basic");
    public static final Param.Type ADVANCED = new Param.Type("advanced");

    public Type getType();

    public void setType(Type type);

    public String getName();

    public String getHtmlLabel_v2();

    public String getHtmlLabel_v3();

    public String getDesc();

    public String getNameEnglish();

    public Object[] getHints();

    public boolean isReqd();

    /**
     * note diff from isTrue
     * This doesnt check the value, just for the existence of a non-null value
     *
     * @return
     */
    public boolean isSpecified();

    /**
     * default value if any. Typically one of the Hint values
     */
    public Object getDefault();

    public Object getValue();

    public void setValue(Object val);

    /**
     * str rep of value
     * appropriate for use in properties objects
     * Example -< if dataset, then file path
     * If cnpair then cnpair name
     * <p/>
     * must be null if value is null
     */
    public String getValueStringRepresentation(boolean full);

    public String formatForCmdLine();

    /**
     * is the data for this param coming from a file
     */
    public boolean isFileBased();

    /**
     * component which represents the choices allowed for this Param
     * should be selectable
     *
     * @return
     */
    public GFieldPlusChooser getSelectionComponent();

    /**
     * CONSTANTS FOR PARAM NAMES AND DESCS
     */

    public static final String DF_FILE = "df_file";
    public static final String DF_FILE_DESC = "Path to the File containing the Dataframe ";

    public static final String CHIP = "chip";
    public static final String CHIP_ENGLISH = "Chip platform";
    public static final String CHIP_ENGLISH_MULTI = "Chip platform(s)";
    public static final String CHIP_DESC = "Name of the chip to use - only 1 chip can be choosen";
    public static final String CHIP_DESC_MULTI = "Name(s) of the chip(s) to use - more than one chip can be choosen";

    public static final String CHIPCOMBOTYPE = "chip_combo_type";
    public static final String CHIPCOMBOTYPE_ENGLISH = "Combine chips mode";
    public static final String CHIPCOMBOTYPE_DESC = "chip combination mode";

    public static final String CMETRIC = "cmetric";
    public static final String CMETRIC_DESC = "Metric to calculate correlation between vectors";

    public static final String CLS = Constants.CLS;
    public static final String CLS_ENGLISH = "Phenotype labels";
    public static final String CLS_DESC = "Phenotype labels for the samples in the expression dataset (cls file)";

    public static final String RES = Constants.RES;
    public static final String RES_ENGLISH = "Expression dataset";
    public static final String RES_DESC = "Expression dataset - with rows as genes and columns as samples (for instance: res, gct, pcl files)";
    public static final String RES_DESC_MULTI = "Path to files containing the RES formatted datasets, each path should be comma delimited";

    public static final String OUT = "out";
    public static final String OUT_ENGLISH = "Save results in this folder";
    public static final String OUT_DESC = "Path of the directory in which to place output from the analysis (any existing files will NOT be overwritten)";

    public static final String SDF = Constants.SDF;
    public static final String SDF_DESC = "Path to file containing sdf data";
    public static final String SDF_DESC_MULTI = "Path to files containing the SDF formatted datasets, each path should be comma delimited";

    public static final String DF = Constants.DF;
    public static final String DF_DESC = "Path to file containing df data";
    public static final String DF_DESC_MULTI = "Path to files containing the DF formatted datasets, each path should be comma delimited";

    public static final String GIN = Constants.GIN;
    public static final String GIN_DESC = "Path to file containing GIN data";
    public static final String GIN_DESC_MULTI = "Path to files containing the genesofInterest formatted data, each path should be comma delimited";

    public static final String EDB = Constants.EDB;
    public static final String EDB_DESC = "Path to file containing edb data";
    public static final String EDB_DESC_MULTI = "Path to files containing the edb formatted enrichement databases, each path should be comma delimited";

    public static final String GRP = Constants.GRP;
    public static final String GRP_ENGLISH = "Gene set";
    public static final String GRP_DESC = "GeneSet (grp file; only 1 allowed)";
    public static final String GRP_DESC_MULTI = "GeneSets (grp files - one or more are allowed)";

    public static final String RNK = Constants.RNK;
    public static final String RNK_ENGLISH = "Ranked List";
    public static final String RNK_DESC = "RankedList (rnk file; only 1 allowed)";
    public static final String RNK_DESC_MULTI = "RankedLists (rnk files - one or more are allowed)";

    public static final String GMX = Constants.GMX;
    public static final String GMX_ENGLISH = "Gene sets database";
    public static final String GMX_DESC = "GeneSetMatrix (gmx or gmt file; only 1 allowed)";
    public static final String GMX_DESC_MULTI = "Gene sets database (gmx or gmt files - one or more are allowed)";

    public static final String DELIM = "delim";
    public static final String DELIM_DESC = "Field delimiter";

    public static final String GUI = "gui";
    public static final String GUI_DESC = "Display any reports that the tool produces in a Graphical User Interface";

    public static final String SCORING_SCHEME = "scoring_scheme";
    public static final String SCORING_SCHEME_ENGLISH = "Enrichment statistic";
    public static final String SCORING_SCHEME_DESC = "The statistic used to score hits (gene set members) and misses (non-members)";

    public static final String MMODE = "mmode";
    public static final String MMODE_DESC = "probe merge mode - how to deal with one->many mappings for Probes";

    public static final String MAP = "map";
    public static final String MAP_ENGLISH = "Mapping database";
    public static final String MAP_DESC = "Mapping database to use to convert probes from chip to chip";

    public static final String METRIC = "metric";
    public static final String METRIC_ENGLISH = "Metric for ranking genes";
    public static final String METRIC_DESC = "Class seperation metric - gene markers are ranked using this metric to produce the gene list";

    public static final String MSIGDB = "msigdb";
    public static final String MSIGDB_ENGLISH = "MSigDB file";
    public static final String MSIGDB_DESC = "MSigDB file)";

    public static final String FEATURE_SPACE = "collapse";
    public static final String FEATURE_SPACE_ENGLISH = "Collapse dataset to gene symbols";
    public static final String FEATURE_SPACE_DESC = "Perform the analysis in the specified feature space - either gene symbols or as is (any mappings required will be done internally using the Chip/Platform specified)";

    public static final String DATASET_MODE = "dmode";
    public static final String DATASET_MODE_ENGLISH = "Dataset mode";
    public static final String DATASET_MODE_DESC = "Dataset mode";

    public static final String RANKED_LIST_PROCESSORS = "ranked_list_proc";
    public static final String RANKED_LIST_PROCESSORS_ENGLISH = "Gene list pre-processing";
    public static final String RANKED_LIST_PROCESSORS_DESC = "Ranked list processor";

    public static final String COLOR_SCHEME = "color";
    public static final String COLOR_SCHEME_DESC = "color scheme";

    public static final String GSEA_PVALUE_MODE = "pvalue_mode";
    public static final String GSEA_PVALUE_MODE_DESC = "Pvalue mode";

    public static final String IMPUTER = "imputer";
    public static final String IMPUTER_DESC = "Imputer - algorithm used to fill in missing values";

    public static final String RND_SEED = "rnd_seed";
    public static final String RND_SEED_ENGLISH = "Seed for permutation";
    public static final String RND_SEED_DESC = "Seed to use for randomization (a long number)";

    public static final String ORDER = "order";
    public static final String ORDER_ENGLISH = "Gene list ordering mode";
    public static final String ORDER_DESC = "Direction in which the gene list should be ordered";

    public static final String MAT = Constants.MAT;
    public static final String MAT_DESC = "Path to file containing Matrix data";

    public static final String PROBE = "probe";
    public static final String PROBE_ENGLISH = "Name of probe";
    public static final String PROBE_DESC = "Name of the probe of interest";

    public static final String RND = "rnd";
    public static final String RND_DESC = "Randomized Dataset/Template";

    public static final String RNDTYPE = "rnd_type";
    public static final String RNDTYPE_ENGLISH = "Randomization mode";
    public static final String RNDTYPE_DESC = "Type of phenotype randomization (does NOT apply to gene set permutations)";

    public static final String RND_LIST_TYPE = "rnd_list_type";
    public static final String RND_LIST_TYPE_DESC = "Type of random list";

    public static final String GENESET_RNDTYPE = "geneset_rnd_type";
    public static final String GENESET_RNDTYPE_DESC = "Type of Geneset randomization";

    public static final String RPT = "rpt_label";
    public static final String RPT_ENGLISH = "Analysis name";
    public static final String RPT_DESC = "Label for the analysis - any short phrase meaningful to you";

    public static final String RPT_DIR = "rpt_dir";
    public static final String RPT_DIR_ENGLISH = "Report dir";
    public static final String RPT_DIR_DESC = "Report directory";

    public static final String SORT = "sort";
    public static final String SORT_ENGLISH = "Gene list sorting mode";
    public static final String SORT_DESC = "Mode in which scores from the gene list should be considered ";

    public static final String SCORE_MODE = "score_mode";
    public static final String SCORE_MODE_DESC = "Score mode";

    public static final String PERMUTE = "permute";
    public static final String PERMUTE_ENGLISH = "Permutation type";
    public static final String PERMUTE_DESC = "Type of permutation - generate random phenotypes or random gene sets";

    public static final String DATAFORMAT_DATASET = "dataset_format";
    public static final String DATAFORMAT_DATASET_ENGLISH = "Dataset output format";
    public static final String DATAFORMAT_DATASET_DESC = "Format of the Dataset created/exported";

    public static final String DATAFORMAT_GENESETMATRIX = "genesetmatrix_format";
    public static final String DATAFORMAT_GENESETMATRIX_ENGLISH = "Gene set matrix output format";
    public static final String DATAFORMAT_GENESETMATRIX_DESC = "Format of the Gene set matrix created/exported";

    // ---- META ---- //
    public static final String FILE = "file";
    public static final String FILE_DESC = "Path to data file";
    public static final String FILE_ENGLISH = "Path to file";

    public static final String DIR = "dir";
    public static final String DIR_DESC = "Path to data dir";
    public static final String DIR_ENGLISH = "Path to dir";

    public static final String FILES = "files";
    public static final String FILES_DESC = "Paths to data files";
    public static final String FILES_ENGLISH = "Paths to files";

    public static final String DT_FILE = "dtfile";
    public static final String DT_FILE_DESC = "File with paths to dataset and template files";
    public static final String DT_FILE_ENGLISH = "File with paths to dataset and template files";


    public static class Type {

        private String type;

        private Type(String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }

        public boolean equals(Type type) {
            if (type == this) {
                return true;
            }

            return type != null && type.toString().equals(this.toString());

        }

    } // End class Type

} // End class Param
