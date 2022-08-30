/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.swing.GuiHelper;

import java.awt.*;

/**
 * Object encapsulating Tool's execution state -- name and color
 * 
 * @author Aravind Subramanian, David Eby
 */
class ExecState {
    public static final ExecState PAUSED = new ExecState("Paused", Color.ORANGE);
    public static final ExecState KILLED = new ExecState("Killed", Color.MAGENTA);
    public static final ExecState CANCELED = new ExecState("Canceled", GuiHelper.COLOR_DARK_BROWN);

    public static final ExecState WAITING = new ExecState("Waiting", Color.GRAY);
    public static final ExecState RUNNING = new ExecState("Running", Color.BLUE);
    public static final ExecState SUCCESS = new ExecState("Success", Color.GREEN);
    public static final ExecState SUCCESS_WARN = new ExecState("Success (with warnings)", Color.MAGENTA);
    public static final ExecState PARAM_ERROR = new ExecState("Invalid Param(s)",
            GuiHelper.COLOR_DARK_BROWN);
    public static final ExecState EXEC_ERROR = new ExecState("Error!", Color.RED);

    public final String name;
    public final Color color;

    private ExecState(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String toString() { return name; }

    public boolean equals(Object obj) {
        if (obj instanceof ExecState) {
            return (equals((ExecState) obj));
        }

        return false;
    }

    public boolean equals(ExecState es) {
        if (es == null) {
            return false;
        }

        return es.name.equals(name);
    }

    public int hashCode() { return name.hashCode(); }
}
