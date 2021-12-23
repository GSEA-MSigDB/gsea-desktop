/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.Annot;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.FeatureAnnot;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.SampleAnnot;
import edu.mit.broad.genome.utils.ClassUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Base class for Parser implementations.
 *
 * @author Aravind Subramanian
 * @author David Eby
 */
public abstract class AbstractParser implements Parser {
    //TODO: let's just have ONE Logger, and use the actual subclass
    protected final Logger log;
    protected static final Logger klog = Logger.getLogger(AbstractParser.class);

    protected final Comment fComment;

    // TODO: we ought to represent this instead as a Generic type parameter on AbstractParser instead.
    private Class fRepClass;

    private String fRepClassName;

    // not always filled -- used ONLY for logging
    private File _importFile;
    private Object _importObjName;

    // not always filled for exports
    private PrintWriter _exportPw;

    private boolean fSilentMode;

    protected AbstractParser(Class repClass) {
        if (repClass == null) {
            throw new IllegalArgumentException("Parameter repClass cannot be null");
        }

        //this.log = Logger.getLogger(this.getClass());
        this.log = Logger.getLogger(AbstractParser.class);
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
    }

    protected void startImport(String sourcepath) {
        if (!fSilentMode) {
            //TraceUtils.showTrace();
            log.info("Begun importing: " + fRepClassName + " from: " + sourcepath);
        }
    }

    protected void doneImport() {
        //log.info("Done importing: " + fRepClassName);
    }

    protected float parseStringToFloat(String s, boolean correctDoubleQuotes) {
        s = s.trim();
        if (s.length() == 0) {
            return Float.NaN;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException nfe) {
            if (s.equalsIgnoreCase(Constants.NA)) { return Float.NaN; }
            // Before failing, check for a double-quoted value (if indicated)
            if (correctDoubleQuotes && s.startsWith("\"") && s.endsWith("\"")  && s.length() > 1) {
                return parseStringToFloat(s.substring(1, s.length()-1), false);
            }
            
            throw nfe;
        }
    }

    protected List parseTextMatrixToDataset(String objName, List<String> lines, List<String> colNames, boolean hasDesc) throws Exception {
        final int lineCount = lines.size();
        List<String> rowNames = new ArrayList<String>(lineCount);
        List<String> rowDescs = hasDesc ? new ArrayList<String>(lineCount) : null;
        List<float[]> data = new ArrayList<float[]>(lineCount);
        int skippedMissingRows = 0, partialMissingRows = 0;
        boolean foundInfiniteValues = false;

        int startPos = (hasDesc) ? 2 : 1;
        int expFields = colNames.size() + startPos;
        for (int i = 0; i < lineCount; i++) {
            String currLine = lines.get(i);
            List<String> fields = string2stringsV2(currLine, expFields); // spaces allowed in name & desc field so DONT tokenize them
            String rowname = parseRowname(fields.get(0).trim(), i);

            float[] dataRow = parseFieldsIntoFloatArray(fields, i, startPos, rowname);
            int countMissing = countMissingValues(dataRow, i, rowname);
            if (countMissing < dataRow.length) {
                if (countMissing > 0) { partialMissingRows++; }
                data.add(dataRow);
                rowNames.add(rowname);

                if (hasDesc) {
                    String desc = fields.get(1).trim();
                    if (desc.length() == 0) { desc = Constants.NA; }
                    rowDescs.add(desc);
                }
            } else {
                skippedMissingRows++;
            }
            foundInfiniteValues |= checkForInfiniteValues(dataRow, i, rowname);
        }

        if (data.isEmpty()) { throw new ParserException("Data was missing in all rows!"); }

        Matrix matrix = new Matrix(data.size(), colNames.size());
        for (int i = 0; i < data.size(); i++) {
            matrix.setRow(i, data.get(i));
        }

        final FeatureAnnot ann = new FeatureAnnot(objName, rowNames, rowDescs);
        ann.addComment(fComment.toString());
        final SampleAnnot sann = new SampleAnnot(objName, colNames);

        final Dataset ds = new DefaultDataset(objName, matrix, rowNames, colNames, new Annot(ann, sann));
        ds.addComment(fComment.toString());
        if (foundInfiniteValues) {
            String warning = "Infinite values detected in this dataset. This may cause unexpected results in the calculations or failures in plotting.";
            log.warn(warning);
            ds.addWarning(warning + "  See the log for more details.");
        }
        if (partialMissingRows > 0) {
            String warning = "There were " + partialMissingRows + " row(s) in total with partially missing data in this dataset.";
            log.warn(warning);
            ds.addWarning(warning + "  See the log for more details.");
        }
        if (skippedMissingRows > 0) {
            String warning = "There were " + skippedMissingRows + " row(s) in total with all data missing in this dataset.  These will be ignored.";
            log.warn(warning);
            ds.addWarning(warning + "  See the log for more details.");
        }
        doneImport();
        return unmodlist(new PersistentObject[]{ds});
    }
    
