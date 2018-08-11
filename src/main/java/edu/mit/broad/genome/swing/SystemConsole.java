/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextArea;
import javax.swing.text.Document;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.io.LoopedStreams;
import edu.mit.broad.xbench.core.api.Application;
import xapps.api.frameworks.fiji.StatusBarAppender;

/**
 * Redirects and captures stdout and stderr to a swing component.  
 * Log messages are also captured.
 */
public class SystemConsole extends JTextArea {

    public SystemConsole() {

        this.setText("< Process output will appear below >\n\n");

        if (!Conf.isDebugMode()) {
            try {
                final LoopedStreams ls = new LoopedStreams();
                // Redirect System.out & System.err.
                PrintStream ps = new PrintStream(ls.getOutputStream());
                System.setOut(ps);
                System.setErr(ps);
                
                // Direct logging to the PrintStream as well
                addAppender(ps, "ConsoleViewer");

                startConsoleReaderThread(ls.getInputStream());

                // as changes
                setAutoscrolls(true);
            } catch (Throwable t) {
                this.setText("Could not open system console properly. Error: " + TraceUtils.getAsString(t));
            }
        } else {
            System.out.println("Skipping sysout redirection as in debug mode");
        }
    }

    public static void addAppender(final OutputStream outputStream, final String outputStreamName) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        // Note that this pattern is also used in the log4j2.xml file.  
        // Any changes here should be duplicated there.
        final PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(config).withPattern("%-8r [%-6p] - %m%n").build();
        final Appender appender = OutputStreamAppender.createAppender(layout, null, outputStream, outputStreamName, false, true);
        appender.start();
        config.addAppender(appender);
        updateLoggers(appender, config);
    }

    public static StatusBarAppender createStatusBarAppender(String name) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        // Note that this pattern is also used in the log4j2.xml file.  
        // Any changes here should be duplicated there.
        final PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(config).withPattern("%-8r [%-6p] - %m%n").build();
        final StatusBarAppender statusBar = new StatusBarAppender(name, null, layout);
        statusBar.start();
        config.addAppender(statusBar);
        updateLoggers(statusBar, config);
        return statusBar;
    }
    
    public static void updateLoggers(final Appender appender, final Configuration config) {
        final Level level = null;
        final Filter filter = null;
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.addAppender(appender, level, filter);
        }
        config.getRootLogger().addAppender(appender, level, filter);
    }
    
    private void startConsoleReaderThread(InputStream inStream) {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        new Thread(new Runnable() {
            public void run() {
                char[] buff = new char[256];
                int count;
                // as comm out the caretEnd part - that didnt seem to result in the autosrolling thing
                //boolean caretAtEnd = false;
                try {
                    Document doc = getDocument();
                    while (-1 != (count = reader.read(buff, 0, buff.length))) {
                        //caretAtEnd = getCaretPosition() == doc.getLength() ? true : false;
                        append(String.valueOf(buff, 0, count));
                        //if (caretAtEnd) {
                        setCaretPosition(doc.getLength());
                        //}
                    }
                } catch (IOException e) {
                    Application.getWindowManager().showMessage(null, "Error reading from BufferedReader: " + e);
                }
            }
        }).start();
    }
}