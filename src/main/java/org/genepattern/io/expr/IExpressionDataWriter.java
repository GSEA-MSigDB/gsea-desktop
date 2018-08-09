/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io.expr;

import org.genepattern.data.expr.IExpressionData;

import java.io.IOException;

/**
 * Interface for expression data writers.
 *
 * @author Joshua Gould
 */
public interface IExpressionDataWriter {

    /**
     * Writes an <code>IExpressionData</code> instance to a stream.
     *
     * @param data the data
     * @param os   the output stream
     * @throws IOException if an I/O error occurs during writing.
     */
    public void write(IExpressionData data, java.io.OutputStream os)
            throws java.io.IOException;

    /**
     * Appends the correct file extension to the pathname if it does not exist.
     *
     * @param pathname a pathname string
     * @return The corrected pathname
     */
    public String checkFileExtension(String pathname);

}

