/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.expr.*;
import org.genepattern.io.expr.cls.ClsReader;
import org.genepattern.io.expr.gct.GctWriter;
import org.genepattern.io.expr.res.ResWriter;

import edu.mit.broad.genome.utils.ParseException;

import java.io.*;
import java.util.*;

/**
 * A class containing static convenience methods for reading and writing data
 *
 * @author Joshua Gould
 */
public class IOUtil {
    private static ClsReader clsReader = new ClsReader();

    private static FeatureListReader featureListReader = new FeatureListReader();

    private static GctWriter gctWriter = new GctWriter();
    
    private static ResWriter resWriter = new ResWriter();
    
    private IOUtil() {
    }

    /**
     * Reads the cls document at the given pathname
     *
     * @param pathname The pathname string
     * @return The class vector
     * @throws IOException    If an error occurs while reading from the file
     * @throws ParseException If there is a problem with the data
     */
    public static ClassVector readCls(String pathname) throws IOException,
            ParseException {
        return clsReader.read(pathname);
    }

    /**
     * Gets a list of features at the given file pathname string
     *
     * @param pathname The pathname string
     * @return the feature list
     * @throws IOException If an error occurs while reading from the file
     */
    public static List readFeatureList(String pathname) throws IOException {
        return featureListReader.read(pathname);
    }

    /**
     * Writes expression data to a file in the given format. The correct file
     * extension will be added to the pathname if it is not present. If there is
     * already a file present at the given pathname, its contents are discarded.
     *
     * @param data               the expression data.
     * @param formatName         a String containing the informal name of a format (e.g., "res"
     *                           or "gct".)
     * @param pathname           a pathname string
     * @param checkFileExtension Whether the correct file extension will be added to the
     *                           pathname if it is not present.
     * @return The pathname that the data was saved to
     * @throws IOException If an error occurs while saving the data
     */
    public static String write(IExpressionData data, String formatName,
                               String pathname, boolean checkFileExtension) throws IOException {
        IExpressionDataWriter writer = getWriterForFormat(formatName);
        if (checkFileExtension) {
            pathname = writer.checkFileExtension(pathname);
        }
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(pathname));
            writer.write(data, os);
        } finally {
            if (os != null) {
                os.close();
            }
        }
        return pathname;
    }

    private static IExpressionDataWriter getWriterForFormat(String formatName) throws IOException {
        if ("gct".equalsIgnoreCase(formatName)) {
            return gctWriter;
        }
        
        if ("res".equalsIgnoreCase(formatName)) {
            return resWriter;
        }

        throw new IOException("No writer to save the data in " + formatName + " format.");
    }
}