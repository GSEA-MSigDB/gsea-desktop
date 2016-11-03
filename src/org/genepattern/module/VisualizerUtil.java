/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.module;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.IOUtil;

import edu.mit.broad.genome.utils.ParseException;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A class containing static convenience methods for tasks that are frequently
 * encountered when writing GenePattern visualizers
 *
 * @author Joshua Gould
 */
public class VisualizerUtil {

    private VisualizerUtil() {
    }

    /**
     * Brings up an error message dialog
     *
     * @param parentComponent determines the <code>Frame</code> in which the dialog is
     *                        displayed; if <code>null</code>, or if the
     *                        <code>parentComponent</code> has no <code>Frame</code>, a
     *                        default <code>Frame</code> is used
     * @param message         The message
     */
    public static void error(Component parentComponent, String message) {
        JOptionPane.showMessageDialog(parentComponent, message, "Error",
                JOptionPane.ERROR_MESSAGE);
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
            return IOUtil.readCls(pathname);
        } catch (IOException e) {
            error(parentComponent, "An error occured while reading the file "
                    + new File(pathname).getName());
            return null;
        } catch (ParseException e) {
            fileReadError(e, parentComponent, pathname);
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
    public static List readFeatureList(Component parentComponent,
                                       String pathname) {
        try {
            return IOUtil.readFeatureList(pathname);
        } catch (IOException e) {
            error(parentComponent, "An error occured while reading the file "
                    + new File(pathname).getName());
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
            return IOUtil.write(data, formatName, pathname, checkFileExtension);
        } catch (IOException ioe) {
            fileSaveError(ioe, parentComponent, pathname);
            return null;
        }
    }

    private static void fileReadError(Exception e, Component parentComponent,
                                      String pathname) {
        String message = "An error occured while reading the file "
                + VisualizerUtil.getFileName(pathname) + ".";
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
            message += "\nCause: " + exceptionMessage;
        }
        error(parentComponent, message);
    }

    private static void fileSaveError(Exception e, Component parentComponent,
                                      String pathname) {
        String msg = "An error occured while attempting to save the file "
                + VisualizerUtil.getFileName(pathname) + ".";
        String excMsg = e.getMessage();
        if (excMsg != null) {
            msg += "\nCause: " + excMsg;
        }
        error(parentComponent, msg);
    }

    /**
     * Gets the name of the file at the given pathname. Removes the axis prefix
     * if necessary.
     *
     * @param pathname The pathname string
     * @return The file name
     */
    private static String getFileName(String pathname) {
        String name = new File(pathname).getName();
        return name.replaceFirst("Axis[0-9]*axis_", "");
    }
}
