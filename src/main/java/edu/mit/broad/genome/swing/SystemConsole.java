/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.io.LoopedStreams;
import edu.mit.broad.xbench.core.api.Application;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.SimpleLayout;

import javax.swing.*;
import javax.swing.text.Document;
import java.io.*;

/**
 * Redirects and captures stdout and stderr to a swing component
 */
public class SystemConsole extends JTextArea {

    /**
     * @throws java.io.IOException
     */
    public SystemConsole() {

        this.setText("< Process output will appear below >\n\n");

        // @todo need to figure out how to get concurrent sys out on bith the sttaus thing and idea output going

        if (Conf.isDebugMode() == false) {
            try {


                final LoopedStreams ls = new LoopedStreams();
                // Redirect System.out & System.err.
                PrintStream ps = new PrintStream(ls.getOutputStream());
                System.setOut(ps);
                System.setErr(ps);

                //Layout layout = new MyPatternLayout();
                Layout layout = new SimpleLayout();
                BasicConfigurator.configure(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));

                startConsoleReaderThread(ls.getInputStream());
                //startConsoleReaderThread(System.out);

                // as changes
                setAutoscrolls(true);
            } catch (Throwable t) {
                this.setText("Could not open system console properly. Error: " + TraceUtils.getAsString(t));
            }
        } else {
            System.out.println("Skipping sysout redirection as in debug mode");
        }
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

} // SystemConsole

