/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import java.io.*;

/**
 * ByteArrayOutputStream to provide the same functionality as Java's piped streams
 * but without the deadlock and IOExceptions. The
 *
 * @see http://archive.devx.com/upload/registered/features/javapro/1999/11nov99/tl1199/tl1199.asp
 */
//TODO: examine java.nio as an alternative
public class LoopedStreams {

    private PipedOutputStream pipedOS = new PipedOutputStream();

    private boolean keepRunning = true;

    private ByteArrayOutputStream byteArrayOS =
            new ByteArrayOutputStream() {
                public void close() {
                    keepRunning = false;
                    try {
                        super.close();
                        pipedOS.close();
                    } catch (IOException e) {
                        // Do something to log the error--
                        // perhaps invoke a Runnable.
                        e.printStackTrace();
                    }
                }
            };


    private PipedInputStream pipedIS =
            new PipedInputStream() {
                public void close() {
                    keepRunning = false;
                    try {
                        super.close();
                    } catch (IOException e) {
                        // Do something to log the error--
                        // perhaps invoke a Runnable.
                        e.printStackTrace();
                    }
                }
            };


    /**
     * Class constructor
     *
     * @throws IOException
     */
    public LoopedStreams() throws IOException {
        pipedOS.connect(pipedIS);
        startByteArrayReaderThread();
    }


    public InputStream getInputStream() {
        return pipedIS;
    }

    public OutputStream getOutputStream() {
        return byteArrayOS;
    }

    private void startByteArrayReaderThread() {
        new Thread(new Runnable() {
            public void run() {
                while (keepRunning) {
                    // Check for bytes in the stream.
                    if (byteArrayOS.size() > 0) {
                        byte[] buffer = null;
                        synchronized (byteArrayOS) {
                            buffer = byteArrayOS.toByteArray();
                            byteArrayOS.reset(); // Clear the buffer.
                        }
                        try {
                            // Send the extracted data to
                            // the PipedOutputStream.
                            pipedOS.write(buffer, 0, buffer.length);
                        } catch (IOException e) {
                            // Do something to log the error
                            e.printStackTrace();
                        }
                    } else // No data available, go to sleep.
                        try {
                            // Check the ByteArrayOutputStream every
                            // 1 second for new data.
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                }
            }
        }).start();
    } // startByteArrayReaderThread()
} // LoopedStreams


