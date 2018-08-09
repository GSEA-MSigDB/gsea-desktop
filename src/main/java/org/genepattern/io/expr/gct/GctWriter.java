/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io.expr.gct;

import org.genepattern.data.expr.ExpressionConstants;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.io.expr.IExpressionDataWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Writes gct files.
 *
 * @author Joshua Gould
 */
public class GctWriter implements IExpressionDataWriter {
    public String checkFileExtension(String filename) {
        if (!filename.toLowerCase().endsWith(".gct")) {
            filename += ".gct";
        }
        return filename;
    }

    public void write(IExpressionData expressionData, OutputStream os)
            throws IOException {

        PrintWriter out = new PrintWriter(os);

        int rows = expressionData.getRowCount();
        int columns = expressionData.getColumnCount();

        String version = "#1.2";
        out.print(version);
        out.print("\n");

        out.print(rows + "\t" + columns);
        out.print("\n");

        out.print("Name");
        out.print("\t");
        out.print("Description");

        for (int j = 0; j < columns; j++) {
            out.print("\t");
            out.print(expressionData.getColumnName(j));
        }

        for (int i = 0; i < rows; i++) {
            out.print("\n");
            out.print(expressionData.getRowName(i));
            out.print("\t");
            String rowDescription = expressionData.getRowMetadata(i,
                    ExpressionConstants.DESC);
            if (rowDescription == null) {
                rowDescription = "";
            }
            out.print(rowDescription);
            for (int j = 0; j < columns; j++) {
                out.print("\t");
                out.print(expressionData.getValueAsString(i, j));
            }
        }
        out.print("\n");
        out.flush();
    }
}

