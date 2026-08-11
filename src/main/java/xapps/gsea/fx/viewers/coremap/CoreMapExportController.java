/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import org.gsea_msigdb.gsea.runtime.AppServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapJob;
import edu.mit.broad.coremap.CoreMapJobStore;
import edu.mit.broad.coremap.CoreMapJson;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import xapps.gsea.fx.params.FxFileChooserUtil;

/**
 * CoreMap PNG / JSON / TSV export and job save/open.
 */
final class CoreMapExportController {

    private static final Logger klog = LoggerFactory.getLogger(CoreMapExportController.class);

    interface Host {
        Node owner();
        AppServices services();
        CoreMapGraphView graphView();
        IntegrationResult lastResult();
        CoreMapJob buildJobSnapshot();
        void loadJob(File dir);
        void setBusy(boolean busy, String message);
        void setStatus(String message);
    }

    private final Host host;

    CoreMapExportController(Host host) {
        this.host = host;
    }

    void exportPng(boolean selectedOnly) {
        if (selectedOnly && !host.graphView().hasSelection()) {
            host.services().dialogs().showMessage("Select genes, bridges, or edges on the map first.");
            return;
        }
        String dataUri = selectedOnly ? host.graphView().exportSelectedPngDataUri()
                : host.graphView().exportPngDataUri();
        if (dataUri == null || !dataUri.contains(",")) {
            host.services().dialogs().showMessage("Graph PNG export is not ready yet.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(selectedOnly ? "Export selected CoreMap PNG" : "Export CoreMap PNG");
        chooser.setInitialFileName(selectedOnly ? "coremap-selected.png" : "coremap.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(host.owner()));
        if (target == null) {
            return;
        }
        try {
            String b64 = dataUri.substring(dataUri.indexOf(',') + 1);
            Files.write(target.toPath(), Base64.getDecoder().decode(b64));
            FxFileChooserUtil.registerOpened(target);
            host.setStatus("Exported PNG → " + target.getName());
        } catch (Throwable ex) {
            klog.error("PNG export failed", ex);
            host.services().dialogs().showError("Could not export PNG", ex);
        }
    }

    void exportSelectedJson() {
        if (!host.graphView().hasSelection()) {
            host.services().dialogs().showMessage(
                    "Nothing highlighted. Select a bridge, driver, hub, or node first.");
            return;
        }
        saveJsonFile(host.graphView().exportSelectedJson(),
                "Export selected CoreMap JSON", "coremap-selected.json");
    }

    void exportCurrentJson() {
        String json = host.graphView().exportCurrentJson();
        if (json == null || json.isBlank()) {
            host.services().dialogs().showMessage("Current graph view is not ready yet.");
            return;
        }
        saveJsonFile(json, "Export current CoreMap JSON", "coremap-current.json");
    }

    void exportCompleteJson() {
        IntegrationResult last = host.lastResult();
        if (last == null || last.elements == null) {
            host.services().dialogs().showMessage("Integrate first to export the complete graph.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export complete CoreMap JSON");
        chooser.setInitialFileName("coremap-complete.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(host.owner()));
        if (target == null) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                String json = CoreMapJson.elementsJson(last.elements.nodes, last.elements.edges);
                Files.writeString(target.toPath(), json, StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    FxFileChooserUtil.registerOpened(target);
                    host.services().dialogs().showMessage("Saved:\n" + target.getAbsolutePath());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> host.services().dialogs().showError("Could not export JSON", ex));
            }
        }, "coremap-export-complete-json");
        t.setDaemon(true);
        t.start();
    }

    void exportBridgesTsv() {
        IntegrationResult last = host.lastResult();
        if (last == null || last.bridges == null || last.bridges.isEmpty()) {
            host.services().dialogs().showMessage("No bridges to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export bridges TSV");
        chooser.setInitialFileName("coremap-bridges.tsv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TSV", "*.tsv", "*.txt"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(host.owner()));
        if (target == null) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Files.writeString(target.toPath(), CoreMapJobStore.formatBridgesTsv(last),
                        StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    FxFileChooserUtil.registerOpened(target);
                    host.services().dialogs().showMessage("Saved:\n" + target.getAbsolutePath());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> host.services().dialogs().showError("Could not export TSV", ex));
            }
        }, "coremap-export-tsv");
        t.setDaemon(true);
        t.start();
    }

