/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.utils.ClassUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Base class for Parser implementations.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public abstract class AbstractParser implements Parser {

    protected final Logger log;

    protected static final Logger klog = XLogger.getLogger(AbstractParser.class);

    protected final Comment fComment;

    private Class fRepClass;

    private String fRepClassName;

    // not always filled -- used ONLY for logging
    private File _importFile;
    private Object _importObjName;

    // not always filled for exports
    private PrintWriter _exportPw;

    private boolean fSilentMode;

    /**
     * Class Constructor.
     */
    protected AbstractParser(Class repClass) {
        if (repClass == null) {
            throw new IllegalArgumentException("Parameter repClass cannot be null");
        }

        //this.log = XLogger.getLogger(this.getClass());
        this.log = XLogger.getLogger(AbstractParser.class);
        this.fComment = new Comment();
        this.fRepClass = repClass;
        this.fRepClassName = ClassUtils.shorten(fRepClass);
    }

    public void export(final PersistentObject pob, final OutputStream os) throws Exception {
        throw new NotImplementedException();
    }

    public List parse(String objname, File file) throws Exception {
        this._importFile = file;
        this._importObjName = objname;
        return parse(objname, new FileInputStream(file));
    }

    public void setSilentMode(boolean silent) {
        this.fSilentMode = silent;
    }

    protected boolean isSilentMode() {
        return fSilentMode;
    }

    /**
     * Utility method to tourn a pob into an unmodifiable list
     */
    protected static List unmodlist(PersistentObject pob) {

        List list = new ArrayList(1);

        list.add(pob);

        return Collections.unmodifiableList(list);
    }

    /**
     * Utility method to place specified pobs in an unmodifiable list
     */
    protected static List unmodlist(PersistentObject[] pobs) {

        List list = new ArrayList(pobs.length);

        for (int i = 0; i < pobs.length; i++) {
            list.add(pobs[i]);
        }

        return Collections.unmodifiableList(list);
    }

    /**
     * adds any comment lines found to the fComnent class var
     *
     * @param bin
     * @return
     * @throws IOException
     */
    protected String nextLine(final BufferedReader bin) throws IOException {
        return nextLine(bin, true);
    }

    protected String nextLine(final BufferedReader bin, final boolean autoAdd2Comment) throws IOException {

        String currLine = bin.readLine();
        if (currLine == null) {
            return null;
        }

        currLine = currLine.trim();

        while ((currLine != null) && ((currLine.length() == 0) || (currLine.startsWith(Constants.COMMENT_CHAR)))) {
            // System.out.println(">>> " + currLine);
            if (currLine.startsWith(Constants.COMMENT_CHAR) && autoAdd2Comment) {
                fComment.add(currLine);
            }

            currLine = bin.readLine();
            if (currLine != null) {
                currLine = currLine.trim();
            }
        }

        //log.debug("comments=" + fComment.toString());

        return currLine;
    }

    // comments are not next'ed over
    protected String nextNonEmptyLine(BufferedReader bin) throws IOException {

        String currLine = bin.readLine();
        if (currLine == null) {
            return null;
        }

        currLine = currLine.trim();

        while ((currLine != null) && ((currLine.length() == 0))) {

            currLine = bin.readLine();
            if (currLine != null) {
                currLine = currLine.trim();
            }
        }

        return currLine;

    }

    protected String nextLineTrimless(BufferedReader bin) throws IOException {
        String currLine = bin.readLine();
        if (currLine == null) {
            return null;
        }

        //currLine = currLine.trim();

        while ((currLine != null) && ((currLine.length() == 0) || (currLine.startsWith(Constants.COMMENT_CHAR)))) {

            if (currLine.startsWith(Constants.COMMENT_CHAR)) {
                fComment.add(currLine);
            }
            currLine = bin.readLine();

            /*
            if (currLine != null) {
                currLine = currLine.trim();
            }
            */
        }

        return currLine;

    }

    protected boolean isNull(Object obj) {
        if (obj == null) {
            return true;
        }

        String s = obj.toString();
        s = s.trim();
        if (s.length() == 0) {
            return true;
        }

        return Constants.NULL.equalsIgnoreCase(s.trim());
    }

    protected boolean isNa(final String s) {
        return Constants.NA.equalsIgnoreCase(s.trim());
    }

    protected boolean isNullorNa(final String s) {
        if (isNull(s)) {
            return true;
        }

        return isNa(s);
    }

    protected PrintWriter startExport(PersistentObject pob, File file) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }

        return startExport(pob, new FileOutputStream(file), file.getName());
    }

    protected PrintWriter startExport(PersistentObject pob, OutputStream os, String toName) throws IOException {
        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }

        if (os == null) {
            throw new IllegalArgumentException("Parameter os cannot be null");
        }

        /* @todo buggy
        if (DataFormat.isCompatibleRepresentationClass(pob, fRepClass) == false) {
            throw new IllegalArgumentException("Invalid pob for this parser - expecting: " + getRepresentationClass() + " but got: " + pob.getClass());
        }
        */

        if (!fSilentMode) {
            //TraceUtils.showTrace();
            log.debug("Exporting: " + pob.getName() + " to: " + toName + " " + pob.getClass());
        }

        _exportPw = new PrintWriter(os);
        return _exportPw;
    }

    protected void doneExport() {
        if (_exportPw != null) {
            _exportPw.flush();
            _exportPw.close();
        }

        //log.info("Done exporting " + fRepClassName);
    }

    //protected void startImport() {
    // dont check as iffy
    /*
    if (_file == null) {
        throw new IllegalStateException("Parameter _file cannot be null");
    }

    if (_objname == null) {
        throw new IllegalStateException("Parameter _objname cannot be null");
    }
    */

    //  log.info("Importing: " + _importObjName + " from: " + _importFile);

    //}

    protected void startImport(String sourcepath) {
        if (!fSilentMode) {
            //TraceUtils.showTrace();
            log.info("Begun importing: " + fRepClassName + " from: " + sourcepath);
        }
    }


    protected void doneImport() {
        //log.info("Done importing: " + fRepClassName);
    }


    protected class Comment {

        private List fLines;

        private Map fKeyValues;

        protected void add(String s) {
            if (s == null || s.length() == 0) {
                return;
            }

            s = s.substring(1, s.length()); // get rid of comm char

            if (s.length() == 0) {
                return;
            }

            if (s.indexOf('=') != -1) {
                if (fKeyValues == null) {
                    fKeyValues = new HashMap();
                }
                String[] fields = ParseUtils.string2strings(s, "= ", true);

                if (fields.length == 1) {
                    // nothing
                } else if (fields.length == 2) {
                    fKeyValues.put(fields[0].toUpperCase(), fields[1]);
                } else {
                    log.warn("Bad comment KEY=VALUE field: Got more tokens than expected: " + fields.length);
                }
            } else {
                if (fLines == null) {
                    fLines = new ArrayList();
                }
                fLines.add(s);
            }
        }

        

        /* @todo deprec after vdb cleanup
        protected Chip getChip() {
            String chipName = getValue(Headers.CHIP);

            if (chipName == null) {
                chipName = getValue(Headers.CHIP.toLowerCase());
            }

            if (chipName != null) {
                return VdbRuntimeResources.getChip(chipName);
            } else {
                return null;
            }
        }
        */

        public String toString() {
            if (fLines == null && fKeyValues == null) {
                return "";
            }

            StringBuffer buf = new StringBuffer();

            if (fLines != null && !fLines.isEmpty()) {
                for (int i = 0; i < fLines.size(); i++) {
                    buf.append(fLines.get(i).toString()).append('\n');
                }
            }

            if (fKeyValues != null && !fKeyValues.isEmpty()) {
                buf.append(fKeyValues.toString());
            }

            return buf.toString();
        }

    }

    protected static List string2stringsV2(String s, int expectedLen) throws IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        String delim = "\t"; // @note, always
        StringTokenizer tok = new StringTokenizer(s, delim, true); // note including the delim in rets
        List ret = new ArrayList();
        String prev = null;
        //System.out.println("PARSING>" + s + "<"  + tok.countTokens());

        int cnt = 0;
        while (tok.hasMoreTokens()) {
            final String curr = tok.nextToken(); // dont trim as curr might be a tab!

            if (cnt == 0) { // the first field
                ret.add(curr.trim()); // always add it, empty or not
            } else {
                if (curr.equals(delim)) {
                    if (prev.equals(delim)) { // 2 consecutive tabs
                        ret.add(""); //empty field
                    } else { // omit because its _the delim_

                    }
                } else {
                    ret.add(curr.trim()); // a real word, ok to trim. Then add
                }
            }

            prev = curr;
            cnt++;
        }

        if (ret.size() == expectedLen) {
            return ret;
        } else if (ret.size() < expectedLen) {
            /// fill out whatever's left with empty
            for (int i = ret.size(); i < expectedLen; i++) {
                ret.add("");
            }
            return ret;

        } else {
            // @note added Nov 28, 2005
            // delete any extra tabs (ret.size() > expectedLen
            List real_ret = new ArrayList();
            for (int i = 0; i < ret.size(); i++) {
                if (i < expectedLen) {
                    real_ret.add(ret.get(i));
                } else {
                    // dont add empty
                    Object obj = ret.get(i);
                    if (obj == null || obj.toString().trim().length() == 0) {
                        // dont add
                    } else {
                        real_ret.add(obj); // cant do anything  might be a genuine format error
                    }
                }
            }

            return real_ret;
        }
    }


    protected static int indexOf(final String s, final List list, final boolean barfIfMising) throws ParserException {
        int index = list.indexOf(s);
        if (index == -1 && barfIfMising) {
            throw new ParserException("Column not found: " + s);
        }
        return index;
    }

}    // End AbstractParser
