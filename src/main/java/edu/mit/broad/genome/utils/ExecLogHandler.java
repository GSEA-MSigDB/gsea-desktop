/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import org.apache.commons.exec.LogOutputStream;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;

/**
 * Helper class for use in running external programs with Apache Commons Exec, to capture
 * the subprocess stdout/stderr in the GSEA log.
 * Based on https://stackoverflow.com/questions/5499042/writing-output-error-to-log-files-using-pumpstreamhandler
 */
public class ExecLogHandler extends LogOutputStream {
    private Logger log;

    public ExecLogHandler(Logger log) {
        super(Level.DEBUG.intLevel());
        this.log = log;
    }

    @Override
    protected void processLine(String line, int logLevel) {
        // Unlike the code at the above URL, we don't ever want to expose this to the user.  We
        // need it for debugging purposes only.  
        log.debug(line);
    }
}