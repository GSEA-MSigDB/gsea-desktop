/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import java.io.File;
import java.io.FileInputStream;

import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.xbench.core.api.Application;

/**
 * Worker to parse a file on a background thread in the UI (via SwingWorker), providing a
 * progress bar if it takes too long.
 * 
 * @author David Eby
 */
public class ParserWorker extends SwingWorker<Object, Void> {
    private static final Logger klog = Logger.getLogger(ParserWorker.class);

    private File[] files;

    public ParserWorker(File[] files) {
        this.files = files;
    }

    @Override
    protected Object doInBackground() throws Exception {
        final StringBuilder buf_s = new StringBuilder("<html>Loading ... " + files.length + " files<br><br>");
        int sucess = 0;

        final Errors errors = new Errors();
        boolean hadWarnings = false;
        for (int f = 0; f < files.length; f++) {
            if (files[f].isDirectory()) {
                errors.add(new RuntimeException(
                        "Only files can be choosen - a directory was specified: "
                                + files[f].getPath()));
            } else {
                try {
                    final FileInputStream in = new FileInputStream(files[f]);
                    ProgressMonitorInputStream pis = new ProgressMonitorInputStream(Application
                            .getWindowManager().getRootFrame(), "Loading file "
                            + files[f].getName(), in);
                    // Loads & parses the file into memory
                    PersistentObject pob = ParserFactory.read(files[f].getPath(), pis);
                    if (pob == null || pis.getProgressMonitor().isCanceled()) {
                        throw new RuntimeException("Loading of file '" + files[f].getName() +
                                "' canceled.");
                    }
                    else {
                        klog.info("Loaded file: " + files[f].getPath());
                        if (!pob.getWarnings().isEmpty()) { hadWarnings = true; }
                        Application.getFileManager().registerRecentlyOpenedFile(files[f]);
                        buf_s.append(files[f].getName()).append("\n");
                        sucess++;
                    }
                } catch (Throwable t) {
                    errors.add("Parsing trouble", t);
                }
            }
        }

        buf_s.append("<br>Files loaded successfully: ").append(sucess).append(" / ")
                .append(files.length).append("<br>");
        if (errors.isEmpty()) {
            buf_s.append("There were NO errors");
            if (hadWarnings) { buf_s.append("<br><br><b>There were warnings. See the [+] console log for details.</b>"); }
            buf_s.append("</html>");
            Application.getWindowManager().showMessage(buf_s.toString());
        } else {
            Application.getWindowManager().showError(errors);
        }

        return null;
    }
}
