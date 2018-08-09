/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * @author Aravind Subramanian
 */
// struc

public class TemplateMode {


    public static final TemplateMode CATEGORICAL_2_CLASS_ONLY = new TemplateMode(0, "Categorical, 2 class only");
    public static final TemplateMode CATEGORICAL_2_CLASS_AND_NUMERIC = new TemplateMode(3, "Categorical 2 class and numeric");
    public static final TemplateMode CATEGORICAL_ONLY = new TemplateMode(1, "Categorical template only");
    public static final TemplateMode CONTINUOUS_ONLY = new TemplateMode(2, "Continuous template only");

    public static final TemplateMode UNIPHASE_ONLY = new TemplateMode(7, "Uniclass templates only");

    public static final TemplateMode ALL = new TemplateMode(10, "All modes of Templates");

    private int fMode;
    private String fDesc;

    /**
     * Class constructor
     *
     * @param mode
     * @param desc
     */
    TemplateMode(int mode, String desc) {
        this.fMode = mode;
        this.fDesc = desc;
    }

    public String getDesc() {
        return fDesc;
    }

} // End class TemplateMode
