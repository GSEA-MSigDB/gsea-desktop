/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.parsers.ParseUtils;

/**
 * choose 1 or more string
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note diff from StringsInputParam -> the chpices are presented in a JListWindow and not
 * in a text area
 */
abstract class StringChooserAbstractParam extends AbstractObjectChooserParam {

    StringChooserAbstractParam(String name, String nameEnglish, String desc, String[] defs, String[] hints, boolean reqd) {
        super(name, nameEnglish, String.class, desc, defs, hints, reqd);
    }

    /**
     * @param value String[] or a String(comma delimited parsed, space insensitive)
     */
    public void setValue(Object value) {

        if (value == null) {
            super.setValue(value);
        } else if (value instanceof String[]) {
            super.setValue(value);
        } else if (value instanceof String) {
            super.setValue(_parse((String) value));
        } else {
            throw new IllegalArgumentException("Invalid type, only String[] and comma-delim parsable String accepted. Specified: "
                    + value + " class: " + value.getClass());
        }
    }

    protected String[] _parse(String s) {
        return ParseUtils.string2strings(s, ",", false); // only commas!!
    }

    public String[] getStrings() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (String[]) val;
    }

    // param full ignored
    public String getValueStringRepresentation(boolean full) {

        Object val = getValue();

        if (val == null) {
            return null;
        }

        String[] ss = (String[]) val;

        return format(ss);
    }

    public boolean isFileBased() {
        return false;
    }

}    // End class AbstractStringChooserParam
