/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.math.Matrix;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.format.CellFormat;
import jxl.format.RGB;

import java.awt.*;
import java.io.*;
import java.util.*;

public class MiscParsers {

    /**
     * Class Constructor.
     */
    private MiscParsers() {
    }

    private static String _txt(int r, int c, Sheet sheet, boolean reqd) throws ParserException {
        final Cell cell = sheet.getCell(c, r); // @note
        String txt = cell.getContents();
        if (txt != null) {
            txt = txt.trim();
        }

        if (reqd && (txt == null || txt.length() == 0)) {
            throw new ParserException("Null or empty for r: " + r + " c: " + c);
        }

        return txt;
    }

    public static Map parseColorMapFromExcel(final File xlsFile) throws Exception {
        final Workbook wk = Workbook.getWorkbook(xlsFile);
        final Sheet sheet = wk.getSheet(0);
        return parseColorMapFromExcel(xlsFile.getPath(), sheet);
    }

    /**
     * key -> string, Value -> BACKGROUND color
     * Duplicates are overwritten
     */
    public static Map parseColorMapFromExcel(final String sourcepath, final Sheet sheet) throws Exception {

        final Map valueColorMap = new HashMap();

        for (int c = 0; c < sheet.getColumns(); c++) {
            for (int r = 0; r < sheet.getRows(); r++) {
                String txt = _txt(r, c, sheet, false); // nulls are ok
                if (txt != null && txt.length() > 0) {
                    final Cell cell = sheet.getCell(c, r); // @note
                    final CellFormat format = cell.getCellFormat();
                    final jxl.format.Colour color = format.getBackgroundColour();
                    if (color != null) {
                        final RGB rgb = color.getDefaultRGB();
                        final Color cl = new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
                        if (cl != Color.WHITE) {
                            //System.out.println(txt + "\t" + cl + " " + r + ":" + c);
                            if (valueColorMap.containsKey(txt)) {
                                // fine easier to just paste them all in
                                //klog.warn("Duplicate colormap entry for: " + txt);
                            } else {
                                valueColorMap.put(txt, cl);
                            }
                        }
                    }
                }
            }
        }

        return valueColorMap;
    }

    public static void save(Matrix m, File toFile) throws Exception {
        PrintWriter pw = new PrintWriter(new FileOutputStream(toFile));

        for (int r = 0; r < m.getNumRow(); r++) {
            pw.println(m.getRowV(r).toString('\t'));
        }

        pw.close();
    }


}    // End of class MiscParsers
