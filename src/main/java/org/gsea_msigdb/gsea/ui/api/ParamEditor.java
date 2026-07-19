/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

/**
 * Editor for a single {@link xtools.api.param.Param} value.
 * The view object is a JavaFX {@code Node}.
 */
public interface ParamEditor {

    /**
     * @return the value currently chosen in the editor
     */
    Object getValue();

    /**
     * Programmatically set the editor value (and sync the view).
     */
    void setValue(Object value);

    /**
     * JavaFX view node for this editor.
     */
    Object getView();

    /**
     * Push the editor's current value into the backing Param (if bound).
     */
    void commitToParam();

    /**
     * Register a listener invoked whenever the user edits this control's value, so forms can
     * (e.g.) re-check whether all required params are now set. Optional; default is a no-op.
     */
    default void onChange(Runnable listener) {
    }
}
