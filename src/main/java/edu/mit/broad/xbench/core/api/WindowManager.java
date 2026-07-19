/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.Errors;

/**
 * Toolkit-agnostic window / dialog facade used by tools and the FX shell.
 */
public interface WindowManager {
    void showError(String msg);

    void showError(Throwable t);

    void showError(Errors errors);

    void showError(String msg, Throwable t);

    boolean showConfirm(String msg);

    boolean showConfirm(String title, String msg);

    void showMessage(String msg);

    void showMessage(String title, String msg);
}
