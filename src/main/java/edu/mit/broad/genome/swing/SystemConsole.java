/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.swing.JTextArea;
import javax.swing.text.Document;

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
        try {
            final LoopedStreams ls = new LoopedStreams();
            // Redirect System.out & System.err.
            PrintStream ps = new PrintStream(ls.getOutputStream());
            System.setOut(ps);
            System.setErr(ps);
            startConsoleReaderThread(ls.getInputStream());

            // as changes
            setAutoscrolls(true);
        } catch (Throwable t) {
            this.setText("Could not open system console properly. Error: " + TraceUtils.getAsString(t));
        }
    }

    public static StatusBarAppender createStatusBarAppender(String name) {
        final StatusBarAppender statusBar = new StatusBarAppender(name);
        return statusBar;
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
                        append(String.valueOf(buff, 0, count));
                        setCaretPosition(doc.getLength());
                    }
                } catch (IOException e) {
                    Application.getWindowManager().showMessage(null, "Error reading from BufferedReader: " + e);
                }
            }
        }).start();
    }
}
