/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import org.apache.log4j.Logger;

/**
 * Simple timer mechanism to help with performance benchmarking.
 * <p/>
 * Is designed for simple procedural use:
 * <p/>
 * <code>
 * timer.start();
 * // do some task(s)
 * timer.stop()
 * timer.timeTaken() // prints to stdout teh time taken
 * <p/>
 * </code>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @todo See if there is an open-source junit-like class for this.
 * There is one junitperf but its more geared towards stress tests
 * rather than simple timing. The hunt continues.
 */
public class Timer {

    protected static final Logger klog = Logger.getLogger(Timer.class);
    private long fStart = 0;
    private long fStop = 0;
    private boolean stopped = false;

    public Timer() {
    }

    /**
     * Starts the timer
     */
    public void start() {
        fStart = System.currentTimeMillis();
        stopped = false;
    }

    /**
     * Stops the timer
     */
    public void stop() {
        fStop = System.currentTimeMillis();
        stopped = true;
    }

    public void printTimeTakenS() {
        System.out.println(_timeTaken(null));
    }

    private String _timeTaken(String label) {
        //if (fStop < fStart) log.warn("Looks like the timer was not stop()'ed");
        if (!stopped) {
            stop();
        }

        long t = fStop - fStart;

        if (label != null) {
            if (t > 1000) {
                return ("Time taken for " + label + ": " + t / 1000 + " secs");
            } else {
                return ("Time taken for " + label + ": " + t + " ms");
            }
        } else {
            if (t > 1000) {
                return ("Time taken: " + t / 1000 + " secs");
            } else {
                return ("Time taken: " + t + " ms");
            }
        }
    }

}    // End Timer
