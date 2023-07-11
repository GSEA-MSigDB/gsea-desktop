/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.io.expr.cls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import edu.mit.broad.genome.utils.ParseException;

/**
 * Class for reading cls files using callbacks.
 * <p/>
 * <p/>
 * The CLS files are simple files created to load class information into
 * GeneCluster. These files use spaces to separate the fields.
 * </P>
 * <UL>
 * <LI>The first line of a CLS file contains numbers indicating the number of
 * samples and number of classes. The number of samples should correspond to the
 * number of samples in the associated RES or GCT data file.</LI>
 * <p/>
 * <UL>
 * <LI>Line format: (number of samples) (space) (number of classes) (space) 1
 * </LI>
 * <LI>For example: 58 2 1</LI>
 * </UL>
 * <p/>
 * <LI>The second line in a CLS file contains names for the class numbers. The
 * line should begin with a pound sign (#) followed by a space.</LI>
 * <p/>
 * <UL>
 * <LI>Line format: # (space) (class 0 name) (space) (class 1 name)</LI>
 * <p/>
 * <LI>For example: # cured fatal/ref.</LI>
 * </UL>
 * <p/>
 * <LI>The third line contains numeric class labels for each of the samples.
 * The number of class labels should be the same as the number of samples
 * specified in the first line.</LI>
 * <UL>
 * <LI>Line format: (sample 1 class) (space) (sample 2 class) (space) ...
 * (sample N class)</LI>
 * <LI>For example: 0 0 0 ... 1
 * </UL>
 * <p/>
 * </UL>
 *
 * @author kohm
 * @author Joshua Gould
 */
public class ClsParser {
    final static String formatName = "cls";

    List suffixes = Collections.unmodifiableList(Arrays
            .asList(new String[]{"cls"}));

    BufferedReader reader;

    int numClasses;

    int numItems;

    IClsHandler handler;

    /**
     * Parses the <CODE>InputStream</CODE> to create a <CODE>ClassVector
     * </CODE> instance
     *
     * @param is The input stream
     * @throws org.genepattern.io.ParseException
     *                     If there is a problem with the data
     * @throws IOException if an I/O error occurs while reading the stream.
     */
    public void parse(InputStream is) throws IOException,
            ParseException {
        this.reader = new BufferedReader(new java.io.InputStreamReader(is));
        read();
    }

    void read() throws IOException, ParseException {
        processHeader();// <num_data> <num_classes> 1
        String classifierLine = reader.readLine();
        String[] names = null;
        String dataLine = null;
        String[] assignments = null;
        Map classNumber2NameMap = new HashMap();

        if (hasClassNames(classifierLine)) {
            names = readClassNamesLine(classifierLine);
            for (int i = 0, length = names.length; i < length; i++) {
                classNumber2NameMap.put(i, names[i]);
            }
            dataLine = reader.readLine();
            assignments = processData(dataLine, classNumber2NameMap);
        } else {// assume classifier line was skipped (second line) so try it as
            // data
            names = new String[numClasses];
            for (int i = 0; i < numClasses; i++) {
                names[i] = Integer.toString(i);
                classNumber2NameMap.put(i, names[i]);
            }
            dataLine = classifierLine;
            assignments = processData(dataLine, classNumber2NameMap);
        }
        if (handler != null) {
            handler.classes(names);
            handler.assignments(assignments);
        }
    }

    private boolean hasClassNames(String classifierLine) {
        return (classifierLine != null && classifierLine.length() > 2 && classifierLine
                .startsWith("#"));
    }

    private void processHeader() throws IOException,
            ParseException {
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new ParseException("No header line");
        }
        int[] hdrInts = new int[3];
        StringTokenizer tok = new StringTokenizer(headerLine, " \t");
        if (tok.countTokens() != 3) {
            throw new ParseException(
                    "Header line needs three numbers!\n" + "\"" + headerLine
                            + "\"");
        }
        try {
            for (int i = 0; i < 3; i++) {
                hdrInts[i] = Integer.parseInt(tok.nextToken().trim());
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Header line element '"
                    + e.getMessage() + "' is not a number");
        }

        if (hdrInts[0] <= 0) {
            throw new ParseException(
                    "Header line missing first number, number of data points");
        }
        if (hdrInts[1] <= 0) {
            throw new ParseException(
                    "Header line missing second number, number of classes");
        }

        /*
         * if(hdrInts[2] != 1) { throw new
         * org.genepattern.io.ParseException("Third number on 1st line must
         * '1'."); }
         */
        numClasses = hdrInts[1];
        numItems = hdrInts[0];

    }

    private String[] readClassNamesLine(String classifierLine)
            throws ParseException {
        // optional: class label line, otherwise the data line
        // # Breast Colon Pancreas ...

        // remove the # because it could be "# CLASS1" or "#CLASS1"
        classifierLine = classifierLine
                .substring(classifierLine.indexOf('#') + 1);
        StringTokenizer st = new StringTokenizer(classifierLine, " \t");
        if (st.countTokens() != numClasses) {
            throw new ParseException("First line specifies "
                    + numClasses + " classes, but found " + (st.countTokens())
                    + ".");
        }
        String[] names = new String[numClasses];
        for (int ic = 0; st.hasMoreTokens(); ic++) {
            names[ic] = st.nextToken();
        }
        return names;
    }

    /**
     * Parses the data line
     *
     * @param data_line   The line to parse
     * @param num_classes The number of classes
     * @param num_data    The number of data points
     * @return the assignments
     * @throws org.genepattern.io.ParseException
     *          Description of the Exception
     */
    private String[] processData(String data_line, Map classNumber2ClassNameMap)
            throws ParseException {
        // data line <int0> <space> <int1> <space> <int2> <space> ...
        if (data_line == null) {
            throw new ParseException(
                    "Missing data (numbers seperated by spaces) on 3rd line");
        }
        try {
            String[] assignments = new String[numItems];
            String[] tokens = data_line.split("[ \t]");

            if (tokens.length != numItems) {
                throw new ParseException("Header specifies "
                        + numItems + " data points, but file contains "
                        + tokens.length + " data points.");

            }
            for (int i = 0; i < tokens.length; i++) {
                int classNumber = Integer.parseInt(tokens[i].trim());
                if (classNumber >= numClasses || classNumber < 0) {
                    throw new ParseException(
                            "Header specifies "
                                    + numClasses
                                    + " classes, but data line contains a "
                                    + classNumber
                                    + ", a value "
                                    + "that is too "
                                    + (classNumber < 0 ? "small" : "large")
                                    + "."
                                    + " All data for this file must be in the range 0-"
                                    + (numClasses - 1) + ".");
                }
                String name = (String) classNumber2ClassNameMap.get(classNumber);
                assignments[i] = name;
            }

            return assignments;
        } catch (NumberFormatException ex) {
            throw new ParseException(
                    "All values on the 3rd lines must be numbers.");
        }
    }

    public void setHandler(IClsHandler handler) {
        this.handler = handler;
    }
}
