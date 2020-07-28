/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.math.Vector;

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
 */
public class ParseUtils {

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
                                          final String delim) throws IllegalArgumentException {

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

    public static List<String> string2stringsList(String s, String delim) throws IllegalArgumentException {

        if (null == s) {
            throw new NullPointerException("Cannot work on null String");
        }

        StringTokenizer tok = new StringTokenizer(s, delim);
        List<String> ret = new ArrayList<String>(tok.countTokens());

        while (tok.hasMoreTokens()) {
            ret.add(tok.nextToken().trim());
        }

        return ret;
    }

    public static Set<String> string2stringsSet(String s, String delim) throws IllegalArgumentException {
        Set<String> ret = new HashSet<String>();
        if (null == s) {
            return ret;
        }

        StringTokenizer tok = new StringTokenizer(s, delim);

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

    public static List<String> getUniqueTokens(StringTokenizer tok) {

        List<String> uniqs = new ArrayList<String>();
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
    public static List<String> readFfn(File aFile) throws IOException {

        BufferedReader buf = new BufferedReader(new FileReader(aFile));
        String line;
        ArrayList<String> lines = new ArrayList<String>();

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
    /*
     * The filepath was formerly treated as a path OR a URL, but this lead to errors on Windows where
     * our URL detection fails to identify 'C:/' as a path.  We might revamp this later (see GSEA-1170)
     * but for now these will be restricted to being handled as *local files only*.  Loading param_files
     * from URL seems like an unlikely use-case anyway.
     */
    public static Properties readKeyVal(final String filepath) throws IOException {
        // Inlining code from working code path formerly in FileUtils.toBufferedReader().
        URI uri = new File(filepath).toURI();
        URL url = uri.toURL();
        BufferedReader buf = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            String line;
            final Properties prp = new Properties();
    
            line = nextLine(buf);
    
            int lineNum = 0;
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
    
                if (val == null || val.length() == 0) {
                    throw new RuntimeException("Value must exist. Missing on line: " + lineNum + " val: " + val);
                }
    
                if (key.length() == 0) {
                    throw new RuntimeException("Empty key on line: " + lineNum);
                }
    
                prp.setProperty(key, val);
                line = nextLine(buf);
                lineNum++;
            }
    
    
            if (lineNum == 0) {
                throw new IllegalArgumentException("Empty input stream!! ");
            }
    
            return prp;
        } finally {
            buf.close();
        }
        
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

    private static BufferedReader _buf(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }

        return new BufferedReader(new FileReader(file));
    }
}