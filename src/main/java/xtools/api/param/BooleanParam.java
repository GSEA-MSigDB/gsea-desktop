/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;



/**
 * Object to capture a boolean parameter
 *
 * @author Aravind Subramanian
 */
public class BooleanParam extends AbstractParam {

    public BooleanParam(String name, String desc, boolean reqd) {
        super(name, Boolean.class, desc, reqd, // the default
                new Boolean[]{Boolean.TRUE, Boolean.FALSE}, reqd);
    }

    public BooleanParam(String name, String nameEnglish, String desc, boolean def, boolean reqd) {
        super(name, nameEnglish, Boolean.class, desc, def, new Boolean[]{Boolean.TRUE,
                Boolean.FALSE}, reqd);
    }

    public BooleanParam(String name, String nameEnglish, String desc, boolean def, boolean reqd, Param.Type type) {
        super(name, nameEnglish, Boolean.class, desc, def, new Boolean[]{Boolean.TRUE,
                Boolean.FALSE}, reqd, type);
    }

    public BooleanParam(String name, String desc, boolean def, boolean reqd) {
        super(name, Boolean.class, desc, def, new Boolean[]{Boolean.TRUE,
                Boolean.FALSE}, reqd);
    }

    public void setValue(Object value) {
        if (value == null) {
            super.setValue(value);
        } else if (value instanceof Boolean) {
            super.setValue(value);
        } else if (value instanceof String) {
            this.setValue((String) value);
        } else {
            throw new IllegalArgumentException("Invalid type, only Boolean accepted. Specified: "
                    + value + " class: " + value.getClass());
        }
    }

    public void setValue(Boolean value) {
        super.setValue(value);
    }

    public void setValue(String trueorfalse) {
        setValue(Boolean.valueOf(trueorfalse));
    }

    public boolean isFileBased() {
        return false;
    }

    public boolean isTrue() {
        Object val = getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return ((Boolean) val).booleanValue();

        //return false; // if null -- hmm no
    }

    public boolean isFalse() {
        Object val = getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        boolean valb = ((Boolean) val).booleanValue();
        if (valb == true) {
            return false;
        } else {
            return true;
        }
    }
}
