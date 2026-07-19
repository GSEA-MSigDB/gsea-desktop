/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.gsea_msigdb.gsea.ui.api.ParamEditors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xapps.gsea.fx.params.JavaFxParamEditorFactory;
import xapps.gsea.fx.shell.GseaFxShell;

/**
 * JavaFX entry for the GSEA desktop UI.
 */
public class GseaFxApplication extends Application {

    private static final Logger klog = LoggerFactory.getLogger(GseaFxApplication.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            ParamEditors.setFactory(new JavaFxParamEditorFactory());
            GseaFxShell shell = new GseaFxShell(primaryStage);
            shell.show();
        } catch (Throwable t) {
            klog.error("Failed to start JavaFX GSEA UI", t);
            Platform.exit();
        }
    }

    public static void launchFx(String[] args) {
        launch(GseaFxApplication.class, args);
    }
}
