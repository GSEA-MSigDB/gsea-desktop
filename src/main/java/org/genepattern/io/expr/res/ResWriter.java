/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io.expr.res;

import org.genepattern.data.expr.ExpressionConstants;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.expr.Util;
import org.genepattern.io.expr.IExpressionDataWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Writes res files.
 *
 * @author Joshua Gould
 */
public class ResWriter implements IExpressionDataWriter {
    public String checkFileExtension(String filename) {
        if (!filename.toLowerCase().endsWith(".res")) {
            filename += ".res";
        }
        return filename;
    }

    public void write(IExpressionData data, OutputStream os) throws IOException {
        if (!(Util.containsData(data, ExpressionConstants.CALLS))) {
            throw new IOException(
                    "Can't write in res format. Data does not have calls.");
        }

        PrintWriter out = new PrintWriter(os);
        int rows = data.getRowCount();
        int columns = data.getColumnCount();

        out.print("Description");// Line format: Description (tab) Accession
        // (tab) (sample 1 name) (tab) (tab) (sample 2
        // name) (tab) (tab) ... (sample N name)
        out.print("\t");
        out.print("Accession");
        out.print("\t");
        out.print(data.getColumnName(0));
        for (int j = 1; j < columns; j++) {
            out.print("\t\t");
            out.print(data.getColumnName(j));
        }
        out.print("\n");

        out.print("\t");

        String columnDescription = data.getColumnMetadata(0,
                ExpressionConstants.DESC);
        if (columnDescription == null) {
            columnDescription = "";
        }
        out.print(columnDescription);// Line format: (tab) (sample 1
        // description) (tab) (tab) (sample 2
        // description) (tab) (tab) ... (sample N
        // description)
        for (int j = 1; j < columns; j++) {
            out.print("\t\t");
            columnDescription = data.getColumnMetadata(j,
                    ExpressionConstants.DESC);
            if (columnDescription == null) {
                columnDescription = "";
            }
            out.print(columnDescription);
        }

        out.print("\n");
        out.print(rows);

        // Line format: (gene description) (tab) (gene name) (tab) (sample 1
        // data) (tab) (sample 1 A/P call) (tab) (sample 2 data) (tab) (sample 2
        // A/P call) (tab) ... (sample N data) (tab) (sample N A/P call)

        for (int i = 0; i < rows; i++) {
            out.print("\n");
            String rowDescription = data.getRowMetadata(i,
                    ExpressionConstants.DESC);
            if (rowDescription == null) {
                rowDescription = "";
            }
            out.print(rowDescription);
            out.print("\t");
            out.print(data.getRowName(i));
            for (int j = 0; j < columns; j++) {
                out.print("\t");
                out.print(data.getValueAsString(i, j));
                out.print("\t");
                out.print(data.getData(i, j, ExpressionConstants.CALLS));
            }
        }
        out.flush();
    }

}
