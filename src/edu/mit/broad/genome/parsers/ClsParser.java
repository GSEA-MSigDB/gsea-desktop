/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.utils.ParseException;

import java.io.*;
import java.util.*;

/**
 * Parses a data store in cls format to produce a single Template object.
 * <p/>
 * Adv of new parser scheme - the parse in and parse out are in the same
 * physical file and thus the fomrat is set in one and only one place.
 * <p/>
 * <p/>
 * Auxillary parsing:
 * <p/>
 * Client tools are responsinble for inspectig if a supplied template is
 * aux or not. If so, then they should extract the sub-dataset using the supplied
 * aux template. Cant really do this elsewhere.
 *
 * @author Michael Angelo
 * @author Aravind Subramanian (adapted for xomics)
 * @version %I%, %G%
 */
public class ClsParser extends AbstractParser implements Constants {

    /**
     * For format checking only
     */
    private int[] fHdrInts;

    /**
     * For format checking only
     */
    private int fItemCnt;

    /**
     * Class Constructor.
     */
    public ClsParser() {
        super(Template.class);
    }

    /**
     * Only accepts Template objects.
     */
    public void export(final PersistentObject template, final File file) throws Exception {

        PrintWriter pw = startExport(template, file);

        pw.println(((Template) template).getAsString(false));
        pw.close();

        doneExport();
    }

    public void export(final PersistentObject template, final boolean gcFormat, final File file) throws Exception {

        PrintWriter pw = startExport(template, file);

        pw.println(((Template) template).getAsString(gcFormat));
        pw.close();

        doneExport();
    }

    /**
     * Parses in Cls files.
     * Comment lines are allowed ONLY BEFORE the first line of data
     * (as the # on the sample line is the same as the comm char)
     * Format:
     * #optional comment lines ...
     * 38 3 1
     * # ALL1 ALL2 AML
     * 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 2
     * my_anal_1_template ALL1 AML
     * my_anal_2_template ALL2 AML
     */
    private boolean isNumericLine(String firstline) {
        if (firstline == null) {
            throw new IllegalArgumentException("No content in cls file");
        }

        firstline = firstline.trim();

        boolean isNum = firstline.equalsIgnoreCase(Template.NUMERIC);
        if (isNum == false) {
            isNum = firstline.equalsIgnoreCase(Template.NUMERIC_1);
        }

        return isNum;
    }

    /**
     * #probes
     *
     * @param firstline
     * @return
     */
    private boolean isProbesLine(String firstline) {
        if (firstline == null) {
            throw new IllegalArgumentException("No content in cls file");
        }

        firstline = firstline.trim();
        return firstline.equalsIgnoreCase(Template.PROBES);
    }

    /**
     * Format:
     * <p/>
     * #numeric
     * <p/>
     * #name
     * 12 23 77 33 33 ...
     * <p/>
     * #name
     * 377 33 33 33 ...
     *
     * @param bin
     * @return
     */
    private List parseContinuousTemplate(final String fileName, final BufferedReader bin) throws IOException, ParserException {

        Map nameProfileMap = new HashMap();

        // String currLine = firstLine; // #numeric ignore
        String currLine = nextNonEmptyLine(bin);

        while (currLine != null) {

            if (currLine.startsWith("#")) {
                String profileLine = nextNonEmptyLine(bin);
                if (profileLine == null) {
                    throw new ParserException("Bad cls data format - missing profile line for: " + currLine);
                }

                if ((profileLine.length() == 0) || (profileLine.startsWith("#"))) {
                    throw new ParserException("Bad cls data format - missing profile line for: " + currLine);
                }

                nameProfileMap.put(currLine, profileLine);

            }

            currLine = nextNonEmptyLine(bin);
        }

        Iterator it = nameProfileMap.keySet().iterator();
        List templates = new ArrayList();
        while (it.hasNext()) {
            String name = (String) it.next();
            String profile = (String) nameProfileMap.get(name);
            // 206.0 31.0 252.0 -20.0 -169.0 -66.0 230.0 -23.0 67.0
            float[] floats = ParseUtils.string2floats(profile);

            name = name.substring(1, name.length()); // get rid of the #
            // @note the file name is useful as it make its unique across cls files and also
            // because then its is a diff pseudo file for object cache
            Template t = TemplateFactory.createContinuousTemplate(fileName + "#" + name, new Vector(floats));
            //Template t = TemplateFactory.createContinuousTemplate(name, new Vector(floats));
            templates.add(t);
        }

        log.info("Completed parsing NUMERIC CLS file");

        return Collections.unmodifiableList(templates);
    }

