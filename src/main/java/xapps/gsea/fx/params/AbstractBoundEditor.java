/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import org.gsea_msigdb.gsea.ui.api.ParamEditor;
import xtools.api.param.Param;

abstract class AbstractBoundEditor implements ParamEditor {
    protected final Param param;
    private Runnable changeListener;

    protected AbstractBoundEditor(Param param) {
        this.param = param;
    }

    @Override
    public void commitToParam() {
        param.setValue(getValue());
    }

    @Override
    public void onChange(Runnable listener) {
        this.changeListener = listener;
    }

    protected void fireChange() {
        if (changeListener != null) {
            changeListener.run();
        }
    }
}
