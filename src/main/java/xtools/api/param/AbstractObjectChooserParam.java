/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

public abstract class AbstractObjectChooserParam extends AbstractParam {

    AbstractObjectChooserParam(String name, String nameEnglish, Class[] classes, String desc, Object[] def,
            Object[] hints, boolean reqd) {
        super(name, nameEnglish, classes, desc, def, hints, reqd);
    }

    AbstractObjectChooserParam(String name, String nameEnglish, Class cl, String desc, Object[] def, Object[] hints,
            boolean reqd) {
        this(name, nameEnglish, new Class[] { cl }, desc, def, hints, reqd);
    }

    protected static String format(final Object[] vals) {
        if (vals == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                continue;
            }
            buf.append(vals[i].toString().trim());
            if (i != vals.length - 1) {
                buf.append(',');
            }
        }
        return buf.toString();
    }

    public void setValue(String[] ss) {
        super.setValue(ss);
    }

    public String getValueStringRepresentation(boolean full) {
        Object val = getValue();
        if (val == null) {
            return null;
        }
        Object[] objs = (Object[]) val;
        return format(objs);
    }

    public boolean isFileBased() {
        return false;
    }
}
