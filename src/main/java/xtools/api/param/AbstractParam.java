/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Object to capture commandline params / params that a Tool accepts/needs
 */
public abstract class AbstractParam implements Param {
    // TODO: consolidate to *one* logger
    protected static final Logger klog = LoggerFactory.getLogger(AbstractParam.class);

    protected Logger log;
    private String fName;
    private Class[] fClassTypes;
    private String fDesc;
    private String fNameEnglish;
    private StringBuffer fHtmlLabel_v2;
    private StringBuffer fHtmlLabel_v3;
    private boolean fReqd;
    private Object fValue;
    private Object[] fHints;
    private Object fDefault;
    private Param.Type fType;

    // users must call init
    protected AbstractParam() {
    }

    protected AbstractParam(final String name,
                            final Class type,
                            final String desc,
                            final Object[] hintsanddef,
                            final boolean reqd) {

        this(name, null, new Class[]{type}, desc, hintsanddef, reqd);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param type
     * @param desc
     * @param def
     * @param hints
     * @param reqd
     */
    protected AbstractParam(final String name,
                            final Class type,
                            final String desc,
                            final Object def,
                            final Object[] hints,
                            final boolean reqd) {

        this(name, null, new Class[]{type}, desc, def, hints, reqd);
    }

    /**
     * Class constructor
     * <p/>
     * The first object in the hints array is used as a default if non-null
     *
     * @param name
     * @param types
     * @param desc
     */
    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class[] types,
                            final String desc,
                            final Object[] hintsanddef,
                            final boolean reqd) {

        if (hintsanddef == null) {
            throw new IllegalArgumentException("hint param cannot be null");
        }

        Object def;
        if (hintsanddef.length > 0) {
            def = hintsanddef[0];
        } else {
            def = null;
        }

        init(name, nameEnglish, types, desc, hintsanddef, def, reqd, null);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param type
     * @param desc
     * @param hintsanddef
     * @param reqd
     */
    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class type,
                            final String desc,
                            final Object[] hintsanddef,
                            final boolean reqd) {

        this(name, nameEnglish, new Class[]{type}, desc, hintsanddef, reqd);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param types
     * @param desc
     * @param def_andonly_hint
     * @param reqd
     */
    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class[] types,
                            final String desc,
                            final Object def_andonly_hint,
                            final boolean reqd) {

        if (def_andonly_hint == null) {
            throw new IllegalArgumentException("def_andonly_hint param cannot be null");
        }

        init(name, nameEnglish, types, desc, new Object[]{def_andonly_hint}, def_andonly_hint, reqd, null);
    }

    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class type,
                            final String desc,
                            final Object def_andonly_hint,
                            final boolean reqd) {
        this(name, nameEnglish, new Class[]{type}, desc, def_andonly_hint, reqd);
    }

    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class type,
                            final String desc,
                            final Object def_andonly_hint,
                            final boolean reqd,
                            final Param.Type paramType) {
        init(name, nameEnglish, new Class[]{type}, desc, new Object[]{def_andonly_hint}, def_andonly_hint, reqd, paramType);
    }

    /**
     * Explicitly specified default
     *
     * @param name
     * @param types
     * @param desc
     * @param def
     * @param hints
     * @param reqd
     */
    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class[] types,
                            final String desc,
                            final Object def,
                            final Object[] hints,
                            final boolean reqd) {
        init(name, nameEnglish, types, desc, hints, def, reqd, null);
    }

    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class[] types,
                            final String desc,
                            final Object def,
                            final Object[] hints,
                            final boolean reqd,
                            final Param.Type type) {
        init(name, nameEnglish, types, desc, hints, def, reqd, type);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param type
     * @param desc
     * @param def
     * @param hints
     * @param reqd
     */
    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class type,
                            final String desc,
                            final Object def,
                            final Object[] hints,
                            final boolean reqd) {
        this(name, nameEnglish, new Class[]{type}, desc, def, hints, reqd);
    }

    protected AbstractParam(final String name,
                            final String nameEnglish,
                            final Class type,
                            final String desc,
                            final Object def,
                            final Object[] hints,
                            final boolean reqd,
                            final Param.Type paramType) {
        this(name, nameEnglish, new Class[]{type}, desc, def, hints, reqd, paramType);
    }

    /**
     * Common initialization routine
     *
     * @param name
     * @param type
     * @param desc
     * @param hintsanddef
     * @param reqd
     */
    protected void init(final String name,
                        final String nameEnglish,
                        final Class[] classTypes,
                        final String desc,
                        final Object[] hints,
                        final Object def,
                        final boolean reqd,
                        final Param.Type type) {

        if (name == null) {
            throw new IllegalArgumentException("name param cannot be null");
        }

        if (classTypes == null) {
            throw new IllegalArgumentException("types param cannot be null");
        }

        if (classTypes.length == 0) {
            throw new IllegalArgumentException("types param cannot be empty");
        }

        for (int i = 0; i < classTypes.length; i++) {
            if (classTypes[i] == null) {
                throw new IllegalArgumentException("types param cannot be null at: " + i);
            }
        }

        if (desc == null) {
            throw new IllegalArgumentException("desc param cannot be null");
        }

        if (hints == null) {
            throw new IllegalArgumentException("hint param cannot be null");
        }

        if (name.indexOf(" ") != -1) {
            throw new IllegalArgumentException("Programmatic error: param name cannot contain whitespace");
        }

        if (name.indexOf("'") != -1) {
            throw new IllegalArgumentException("Programmatic error: param name cannot contain quote '");
        }

        if (name.indexOf("\"") != -1) {
            throw new IllegalArgumentException("Programmatic error: param name cannot contain quote \"");
        }

        this.fName = name;
        this.fNameEnglish = nameEnglish;
        this.fClassTypes = classTypes;
        this.fDesc = desc;
        this.fHints = hints;
        this.fReqd = reqd;
        this.fDefault = def;
        this.fType = type;
        this.log = LoggerFactory.getLogger(this.getClass());
    }

    public String getName() {
        return fName;
    }

    protected Action createHelpAction() {
        return JarResources.createHelpAction(getName());
    }

    public Param.Type getType() {
        if (fType != null) {
            return fType;
        } else if (fReqd) {
            return Param.REQUIRED;
        } else {
            return Param.BASIC;
        }
    }

    public void setType(Param.Type type) {
        this.fType = type;
    }

    public String getHtmlLabel_v2() {
        if (fHtmlLabel_v2 == null) {
            fHtmlLabel_v2 = new StringBuffer("<Html><body><b>").append(getNameEnglish()).append("</b></body></Html>");
        }

        return fHtmlLabel_v2.toString();
    }

    public String getHtmlLabel_v3() {
        if (fHtmlLabel_v3 == null) {
            fHtmlLabel_v3 = new StringBuffer("<Html><body><b>").append(getNameEnglish()).append("</b>");

            if (isReqd() || getType() == Param.PSEUDO_REQUIRED) {
                fHtmlLabel_v3.append("<font color=\"red\">*</font>");
            }

            fHtmlLabel_v3.append("</body></Html>");
        }

        return fHtmlLabel_v3.toString();
    }

    public String getDesc() {
        return fDesc;
    }

    public String getNameEnglish() {
        if (fNameEnglish == null) {
            return fName;
        } else {
            return fNameEnglish;
        }
    }

    public Class[] getTypes() {
        return fClassTypes;
    }

    public String formatForCmdLine() {
        StringBuffer buf = new StringBuffer();

        String ct = Constants.NA;
        if (fClassTypes != null && fClassTypes.length > 0) {
            ct = ClassUtils.shorten(fClassTypes[0]);
        }

        buf.append("-").append(fName).append(' ').append('<').append(ct).append('>').append("\n");
        buf.append("\t").append(fDesc).append("\n");

        Object def = _getCmdLineVersion(fDefault);
        if (def != null) {
            buf.append("\tDefault: ");
            buf.append(def.toString());
        }

        boolean added_start = false;
        if ((fHints != null) && (fHints.length > 0)) {
            for (int i = 0; i < fHints.length; i++) {
                String hn = _getCmdLineVersion(fHints[i]);
                if (hn != null) {
                    if (!added_start) {
                        buf.append("\n\t");
                        buf.append("Hints  : ");
                        added_start = true;
                    }
                    buf.append(hn);
                    if (i != fHints.length - 1) {
                        buf.append(',');
                    }
                }
            }
        }

        if (added_start) {
            buf.append('\n');
        }

        return buf.toString();
    }

    private static String _getCmdLineVersion(final Object obj) {
        String def = null;
        if (obj instanceof PersistentObject) {
            def = ((PersistentObject) obj).getName();
        } else if (obj == null) {
            //def = Constants.NA;
        } else if (obj instanceof PersistentObject[]) {
            PersistentObject[] objs = (PersistentObject[]) obj;
            if (objs.length > 0) {
                def = objs[0].getName();
            } else {
                //def = Constants.NA;
            }
        } else if (obj instanceof Object[]) {
            Object[] objs = (Object[]) obj;
            if (objs.length > 0) {
                def = objs[0].toString();
            } else {
                //def = Constants.NA;
            }
        } else {
            def = obj.toString();
        }

        return def;
    }

    public boolean isReqd() {
        return fReqd;
    }

    /**
     * IMP doesnt evaluate equality of value - just the name
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (obj instanceof AbstractParam) {
            if (fName.equals(((AbstractParam) obj).fName)) {
                return true;
            }
        }

        return false;
    }

    public int hashCode() {
        return fName.hashCode();
    }

    public void setValue(Object value) {
        //if (value != null) log.debug("Setting value: " + value + " for: " + getName() + " class: " + value.getClass());
        //TraceUtils.showTrace();
        //log.debug("#### setting value: " + value);
        this.fValue = value;
    }

    /**
     * if value is not null, returns it
     * else return the default (which can be null too)
     */
    public Object getValue() {
        if (fValue != null) {
            return fValue;
        } else {
            return fDefault;
        }
    }
    
    /**
     * Returns the raw internal value, not replaced by the default.
     */
    protected Object getValueRaw() {
        return fValue;
    }

    /**
     * IMP subclasses must assess whether to override or not
     *
     * @return
     */
    public String getValueStringRepresentation(boolean full) {
        // full has no effect
        Object val = getValue();

        if (val == null) {
            return null;
        } else {
            return val.toString();
        }
    }

    public Object[] getHints() {
        return fHints;
    }

    protected void setHints(Object[] hints) {
        this.fHints = hints;
    }

    public Object getDefault() {
        return fDefault;
    }

    public boolean isSpecified() {
        boolean defaultAvailable;
        final Object def = getDefault();

        if (def != null) {
            defaultAvailable = true;
            if (def instanceof Object[] && ((Object[]) def).length == 0) {
                defaultAvailable = false;
            }

        } else {
            defaultAvailable = false;
        }

        if (this.fValue == null && !defaultAvailable) { // @todo check impact
            return false;
        } else if (this.fValue != null && this.fValue instanceof Object[] && ((Object[]) fValue).length != 0) {
            return true;
        } else if (this.fValue != null && this.fValue.toString().length() == 0 && !defaultAvailable) {
            return false;
        } else {
            return true;    // irrespective of the value
        }
    }

    protected static Integer[] toInts(int[] ints) {
        Integer[] fl = new Integer[ints.length];

        for (int i = 0; i < ints.length; i++) {
            fl[i] = new Integer(ints[i]);
        }

        return fl;
    }
}