    private List parseProbeTemplate(final String fileName, final BufferedReader bin) throws IOException {

        // firstLine is #probes, ignore
        String currLine = nextNonEmptyLine(bin);

        List probePreTemplates = new ArrayList();

        while (currLine != null) {
            //log.debug("Adding new ProbePreTemplate: " + currLine);
            probePreTemplates.add(new ProbePreTemplate(fileName, currLine));
            currLine = nextNonEmptyLine(bin);
        }

        return probePreTemplates;
    }

    /**
     * @param file
     * @return The first line that is NOT empty nor a comment line
     * @throws ParserException
     */
    public List parse(final String fileName, final InputStream is) throws Exception {
        startImport(fileName);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        String firstLine = nextNonEmptyLine(bin);

        if (firstLine == null) {
            throw new ParserException("Bad format for cls fike - check lines 1 and 2 --- they seem to have no content");
        }

        if (isProbesLine(firstLine)) {
            return parseProbeTemplate(fileName, bin);
        } else if (isNumericLine(firstLine)) {
            return parseContinuousTemplate(fileName, bin);
        } else {
            StringTokenizer tok = new StringTokenizer(firstLine, " \t");
            boolean oldStyle = false;
            try {
                if (tok.countTokens() == 3) {
                    Float.parseFloat(tok.nextToken());
                    Float.parseFloat(tok.nextToken());
                    Float.parseFloat(tok.nextToken());
                    oldStyle = true;
                }
            } catch (NumberFormatException nfe) {
                oldStyle = false;
            }

            if (oldStyle) {
                return _parse_genecluster_style_categorical(fileName, bin, firstLine);
            } else {
                return _parse_new_style(fileName, bin, firstLine);
            }

        }
    }

    private List _parse_genecluster_style_categorical(final String sourcepath, final BufferedReader bin, String firstLine) throws Exception {

        String currLine = firstLine;

        boolean continuous = false;

        fHdrInts = parseHeaderLine(currLine);                    // step 1, fHdrInts gets filled.

        // class label line
        currLine = nextNonEmptyLine(bin); // non-empty as # is valid
        final Template.Class[] classes = generateClasses(currLine);

        // item line
        currLine = nextLine(bin);
        Template.Item[] items = generateItems(currLine);

        Template template = TemplateFactory.createTemplate(sourcepath, items, classes, continuous);
        template.addComment(fComment.toString());
        doSanityChecks(template);

        // aux parsing for cls format extension
        // must call AFTER generating the main template
        //List auxTemplates = doAuxParsing(bin);

        //doNameChecks(auxTemplates);

        doneImport();
        //return unmodlist(fMainTemplate, auxTemplates);
        return unmodlist(template);
    }

    /**
     * @param file
     * @return The first line that is NOT empty nor a comment line
     * @throws ParserException
     */
    private List _parse_new_style(String sourceFileName, BufferedReader buf, String firstLine) throws Exception {

        // read in the entire file
        final StringDataframe sdf = new StringDataframeParser().parseSdf(sourceFileName, buf, firstLine);
        // 'row' names are the sample names
        final String[] sampleNames = sdf.getRowNamesArray();
        final List preTemplates = new ArrayList();

        for (int c = 0; c < sdf.getNumCol(); c++) {
            final String colName = sdf.getColumnName(c);
            if (colName == null || colName.length() == 0) {
                throw new ParserException("Bad column identifier (it was null or empty) >" + colName + "< at column: " + (c + 1));
            } else if (isCommentColumn(colName)) {
                // do nothing
            } else if (isNumericColumn(colName)) {
                preTemplates.add(_createNumericPreTemplate(sourceFileName, colName, sampleNames, sdf.getColumn(c)));
            } else { // assume categorical classes
                preTemplates.add(_createCategoricalPreTemplate(sourceFileName, colName, sampleNames, sdf.getColumn(c)));
            }
        }

        doneImport();
        return preTemplates;
    }

    /*
    private List _parse_new_style_xls(String sourcepath, BufferedReader buf, String firstLine) throws Exception {
        throw new NotImplementedException();
    }
    */

