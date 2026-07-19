/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

/**
 * Workspace API for the OpenJFX shell (tabs, dialogs, messages).
 */
public interface Workspace {

    void openPage(ViewPage page);

    void showError(String message);

    void showError(String message, Throwable t);

    void showMessage(String title, String message);

    boolean showConfirm(String title, String message);
}
