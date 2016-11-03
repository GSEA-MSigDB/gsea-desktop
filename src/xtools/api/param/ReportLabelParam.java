/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import org.apache.commons.lang3.StringUtils;

/**
 * To capture a label to affix to a reports
 * Different from analysis name -> typically not shared very much
 * analysis name -> several diff iterations, all kinds of stuff
 * reports name -> specific to one or a few diff types of analysis done in a larger analysis project
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ReportLabelParam extends StringInputParam {

    public static final String DEFAULT_LABEL = "my_analysis";

    /**
     * Class constructor
     *
     * @param reqd
     */
    public ReportLabelParam(boolean reqd) {
        this(DEFAULT_LABEL, reqd);
    }

    /**
     * Class constructor
     *
     * @param def
     * @param reqd
     */
    public ReportLabelParam(String def, boolean reqd) {
        super(RPT, RPT_ENGLISH, RPT_DESC, def, reqd);
    }

    //overridden for the space check
    public void setValue(String value) {

        if (StringUtils.contains(value, " ")) {
            throw new IllegalArgumentException("Analysis name cannot contain spaces. Specified: "
                    + value);
        }

        //log.debug("Setting analysis name to: " + value);
        super.setValue(value);
    }

    // DONT rename this getName!!
    public String getReportLabel() {

        Object val = super.getValue();

        if (val == null) {
            return "";
        } else {
            return val.toString();
        }
    }

    public boolean isFileBased() {
        return false;
    }

}    // End class ReportLabelParam
