/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import xtools.api.param.Param;

/**
 * Creates toolkit-specific {@link ParamEditor} instances for params.
 */
public interface ParamEditorFactory {

    ParamEditor createEditor(Param param);
}
