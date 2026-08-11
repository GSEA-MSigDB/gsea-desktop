/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.util.Objects;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import xapps.gsea.fx.jobs.ApplicationLog;

/**
 * View for {@link ApplicationLog} — unattributed application messages.
 */
public class FxConsoleViewer implements ViewPage {

    private final BorderPane root = new BorderPane();
    private final TextArea area = new TextArea();

    public FxConsoleViewer(ApplicationLog log) {
        ApplicationLog applicationLog = Objects.requireNonNull(log, "applicationLog");
        area.setEditable(false);
        area.setWrapText(true);
        applicationLog.setActiveArea(area);
        root.setCenter(area);

        Button clear = new Button("Clear All Output");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(clear);
        clear.setOnAction(e -> applicationLog.clear());
        Button copy = new Button("Copy");
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(copy);
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(area.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        HBox actions = xapps.gsea.fx.widgets.FxButtons.row(clear, copy);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8));
        root.setBottom(actions);
    }

    @Override
    public String getTitle() {
        return "Application messages";
    }

    @Override
    public String getIconResourceId() {
        return "expandall.png";
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
