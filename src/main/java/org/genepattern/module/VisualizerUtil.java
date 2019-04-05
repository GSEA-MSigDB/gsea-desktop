/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.module;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.FeatureListReader;
import org.genepattern.io.expr.IExpressionDataWriter;
import org.genepattern.io.expr.cls.ClsReader;
import org.genepattern.io.expr.gct.GctWriter;
import org.genepattern.io.expr.res.ResWriter;

import edu.mit.broad.genome.utils.ParseException;

import javax.swing.*;

import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A class containing static convenience methods for tasks that are frequently
 * encountered when writing GenePattern visualizers
 *
 * @author Joshua Gould
 */
public class VisualizerUtil {

    public static ClsReader clsReader = new ClsReader();
    private static FeatureListReader featureListReader = new FeatureListReader();
    private static GctWriter gctWriter = new GctWriter();
    public static ResWriter resWriter = new ResWriter();

    private VisualizerUtil() {
    }

    /**
     * Reads the cls document at the given pathname. Brings up an error message
     * dialog if an error occurs.
     *
     * @param parentComponent determines the <code>Frame</code> in which the dialog is
     *                        displayed; if <code>null</code>, or if the
     *                        <code>parentComponent</code>
     * @param pathname        The pathname string
     * @return The class vector
     */
    public static ClassVector readCls(Component parentComponent, String pathname) {

        try {
            return clsReader.read(pathname);
        } catch (ParseException | IOException e) {
            fileOperationError("An error occured while reading the file ", e, parentComponent, pathname);
            return null;
        }
    }

    /**
     * Gets a list of features at the given file pathname string. Brings up an
     * error message dialog if an error occurs.
     *
     * @param parentComponent determines the <code>Frame</code> in which the dialog is
     *                        displayed; if <code>null</code>, or if the
     *                        <code>parentComponent</code>
     * @param pathname        The pathname string
     * @return the feature list
     */
    public static List<String> readFeatureList(Component parentComponent, String pathname) {
        try {
            return featureListReader.read(pathname);
        } catch (IOException e) {
            fileOperationError("An error occured while reading the file ", e, parentComponent, pathname);
            return null;
        }
    }

    /**
     * Writes expression data to a file in the given format. The correct file
     * extension will be added to the pathname if it is not present. If there is
     * already a file present at the given pathname, its contents are discarded.
     * Brings up an error message dialog if an error occurs.
     *
     * @param parentComponent    determines the <code>Frame</code> in which the dialog is
     *                           displayed; if <code>null</code>, or if the
     *                           <code>parentComponent</code>
     * @param data               the expression data.
     * @param formatName         a String containing the informal name of a format (e.g., "res"
     *                           or "gct".)
     * @param pathname           a pathname string
     * @param checkFileExtension Whether the correct file extension will be added to the
     *                           pathname if it is not present.
     * @return The pathname that the data was saved to
     */
    public static String write(Component parentComponent, IExpressionData data,
                               String formatName, String pathname, boolean checkFileExtension) {
        try {
            String modPathname = pathname;
            IExpressionDataWriter writer = getWriterForFormat(formatName);
            if (checkFileExtension) {
                modPathname = writer.checkFileExtension(modPathname);
            }
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(modPathname));
                writer.write(data, os);
            } finally {
                if (os != null) {
                    os.close();
                }
            }
            return modPathname;
        } catch (IOException ioe) {
            fileOperationError("An error occured while attempting to save the file ", ioe, parentComponent, pathname);
            return null;
        }
    }

    private static void fileOperationError(String baseMsg, Exception e, Component parentComponent, String pathname) {
        String name = new File(pathname).getName().replaceFirst("Axis[0-9]*axis_", "");
        String message = baseMsg + name + ".";
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
            message += "\nCause: " + exceptionMessage;
        }
        JOptionPane.showMessageDialog(parentComponent, message, "Error", JOptionPane.ERROR_MESSAGE);
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