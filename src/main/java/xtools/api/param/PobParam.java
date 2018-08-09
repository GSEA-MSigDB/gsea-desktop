/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * tagging subclass for pob params
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

abstract class PobParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    protected PobParam(String name, String nameEnglish, Class type, String desc, Object def_andonly_hint,
                       boolean reqd) {

        super(name, nameEnglish, type, desc, def_andonly_hint, reqd);

    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(value);
        } else if (value instanceof String) {
            this.setValue((String) value);
        } else if (value instanceof File) {
            this.setValue((File) value);
        } else if (DataFormat.isCompatibleRepresentationClass(value, getTypes())) {
            super.setValue(value);
        } else {
            throw new IllegalArgumentException("Invalid type! Only " + getTypes() + " File and String (path) accepted. Specified: "
                    + value + " class: " + value.getClass());
        }

    }

    public void setValue(String filepath) {
        super.setValue(new File(filepath));
    }

    public void setValue(File file) {
        super.setValue(file);
    }

    /**
     * Subclasses can have a custom-more-specifically (i.e type-cast) wrapper if they choose
     *
     * @return
     * @throws Exception
     */
    protected PersistentObject getPob() throws Exception {
        Object val = getValue();

        // @note added march 2006
        if (val instanceof Object[] && ((Object[]) val).length == 1) {
            val = ((Object[]) val)[0];
        }

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        } else if (val instanceof PersistentObject) {
            return (PersistentObject) val;
        } else if (val instanceof File) {
            return ParserFactory.read((File) val);
        } else {
            throw new RuntimeException("Unexpected value >" + val + "< of class: " + val.getClass() + " for param: " + getName());
        }

    }

    public boolean isFileBased() {
        return true;
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundPobComboBox(this, getTypes());
            ParamHelper.safeSelectPobValueDefaultOrFirst(cbOptions.getComboBox(), this);
        }

        return cbOptions;
    }

    // One problem with this is that we will respond even when a new pob
    // is parsed into the application
    public void actionPerformed(ActionEvent evt) {
        this.setValue(((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }

    public String getValueStringRepresentation(boolean full) {

        try {
            if (getValue() == null) {
                return null;
            } else {
                File file = ParserFactory.getCache().getSourceFile(getPob());
                if (full) {
                    return file.getAbsolutePath();
                } else {
                    return file.getName();
                }
            }
        } catch (Exception e) {
            log.warn("Unexpected error", e);
            return null;
        }
    }

} // End PobParam

