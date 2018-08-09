/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.swing.choosers.GFileFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import java.io.File;
import java.net.URL;

/**
 * Object to capture commandline params
 */
public class FileParam extends AbstractParam {

    private GFileFieldPlusChooser fChooser;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param hints
     * @param reqd
     */
    public FileParam(final String name, final String nameEnglish, final String desc, final File[] hints, final boolean reqd) {
        super(name, nameEnglish, File.class, desc, hints, reqd);
    }

    public void setValue(final Object value) {

        if (value == null) {
            super.setValue(value);
        } else if (value instanceof File) {
            super.setValue(value);
        } else if (value instanceof String) {
            this.setValue(value.toString());
        } else if (value instanceof URL) {
            this.setValue(value.toString());
        } else {
            throw new IllegalArgumentException("Invalid type, only File and String accepted. Specified: " + value + " class: "
                    + value.getClass());
        }
    }

    public void setValue(final File file) {
        super.setValue(file);
    }

    public void setValue(final String path) {
        super.setValue(path); // changed for the url thing Feb 2006
        //super.setValue(new File(filepath));
    }

    public boolean isFileBased() {
        return true;
    }

    public File getFile() {

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

            if (val instanceof URL) {
                return val.toString();
            } else if (val instanceof File) {
                final File file = (File) getValue();
                if (full) {
                    return file.getAbsolutePath();
                } else {
                    return file.getName();
                }
            } else if (NamingConventions.isURL(val.toString())) {
                return val.toString();
            } else {
                return val.toString();
            }
        }
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (fChooser == null) {
            fChooser = new GFileFieldPlusChooser();
            if (getValue() != null) {
                fChooser.setValue(getValue());
            } else {
                fChooser.setValue(getDefault());
            }

            ParamHelper.addDocumentListener(fChooser.getTextField(), this);

        }

        return fChooser;
    }


}    // End class FileParam
