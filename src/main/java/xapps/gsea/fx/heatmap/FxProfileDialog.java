/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.heatmap;

import org.genepattern.data.expr.IExpressionData;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GPWrappers;
import edu.mit.broad.genome.plots.PlotBuilders;
import edu.mit.broad.xbench.core.api.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import xapps.gsea.fx.plots.FxPlotPane;

/** FX Profile plot dialog (PlotSpec / JavaFX; no JFreeChart). */
public final class FxProfileDialog {

    private FxProfileDialog() {
    }

    public static void show(Dataset dataset, int[] rowIndices) {
        if (dataset == null) {
            Application.getWindowManager().showMessage("No dataset for Profile.");
            return;
        }
        // Touch GPWrappers so expression-data adapters stay initialized.
        IExpressionData ignored = GPWrappers.createIExpressionData(dataset);
        if (ignored == null) {
            Application.getWindowManager().showMessage("No dataset for Profile.");
            return;
        }

        FxPlotPane chartPane = new FxPlotPane("Profile");
        chartPane.setSpec(PlotBuilders.profile(dataset, rowIndices), 800, 480);

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("Profile");
        Button close = new Button("Close");
        xapps.gsea.fx.FxButtons.styleSecondary(close);
        close.setOnAction(e -> stage.close());
        BorderPane root = new BorderPane(chartPane.getNode());
        HBox bottom = xapps.gsea.fx.FxButtons.row(close);
        bottom.setPadding(new Insets(8));
        root.setBottom(bottom);
        BorderPane.setMargin(root.getBottom(), new Insets(8));
        xapps.gsea.fx.FxTheme.prepareRoot(root);
        Scene scene = new Scene(root, 860, 560);
        xapps.gsea.fx.FxTheme.apply(scene);
        stage.setScene(scene);
        stage.show();
    }
}
