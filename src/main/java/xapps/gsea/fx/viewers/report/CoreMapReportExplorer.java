/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapJob;
import edu.mit.broad.coremap.CoreMapJobStore;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.core.api.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.FxButtons;
import xapps.gsea.fx.viewers.coremap.CoreMapWorkspace;

/**
 * Results explorer for saved CoreMap jobs (Analysis History / FxReportViewer).
 */
public final class CoreMapReportExplorer implements ReportExplorer {

    private static final Logger klog = LoggerFactory.getLogger(CoreMapReportExplorer.class);

    @Override
    public Node create(Report report, Consumer<ViewPage> openPage) {
        File dir = ReportExplorerSupport.reportDir(report);
        if (dir == null || !CoreMapJobStore.looksLikeJobDir(dir)) {
            return new GenericFilesExplorer().create(report, openPage);
        }

        TextArea summary = new TextArea(buildSummary(dir));
        summary.setEditable(false);
        summary.setWrapText(true);

        Button open = new Button("Open in CoreMap");
        FxButtons.stylePrimary(open);
        open.setOnAction(e -> {
            try {
                CoreMapWorkspace.openJob(dir, openPage);
            } catch (Throwable t) {
                klog.error("Could not open CoreMap job", t);
                Application.getWindowManager().showError("Could not open CoreMap job", t);
            }
        });

        HBox actions = new HBox(8, open);
        actions.setPadding(new Insets(8, 0, 0, 0));
        VBox box = new VBox(8, new Label("CoreMap job"), summary, actions);
        VBox.setVgrow(summary, Priority.ALWAYS);
        box.setPadding(new Insets(12));
        BorderPane pane = new BorderPane(box);
        return ReportExplorerSupport.tabPane(
                new Tab("CoreMap", pane),
                new Tab("Files", new GenericFilesExplorer().create(report, openPage)));
    }

    private static String buildSummary(File dir) {
        try {
            File jobFile = new File(dir, CoreMapJob.JOB_FILE);
            String text = Files.readString(jobFile.toPath(), StandardCharsets.UTF_8);
            Object parsed = new JSONParser().parse(text);
            if (!(parsed instanceof JSONObject o)) {
                return "CoreMap job folder:\n" + dir.getAbsolutePath();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Folder: ").append(dir.getAbsolutePath()).append('\n');
            append(sb, "Saved", o.get("saved_at"));
            append(sb, "Mechanistic", o.get("mechanistic_path"));
            append(sb, "Phenotypic", o.get("phenotypic_path"));
            append(sb, "View mode", o.get("view_mode"));
            append(sb, "Provider key", o.get("provider_key"));
            Object opts = o.get("options");
            if (opts instanceof JSONObject oo) {
                append(sb, "Interactome", oo.get("interactome_source"));
            }
            sb.append('\n').append("Open in CoreMap to reload the graph and tables.");
            sb.append('\n').append("After reload, run Integrate again before Rescore.");
            return sb.toString();
        } catch (Exception ex) {
            return "CoreMap job folder:\n" + dir.getAbsolutePath() + "\n\n(Could not read summary: "
                    + ex.getMessage() + ")";
        }
    }

    private static void append(StringBuilder sb, String label, Object val) {
        if (val != null && !String.valueOf(val).isBlank()) {
            sb.append(label).append(": ").append(val).append('\n');
        }
    }
}
