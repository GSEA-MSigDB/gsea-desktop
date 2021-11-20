/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
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
 * @author Aravind Subramanian, David Eby
 */
public class PclParser extends AbstractParser {
    protected PclParser() { super(Dataset.class); }

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
    }

    public List parse(String hackINeedFullPath, InputStream is) throws Exception {
        startImport(hackINeedFullPath);
        int nlines = FileUtils.countLines(hackINeedFullPath, true);
        int nfloatlines = nlines - 2;
        log.debug("Number of float lines = " + nfloatlines);

        try (BufferedReader bin = new BufferedReader(new InputStreamReader(is))) {
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
            List<float[]> data = new ArrayList<float[]>(nfloatlines);
            int skippedMissingRows = 0, partialMissingRows = 0;
            boolean foundInfiniteValues = false;
    
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
    
                String desc = fields.get(1).toString().trim();
                if (desc.length() == 0) {
                    throw new ParserException("Bad rowdescname - cant be empty at: " + r + " >" + currLine);
                }
    
                // ignore the GWEIGHT, actually just a check
                if (Integer.parseInt(fields.get(2).toString()) != 1) {
                    throw new ParserException("Expected field was not 1 for GWEIGHT: " + fields.get(2) + " " + currLine);
                }
    
                // -- then, onto the floats
                float[] dataRow = parseFieldsIntoFloatArray(fields, r, 3, rowName);
                int countMissing = countMissingValues(dataRow, r, rowName);
                if (countMissing < dataRow.length) {
                    if (countMissing > 0) { partialMissingRows++; }
                    data.add(dataRow);
                    rowNames.add(rowName);
                    rowDescs.add(desc);
                } else {
                    skippedMissingRows++;
                }
                foundInfiniteValues |= checkForInfiniteValues(dataRow, r, rowName);

                r++;
                currLine = nextLineTrimless(bin);
            }
            
            if (data.isEmpty()) { throw new ParserException("Data was missing in all rows!"); }

            Matrix matrix = new Matrix(data.size(), colNames.size());
            for (int i = 0; i < data.size(); i++) {
                matrix.setRow(i, data.get(i));
            }
    
            // Initialize the Dataset and Annotation
            String name = new File(hackINeedFullPath).getName();
    
            FeatureAnnot fann = new FeatureAnnot(name, rowNames, rowDescs);
            fann.addComment(fComment.toString());
    
            final SampleAnnot sann = new SampleAnnot(name, colNames);
    
            Dataset ds = new DefaultDataset(name, matrix, rowNames, colNames, new Annot(fann, sann));
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
    }
}
