/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import java.io.File;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DirParam extends FileParam {

    protected GDirFieldPlusChooser fChooser;

    /**
     * Class constructor
     *
     * @param name
     * @param type
     * @param desc
     */
    public DirParam(String name, String nameEnglish, String desc, File hint, boolean reqd) {
        this(name, nameEnglish, desc, new File[]{hint}, reqd);
    }

    public DirParam(boolean reqd) {
        this(DIR, DIR_ENGLISH, DIR_DESC, new File[]{}, reqd);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param hints
     * @param reqd
     */
    public DirParam(final String name, final String nameEnglish, final String desc, final File[] hints, final boolean reqd) {
        super(name, nameEnglish, desc, hints, reqd);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param reqd
     */
    public DirParam(final String name, final String nameEnglish, final String desc, final boolean reqd) {
        super(name, nameEnglish, desc, new File[]{}, reqd);
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
        /*
        if( !dir.isDirectory()) {
            throw new IllegalArgumentException("Only a dir allowed. Specified: "
                                               + dir.getAbsolutePath());
        }
        */

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

        return (File) val;
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

}    // End class DirParam
