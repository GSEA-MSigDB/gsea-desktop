/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.reports.api.Report;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Properties;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class DefaultReport extends AbstractObject implements Report {

    private Class fProducerClass;

    private long fTimestamp;
    private transient Date fDate;

    private File[] fFiles;

    private Properties fParams;

    private File fReportDir;

    /**
     * Safe copies are made upon construction if not in shared mode
     *
     * @param name
     * @param files
     * @param params
     */
    public DefaultReport(final String name,
                         final long timestamp,
                         final Class producerClass,
                         final File[] files,
                         final Properties params,
                         final boolean shared) throws ClassNotFoundException {
        super(name);

        _runParamChecks(producerClass, files, params);

        if (shared) {
            this.fTimestamp = timestamp;
            this.fProducerClass = producerClass;
            this.fFiles = files;
            this.fParams = params;
        } else {
            this.fTimestamp = timestamp;
            this.fProducerClass = Class.forName(producerClass.getName());
            this.fParams = (Properties) params.clone();
            this.fFiles = new File[files.length];
            for (int i = 0; i < files.length; i++) {
                fFiles[i] = new File(files[i].getPath());
            }
        }

        final String val = fParams.getProperty("out");
        final File baseDir = new File(val);
        this.fReportDir = new File(baseDir, NamingConventions.removeExtension(name));
    }

    private static void _runParamChecks(Class cl, File[] files, Properties params) {
        if (files == null) {
            throw new IllegalArgumentException("Param files cannot be null");
        }

        if (params == null) {
            throw new IllegalArgumentException("Param params cannot be null");
        }

        if (cl == null) {
            throw new IllegalArgumentException("Param cl cannot be null");
        }

    }

    public Class getProducer() {
        return fProducerClass;
    }

    public long getTimestamp() {
        return fTimestamp;
    }
    
    public Date getDate() {
        if (fDate == null) {
            fDate = new Date(fTimestamp);
        }
        return fDate;
    }

    public File[] getFilesProduced() {
        File[] files = new File[fFiles.length];
        for (int f = 0; f < fFiles.length; f++) {
            files[f] = fFiles[f];
        }
        return files;
    }

    public int getNumPagesMade() {
        return fFiles.length;
    }

    public Properties getParametersUsed() {
        return (Properties) fParams.clone();
    }

    public void setErroredOut() {
        // do nothing
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer().append(fFiles.length).append(" files");
        return buf.toString();
    }

    public URI getReportIndex() {
        return fFiles[0].toURI(); // @note assume that the first file is the index file
    }

    public File getReportDir() {
        return fReportDir;
    }

} // End ToolReport