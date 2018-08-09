/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.parsers.DataFormat;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetMatrixFormatParam extends DataFormatAbstractParam {

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public GeneSetMatrixFormatParam(boolean reqd) {
        this(DataFormat.GMT_FORMAT, reqd);
    }

    /**
     * Class constructor
     *
     * @param def
     * @param reqd
     */
    public GeneSetMatrixFormatParam(DataFormat def, boolean reqd) {
        super(DATAFORMAT_GENESETMATRIX, DATAFORMAT_GENESETMATRIX_ENGLISH, DATAFORMAT_GENESETMATRIX_DESC,
                def, DataFormat.ALL_GENESETMATRIX_FORMATS, reqd);
    }

}    // End class GeneSetMatrixFormatParam