    /**
     * Header line: <numData> <tab> <numClasses> <tab> <numAssignments>
     *
     * @param line Description of the Parameter
     * @throws ParserException Description of the Exception
     */
    private int[] parseHeaderLine(String line) throws Exception {

        //if (log.isDebugEnabled()) log.debug(">>" + line);
        char theDelim = ParseUtils.getDelim(line);
        int[] inds = new int[3];

        try {
            ParseUtils.splitIntegers(line, theDelim, inds);
        } catch (ParseException e) {
            throw new ParserException(e);
        }

        if (inds.length != 3) {
            throw new ParserException("Missing data in header " + line + " found only "
                    + inds.length + " fields .. expecting 3");
        }

        //log.info("fHdrInts " + inds[0] + " " + inds[1] + " " + inds[2]);
        return inds;
    }

    /**
     * The class label line is optional. When specified its of the form: #
     * Breast Colon Pancreas ... If its present, parse it out for class labels.
     * Else generate pseodo names for the Septs
     *
     * @param currLine Description of the Parameter
     * @return Description of the Return Value
     */
    private Template.Class[] generateClasses(String currLine) throws ParserException {

        if (currLine.startsWith("#")) {
            currLine = currLine.substring(1, currLine.length());
            currLine = currLine.trim();
            StringTokenizer st = new StringTokenizer(currLine, " \t");
            Template.Class[] cls = new Template.Class[st.countTokens()];

            int cnt = 0;

            while (st.hasMoreTokens()) {
                cls[cnt++] = new TemplateImpl.ClassImpl(st.nextToken().trim());
            }

            return cls;
        } else {
            throw new ParserException("Bad format in cls file - expected the line to be of the form '# foo bar ...'");
        }
    }

    /**
     * @param currLine Description of the Parameter
     */
    private Template.Item[] generateItems(String currLine) {

        fItemCnt = 0;
        if (currLine == null) {
            throw new NullPointerException("Bad cls data format -- check the item descriptor line (line 2)");
        }

        StringTokenizer st = new StringTokenizer(currLine, " \t");
        Template.Item[] items = new Template.Item[st.countTokens()];

        if (!isSilentMode()) {
            log.debug("# of items = " + st.countTokens());
        }

        while (st.hasMoreTokens()) {
            String className = st.nextToken().trim();
            items[fItemCnt] = TemplateImpl.ItemImpl.createItem(className, fItemCnt);
            fItemCnt++;
        }

        return items;
    }

    /**
     * data integrity, sanity checks
     */
    private void doSanityChecks(final Template template) throws ParserException {

        if (template.getNumItems() != fHdrInts[0]) {
            throw new ParserException("Number of items found in cls data " + fItemCnt
                    + " is not equal to the number specified on the header line "
                    + fHdrInts[0]);
        }

        if (template.getNumClasses() != fHdrInts[1]) {
            throw new ParserException("Number of classes found in cls data " + template.getNumClasses()
                    + " is not equal to the number of classes specified on the header line " + fHdrInts[1]);
        }
    }

    private static PreTemplateImpl _createNumericPreTemplate(String sourceFileName,
                                                             String name,
                                                             String sampleNames[],
                                                             String[] values) {
        List pairs = new ArrayList();
        for (int i = 0; i < sampleNames.length; i++) {
            if (sampleNames[i] != null && values[i] != null) {
                pairs.add(new PreTemplateImpl.NumPair(sampleNames[i], values[i])); // values can be null (i.e na in the cls file)
            }
        }

        return new PreTemplateImpl(sourceFileName, name, (PreTemplateImpl.Pair[]) pairs.toArray(new PreTemplateImpl.Pair[pairs.size()]), true);
    }

    private static PreTemplateImpl _createCategoricalPreTemplate(final String sourceFileName,
                                                                 final String name,
                                                                 final String sampleNames[],
                                                                 final String[] values) {

        List pairs = new ArrayList();
        for (int i = 0; i < sampleNames.length; i++) {
            if (sampleNames[i] != null && values[i] != null) { // values can be null (i.e na in the cls file
                pairs.add(new PreTemplateImpl.StringPair(sampleNames[i], values[i]));
            }
        }

        return new PreTemplateImpl(sourceFileName, name, (PreTemplateImpl.Pair[]) pairs.toArray(new PreTemplateImpl.Pair[pairs.size()]), false);
    }

    private static boolean isCommentColumn(final String colName) {
        return colName.equalsIgnoreCase("COMMENT") || colName.equalsIgnoreCase("COMMENTS");
    }

    private static boolean isNumericColumn(final String colName) {
        return colName.equalsIgnoreCase("NUMERIC") || colName.equalsIgnoreCase("FLOAT");
    }


}    // End ClsParser

/*--- Formatted in Sun Java Convention Style on Fri, Sep 27, '02 ---*/