/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.tui;

/**
 * Typed execution state for a tool run. Display labels are separate from this enum.
 */
public enum JobState {
    WAITING,
    RUNNING,
    CANCELING,
    CANCELED,
    SUCCESS,
    SUCCESS_WARN,
    ERROR,
    INVALID_PARAM;

    public boolean isActive() {
        return this == WAITING || this == RUNNING || this == CANCELING;
    }

    public boolean isTerminal() {
        return !isActive();
    }

    public boolean isSuccess() {
        return this == SUCCESS || this == SUCCESS_WARN;
    }

    /** Default status cell / badge label (Swing ExecState name parity where applicable). */
    public String defaultLabel() {
        switch (this) {
            case WAITING:
                return "Waiting";
            case RUNNING:
                return "Running";
            case CANCELING:
                return "Canceling…";
            case CANCELED:
                return "Canceled";
            case SUCCESS:
                return "Success";
            case SUCCESS_WARN:
                return "Success (with warnings)";
            case ERROR:
                return "Error!";
            case INVALID_PARAM:
                return "Invalid Param(s)";
            default:
                return name();
        }
    }
}
