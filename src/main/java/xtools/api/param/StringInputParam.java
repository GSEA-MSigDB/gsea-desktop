/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GSafeCharsField;

/**
 * @author Aravind Subramanian
 */
public class StringInputParam extends AbstractParam {

    private GSafeCharsField tfName;

    private char[] addSafe;

    public StringInputParam(String name, String nameEnglish, String desc, String def_andonly_hint, boolean reqd) {
        super(name, nameEnglish, String.class, desc, new String[]{def_andonly_hint}, reqd);
    }

    public StringInputParam(String name, String nameEnglish, String desc, String def, String[] hints, boolean reqd) {
        super(name, nameEnglish, String.class, desc, def, hints, reqd);
    }

    public StringInputParam(String name, String nameEnglish, String desc, String def_andonly_hint, boolean reqd, char[] addSafeChars, Type type) {
        super(name, nameEnglish, String.class, desc, new String[]{def_andonly_hint}, reqd);
        this.addSafe = addSafeChars;
        setType(type);
    }

    public void setValue(Object value) {
        if (value == null) {
            super.setValue(value);
        } else if (value instanceof String) {
            String ss = ((String) value).trim();
            if (ss.length() == 0) {
                super.setValue(null);
            } else {
                this.setValue(ss);
            }
        } else {
            throw new IllegalArgumentException("Invalid type, only String accepted. Specified: "
                    + value + " class: " + value.getClass());
        }
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    /**
     * Null-safe version of getValue().  Returns an empty String if the value is null,
     * so use getValue() if you need to distinguish between null and the empty string.
     */
    public String getNullSafeValue() {
        Object value = this.getValue();
        return (value == null) ? "" : value.toString();
    }
    
    public GFieldPlusChooser getSelectionComponent() {
        if (tfName == null) {
            Object t = this.getValue();

            if (t == null) {
                t = this.getDefault();
            }

            if (t != null) {
                tfName = new GSafeCharsField(t.toString());
            } else {
                tfName = new GSafeCharsField();
            }

            if (addSafe != null) {
                tfName.addSafeChars(addSafe);
            }

            ParamHelper.addDocumentListener(tfName, this);
        }

        return tfName;
    }

    public boolean isFileBased() {
        return false;
    }
}
