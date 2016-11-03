/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GStringsInputFieldPlusChooser;

/**
 * entry 1 or more string
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @imp enters in a text are not a list chooser
 */
public class StringMultiInputParam extends AbstractParam {
    private GStringsInputFieldPlusChooser fChooser;
    private static final String DEFAULT_PARSE_WS_DELIMS = "\t\n";
    private static final String PARSE_DELIMS = "," + DEFAULT_PARSE_WS_DELIMS;// dont parse on spaces

    // An alternate delimiter to use in place of commas
    private String alternateDelimiter = null;
    private String parseDelims = PARSE_DELIMS;
    
    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param reqd
     */
    public StringMultiInputParam(String name, String desc, boolean reqd) {
        this(name, desc, new String[]{}, reqd);
    }

    public StringMultiInputParam(String name, String desc, String[] def_and_hints, boolean reqd) {
        super(name, String.class, desc, def_and_hints, reqd);
    }
    
    /**
     * Set an alternate delimiter to <em>replace</em> the default comma separator.  This must be a 
     * single character.  A null or empty value will revert to use of the comma.
     * 
     * This <em>must</em> be set prior to use of getValue() or getStrings() in order for the 
     * parameter to be properly parsed using the alternativeDelimiter.
     * 
     * @param alternateDelimiter
     */
    public void setAlternateDelimiter(String alternateDelimiter) {
        if (StringUtils.length(alternateDelimiter) > 1) {
            throw new IllegalArgumentException("Illegal alternate delimiter '"
                    + alternateDelimiter + "'; must be a single character only.");
        }
        this.alternateDelimiter = alternateDelimiter;
        if (StringUtils.isBlank(alternateDelimiter)) {
            this.parseDelims = PARSE_DELIMS;
        } else {
            this.parseDelims = alternateDelimiter + DEFAULT_PARSE_WS_DELIMS;
        }
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
            super.setValue(value);
        } else {
            throw new IllegalArgumentException("Invalid type, only String[] and comma (or alternate) delim parsable String accepted. Specified: "
                    + value + " class: " + value.getClass());
        }
    }

    public boolean isSpecified() {
        boolean is = super.isSpecified();

        if (!is) {
            return is;
        }

        Object val = super.getValue();
        if (val == null) {
            return false;
        }

        if (val instanceof String) {
            return val.toString().length() > 0;
        }
        if (val instanceof String[]) {
            return ((String[])val).length > 0;
        } else {
            throw new IllegalArgumentException("Invalid type, only String[] and comma (or alternate) delim parsable String accepted. Specified: "
                    + val + " class: " + val.getClass());
        }
    }

    private String[] _parse(String s) {
        return ParseUtils.string2strings(s, parseDelims, false);
    }

    public void setValue(String[] values) {
        super.setValue(values);
    }

    public Object getValue() {
        Object val = super.getValue();
        
        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        if (val instanceof String[]) {
            return val;
        }

        return _parse(val.toString());
    }
    
    public String[] getStrings() {
        return (String[]) getValue();
    }

    // param full ignored.
    public String getValueStringRepresentation(boolean full) {

        Object val = getValue();

        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            return (String) val;
        }

        String[] ss = (String[]) val;

        return format(ss);
    }

    private String format(Object[] vals) {

        if (vals == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();

        String sep = (alternateDelimiter == null) ? "," : alternateDelimiter;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                // do nothing
            } else {
                buf.append(vals[i].toString().trim());
                if (i != vals.length - 1) {
                    buf.append(sep);
                }
            }
        }

        return buf.toString();
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fChooser == null) {
            String text = this.getValueStringRepresentation(false);
            if (text == null) {
                text = format((String[]) getDefault());
            }

            if (text == null) {
                text = "";
            }

            fChooser = new GStringsInputFieldPlusChooser(text);
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }

        return fChooser;

    }

    public boolean isFileBased() {
        return false;
    }

}    // End class StringMultiInputParam
