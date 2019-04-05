/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Parser for the MIT res file format. All the vagaries of the res file
 * format and the hacks etc to parse them are contained in this class.
 * <p/>
 * Parsing a Res file produces two PersistentObjects - a Dataset and an
 * DatasetAnnotation object.
 *
 * @author Michael Angelo - original GeneCluster implementaion
 * @author Aravind Subramanian - adapted for xomics.
 * @version %I%, %G%
 * @todo consider ignoring the meg numrows line in favor of just using however many rows happen
 * to be in the file. Not doing this causes the "missing" rows to have wierd matrix values.
 */
public class ResParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public ResParser() {
        super(Dataset.class);
    }

    /**
     * Accepts only Dataset objects.
     * <p/>
     * Description      Accession       ALL_19769_B-cell                ALL_23953_B-cell                ALL_28373_B-cell                ALL_9335_B-cell         ALL_9692_B-cell         ALL_14749_B-cell                ALL_17281_B-cell                ALL_19183_B-cell                ALL_20414_B-cell                ALL_21302_B-cell                ALL_549_B-cell          ALL_17929_B-cell                ALL_20185_B-cell                ALL_11103_B-cell                ALL_18239_B-cell                ALL_5982_B-cell         ALL_7092_B-cell         ALL_R11_B-cell          ALL_R23_B-cell          ALL_16415_T-cell                ALL_19881_T-cell                ALL_9186_T-cell         ALL_9723_T-cell         ALL_17269_T-cell                ALL_14402_T-cell                ALL_17638_T-cell                ALL_22474_T-cell                AML_12          AML_13          AML_14          AML_16          AML_20          AML_1           AML_2           AML_3           AML_5           AML_6           AML_7
     * CH1999021515AA           CH1999021511AA/scale factor=1.0657              CH1999021507AA/scale factor=1.0379              CH1999021312AA/scale factor=1.6802              CH1999021109AA/scale factor=1.4991              CH1999021508AA/scale factor=1.4372              CH1999021314AA/scale factor=1.1240              CH1999021321AA/scale factor=0.9211              CH1999021322AA/scale factor=1.0511              CH1999021111AA/scale factor=1.0932              CH1999021509AA/scale factor=1.1876              CH1999021323AA/scale factor=1.1292              CH1999021104AA/scale factor=0.8147              CH1999021308AA/scale factor=1.2493              CH1999021512AA/scale factor=1.3076              CH1999021501AA/scale factor=1.0876              CH1999021317AA/scale factor=1.0960              CH1999021110AA/scale factor=1.0199              CH1999021303AA/scale factor=1.5644              CH1999021306AA/scale factor=0.9564              CH1999021103AA/scale factor=1.1401              CH1999021520AA/scale factor=1.7782              CH1999021305AA/scale factor=0.9248              CH1999021310AA/scale factor=1.2084              CH1999021309AA/scale factor=1.0708              CH1999021101AA/scale factor=0.9890              MG1999021102AA/scale factor=1.9140              CH1999021319AA/scale factor=1.0545              MG1999021105AA/scale factor=2.4045              CH1999021320AA/scale factor=1.1998              CH1999021514AA/scale factor=1.0694              CH1999021304AA/scale factor=1.1488              CH1999021516AA/scale factor=0.9554              MG1999021109AA/scale factor=1.3322              CH1999021106AA/scale factor=1.0133              CH1999021108AA/scale factor=1.3079              CH1999021107AA/scale factor=1.0749              CH1999021311AA/scale factor=1.0951
     * 7129
     * AFFX-BioB-5_at (endogenous control)      AFFX-BioB-5_at  -214    A       -135    A       -106    A       -72     A       -413    A       -67     A       -92     A       -107    A       -117    A       -476    A       -81     A       -44     A       17      A       -144    A       -247    A       -120    A       -81     A       -112    A       -273    A       -139    A       -76     A       -138    A       5       A       -88     A       -165    A       -113    A       -74     A       -20     A       7       A       -213    A       -25     A       -72     A       -4      A       15      A       -318    A       -32     A       -124    A       -135    A
     * AFFX-BioB-M_at (endogenous control)      AFFX-BioB-M_at  -153    A       -114    A       -125    A       -144    A       -260    A       -93     A       -119    A       -72     A       -219    A       -213    A       -150    A       -51     A       -229    A       -199    A       -90     A       -263    A       -150    A       -233    A       -326    A       -73     A       -49     A       -85     A       -127    A       -105    A       -155    A       -147    A       -323    A       -207    A       -100    A       -253    A       -20     A       -139    A       -116    A       -114    A       -192    A       -49     A       -79     A       -186    A
     * AFFX-BioB-3_at (endogenous control)      AFFX-BioB-3_at  -58     A       265     A       -76     A       238     A       7       A       84      A       -31     A       -126    A       -50     A       -18     A       -119    A       100     A       79      A       -157    A       -168    A       -114    A       -85     A       -78     A       -76     A       -1      A       -307    A       215     A       106     A       42      A       -71     A       -118    A       -11     A       -50     A       -57     A       136     A       124     A       -1      A       -125    A       2       A       -95     A       49      A       -37     A       -70     A
     */
    public void export(final PersistentObject pob, final File file) throws Exception {

        PrintWriter pw = startExport(pob, file);
        Dataset ds = (Dataset) pob;
        APMMatrix apm = null;

        if (ds instanceof DefaultDataset) {
            apm = ((DefaultDataset) ds).getAPMMatrix();
        }

        FeatureAnnot ann = ds.getAnnot().getFeatureAnnot();

        // null is valid as not all datasets have anns
        pw.print(Constants.DESCRIPTION + "\t" + Constants.NAME + "\t"); // @note reverse order compared to gct!!

        for (int i = 0; i < ds.getNumCol(); i++) {
            pw.print(ds.getColumnName(i) + '\t'); // extra empty tab for the CALL cell
            if (i != ds.getNumCol() - 1) {
                pw.print('\t');
            }
        }

        pw.println();
        pw.println("");    // omit the scaling factor line
        pw.println(ds.getNumRow());

        for (int r = 0; r < ds.getNumRow(); r++) {
            String featName = ds.getRowName(r);
            // Give preference to Native desc if it exists
            // If not, use the symbol desc
            String featDesc = Constants.NA;
            if (ann != null) {
                if (ann.hasNativeDescriptions()) {
                    featDesc = ann.getNativeDesc(featName);
                } else {
                    featDesc = ann.getGeneSymbol(featName) + ":" + ann.getGeneTitle(featName);
                }
            }

            pw.print(featDesc);
            pw.print('\t');
            pw.print(featName);
            pw.print('\t');

            Vector row = ds.getRow(r);
            StringBuffer buf = new StringBuffer();
            for (int c = 0; c < row.getSize(); c++) {
                if (apm == null) {
                    buf.append(row.getElement(c)).append('\t').append('P');// @note fill in dummy CALL values
                } else {
                    buf.append(row.getElement(c)).append('\t').append(apm.getElement_char(r, c));
                }
                if (c != row.getSize()) {
                    buf.append('\t');
                }
            }

            pw.println(buf.toString());
        }

        pw.close();
        doneExport();
    }    // End export

    /**
     * The guts of the parsing
     * <p/>
     * Format:
     * <p/>
     * <pre>
     * <p/>
     * Description      Accession       ALL_19769_B-cell                ALL_23953_B-cell                ALL_28373_B-cell                ALL_9335_B-cell         ALL_9692_B-cell         ALL_14749_B-cell                ALL_17281_B-cell                ALL_19183_B-cell                ALL_20414_B-cell                ALL_21302_B-cell                ALL_549_B-cell          ALL_17929_B-cell                ALL_20185_B-cell                ALL_11103_B-cell                ALL_18239_B-cell                ALL_5982_B-cell         ALL_7092_B-cell         ALL_R11_B-cell          ALL_R23_B-cell          ALL_16415_T-cell                ALL_19881_T-cell                ALL_9186_T-cell         ALL_9723_T-cell         ALL_17269_T-cell                ALL_14402_T-cell                ALL_17638_T-cell                ALL_22474_T-cell                AML_12          AML_13          AML_14          AML_16          AML_20          AML_1           AML_2           AML_3           AML_5           AML_6           AML_7
     * CH1999021515AA           CH1999021511AA/scale factor=1.0657              CH1999021507AA/scale factor=1.0379              CH1999021312AA/scale factor=1.6802              CH1999021109AA/scale factor=1.4991              CH1999021508AA/scale factor=1.4372              CH1999021314AA/scale factor=1.1240              CH1999021321AA/scale factor=0.9211              CH1999021322AA/scale factor=1.0511              CH1999021111AA/scale factor=1.0932              CH1999021509AA/scale factor=1.1876              CH1999021323AA/scale factor=1.1292              CH1999021104AA/scale factor=0.8147              CH1999021308AA/scale factor=1.2493              CH1999021512AA/scale factor=1.3076              CH1999021501AA/scale factor=1.0876              CH1999021317AA/scale factor=1.0960              CH1999021110AA/scale factor=1.0199              CH1999021303AA/scale factor=1.5644              CH1999021306AA/scale factor=0.9564              CH1999021103AA/scale factor=1.1401              CH1999021520AA/scale factor=1.7782              CH1999021305AA/scale factor=0.9248              CH1999021310AA/scale factor=1.2084              CH1999021309AA/scale factor=1.0708              CH1999021101AA/scale factor=0.9890              MG1999021102AA/scale factor=1.9140              CH1999021319AA/scale factor=1.0545              MG1999021105AA/scale factor=2.4045              CH1999021320AA/scale factor=1.1998              CH1999021514AA/scale factor=1.0694              CH1999021304AA/scale factor=1.1488              CH1999021516AA/scale factor=0.9554              MG1999021109AA/scale factor=1.3322              CH1999021106AA/scale factor=1.0133              CH1999021108AA/scale factor=1.3079              CH1999021107AA/scale factor=1.0749              CH1999021311AA/scale factor=1.0951
     * 7129
     * AFFX-BioB-5_at (endogenous control)      AFFX-BioB-5_at  -214    A       -135    A       -106    A       -72     A       -413    A       -67     A       -92     A       -107    A       -117    A       -476    A       -81     A       -44     A       17      A       -144    A       -247    A       -120    A       -81     A       -112    A       -273    A       -139    A       -76     A       -138    A       5       A       -88     A       -165    A       -113    A       -74     A       -20     A       7       A       -213    A       -25     A       -72     A       -4      A       15      A       -318    A       -32     A       -124    A       -135    A
     * AFFX-BioB-M_at (endogenous control)      AFFX-BioB-M_at  -153    A       -114    A       -125    A       -144    A       -260    A       -93     A       -119    A       -72     A       -219    A       -213    A       -150    A       -51     A       -229    A       -199    A       -90     A       -263    A       -150    A       -233    A       -326    A       -73     A       -49     A       -85     A       -127    A       -105    A       -155    A       -147    A       -323    A       -207    A       -100    A       -253    A       -20     A       -139    A       -116    A       -114    A       -192    A       -49     A       -79     A       -186    A
     * AFFX-BioB-3_at (endogenous control)      AFFX-BioB-3_at  -58     A       265     A       -76     A       238     A       7       A       84      A       -31     A       -126    A       -50     A       -18     A       -119    A       100     A       79      A       -157    A       -168    A       -114    A       -85     A       -78     A       -76     A       -1      A       -307    A       215     A       106     A       42      A       -71     A       -118    A       -11     A       -50     A       -57     A       136     A       124     A       -1      A       -125    A       2       A       -95     A       49      A       -37     A       -70     A
     * <p/>
     * </pre>
     * <p/>
     * ADDED: can add comment lines (prefix with #) at very start of file if needed
     * <p/>
     * # some comment
     * # another line of comments
     * Description      Accession       ALL_19769_B-cell                ALL_23953_B-cell                ALL_28373_B-cell                ALL_9335_B-cell         ALL_9692_B-cell         ALL_14749_B-cell                ALL_17281_B-cell                ALL_19183_B-cell                ALL_20414_B-cell                ALL_21302_B-cell                ALL_549_B-cell          ALL_17929_B-cell                ALL_20185_B-cell                ALL_11103_B-cell                ALL_18239_B-cell                ALL_5982_B-cell         ALL_7092_B-cell         ALL_R11_B-cell          ALL_R23_B-cell          ALL_16415_T-cell                ALL_19881_T-cell                ALL_9186_T-cell         ALL_9723_T-cell         ALL_17269_T-cell                ALL_14402_T-cell                ALL_17638_T-cell                ALL_22474_T-cell                AML_12          AML_13          AML_14          AML_16          AML_20          AML_1           AML_2           AML_3           AML_5           AML_6           AML_7
     * CH1999021515AA           CH1999021511AA/scale factor=1.0657              CH1999021507AA/scale factor=1.0379              CH1999021312AA/scale factor=1.6802              CH1999021109AA/scale factor=1.4991              CH1999021508AA/scale factor=1.4372              CH1999021314AA/scale factor=1.1240              CH1999021321AA/scale factor=0.9211              CH1999021322AA/scale factor=1.0511              CH1999021111AA/scale factor=1.0932              CH1999021509AA/scale factor=1.1876              CH1999021323AA/scale factor=1.1292              CH1999021104AA/scale factor=0.8147              CH1999021308AA/scale factor=1.2493              CH1999021512AA/scale factor=1.3076              CH1999021501AA/scale factor=1.0876              CH1999021317AA/scale factor=1.0960              CH1999021110AA/scale factor=1.0199              CH1999021303AA/scale factor=1.5644              CH1999021306AA/scale factor=0.9564              CH1999021103AA/scale factor=1.1401              CH1999021520AA/scale factor=1.7782              CH1999021305AA/scale factor=0.9248              CH1999021310AA/scale factor=1.2084              CH1999021309AA/scale factor=1.0708              CH1999021101AA/scale factor=0.9890              MG1999021102AA/scale factor=1.9140              CH1999021319AA/scale factor=1.0545              MG1999021105AA/scale factor=2.4045              CH1999021320AA/scale factor=1.1998              CH1999021514AA/scale factor=1.0694              CH1999021304AA/scale factor=1.1488              CH1999021516AA/scale factor=0.9554              MG1999021109AA/scale factor=1.3322              CH1999021106AA/scale factor=1.0133              CH1999021108AA/scale factor=1.3079              CH1999021107AA/scale factor=1.0749              CH1999021311AA/scale factor=1.0951
     * 7129
     * AFFX-BioB-5_at (endogenous control)      AFFX-BioB-5_at  -214    A       -135    A       -106    A       -72     A       -413    A       -67     A       -92     A       -107    A       -117    A       -476    A       -81     A       -44     A       17      A       -144    A       -247    A       -120    A       -81     A       -112    A       -273    A       -139    A       -76     A       -138    A       5       A       -88     A       -165    A       -113    A       -74     A       -20     A       7       A       -213    A       -25     A       -72     A       -4      A       15      A       -318    A       -32     A       -124    A       -135    A
     * AFFX-BioB-M_at (endogenous control)      AFFX-BioB-M_at  -153    A       -114    A       -125    A       -144    A       -260    A       -93     A       -119    A       -72     A       -219    A       -213    A       -150    A       -51     A       -229    A       -199    A       -90     A       -263    A       -150    A       -233    A       -326    A       -73     A       -49     A       -85     A       -127    A       -105    A       -155    A       -147    A       -323    A       -207    A       -100    A       -253    A       -20     A       -139    A       -116    A       -114    A       -192    A       -49     A       -79     A       -186    A
     * AFFX-BioB-3_at (endogenous control)      AFFX-BioB-3_at  -58     A       265     A       -76     A       238     A       7       A       84      A       -31     A       -126    A       -50     A       -18     A       -119    A       100     A       79      A       -157    A       -168    A       -114    A       -85     A       -78     A       -76     A       -1      A       -307    A       215     A       106     A       42      A       -71     A       -118    A       -11     A       -50     A       -57     A       136     A       124     A       -1      A       -125    A       2       A       -95     A       49      A       -37     A       -70     A
     *
     * @done Add AP call parsing and include in the dara structure
     * <br>-> there is one AP call per float and these get fed into a parallel BooleanMatrix
     */
    public List parse(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);
        int currLineNum = 0;
        StringBuffer comment = new StringBuffer();

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        //bin = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        //bin = new BufferedReader(new InputStreamReader(pis));

        // 1st header line: Description <tab> Accession <tab> <sample 1> <tab> <tab> <sample 2> ...
        // (column names)
        // NOTE: if line ends in CR-LF, then we have actually read currLine.length() + 2 bytes...
        String currLine = bin.readLine();

        while (currLine.startsWith("#")) {
            comment.append(currLine).append('\n');
            currLine = bin.readLine();
        }

        currLineNum++;

        ArrayList colArr = new ArrayList();
        char theDelim = '\t';
        char[] tmpChar = new char[1];

        if (currLine.length() > 0) {
            theDelim = ParseUtils.getDelim(currLine);
            tmpChar[0] = theDelim;

            StringTokenizer st = new StringTokenizer(currLine, new String(tmpChar));

            if (st.hasMoreTokens()) {
                st.nextToken();                                    // Description
            }

            if (st.hasMoreTokens()) {
                st.nextToken();                                    // Accession
            }

            while (st.hasMoreTokens()) {

                // System.out.println(st.nextToken());
                colArr.add(st.nextToken());
            }
        }

        // 2nd header line, chip scaling factors or blank
        // <tab> CH1999021515AA <tab> <tab> CH1999021306AA/scale factor=0.9564 <tab> <tab>
        bin.readLine();
        currLineNum++;

        // 3rd header line optional, number of rows or blank or first data line
        currLine = bin.readLine();

        currLineNum++;

        int numRows = 0,
                numCols = colArr.size();

        // Try parsing to see if there is a numRows fields
        //boolean haveNumRows;
        try {
            // parseInt does not work if there is extra white-space
            String intStr = currLine.trim();

            numRows = Integer.parseInt(intStr);
            currLine = bin.readLine();

            currLineNum++;

            //haveNumRows = true;
        } catch (Exception e) {
            //haveNumRows = false;
        }

        log.info("Found meg data as numRows:" + numRows + " numCols:" + numCols);

        // Now that dataset has been initialized, assign column names
        List colNames = new ArrayList(numCols);

        for (int i = 0; i < numCols; ++i) {
            colNames.add(colArr.get(i));
        }

        // At this point, currLine should contain the first data line
        // data line: <row desc> <tab> <row name> <tab> <ex1> <tab> <call1> <tab> <ex2> <tab> <call2>
        Matrix matrix = new Matrix(numRows, numCols);
        APMMatrix apmMatrix = new APMMatrix(numRows, numCols);
        List rowNames = new ArrayList(numRows);
        List rowDescs = new ArrayList(numRows);
        int dataRowInd = 0;

        while (currLine != null) {
            //currLine = currLine.trim();
            if (currLine.length() == 0) {
                currLine = bin.readLine();
                continue;
            }

            // substring returns a String that references the original String's buffer
            // Upshot is that that the buffer (in our case a whole line) never gets freed
            // Solution is to explicitly create a string from the substring
            int ind1 = currLine.indexOf(theDelim);

            //System.out.println(">>>PPP " + currLine);
            checkIndex(ind1, currLineNum);

            String desc = currLine.substring(0, ind1);
            int ind2 = currLine.indexOf(theDelim, ind1 + 1);
            checkIndex(ind2, currLineNum);
            //System.out.println(">>>TTT " + currLine);
            String name = currLine.substring(ind1 + 1, ind2);
            rowNames.add(name);

            // add to the Annotation
            // for each res file float entry, theres one name and one desc
            //ann.addFeature(name, new String[]{desc});
            float[] theFloats = new float[numCols];
            float[] theCalls = new float[numCols];

            ind1 = ind2;

            //System.out.println(">>> " + currLine);
            for (int i = 0; i < numCols; ++i) {
                ind2 = currLine.indexOf(theDelim, ind1 + 1);

                checkIndex(ind2, currLineNum);

                //System.out.println(">>>RRR " + currLine);
                // get expression level
                String floatStr = currLine.substring(ind1 + 1, ind2);

                //if (floatStr.length() < 1)
                //theFloats[i] = 0;
                //else
                theFloats[i] = Float.valueOf(floatStr).floatValue();

                // call (absent, present, etc.) column
                //int apindx = currLine.indexOf(theDelim, ind1 + 1 + 1);
                String apStr = currLine.substring(ind1 + 1 + floatStr.length() + 1,
                        ind1 + 1 + floatStr.length() + 1 + 1);

                theCalls[i] = APMMatrix.valueOf(apStr);

                //System.out.println(">> " + apStr);
                ind1 = currLine.indexOf(theDelim, ind2 + 1);
            }

            matrix.setRow(dataRowInd, theFloats);
            apmMatrix.setRow(dataRowInd, theCalls);
            rowDescs.add(desc);

            dataRowInd++;

            currLine = bin.readLine();

            currLineNum++;
        }

        doneImport();

        FeatureAnnot fann = new FeatureAnnot(sourcepath, rowNames, rowDescs);
        fann.addComment(fComment.toString());

        final SampleAnnot sann = new SampleAnnot(sourcepath, colNames);

        final Dataset ds = new DefaultDataset(sourcepath, matrix, rowNames, colNames, true, new Annot(fann, sann), apmMatrix);
        ds.addComment(fComment.toString());

        System.out.println(">>>>> DONE PARSING: " + apmMatrix.getQuickInfo());

        return unmodlist(new PersistentObject[]{ds});
    } // End of method parse

    private void checkIndex(int ind, int lineNum) throws ParserException {

        if (ind < 0) {
            throw new ParserException("Invalid line index=" + ind + " . Expecting ind >=0 " + " on line: " + lineNum);
        }
    }

}    // End of class ResParser
