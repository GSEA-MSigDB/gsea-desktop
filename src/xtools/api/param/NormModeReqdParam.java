/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.alg.gsea.Norms;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class NormModeReqdParam extends StringReqdParam {

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public NormModeReqdParam() {
        super("norm", "Normalization mode",
                "normalization mode",
                Norms.MEANDIV_POS_NEG_SEPERATE,
                Norms.createNormModeNames());
    }

    public String getNormModeName() {
        return super.getString();
    }

}    // End class NormModeParam
