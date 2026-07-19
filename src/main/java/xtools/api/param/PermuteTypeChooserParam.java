/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.alg.Metric;


/**
 * Object to capture choices gene set, template
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PermuteTypeChooserParam extends AbstractParam {
public static final String BOTH = "both";
    public static final String GENE_SET = "gene_set";
    public static final String PHENOTYPE = "phenotype";

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    private PermuteTypeChooserParam(String[] options, boolean reqd) {
        super(PERMUTE, PERMUTE_ENGLISH, Metric.class, PERMUTE_DESC, options, reqd);
    }


    public static PermuteTypeChooserParam createTemplateOrGeneSet(boolean reqd) {
        return new PermuteTypeChooserParam(new String[]{PHENOTYPE, GENE_SET}, reqd);
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(value);
        }
    }

    public boolean permuteTemplate() {
        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        if ((val.toString().equals(PHENOTYPE)) || (val.toString().equals(BOTH))) {
            return true;
        } else {
            return false;
        }
    }
    public boolean isFileBased() {
        return false;
    }

}    // End class PermuteTypeChooserParam
