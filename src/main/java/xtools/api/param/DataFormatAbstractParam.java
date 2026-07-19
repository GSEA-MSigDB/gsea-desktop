/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.parsers.DataFormat;


/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class DataFormatAbstractParam extends AbstractParam {
/**
     * Class constructor
     *
     * @param def
     * @param reqd
     */
    public DataFormatAbstractParam(String name, String nameEnglish, String desc, DataFormat def, DataFormat[] hints, boolean reqd) {
        super(name, nameEnglish, DataFormat.class, desc,
                def, hints, reqd);
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(DataFormat.getExtension(value));
        }
    }

    public void setValue(DataFormat df) {
        super.setValue(df);
    }

    public boolean isFileBased() {
        return false;
    }

    public DataFormat getDataFormat() {

        Object val = super.getValue();

        if (val == null) {
            return (DataFormat) getDefault(); // @note
        } else {
            return (DataFormat) val;
        }
    }
}    // End class AbstractDataFormatParam
