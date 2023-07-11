/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import org.apache.commons.lang3.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian, David Eby
 */
public class IntegerParam extends AbstractParam implements ActionListener {
    private GComboBoxField cbOptions;

    public IntegerParam(final String name, final String englishName, final String desc, int def_andonly_hint, boolean reqd) {
        super(name, englishName, Integer.class, desc, def_andonly_hint, reqd);
    }

    public IntegerParam(String name, String englishName, String desc, int def_andonly_hint, boolean reqd, Param.Type type) {
        super(name, englishName, Integer.class, desc, def_andonly_hint, reqd, type);
    }

    public IntegerParam(String name, String englishName, String desc, int def, int[] hintsanddef, boolean reqd) {
        super(name, englishName, Integer.class, desc, def, toInts(hintsanddef), reqd);
    }

    public boolean isFileBased() {
        return false;
    }

    @Override
    public Object getValue() {
        Object value = super.getValue();
        if (value == null || value instanceof Integer) { return value; }
        
        if (value instanceof String) {
          if (StringUtils.isBlank(value.toString())) {
              if (!isReqd()) { return getDefault(); }
              throw new IllegalArgumentException("Parameter '" + getNameEnglish() + "' is required");
          } else {
              try {
                  return Integer.parseInt(value.toString());
              } catch (NumberFormatException nfe) {
                  throw new IllegalArgumentException("Parameter '" + getNameEnglish() + "' had value '" + value.toString()
                          + "'. Must be a valid integer.", nfe);
              }
          }
      } else {
          throw new IllegalArgumentException("Invalid type for Parameter '" + getNameEnglish() + ", only Integer accepted. Specified: "
                  + value + " class: " + value.getClass());
      }
    }

    public int getIValue() {
        Object val = this.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return ((Integer) val).intValue();
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(true, this, this);
        }

        return cbOptions;
    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue(cbOptions.getComboBox().getSelectedItem());
    }
}
