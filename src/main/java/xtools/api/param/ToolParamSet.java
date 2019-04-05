/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.parsers.AuxUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * enforces checking to make sure no parameters are reused
 * only 1 param of each type added
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ToolParamSet implements ParamSet {

    private final List fReqdParams;
    private final List fOptParams;
    private final Logger log = Logger.getLogger(ToolParamSet.class);

    /**
     * Class constructor
     *
     * @param name
     */
    public ToolParamSet() {
        this.fReqdParams = new ArrayList();
        this.fOptParams = new ArrayList();
    }

    /**
     * @param paramName
     * @return
     */
    public Param getParam(final String paramName) {
        for (int i = 0; i < fReqdParams.size(); i++) {
            Param p = (Param) fReqdParams.get(i);
            if (p.getName().equals(paramName)) {
                return p;
            }
        }

        for (int i = 0; i < fOptParams.size(); i++) {
            Param p = (Param) fOptParams.get(i);
            if (p.getName().equals(paramName)) {
                return p;
            }
        }

        return null;
    }

    public void addParam(final Param param) {

        if (param == null) {
            throw new IllegalArgumentException("Null param not allowed");
        }

        //log.debug("Adding opt param: " + param.key + " " + param.val);
        checkUniqueness(param);

        if (param.isReqd()) {
            fReqdParams.add(param);
        } else {
            fOptParams.add(param);
        }
    }

    public void addParamAdv(final Param param) {
        param.setType(Param.ADVANCED);
        addParam(param);
    }

    public void addParamPseudoReqd(final Param param) {
        param.setType(Param.PSEUDO_REQUIRED);
        addParam(param);
    }

    public void addParamBasic(final Param param) {
        param.setType(Param.BASIC);
        addParam(param);
    }

    /**
     * combined total -> reqd + opt
     *
     * @return
     */
    public int getNumParams() {
        return fReqdParams.size() + fOptParams.size();
    }

    /**
     * in order, reqd first and then opt
     *
     * @param pos
     * @return
     */
    public Param getParam(final int pos) {

        if (pos < fReqdParams.size()) {
            return (Param) fReqdParams.get(pos);
        } else {
            return (Param) fOptParams.get(pos - fReqdParams.size());
        }
    }

    /**
     * doesnt barf - just collects and fills *values* for params
     * (not params)
     * Overwrites any existing values
     */
    public void fill(final Properties prop) {
        //TraceUtils.showTrace();
        //log.debug("Filling ToolParamSet with properties: " + prop);
        for (int i = 0; i < fReqdParams.size(); i++) {
            Param param = (Param) fReqdParams.get(i);
            String val = prop.getProperty(param.getName());
            String setval;

            if (val != null) {
                val = val.trim();
            }

            //log.debug(param.getName());
            if ((val != null) && val.equals("")) {
                setval = null;    // note
            } else if (val != null && val.equalsIgnoreCase("null")) {
                setval = null;    // note
            } else if (val != null) {
                setval = val;
            } else {
                setval = null;
            }

            param.setValue(setval);

            //log.debug("For param: " + param.getName() + " set value as: " + param.getValue() + " from specified value: " + val);
        }

        // optional ones
        for (int i = 0; i < fOptParams.size(); i++) {
            Param param = (Param) fOptParams.get(i);
            String val = prop.getProperty(param.getName());
            String setval;

            //log.debug(param.getName() + " " + val);
            if (val != null) {
                val = val.trim();
            }

            if ((val != null) && val.equals("")) {
                setval = null;    // note
            } else if (val != null && val.equalsIgnoreCase("null")) {
                setval = null;    // note
            } else if (val != null) {
                setval = val;
            } else {
                setval = null;
            }

            param.setValue(setval);

            //log.debug("For param: " + param.getName() + " value is: " + param.getValue() + " from spec value: " + val + " comp val: " + param.getSelectionComponent().getValue());
        }

        // sanity checks
        final Set badParamNames = new HashSet();
        for (Iterator it = prop.keySet().iterator(); it.hasNext();) {
            String key = it.next().toString();
            Param p = getParam(key);
            if (p == null && !key.equals(PARAM_FILE)) {
                badParamNames.add(key);
            }
        }

        if (!badParamNames.isEmpty()) {
            StringBuffer buf = new StringBuffer("Some specified parameters are UNKNOWN to this usage: ").append(badParamNames.size()).append('\n');
            for (Iterator it = badParamNames.iterator(); it.hasNext();) {
                Object key = it.next();
                buf.append(key).append('\t').append('>').append(prop.getProperty(key.toString())).append("<\n");
            }
            log.warn(buf.toString());
        }

    }

    public FoundMissingFile fileCheckingFill(final Properties prop) {

        List foundFiles = new ArrayList();
        List missingFiles = new ArrayList();
        List foundFilesParamNames = new ArrayList();

        //log.debug("fileCheckinFill ToolParamSet with properties: " + prop);
        // reqd ones
        for (int i = 0; i < fReqdParams.size(); i++) {
            Param param = (Param) fReqdParams.get(i);
            String val = prop.getProperty(param.getName());
            String setval;

            if (val != null) {
                val = val.trim();
            }

            //log.debug(param.getName());
            if ((val != null) && val.equals("")) {
                setval = null;    // note
            } else if (val != null) {
                setval = val;
            } else {
                setval = null;
            }

            // we dont parse the analysis dir!
            if ((setval != null) && (param.isFileBased()) &&
                    !(param instanceof ReportDirParam) && !(NamingConventions.isURL(setval))) { // check for existence
                File f = new File(setval);
                f = AuxUtils.getBaseFileFromAuxFile(f);
                if (f.exists() == false) {
                    setval = null; // null it
                    missingFiles.add(f);
                } else {
                    foundFilesParamNames.add(param.getName());
                    foundFiles.add(f);
                }
            }

            param.setValue(setval);

            //log.debug("For param: " + param.getName() + " set value as: " + param.getValue() + " from spec value: " + val);
        }

        // optional ones
        for (int i = 0; i < fOptParams.size(); i++) {
            Param param = (Param) fOptParams.get(i);
            String val = prop.getProperty(param.getName());
            String setval;

            //log.debug(param.getName() + " " + val);
            if (val != null) {
                val = val.trim();
            }

            if ((val != null) && val.equals("")) {
                setval = null;    // note
            } else if (val != null) {
                setval = val;
            } else {
                setval = null;
            }

            if ((setval != null) && (param.isFileBased()) && !(NamingConventions.isURL(setval))) { // check for existence
                File f = new File(setval);
                if (f.exists() == false) {
                    setval = null; // null it
                    missingFiles.add(f);
                } else {
                    foundFilesParamNames.add(param.getName());
                    foundFiles.add(f);
                }
            }

            param.setValue(setval);

            //log.debug("For param: " + param.getName() + " value is: " + param.getValue() + " from spec value: " + val + " comp val: " + param.getSelectionComponent().getValue());
        }

        FoundMissingFile fmf = new FoundMissingFile();
        fmf.foundFiles = (File[]) foundFiles.toArray(new File[foundFiles.size()]);
        fmf.foundFilesParamNames = (String[]) foundFilesParamNames.toArray(new String[foundFilesParamNames.size()]);
        fmf.missingFiles = (File[]) missingFiles.toArray(new File[missingFiles.size()]);

        return fmf;
    }

    // NO checking
    public Properties toProperties() {

        final Properties props = new Properties();

        for (int i = 0; i < getNumParams(); i++) {
            final Param param = getParam(i);
            //log.debug("name=" + param.getName() + " value=" + param.getValue() + " str=" + param.getValueStringRepresentation());
            if (param.isSpecified()) {
                String s = param.getValueStringRepresentation(true);
                if (s != null) {
                    s = s.trim();
                    if (s.length() > 0) {
                        props.setProperty(param.getName(), s);
                    }
                }
            }
        }

        return props;
    }

    /**
     * checks and barfs if any of the required ones are missing
     */
    public void check() throws MissingReqdParamException {

        // check
        StringBuffer errors = new StringBuffer("\n\nError: Missing the following required parameters:\n");
        List missingParams = new ArrayList();

        for (int i = 0; i < fReqdParams.size(); i++) {
            Param param = (Param) fReqdParams.get(i);

            /* Orig form
            if ((!param.isSpecified()) && (param.getDefault() == null)) {
                errors.append('\t').append(((Param) fReqdParams.get(i)).formatForCmdLine());
                missingParams.add(param);
            } else {
                log.debug("reqd param: " + param.getName() + " was specified: " + param.isSpecified() + " with value >" + param.getValue() + "<");
            }
            */

            // new form
            if (!param.isSpecified()) {
                errors.append('\t').append(((Param) fReqdParams.get(i)).formatForCmdLine());
                missingParams.add(param);
            } else {
                //log.debug("reqd param: " + param.getName() + " was specified: " + param.isSpecified() + " with value >" + param.getValue() + "<");
            }

        }

        errors.append("\n-----------------------------------------------------------\n");
        errors.append(getUsage());

        if (!missingParams.isEmpty()) {
            throw new MissingReqdParamException((Param[]) missingParams.toArray(new Param[missingParams.size()]));
        }
    }

    public boolean isRequiredAllSet() throws RuntimeException {

        for (int i = 0; i < fReqdParams.size(); i++)
        { // @todo make this actually work (i think its gets confused on Object[]{})
            final Param param = (Param) fReqdParams.get(i);
            //log.debug(param.getName() + " is set: " + param.isSpecified() + " def: " + param.getDefault());
            if ((!param.isSpecified()) && (param.getDefault() == null)) {
                //log.debug("FALSE!");
                return false;
            }
        }

        return true;
    }

    /**
     * returns a safe copy
     *
     * @return
     */
    public Param[] getParams() {

        List all = new ArrayList();

        for (int i = 0; i < fReqdParams.size(); i++) {
            all.add(fReqdParams.get(i));
        }

        for (int i = 0; i < fOptParams.size(); i++) {
            all.add(fOptParams.get(i));
        }

        return (Param[]) all.toArray(new Param[all.size()]);
    }

    public Param[] getParams(Param.Type thisType, boolean excludeCommonOnes) {
        Param[] params = getParams();
        List use = new ArrayList();
        for (int i = 0; i < params.length; i++) {

            if (excludeCommonOnes && params[i] instanceof GuiParam) { // @note
                // do nothing
            } else if (params[i].getType().equals(thisType)) {
                use.add(params[i]);
            }
        }

        return (Param[]) use.toArray(new Param[use.size()]);
    }

    /**
     * @param paramClass
     * @return
     */
    public ReportDirParam getAnalysisDirParam() {
        for (int i = 0; i < getNumParams(); i++) {
            Param p = getParam(i);
            if (p instanceof ReportDirParam) {
                return (ReportDirParam) p;
            }
        }

        return null;

    }

    /**
     * reports label param if available
     * else null
     *
     * @return
     */
    public ReportLabelParam getReportLabelParam() {
        for (int i = 0; i < getNumParams(); i++) {
            Param p = getParam(i);
            if (p instanceof ReportLabelParam) {
                return (ReportLabelParam) p;
            }
        }

        return null;

    }

    /**
     * @param paramClass
     * @return
     */
    public GuiParam getGuiParam() {
        for (int i = 0; i < getNumParams(); i++) {
            Param p = getParam(i);
            if (p instanceof GuiParam) {
                return (GuiParam) p;
            }
        }

        return null;

    }

    public String getAsCommand(final boolean fullfilepaths) {

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < getNumParams(); i++) {
            Param param = getParam(i);

            if (! (param instanceof GuiParam)) {
                String rep = param.getValueStringRepresentation(fullfilepaths);
                if (rep != null) {
                    rep = rep.trim();
                    if (rep.length() > 0) {
                        buf.append(" -").append(param.getName()).append(' ').append(rep);
                    }
                }
            }
        }

        return buf.toString();
    }

    /**
     * prints to system.out all cmd line params - reqd and optional.
     */

    public String getUsage() {

        // TraceUtils.showTrace();

        StringBuffer buf = new StringBuffer("\n######## USAGE ########\n\n");

        if (fReqdParams.size() > 0) {
            buf.append("Required Parameters:").append('\n');

            for (int i = 0; i < fReqdParams.size(); i++) {
                buf.append(((Param) fReqdParams.get(i)).formatForCmdLine()).append('\n');
            }
        } else {
            buf.append("No required parameters").append('\n');
        }

        buf.append('\n');

        if (fOptParams.size() > 0) {
            buf.append("Optional Parameters:").append('\n');

            for (int i = 0; i < fOptParams.size(); i++) {
                buf.append(((Param) fOptParams.get(i)).formatForCmdLine()).append('\n');
            }
        } else {
            buf.append("No optional parameters").append('\n');
        }

        return buf.toString();
    }

    public void printfUsage() {
        System.out.println(getUsage());
    }

    /**
     * param whether reqd or not cannot be in duplicate
     *
     * @param param
     */
    private void checkUniqueness(Param param) {

        if (param == null) {
            throw new IllegalArgumentException("param cannot be null");
        }

        //log.debug("fOptParams: " + fOptParams + " size: " + fOptParams.size());
        //log.debug("fReqdParams: " + fReqdParams + " size: " + fReqdParams.size());
        if (fOptParams.contains(param)) {
            throw new RuntimeException("Duplicated param in declarations - already have param: "
                    + param + " # params: " + getNumParams()
                    + " in the opt param list");
        }

        if (fReqdParams.contains(param)) {
            throw new RuntimeException("Duplicated param in declarations - already have param: "
                    + param.getName() + " # params: " + getNumParams()
                    + " in the reqd param list");
        }
    }

    /**
     * Only for use by a Tool after it declares its parameters
     */
    public void sort() {
        Collections.sort(fReqdParams, new ParamComparator());
        Collections.sort(fOptParams, new ParamComparator());
    }

}    // End ToolParamSet

