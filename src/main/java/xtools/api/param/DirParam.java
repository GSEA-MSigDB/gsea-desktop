/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class DirParam extends AbstractParam {

    protected GDirFieldPlusChooser fChooser;

    public DirParam(String name, String nameEnglish, String desc, File hint, boolean reqd) {
        super(name, nameEnglish, File.class, desc, new File[]{hint}, reqd);
    }

    public DirParam(boolean reqd) {
        super(DIR, DIR_ENGLISH, File.class, DIR_DESC, new File[]{}, reqd);
    }

    public DirParam(final String name, final String nameEnglish, final String desc, final boolean reqd) {
        super(name, nameEnglish, File.class, desc, new File[]{}, reqd);
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(value);
        } else if (value instanceof File) {
            this.setValue((File) value);
        } else if (value instanceof String) {
            this.setValue(new File(value.toString()));
        } else {
            throw new IllegalArgumentException("Invalid type, only File and String accepted. Specified: " + value + " class: "
                    + value.getClass());
        }
    }

    public void setValue(String dirpath) {
        this.setValue(new File(dirpath));
    }

    public void setValue(File dir) {
        super.setValue(dir);
    }

    public boolean isFileBased() {
        return true;
    }

    public File getDir() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }
    
        if (val instanceof File) {
            return (File) val;
        } else {
            return new File(val.toString());
        }
    }

    public String getValueStringRepresentation(boolean full) {

        Object val = getValue();

        if (val == null) {
            return null;
        } else {
            File file = (File) getValue();
            if (full) {
                return file.getAbsolutePath();
            } else {
                return file.getName();
            }
        }
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (fChooser == null) {
            fChooser = new GDirFieldPlusChooser();

            if (getValue() != null) {
                fChooser.setValue(getValue());
            } else {
                fChooser.setValue(getDefault());
            }

            ParamHelper.addDocumentListener(fChooser.getTextField(), this);

        }

        return fChooser;
    }
}