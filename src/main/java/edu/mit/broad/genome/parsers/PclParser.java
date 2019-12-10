/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PCL data format.
 * For format info see:
 * <p/>
 * http://genome-www5.stanford.edu/help/formats.shtml#pcl
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PclParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    protected PclParser() {
        super(Dataset.class);
    }

    public void export(PersistentObject pob, File file) throws Exception {

        PrintWriter pw = new PrintWriter(new FileOutputStream(file));

        Dataset ds = (Dataset) pob;
        FeatureAnnot ann = ds.getAnnot().getFeatureAnnot();

        pw.print("UNIQUID\tNAME\tGWEIGHT\t");

        //UNIQUID	NAME	GWEIGHT	MD_1	MD_2	MD_3	MD_4	MD_5	MD_6	MD_7	MD_8	MD_9	MD_10	MD_11	MD_12	MD_13	MD_14	MD_15	MD_16	MD_17	MD_18	MD_19	MD_20	MD_21	MD_22	MD_23	MD_24	MD_25	MD_26	MD_27	MD_28	MD_29	MD_30	MD_31	MD_32	MD_33	MD_34	MD_35	MD_36	MD_37	MD_38	MD_39	MD_40	MD_41	MD_42	MD_43	MD_44	MD_45	MD_46	MD_47	MD_48	MD_49	MD_50	MD_51	MD_52	MD_53	MD_54	MD_55	MD_56	MD_57	MD_58	MD_59	MD_60
        //EWEIGHT				1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1	1

        for (int i = 0; i < ds.getNumCol(); i++) {
            pw.print(ds.getColumnName(i));

            if (i != ds.getNumCol() - 1) {
                pw.print('\t');
            }
        }

        pw.println();

        pw.print("EWEIGHT\t\t\t");

        // eweight is always 1
        for (int i = 0; i < ds.getNumCol(); i++) {
            pw.print("1");
            if (i != ds.getNumCol() - 1) {
                pw.print('\t');
            }
        }

        pw.println();

        for (int r = 0; r < ds.getNumRow(); r++) {
            StringBuffer buf = new StringBuffer();
            String rowName = ds.getRowName(r);
            buf.append(rowName).append('\t');
            buf.append(ann.getNativeDesc(rowName)).append('\t');
            buf.append("1\t"); // for GWEIGHT
            buf.append(ds.getRow(r).toString('\t'));
            pw.println(buf.toString());
        }

        pw.close();

        doneExport();

    }    // End export

    /**
     *
     *
     */
    public List parse(String hackINeedFullPath, InputStream is) throws Exception {

        startImport(hackINeedFullPath);
        int nlines = FileUtils.countLines(hackINeedFullPath, true);
        int nfloatlines = nlines - 2;
        log.debug("Number of float lines = " + nfloatlines);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        String currLine = nextLine(bin);

        List<String> colNames = ParseUtils.string2stringsList(currLine, "\t"); // spaces allowed in col names
        int expectedNCols = colNames.size();
        colNames.remove(0); // get rid of UNIQUID field name
        colNames.remove(0); // get rid of NAME field name
        colNames.remove(0); // get rid of GWEIGHT field name
        log.debug("Number of columns of floats = " + colNames.size());

        // get rid of the EWUIGHT line
        nextLine(bin);

        // Initialize the Dataset and Annotation
        List<String> rowNames = new ArrayList<String>(nfloatlines);
        List<String> rowDescs = new ArrayList<String>(nfloatlines);
        Matrix matrix = new Matrix(nfloatlines, colNames.size());

        int r = 0;
        currLine = nextLineTrimless(bin); // first line of float data @note trimless as may be missing fields
        while (currLine != null) {
            //System.out.println(">> " + currLine);
            List<String> fields = string2stringsV2(currLine, expectedNCols); // spaces allowed in name & desc field so DONT tokenize them

            if (fields.size() != expectedNCols) { // silly check
                throw new ParserException("Invalid format on line: " + currLine + " expected # fields = " + expectedNCols + " but found: " + fields.size());
            }

            String rowName = fields.get(0).toString().trim();
            if (rowName.length() == 0) {
                throw new ParserException("Bad rowname - cant be empty at: " + r + " >" + currLine);
            }

            rowNames.add(rowName);

            String desc = fields.get(1).toString().trim();
            if (desc.length() == 0) {
                throw new ParserException("Bad rowdescname - cant be empty at: " + r + " >" + currLine);
            }

            rowDescs.add(desc);

            // ignore the GWEIGHT, actually just a check
            if (Integer.parseInt(fields.get(2).toString()) != 1) {
                throw new ParserException("Expected field was not 1 for GWEIGHT: " + fields.get(2) + " " + currLine);
            }

            // -- then, onto the floats
            for (int f = 3; f < fields.size(); f++) {
                String s = fields.get(f).toString().trim();
                float val;
                if (s.length() == 0) {
                    val = Float.NaN;
                } else {
                    val = Float.parseFloat(s);
                }
                matrix.setElement(r, f - 3, val);
            }

            r++;
            currLine = nextLineTrimless(bin);
        }

        bin.close();

        // Initialize the Dataset and Annotation
        String name = new File(hackINeedFullPath).getName();

        FeatureAnnot fann = new FeatureAnnot(name, rowNames, rowDescs);
        fann.addComment(fComment.toString());

        final SampleAnnot sann = new SampleAnnot(name, colNames);

        Dataset ds = new DefaultDataset(name, matrix, rowNames, colNames, new Annot(fann, sann));
        ds.addComment(fComment.toString());

        doneImport();
        return unmodlist(new PersistentObject[]{ds});
    }

}    // End StanfordDatasetFormatParser