    protected boolean checkForInfiniteValues(float[] dataRow, int row, String rowname) {
        for (int i = 0; i < dataRow.length; i++) {
        	if (Float.isInfinite(dataRow[i])) {
                log.warn("Infinite values found in row " + (row+1) + " of the data matrix with Name '" + rowname + "'.");
                return true;
        	}
        }
    	return false;
    }

    protected int countMissingValues(float[] dataRow, int row, String rowname) {
        int missingCount = 0;
        for (int i = 0; i < dataRow.length; i++) {
        	if (Float.isNaN(dataRow[i])) { missingCount++; }
        }
        if (missingCount == dataRow.length) {
            log.warn("All values missing in row " + (row+1) + " of the data matrix with Name '" + rowname + "'.  Row will be ignored.");
        } else  if (missingCount > 0) {
            log.warn("Missing values found in row " + (row+1) + " of the data matrix with Name '" + rowname + "'.");
        }
        return missingCount;
    }
    
    protected float[] parseFieldsIntoFloatArray(List<String> fields, int row, int startingField, String rowname) {
        float[] dataRow = new float[fields.size() - startingField];
        for (int f = startingField, col = 0; f < fields.size(); f++) {
            String s = fields.get(f);
            try {
                dataRow[col++] = parseStringToFloat(s, true);
            } catch (NumberFormatException nfe) {
                log.error("Could not parse '" + s + "' as a floating point number in row " + (row+1) + " of the data matrix with Name '" + rowname + "'.");
                throw nfe;
            }
        }
        return dataRow;
    }

    protected String parseRowname(String rowname, int row) throws ParserException {
        if (rowname.length() == 0) {
            throw new ParserException("Bad rowname - cant be empty at row " + (row+1) + " of data matrix.");
        } else {
            // Strip double-quotes if present (but only if present on *both* ends and not the only character!)
            if (rowname.startsWith("\"") && rowname.endsWith("\"") && rowname.length() > 1) {
                rowname = rowname.substring(1, rowname.length()-1).trim();
                if (rowname.length() == 0) {
                    throw new ParserException("Bad rowname - cant be empty at row " + (row+1) + " of data matrix.");
                }
            }
        }
        return rowname;
    }
    
    protected class Comment {
        private List<String> fLines;

        private Map<String, String> fKeyValues;

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
                    fKeyValues = new HashMap<>();
                }
                String[] fields = ParseUtils.string2strings(s, "= ");

                if (fields.length == 1) {
                    // nothing
                } else if (fields.length == 2) {
                    fKeyValues.put(fields[0].toUpperCase(), fields[1]);
                } else {
                    log.warn("Bad comment KEY=VALUE field: Got more tokens than expected: " + fields.length);
                }
            } else {
                if (fLines == null) {
                    fLines = new ArrayList<>();
                }
                fLines.add(s);
            }
        }

        public String toString() {
            if (fLines == null && fKeyValues == null) {
                return "";
            }

            StringBuilder buf = new StringBuilder();

            if (fLines != null && !fLines.isEmpty()) {
                for (int i = 0; i < fLines.size(); i++) {
                    buf.append(fLines.get(i)).append('\n');
                }
            }

            if (fKeyValues != null && !fKeyValues.isEmpty()) {
                buf.append(fKeyValues);
            }

            return buf.toString();
        }
    }

    protected static List<String> string2stringsV2(String s, int expectedLen) throws IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        String delim = "\t"; // @note, always
        StringTokenizer tok = new StringTokenizer(s, delim, true); // note including the delim in rets
        List<String> ret = new ArrayList<String>();
        String prev = null;

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
            List<String> real_ret = new ArrayList<String>();
            for (int i = 0; i < ret.size(); i++) {
                if (i < expectedLen) {
                    real_ret.add(ret.get(i));
                } else {
                    // dont add empty
                    String item = ret.get(i);
                    if (item == null || item.trim().length() == 0) {
                        // dont add
                    } else {
                        real_ret.add(item); // cant do anything  might be a genuine format error
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
}
