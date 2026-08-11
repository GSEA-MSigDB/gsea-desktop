/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import org.gsea_msigdb.gsea.ui.api.ParamEditor;
import xtools.api.param.Param;

@FunctionalInterface
interface EditorFactory {
    ParamEditor create(Param param);
}
