/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import xtools.api.param.Param;

/**
 * Application-wide registry for the active {@link ParamEditorFactory}.
 * Headless/CLI paths must leave the factory unset (or use a no-op) so UI widgets are never created.
 */
public final class ParamEditors {

    private static volatile ParamEditorFactory factory;

    private ParamEditors() {
    }

    public static void setFactory(ParamEditorFactory editorFactory) {
        factory = editorFactory;
    }

    public static ParamEditorFactory getFactory() {
        return factory;
    }

    public static boolean isFactorySet() {
        return factory != null;
    }

    public static ParamEditor createEditor(Param param) {
        ParamEditorFactory f = factory;
        if (f == null) {
            throw new IllegalStateException("No ParamEditorFactory registered; call ParamEditors.setFactory(...) from the UI bootstrap");
        }
        return f.createEditor(param);
    }
}
