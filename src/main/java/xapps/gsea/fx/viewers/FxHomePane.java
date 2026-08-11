/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.xbench.tui.ReportStub;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.viewers.report.FxReportOpen;
import org.gsea_msigdb.gsea.runtime.AppServices;

/** Home page with quick actions and recent analyses. */
public class FxHomePane implements ViewPage {

    private final BorderPane root = new BorderPane();

    public FxHomePane(FeatureHost host, Runnable openLoadData, Runnable openGsea,
            Runnable openHistory) {
        Objects.requireNonNull(host, "host");
        AppServices services = host.services();

        ImageView imageView = new ImageView();
        try {
            var url = getClass().getResource("/edu/mit/broad/genome/resources/intro_screen.jpg");
            if (url != null) {
                // Background decode so Home layout is not blocked on the JPEG.
                imageView.setImage(new Image(url.toExternalForm(), true));
            }
        } catch (Throwable ignored) {
        }
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(520);

        Label brand = new Label("Gene Set Enrichment Analysis");
        brand.getStyleClass().add("gsea-section-header");
        brand.setStyle("-fx-font-size: 22px;");

        Label blurb = new Label(
                "Load expression data, choose gene sets, and run enrichment — or reopen a recent analysis.");
        blurb.setWrapText(true);
        blurb.getStyleClass().add("gsea-tool-desc");
        blurb.setMaxWidth(520);

        Button loadData = actionButton("Load Data", "Open16.gif", true, openLoadData);
        Button runGsea = actionButton("Run GSEA", "Gsea_app16_v2.png", true, openGsea);
        Button history = actionButton("Analysis history", "past_analysis16.gif", false, openHistory);
        xapps.gsea.fx.widgets.FxButtons.sizeToContent(loadData, runGsea, history);

        Label recentHeader = new Label("Recent analyses");
        recentHeader.getStyleClass().add("gsea-section-header");
        VBox recentBox = new VBox(6, recentHeader);
        recentBox.setPadding(new Insets(8, 0, 0, 0));
        addRecentButtons(recentBox, host, services);

        VBox left = new VBox(14, brand, blurb, xapps.gsea.fx.widgets.FxButtons.row(loadData, runGsea, history),
                recentBox);
        left.setPadding(new Insets(24, 16, 24, 24));
        left.setMaxWidth(560);

        HBox hero = new HBox(24, left, imageView);
        hero.setAlignment(Pos.TOP_LEFT);
        hero.setPadding(new Insets(8));
        HBox.setHgrow(left, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(hero);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        root.setCenter(scroll);
    }

    private static Button actionButton(String text, String icon, boolean primary, Runnable action) {
        Button b = new Button(text);
        b.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource(icon));
        if (primary) {
            xapps.gsea.fx.widgets.FxButtons.stylePrimary(b);
        } else {
            xapps.gsea.fx.widgets.FxButtons.styleSecondary(b);
        }
        if (action != null) {
            b.setOnAction(e -> action.run());
        }
        return b;
    }

    private static void addRecentButtons(VBox recentBox, FeatureHost host, AppServices svc) {
        ReportStub[] all;
        try {
            all = svc.tools().getReportsInCache();
        } catch (Throwable t) {
            all = null;
        }
        if (all == null || all.length == 0) {
            Label empty = new Label("No analyses in the report cache yet.");
            empty.getStyleClass().add("gsea-muted");
            recentBox.getChildren().add(empty);
            return;
        }
        Arrays.sort(all, Comparator.comparingLong(ReportStub::getTimestamp).reversed());
        for (int i = 0; i < Math.min(5, all.length); i++) {
            ReportStub stub = all[i];
            if (stub == null) {
                continue;
            }
            Button b = new Button(stub.getName_without_ts());
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            xapps.gsea.fx.widgets.FxButtons.styleSecondary(b);
            b.setOnAction(e -> {
                try {
                    FxReportOpen.openInApp(stub.getReport(false), host);
                } catch (Exception ex) {
                    svc.dialogs().showError("Could not open report", ex);
                }
            });
            recentBox.getChildren().add(b);
        }
    }

    @Override
    public String getTitle() {
        return "Home";
    }

    @Override
    public String getIconResourceId() {
        return null;
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
