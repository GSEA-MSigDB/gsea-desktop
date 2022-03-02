/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.logging;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class StdOutConsoleHandler extends StreamHandler {
    public StdOutConsoleHandler() {
        super(System.out, new SimpleFormatter());
    }

    /**
     * We don't close System.out, just flush it.
     */
    @Override
    public synchronized void close() throws SecurityException { flush(); }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
    }
}