/**
 * MOVED TO SEPERATE CLASS - DELETE LATER IF FOUND TO BE OK
 *
 * @todo '-' in file paths/names causes the parsing to barf
 *
 * two kinds of keyvals are allowed
 * 1) -cls foo    <-- parameter name "cls" has value foo
 * 2) -tag        <-- parameter name "tag: is TRUE
 * Trouble is that we need to know if tag is a boolean param or not
 * value less specification is only allowed for booleans
 * Other examples
 * -tag false  <-- param tag is Boolean.FALSE
 * -names foo,bar,zok -> param names -> foo, bar, zok (NOT parsed here)
 *
 * @param args
 */
/*
public void fill(String[] args) {

    // first recreate the arg line -- its easier to parse
    StringBuffer buf = new StringBuffer();

    for (int i = 0; i < args.length; i++) {
        buf.append(args[i]).append(" ");
    }

    String argline = buf.toString().trim();

    StringTokenizer tok = new StringTokenizer(argline, "-"); // IMP spaces are NOT delimiters at this stage

    Set keyval = new HashSet();
    while (tok.hasMoreElements()) {
        String kv = tok.nextToken();
        keyval.add(kv);
        //log.debug("found kv: " + kv);
    }

    // now fill them up
    Iterator it = keyval.iterator();
    while (it.hasNext()) {
        String kv = it.next().toString();
        tok = new StringTokenizer(kv, " ="); // '=' as a favor to human errors
        _fillParam(tok, kv);
    }

}

private void _fillParam(StringTokenizer tok, String origString) {
    List tokens = ParseUtils.getUniqueTokens(tok);

    // now the trouble is that the second param, sometimes a file name, can have spaces
    // note that in general there is nothing to prevent the 'value' from having a space
    // as it doesnt affect the '-' based tokenizing (' ' is NOT a delim that seperates 'key-val' pairs
    // so as a mechanism to allow space in file names (the most common instance where space occurs in  a key-val)

    String param_name = null;
    String param_val = null;

    if (tokens.size() == 1) {
        param_name = tokens.get(0).toString();
    } else if (tokens.size() == 2) {
        param_name = tokens.get(0).toString();
        param_val = tokens.get(1).toString();
    } else {
        log.warn("More than 2 tokens for key-value pair >" + origString + "<" + " " + tokens.size());
        int num = tokens.size();
        param_name = tokens.get(0).toString();

        // heres the space fix
        param_val = "";
        for (int i = 1; i < num; i++) {
            param_val = param_val + tokens.get(i) + " ";
        }
    }

    param_name = param_name.trim();
    if (param_val != null) {
        param_val = param_val.trim();
    }

    //System.out.println("Doing >" + param_name + "< >" + param_val + "<");

    Param p = getParam(param_name);
    //log.debug("Asked for: " + param_name + " got: " + p);
    if (p == null) {
        log.warn("Invalid parameter for xtool >" + param_name + "< THIS PARAMETER WAS IGNORED!!");
        // dont barf (happens on cmd line sometimes, no point penalizing totally)
        //throw new IllegalArgumentException("Invalid parameter for xtool: " + param_name);
    }

    if ((param_val == null) || (param_val.length() == 0)) {
        if (p instanceof BooleanParam) {
            param_val = Boolean.TRUE.toString(); // @note
        }
    } else if (param_val == null) {
        throw new IllegalArgumentException("Invalid empty value for parameter: " + param_name);
    }

    p.setValue(param_val);

}
*/