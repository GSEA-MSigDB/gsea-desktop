/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.utils.FileUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * public static utilities to aid in parsing
 * <p/>
 * <bold> Implementation Notes </bold><br>
 * There are a range of possible exceptions that can occur duing parsing.
 * Rather than throw an error-specific exception on encountering an error,
 * a ParseException is thrown that captures the base exception(s).
 * <p/>
 * <confirm-this>
 * In addition,
 * during parsing, often runtime (unchecked) exceptions are show-stoppers. These
 * too are caught and parcelled into ParseException.
 * </confirm-this>
 *
 * @author Michael Angelo
 * @author Aravind Subramanian
 * @version 1.0
 */
public class ParseUtils {

    private static final char DEFAULT_FIELD_SEP_CSV = ',';

    /**
     * NamingConventions delimiters - <pre>" \t\n,;:"</pre>
     */
    public final static String DEFAULT_DELIMS = " \t\n,;:";


    /**
     * the last token
     * Example: foo.baz -> baz
     *
     * @param s
     * @param delimiters
     * @return
     */
    public static String getLastToken(String s, String delimiters) {

        StringTokenizer tok = new StringTokenizer(s, delimiters);

        while (tok.hasMoreTokens()) {
            s = tok.nextToken();
        }

        return s;
    }

    public static float[] string2floats(String s)
            throws NumberFormatException, IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        StringTokenizer tok = new StringTokenizer(s, DEFAULT_DELIMS);

