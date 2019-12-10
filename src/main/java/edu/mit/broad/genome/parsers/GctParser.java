/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a gct formatted dataset -- similar to dataframe except that formatted
 * so that legacy MIT gct data is accepted
 * <p/>
 * Format:
 * <p/>
 * #1.2
 * 12488	41
 * Name	Description	col_0 col_1 col_2 ...
 * row_o
 * row_1
 * ...
 * <p/>
 * NO AP calls
 */
public class GctParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public GctParser() {
        super(Dataset.class);
    }

    /**
     * Export a Dataset to file in gct format
     * Only works with Datasets
     *
     * @see "Above for format"
     */
    public void export(final PersistentObject pob, final File file) throws Exception {
        _export(pob, startExport(pob, file));
    }

    public void export(final PersistentObject pob, final OutputStream os) throws Exception {
        _export(pob, startExport(pob, os, null));
    }

    private void _export(final PersistentObject pob, final PrintWriter pw) throws Exception {

        final Dataset ds = (Dataset) pob;
        FeatureAnnot ann = null;
        if (ds.getAnnot() != null) {
            ann = ds.getAnnot().getFeatureAnnot();
        }

        //log.debug("Annotation is: " + ann);

        pw.println("#1.2"); // not sure what the # means, but give the people what they want
        pw.println(ds.getNumRow() + "\t" + ds.getNumCol());
        pw.print(Constants.NAME + "\t" + Constants.DESCRIPTION + "\t");

        for (int i = 0; i < ds.getNumCol(); i++) {
            pw.print(ds.getColumnName(i));
            if (i != ds.getNumCol() - 1) {
                pw.print('\t');
            }
        }

        pw.println();

        // Give preference to Native desc if it exists
        // If not, use the symbol desc
        for (int r = 0; r < ds.getNumRow(); r++) {
            StringBuffer buf = new StringBuffer();
            String rowName = ds.getRowName(r);
            buf.append(rowName).append('\t');
            String desc = Constants.NA;
            if (ann != null) {
                if (ann.hasNativeDescriptions()) {
                    desc = ann.getNativeDesc(rowName);
                } else {
                    String symbol = ann.getGeneSymbol(rowName);
                    if (symbol != null) {
                        desc = symbol + ":" + ann.getGeneTitle(rowName);
                    }
                }
            }

            if (desc == null) {
                desc = Constants.NA;
            }

            buf.append(desc).append('\t');
            buf.append(ds.getRow(r).toString('\t'));
            pw.println(buf.toString());
        }

        pw.close();

        doneExport();
    }    // End export

    /**
     * @returns 1 Dataset object
     * Always DatasetAnnotation object produced , but if underlying df has none then na is used
     * @see above for format
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        return _parse(sourcepath, bin, true);

    }

    /// does the real parsing
    // expects the bin to be untouched
    private List _parse(String objName, BufferedReader bin, boolean nameBeforeDesc) throws Exception {
        objName = NamingConventions.removeExtension(objName);
        String currLine = nextLine(bin);

        // 1st  non-empty, non-comment line is numrows and numcols
        int[] nstuff = ParseUtils.string2ints(currLine, " \t");
        if (nstuff.length != 2) {
            throw new ParserException("Gct file with bad row/col info on line: " + currLine);
        }

        int nrows = nstuff[0];
        int ncols = nstuff[1];

        // First 2 fields name and desc are to be ignored
        currLine = nextLine(bin);
        //System.out.println(currLine);
        List<String> colnames = ParseUtils.string2stringsList(currLine, "\t"); // colnames can have spaces

        colnames.remove(0);                                 // first elem is always nonsense
        colnames.remove(0);

        if (colnames.size() != ncols) {
            throw new ParserException("Bad gct format -- expected ncols from specification on header line: " + ncols + " but found in data: " + colnames.size());
        }

        // At this point, currLine should contain the first data line
        // data line: <row name> <tab> <ex1> <tab> <ex2> <tab>
        List<String> lines = new ArrayList<String>();

        currLine = nextLineTrimless(bin);

        // save all rows so that we can determine how many rows exist
        while (currLine != null) {
            lines.add(currLine);
            //currLine = nextLine(bin);
            currLine = nextLineTrimless(bin); /// imp for mv datasets -> last col(s) can be a tab
        }

        if (lines.size() != nrows) {
            throw new ParserException("Bad gct format -- exepcted nrows from specification on header line: " + nrows + " but found in data: " + lines.size());
        }

        bin.close();

        return _parseHasDesc(objName, lines, colnames, nameBeforeDesc);
    }

    private List _parseHasDesc(String objName, List<String> lines, List<String> colNames, boolean nameBeforeDesc) throws Exception {
        objName = NamingConventions.removeExtension(objName);
        Matrix matrix = new Matrix(lines.size(), colNames.size());
        List<String> rowNames = new ArrayList<String>();
        List<String> rowDescs = new ArrayList<String>();

        for (int i = 0; i < lines.size(); i++) {
            String currLine = (String) lines.get(i);
            List<String> fields = string2stringsV2(currLine, colNames.size() + 2); // spaces allowed in name & desc field so DONT tokenize them

            if (fields.size() != colNames.size() + 1 + 1) {
                //System.out.println(">> " + fields);
                throw new ParserException("Bad format - expect ncols: " + (colNames.size() + 1 + 1)
                        + " but found: " + fields.size() + " on line >"
                        + currLine + "<\nIf this dataset has missing values, use ImputeDataset to fill these in before importing as a Dataset");
            }

            String rowname = fields.get(0).toString().trim();
            if (rowname.length() == 0) {
                throw new ParserException("Bad rowname - cant be empty at: " + i + " >" + currLine);
            }

            String desc = fields.get(1).toString().trim();
            if (desc.length() == 0) {
                desc = Constants.NA;
            }

            if (nameBeforeDesc) {
                // the standard way, do nothing
            } else { // the flipped one
                String tmp = rowname;
                rowname = desc;
                desc = tmp;
            }

            rowDescs.add(desc);
            rowNames.add(rowname);

            for (int f = 2; f < fields.size(); f++) {
                String s = fields.get(f).toString().trim();
                float val;
                if (s.length() == 0) {
                    val = Float.NaN;
                } else {
                    try {
                        val = Float.parseFloat(s);
                    } catch (Exception e) {
                        System.out.println(">" + s + "<");
                        //val = Float.NaN;
                        throw e;
                    }
                }
                matrix.setElement(i, f - 2, val);
            }
        }

        final FeatureAnnot ann = new FeatureAnnot(objName, rowNames, rowDescs);
        ann.addComment(fComment.toString());
        final SampleAnnot sann = new SampleAnnot(objName, colNames);

        final Dataset ds = new DefaultDataset(objName, matrix, rowNames, colNames, new Annot(ann, sann));
        ds.addComment(fComment.toString());
        doneImport();
        return unmodlist(new PersistentObject[]{ds});
    }

}    // End of class GctParser
