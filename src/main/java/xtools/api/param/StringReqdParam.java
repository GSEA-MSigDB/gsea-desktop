/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;



/**
 * Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class StringReqdParam extends AbstractParam {
/**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param desc
     * @param def_and_hints
     */
    public StringReqdParam(String name, String nameEnglish, String desc, String[] def_and_hints) {
        super(name, nameEnglish, String.class, desc, def_and_hints, true);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param desc
     * @param def
     * @param hints
     */
    public StringReqdParam(String name, String nameEnglish, String desc, String def, String[] hints) {
        super(name, nameEnglish, String.class, desc, def, hints, true);
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(value.toString());
        }
    }

    public String getString() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (String) val;
    }

    public int getStringIndexChoosen() {
        String str = getString();
        Object[] choices = getHints();

        for (int i = 0; i < choices.length; i++) {
            if ((choices[i] != null) && choices[i].toString().equals(str)) {
                return i;
            }
        }


        return -1;
    }
    public boolean isFileBased() {
        return false;
    }

}    // End class StringsChooserParam2
