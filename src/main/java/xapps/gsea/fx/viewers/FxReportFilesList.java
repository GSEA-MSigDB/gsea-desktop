/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.reports.api.Report;
import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import xapps.gsea.fx.widgets.FxFileTransferSupport;

/**
 * Report / results file list with double-click open, context menu, and drag-out.
 * Prefers a full scan of the report directory when available (rpt metadata is incomplete).
 */
public final class FxReportFilesList {

    private FxReportFilesList() {
    }

    public static ListView<File> create(Report report, Consumer<ViewPage> openPage) {
        File reportDir = report != null ? report.getReportDir() : null;
        return create(listReportFiles(report), reportDir, openPage);
    }

    public static ListView<File> create(File[] files, Consumer<ViewPage> openPage) {
        List<File> produced = new ArrayList<>();
        if (files != null) {
            produced.addAll(Arrays.asList(files));
        }
        return create(produced, null, openPage);
    }

    private static ListView<File> create(List<File> files, File reportDir, Consumer<ViewPage> openPage) {
        ListView<File> list = new ListView<>(FXCollections.observableArrayList(files));
        list.setPlaceholder(new Label("No files found"));
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (!files.isEmpty()) {
            list.getSelectionModel().select(0);
        }
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                setText(displayName(item, reportDir));
                setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forFile(item));
                setTooltip(new javafx.scene.control.Tooltip(item.getAbsolutePath()));
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                File selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    FxFileActions.runDefaultFileAction(selected, openPage);
                }
            }
        });
        list.setOnContextMenuRequested(e -> {
            File selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            ContextMenu built = FxFileActions.fileContextMenu(selected, openPage, null);
            list.setContextMenu(built);
            built.show(list, e.getScreenX(), e.getScreenY());
        });
        list.setOnDragDetected(e -> {
            List<File> selected = new ArrayList<>();
            for (File f : list.getSelectionModel().getSelectedItems()) {
                if (f != null) {
                    selected.add(f);
                }
            }
            if (!selected.isEmpty()) {
                FxFileTransferSupport.startFileDrag(list, selected, e);
            }
        });
        return list;
    }

    /**
     * Union of rpt-registered files and a recursive scan of the report directory.
     */
    public static List<File> listReportFiles(Report report) {
        LinkedHashSet<File> all = new LinkedHashSet<>();
        if (report != null) {
            File[] produced = report.getFilesProduced();
            if (produced != null) {
                for (File f : produced) {
                    if (f != null) {
                        all.add(f);
                    }
                }
            }
            File dir = report.getReportDir();
            if (dir != null && dir.isDirectory()) {
                collectFiles(dir, all);
            }
        }
        List<File> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparing(File::getAbsolutePath, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    private static void collectFiles(File dir, LinkedHashSet<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File child : children) {
            if (child == null || child.isHidden()) {
                continue;
            }
            if (child.isDirectory()) {
                collectFiles(child, out);
            } else if (child.isFile()) {
                out.add(child);
            }
        }
    }

    private static String displayName(File file, File reportDir) {
        if (reportDir != null) {
            String base = reportDir.getAbsolutePath();
            String path = file.getAbsolutePath();
            if (path.startsWith(base)) {
                String rel = path.substring(base.length());
                if (rel.startsWith(File.separator)) {
                    rel = rel.substring(1);
                }
                if (!rel.isEmpty()) {
                    return rel;
                }
            }
        }
        return file.getAbsolutePath();
    }
}