        return _doFloatParse(tok);
    }

    public static int[] string2ints(String s, String delims)
            throws NumberFormatException, IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        StringTokenizer tok = new StringTokenizer(s, delims);

        return _doIntParse(tok);
    }

    public static int[] string2ints(String s, char delim)
            throws NumberFormatException, IllegalArgumentException {

        return string2ints(s, Character.toString(delim));
    }

    public static float[] string2floats(String s, String delims)
            throws NumberFormatException, IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        //System.out.println("##### delim>" + delims + "<");
        StringTokenizer tok = new StringTokenizer(s, delims);
        return _doFloatParse(tok);
    }

    public static Vector string2Vector(String s, char delim) {
        return new Vector(string2floats(s, Character.toString(delim)));
    }

    private static int[] _doIntParse(StringTokenizer tok) throws NumberFormatException {

        int[] ret = new int[tok.countTokens()];
        int i = 0;

        String curr;
        while (tok.hasMoreTokens()) {
            curr = tok.nextToken();
            // doing it the long way so that on error, the exact element that
            // caused it is specified in the exception message
            ret[i] = Integer.parseInt(curr);
            i++;
        }

        return ret;
    }

    private static float[] _doFloatParse(StringTokenizer tok) throws NumberFormatException {

        float[] ret = new float[tok.countTokens()];
        //klog.debug("# of tokens: " + tok.countTokens());

        int i = 0;

        String curr;
        while (tok.hasMoreTokens()) {
            curr = tok.nextToken();
            // doing it the long way so that on error, the exact element that
            // caused it is specified in the exception message
            ret[i] = Float.parseFloat(curr);
            i++;
        }

        return ret;
    }

    public static String[] string2strings(final String s,
                                          final String delim,
                                          final boolean useNullonMagicNullWord) throws IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot parse on null String");
        }

        StringTokenizer tok = new StringTokenizer(s, delim);
        String[] ret = new String[tok.countTokens()];
        int i = 0;

        while (tok.hasMoreTokens()) {
            ret[i] = tok.nextToken().trim();
            i++;
        }

        return ret;
    }

    public static String[] string2stringsV2(String s) throws IllegalArgumentException {
        List ret = string2stringsV2_list(s);
        return (String[]) ret.toArray(new String[ret.size()]);
    }

    // double tabs are tolerated and no need for the NULL thing
    public static List string2stringsV2_list(String s) throws IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        s = s.trim(); // no tabs before or after

        StringTokenizer tok = new StringTokenizer(s, "\t", true); // note including the delim in rets
        List ret = new ArrayList();

        //System.out.println("# " + tok.countTokens());
        String prev = null;
        while (tok.hasMoreTokens()) {
            String curr = tok.nextToken(); // dont trim!
            if (!curr.equals("\t")) {
                ret.add(curr);
            }

            if (curr.equals(prev)) { // 2 consecutive tabs
                ret.add(""); //empty field
            }

            prev = curr;
        }

        return ret;
    }

    public static List string2stringsList(String s, String delim) throws IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        StringTokenizer tok = new StringTokenizer(s, delim);
        List ret = new ArrayList(tok.countTokens());

        while (tok.hasMoreTokens()) {
            ret.add(tok.nextToken().trim());
        }

        return ret;
    }

    public static Set string2stringsSet(String s, String delim) throws IllegalArgumentException {
        if (null == s) {
            return new HashSet();
        }

        StringTokenizer tok = new StringTokenizer(s, delim);
        Set ret = new HashSet();

        while (tok.hasMoreTokens()) {
            String elem = tok.nextToken().trim();
            if (elem != null) {
                ret.add(elem);
            }
        }

        return ret;
    }

    /**
     * Searches specified string for presence of a
     * "oft-used" delimiter.
     *
     * @return '\0\ if no hits
     *         NamingConventions delimiters are: {'\t', ' ', ',', '\n'}
     *         From mangelo code base.
     */
    public static char getDelim(final String aStr) {

        char[] theDelims = {'\t', ' ', ',', '\n'};
        char theDelim = '\0';

        int curInd = -1;
        int ind = 0;

        // Check for possible delimiters of the class assignments
        while ((curInd == -1) && (ind < theDelims.length)) {
            theDelim = theDelims[ind];
            curInd = aStr.indexOf(theDelim, 0);

            ind++;
        }

        if (curInd == -1) {
            theDelim = '\0';
        }

        return theDelim;
    }

    /**
     * Splits a String of integers by specified delimiter and feeds the results
     * (the integers) into specified integer array.
     * <p/>
     * This is from ma's code base - hmm why not just return the int[]??
     *
     * @return The number of integers
     */
    public static int splitIntegers(String aStr, char aDelim, int[] aInts) throws Exception {

        int curInd = 0, prevInd = 0;
        int i = 0, theLength = aStr.length();

        while (curInd < theLength) {
            curInd = aStr.indexOf(aDelim, prevInd);

            if (curInd < 0) {
                curInd = theLength;
            }

            String subStr = aStr.substring(prevInd, curInd).trim();

            aInts[i] = Integer.valueOf(subStr).intValue();

            i++;

            prevInd = curInd + 1;

            if (i == aInts.length) {
                break;
            }
        }


        return i;
    }

    public static List getUniqueTokens(StringTokenizer tok) {

        List uniqs = new ArrayList();
        while (tok.hasMoreElements()) {
            String t = tok.nextToken().trim();
            if (!uniqs.contains(t)) {
                uniqs.add(t);
            }
        }

        return uniqs;
    }

    /**
     * Convenience method to read in a ffn into an ArrayList Each vector
     * element corresponds to a single line (in order) from the ffn. (ffn->
     * file of file names) Blank lines in file are ignored Leading and trailing
     * whitespace is trimmed
     *
     * @param aFile Description of the Parameter
     * @return Description of the Return Value
     * @throws IOException Description of the Exception
     */
    public static List readFfn(File aFile) throws IOException {

        BufferedReader buf = new BufferedReader(new FileReader(aFile));
        String line;
        ArrayList lines = new ArrayList();

        line = nextLine(buf);

        while (line != null) {
            if (lines.contains(line) == false) {
                lines.add(line);
            }
            line = nextLine(buf);
        }

        buf.close();

        return lines;
    }

    /*
     * The filepath was formerly treated as a path OR a URL, but this lead to errors on Windows where
     * our URL detection fails to identify 'C:/' as a path.  We might revamp this later (see GSEA-1170)
     * but for now these will be restricted to being handled as *local files only*.  Loading param_files
     * from URL seems like an unlikely use-case anyway.
     */
    public static Properties readKeyVal(final String filepath,
                                        final boolean containsHeaderLine,
                                        final boolean enforceValueMustExist,
                                        final boolean enforceNonRepeatedKeys) throws IOException {
        // Inlining code from working code path formerly in FileUtils.toBufferedReader().
        URI uri = new File(filepath).toURI();
        URL url = uri.toURL();
        BufferedReader buf = new BufferedReader(new InputStreamReader(url.openStream()));
        return readKeyVal(buf, containsHeaderLine, enforceValueMustExist, enforceNonRepeatedKeys);
    }

    /**
     * Convenience method to read in a file with key=value pairs
     * into a hashtable
     * Each element corresponds to a single line (NOT in order)
     * from the file.
     * Blank lines in file are ignored
     * Leading and trailing whitespace is trimmed
     * Optionally lines beginning with Constants.COMMENT_CHAR are ignored
     * key1\tval1
     * key2\tval2
     */
    public static Properties readKeyVal(final BufferedReader buf,
                                        final boolean containsHeaderLine,
                                        final boolean enforceValueMustExist,
                                        final boolean enforceNonRepeatedKeys) throws IOException {

        String line;
        final Properties prp = new Properties();

        line = nextLine(buf);

        if (containsHeaderLine) {
            line = nextLine(buf);
        }

        int lineNum = 0;
        List duplLines = new ArrayList();
        while (line != null) {
            StringTokenizer tok = new StringTokenizer(line, "\t");
            String key;
            String val = null;
            if (tok.countTokens() == 2) {
                key = tok.nextToken().trim();
                val = tok.nextToken().trim();
            } else if (tok.countTokens() == 1) {
                key = tok.nextToken().trim();

            } else {
                throw new IOException("Bad line format: " + line + " # tokens: " + tok.countTokens() + " line: " + lineNum);
            }

            if (enforceValueMustExist) {
                if (val == null || val.length() == 0) {
                    throw new RuntimeException("Value must exist. Missing on line: " + lineNum + " val: " + val);
                }
            }

            if (key.length() == 0) {
                throw new RuntimeException("Empty key on line: " + lineNum);
            }

            if (prp.containsKey(key) && enforceNonRepeatedKeys) {
                duplLines.add(line);
            }

            prp.setProperty(key, val);
            line = nextLine(buf);
            lineNum++;
        }

        buf.close();

        if (!duplLines.isEmpty() && enforceNonRepeatedKeys) {
            StringBuffer sbuf = new StringBuffer();
            for (int i = 0; i < duplLines.size(); i++) {
                sbuf.append(duplLines.get(i)).append('\n');
            }
            throw new RuntimeException("There are repeated keys:" + duplLines.size() + "\n" + duplLines);
        }

        if (lineNum == 0) {
            throw new IllegalArgumentException("Empty input stream!! ");
        }

        return prp;
    }

    public static String slurp(URL url, boolean ignoreanycomments) throws IOException {
        return slurp(_buf(url), ignoreanycomments);
    }

    public static String slurp(BufferedReader buf, boolean ignoreanycomments) throws IOException {

        String line;
        String s = "";

        while ((line = buf.readLine()) != null) {
            if ((ignoreanycomments) && (line.startsWith(Constants.COMMENT_CHAR))) {
                ;
            } else {
                s = s.concat(line).concat("\n");
            }
        }

        buf.close();

        return s;
    }

    public static String[] slurpIntoArray(URL url, boolean ignoreanycomments) throws IOException {
        Set set = slurpIntoSet(_buf(url), ignoreanycomments);
        return (String[]) set.toArray(new String[set.size()]);
    }

    public static Set slurpIntoSet(BufferedReader buf, boolean ignoreanycomments) throws IOException {
        Set set = new HashSet();
        String line;

        while ((line = buf.readLine()) != null) {
            line = line.trim();

            if (line.length() == 0) {
                continue;
            }

            if ((ignoreanycomments) && (line.startsWith(Constants.COMMENT_CHAR))) {
                continue;
            }

            set.add(line);
        }

        buf.close();

        return set;
    }

    public static int countLines(File file, boolean ignoreblanklines) throws IOException {
        return countLines(_buf(file), ignoreblanklines);
    }

    public static int countLines(BufferedReader buf, boolean ignoreblanklines) throws IOException {

        int numberOfLines = 0;

        if (!ignoreblanklines) {
            while (buf.readLine() != null) {
                numberOfLines++;
            }
        } else {
            String currLine = buf.readLine();
            while (currLine != null) {
                if (currLine.trim().length() > 0) {
                    numberOfLines++;
                }

                currLine = buf.readLine();
            }
        }

        buf.close();

        return numberOfLines;
    }

    /**
     * adds any comment lines found to the fComnent class var
     *
     * @param buf
     * @return
     * @throws IOException
     */
    public static String nextLine(BufferedReader buf) throws IOException {

        String currLine = buf.readLine();

        if (currLine == null) {
            return null;
        }

        currLine = currLine.trim();

        while ((currLine != null)
                && ((currLine.length() == 0) || (currLine.startsWith(Constants.COMMENT_CHAR)))) {
            currLine = buf.readLine();

            if (currLine != null) {
                currLine = currLine.trim();
            }
        }

        return currLine;
    }

    private static BufferedReader _buf(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("Parameter url cannot be null");
        }
        InputStreamReader isr = new InputStreamReader(url.openStream());
        return new BufferedReader(isr);
    }

    private static BufferedReader _buf(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }

        return new BufferedReader(new FileReader(file));
    }

    /**
     * parse: break the input String into fields
     *
     * @return java.util.Iterator containing each field
     *         from the original as a String, in order.
     */
    public static String[] string2strings_csv(final String csvLine) {
        final StringBuffer sb = new StringBuffer();

        final List list = new ArrayList();
        int i = 0;

        do {
            sb.setLength(0);
            if (i < csvLine.length() && csvLine.charAt(i) == '"') {
                i = advQuoted_for_csv(csvLine, sb, ++i);    // skip quote
            } else {
                i = advPlain_for_csv(csvLine, sb, i);
            }
            list.add(sb.toString());
            i++;
        } while (i < csvLine.length());

        return (String[]) list.toArray(new String[list.size()]);
    }

    public static List string2stringsList_csv(final String csvLine) {
        final String[] ss = string2strings_csv(csvLine);
        final List list = new ArrayList();
        for (int i = 0; i < ss.length; i++) {
            list.add(ss[i]);
        }

        return list;
    }

    /**
     * advQuoted: quoted field; return index of next separator
     */
    private static int advQuoted_for_csv(final String s, final StringBuffer sb, final int i) {
        int j;
        int len = s.length();
        for (j = i; j < len; j++) {
            if (s.charAt(j) == '"' && j + 1 < len) {
                if (s.charAt(j + 1) == '"') {
                    j++; // skip escape char

                } else if (s.charAt(j + 1) == DEFAULT_FIELD_SEP_CSV) { //next delimeter

                    j++; // skip end quotes

                    break;
                }
            } else if (s.charAt(j) == '"' && j + 1 == len) { // end quotes at end of line

                break; //done

            }
            sb.append(s.charAt(j));    // regular character.

        }
        return j;
    }

    /**
     * advPlain: unquoted field; return index of next separator
     */
    private static int advPlain_for_csv(final String s, final StringBuffer sb, final int i) {
        int j;

        j = s.indexOf(DEFAULT_FIELD_SEP_CSV, i); // look for separator

        //klog.debug("csv: " + "i = " + i + " j = " + j);
        if (j == -1) {                   // none found

            sb.append(s.substring(i));
            return s.length();
        } else {
            sb.append(s.substring(i, j));
            return j;
        }
    }


}    // End ParseUtils
