/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.api;

import edu.mit.broad.genome.objects.PersistentObject;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Properties;

/**
 * Read-Only interface for a Report eg reports already done
 * <p/>
 * Keep this here for junits clas slinker stuff
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Report extends PersistentObject {

    public static final String PRODUCER_CLASS_ENTRY = "producer_class";

    public static final String TIMESTAMP_ENTRY = "producer_timestamp";

    public static final String FILE_ENTRY = "file";

    public static final String PARAM_ENTRY = "param";

    public File[] getFilesProduced();

    public Properties getParametersUsed();

    public Class getProducer();

    public long getTimestamp();
    
    public Date getDate();

    public int getNumPagesMade();

    public void setErroredOut();

    public URI getReportIndex();

    public File getReportDir();

} // End Report

