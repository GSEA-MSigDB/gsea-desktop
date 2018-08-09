/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import org.apache.log4j.Appender;

import javax.swing.*;

/**
 * Class that defines a status bar
 */
public interface StatusBar extends Appender {

    public JComponent getAsComponent();

}