    void saveCoreMapJob() {
        if (host.lastResult() == null) {
            host.services().dialogs().showMessage("Integrate first to save a CoreMap job.");
            return;
        }
        File outDir;
        try {
            outDir = host.services().vdb().getDefaultOutputDir();
        } catch (Throwable t) {
            host.services().dialogs().showError("Could not resolve default output folder", t);
            return;
        }
        if (outDir == null) {
            host.services().dialogs().showMessage(
                    "Set a default output folder in Preferences before saving a CoreMap job.");
            return;
        }
        TextInputDialog labelDialog = new TextInputDialog("CoreMap");
        labelDialog.setTitle("Save CoreMap Job");
        labelDialog.setHeaderText("Saves to the default output folder and Analysis History.");
        labelDialog.setContentText("Analysis name (no spaces):");
        labelDialog.initOwner(FxFileChooserUtil.windowOf(host.owner()));
        xapps.gsea.fx.FxTheme.apply(labelDialog);
        var labelOpt = labelDialog.showAndWait();
        if (labelOpt.isEmpty() || labelOpt.get().isBlank()) {
            return;
        }
        String label = labelOpt.get().trim().replace(' ', '_');
        CoreMapJob job = host.buildJobSnapshot();
        byte[] png = CoreMapJobStore.decodePngDataUri(host.graphView().exportPngDataUri());
        host.setBusy(true, "Saving CoreMap job…");
        Thread t = new Thread(() -> {
            try {
                File reportDir = CoreMapJobStore.save(job, label, outDir, png);
                Platform.runLater(() -> {
                    host.setBusy(false, "Saved CoreMap job.");
                    FxFileChooserUtil.registerOpenedDir(reportDir);
                    host.services().dialogs().showMessage(
                            "Saved CoreMap job (listed in Analysis History):\n" + reportDir.getAbsolutePath());
                });
            } catch (Throwable ex) {
                klog.error("Could not save CoreMap job", ex);
                Platform.runLater(() -> {
                    host.setBusy(false, "Could not save CoreMap job.");
                    host.services().dialogs().showError("Could not save CoreMap job", ex);
                });
            }
        }, "coremap-save-job");
        t.setDaemon(true);
        t.start();
    }

    void openCoreMapJob() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open CoreMap Job folder");
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File dir = chooser.showDialog(FxFileChooserUtil.windowOf(host.owner()));
        if (dir == null) {
            return;
        }
        if (!CoreMapJobStore.looksLikeJobDir(dir)) {
            host.services().dialogs().showMessage(
                    "Folder does not contain " + CoreMapJob.JOB_FILE + ":\n" + dir.getAbsolutePath());
            return;
        }
        host.loadJob(dir);
    }

    private void saveJsonFile(String json, String title, String initialName) {
        if (json == null || json.isBlank()) {
            host.services().dialogs().showMessage("Nothing to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        FxFileChooserUtil.seedInitialDirectory(chooser);
        File target = chooser.showSaveDialog(FxFileChooserUtil.windowOf(host.owner()));
        if (target == null) {
            return;
        }
        final String payload = json;
        Thread t = new Thread(() -> {
            try {
                Files.writeString(target.toPath(), payload, StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    FxFileChooserUtil.registerOpened(target);
                    host.services().dialogs().showMessage("Saved:\n" + target.getAbsolutePath());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> host.services().dialogs().showError("Could not export JSON", ex));
            }
        }, "coremap-export-json");
        t.setDaemon(true);
        t.start();
    }
}
