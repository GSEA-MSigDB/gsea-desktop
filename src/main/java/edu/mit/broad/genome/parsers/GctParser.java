/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
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
        try {
            final Dataset ds = (Dataset) pob;
            FeatureAnnot ann = null;
            if (ds.getAnnot() != null) {
                ann = ds.getAnnot().getFeatureAnnot();
            }
    
            pw.println("#1.2"); // not sure what the # means, but give the people what they want
            pw.println(ds.getNumRow() + "\t" + ds.getNumCol());
            pw.print(Constants.NAME);
            pw.print("\t");
            pw.print(Constants.DESCRIPTION);
    
            for (int i = 0; i < ds.getNumCol(); i++) {
                pw.print("\t");
                pw.print(ds.getColumnName(i));
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
                if (desc == null) { desc = Constants.NA; }
    
                buf.append(desc).append('\t');
                buf.append(ds.getRow(r).toString('\t'));
                pw.println(buf.toString());
            }
        } finally {
            doneExport();  // Handles pw.close()
        }
    }

    /**
     * @returns 1 Dataset object
     * Always DatasetAnnotation object produced , but if underlying df has none then na is used
     * @see above for format
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        try (BufferedReader bin = new BufferedReader(new InputStreamReader(is))) {
            String objName = NamingConventions.removeExtension(sourcepath);
            String currLine = nextLine(bin);
    
            // 1st  non-empty, non-comment line is numrows and numcols
            int[] nstuff = ParseUtils.string2ints(currLine, " \t");
            if (nstuff.length != 2) {
                throw new ParserException("Gct file with bad row/col info on line 2: " + currLine);
            }
    
            int nrows = nstuff[0];
            int ncols = nstuff[1];
    
            // First 2 fields name and desc are to be ignored
            currLine = nextLine(bin);
            List<String> colnames = ParseUtils.string2stringsList(currLine, "\t"); // colnames can have spaces
    
            colnames.remove(0);                                 // first elem is always nonsense
            colnames.remove(0);
    
            if (colnames.size() != ncols) {
                throw new ParserException("Bad gct format -- expected ncols from specification on header line: " + ncols + " but found in data: " + colnames.size());
            }
    
            // At this point, currLine should contain the first data line
            // data line: <row name> <tab> <ex1> <tab> <ex2> <tab>
            List<String> lines = new ArrayList<String>();
        
            // save all rows so that we can determine how many rows exist
            currLine = nextLineTrimless(bin);
            while (currLine != null) {
                lines.add(currLine);
                currLine = nextLineTrimless(bin); /// imp for mv datasets -> last col(s) can be a tab
            }
    
            if (lines.size() != nrows) {
                throw new ParserException("Bad gct format -- exepcted nrows from specification on header line: " + nrows + " but found in data: " + lines.size());
            }

            return _parseHasDesc(objName, lines, colnames);
        }
    }

    private List _parseHasDesc(String objName, List<String> lines, List<String> colNames) throws Exception {
        Matrix matrix = new Matrix(lines.size(), colNames.size());
        List<String> rowNames = new ArrayList<String>();
        List<String> rowDescs = new ArrayList<String>();

        for (int i = 0; i < lines.size(); i++) {
            String currLine = lines.get(i);
            List<String> fields = string2stringsV2(currLine, colNames.size() + 2); // spaces allowed in name & desc field so DONT tokenize them

            if (fields.size() != colNames.size() + 1 + 1) {
                throw new ParserException("Bad format - expect ncols: " + (colNames.size() + 1 + 1)
                        + " but found: " + fields.size() + " on line >"
                        + currLine + "<\nIf this dataset has missing values, use ImputeDataset to fill these in before importing as a Dataset");
            }

            String rowname = parseRowname(fields.get(0).trim(), i, currLine);

            String desc = fields.get(1).trim();
            if (desc.length() == 0) { desc = Constants.NA; }

            rowDescs.add(desc);
            rowNames.add(rowname);

            parseFieldsIntoFloatMatrix(fields, i, 2, matrix);
        }

        final FeatureAnnot ann = new FeatureAnnot(objName, rowNames, rowDescs);
        ann.addComment(fComment.toString());
        final SampleAnnot sann = new SampleAnnot(objName, colNames);

        final Dataset ds = new DefaultDataset(objName, matrix, rowNames, colNames, new Annot(ann, sann));
        ds.addComment(fComment.toString());
        doneImport();
        return unmodlist(new PersistentObject[]{ds});
    }
}
