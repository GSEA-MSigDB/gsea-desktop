/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import xapps.gsea.fx.widgets.FxFileIcons;
import org.gsea_msigdb.gsea.runtime.AppServices;

/** Type-specific actions for files / cached objects. */
public final class FxFileActions {
    private static final Logger klog = LoggerFactory.getLogger(FxFileActions.class);

    private FxFileActions() {
    }

    public static ContextMenu recentFileMenu(File file, Consumer<ViewPage> openPage, Runnable afterChange) {
        return fileContextMenu(file, openPage, afterChange);
    }

    /**
     * Context menu for a produced/report/recent file).
     * Recent-list purge items are added by {@code FxLoadDataPane}, not here.
     */
    public static ContextMenu fileContextMenu(File file, Consumer<ViewPage> openPage, Runnable afterChange) {
        ContextMenu menu = new ContextMenu();
        if (file == null) {
            return menu;
        }
        String ext = extensionOf(file);
        Consumer<ViewPage> open = openPage != null ? openPage : page -> { };
        Runnable refresh = afterChange != null ? afterChange : () -> { };

        if (Constants.DEF.equals(ext)) {
            addCommonActions(menu, file, open);
            return menu;
        }

        List<MenuItem> customs = viewerAndExtraItems(file, ext, open, refresh);
        if (customs.isEmpty()) {
            // Unknown type / html/tsv/csv/xls: FileBrowserAction + OsExplorerAction.
            if (!file.isDirectory()) {
                menu.getItems().add(launchFileItem(file));
            }
            menu.getItems().add(fileExplorerItem(file));
            return menu;
        }

        menu.getItems().addAll(customs);
        menu.getItems().add(new SeparatorMenuItem());
        addCommonActions(menu, file, open);
        return menu;
    }

    private static void addCommonActions(ContextMenu menu, File file, Consumer<ViewPage> open) {
        MenuItem forceReload = item("Force data reload", "Refresh16_2.gif",
                () -> openFile(file, open, false, false));
        menu.getItems().add(forceReload);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(fileExplorerItem(file));
    }

    /** Object-cache context menu. */
    public static ContextMenu objectContextMenu(PersistentObject pob, Consumer<ViewPage> openPage,
            Runnable afterChange) {
        ContextMenu menu = new ContextMenu();
        if (pob == null) {
            return menu;
        }
        Consumer<ViewPage> open = openPage != null ? openPage : page -> { };
        Runnable refresh = afterChange != null ? afterChange : () -> { };
        File src = null;
        try {
            src = AppServices.require().cache().getSourceFile(pob);
        } catch (Throwable ignored) {
            // no source
        }

        List<MenuItem> customs = viewerAndExtraItemsForObject(pob, src, open, refresh);
        if (customs.isEmpty()) {
            return menu;
        }
        menu.getItems().addAll(customs);
        if (src != null) {
            menu.getItems().add(new SeparatorMenuItem());
            addCommonActions(menu, src, open);
        }
        return menu;
    }

    /**
     * html/tsv → browser; otherwise {@code ParserWorker} (progress + cancel + success/error toast, no viewer).
     */
    public static void runDefaultFileAction(File file, Consumer<ViewPage> openPage) {
        if (file == null) {
            return;
        }
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".tsv")) {
            try {
                xapps.gsea.fx.FxDesktopUtil.openUri(file.toURI());
            } catch (Exception e) {
                AppServices.require().dialogs().showError("Could not open file", e);
            }
            return;
        }
        // ParserWorker parity — not openFile (which skips ProgressMonitor + completion toast).
        javafx.concurrent.Task<edu.mit.broad.genome.objects.PersistentObject> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected edu.mit.broad.genome.objects.PersistentObject call() throws Exception {
                        if (file.isDirectory()) {
                            throw new RuntimeException(
                                    "Only files can be choosen - a directory was specified: " + file.getPath());
                        }
                        edu.mit.broad.genome.objects.PersistentObject pob =
                                xapps.gsea.fx.widgets.FxProgressMonitorRead.read(file);
                        if (pob == null) {
                            throw new RuntimeException("Loading of file '" + file.getName() + "' canceled.");
                        }
                        AppServices.require().files().registerRecentlyOpenedFile(file);
                        return pob;
                    }
                };
        task.setOnSucceeded(e -> {
            edu.mit.broad.genome.objects.PersistentObject pob = task.getValue();
            boolean hadWarnings = pob.getWarnings() != null && !pob.getWarnings().isEmpty();
            StringBuilder buf = new StringBuilder("<html>Loading ... 1 files<br><br>");
            buf.append(file.getName()).append("\n");
            buf.append("<br>Files loaded successfully: 1 / 1<br>");
            buf.append("There were NO errors");
            if (hadWarnings) {
                buf.append("<br><br><b>There were warnings. See the [+] console log for details.</b>");
            }
            buf.append("</html>");
            AppServices.require().dialogs().showMessage(buf.toString());
        });
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            klog.error("Default open failed for {}", file, t);
            AppServices.require().dialogs().showError("Could not open file", t);
        });
        xapps.gsea.fx.FxWorkers.start(task, "gsea-default-file-open");
    }

    private static List<MenuItem> viewerAndExtraItems(File file, String ext, Consumer<ViewPage> open,
            Runnable refresh) {
        List<MenuItem> items = new ArrayList<>();
        switch (ext) {
            case Constants.GCT, Constants.RES, Constants.PCL, Constants.TXT ->
                    items.add(item("DatasetViewer", "Res16.gif", () -> openFile(file, open, true)));
            case Constants.RNK ->
                    items.add(item("RankedListViewer", "Rnk.png", () -> openFile(file, open, true)));
            case Constants.CLS ->
                    items.add(item("Phenotype viewer", "Cls.gif", () -> openFile(file, open, true)));
            case Constants.GRP -> {
                items.add(item("GeneSetViewer", "Grp.gif", () -> openFile(file, open, true)));
                items.add(new SeparatorMenuItem());
                items.add(item("=> Remove duplicates from the GeneSet", null,
                        () -> removeGeneSetDuplicates(file, refresh)));
            }
            case Constants.GMX, Constants.GMT -> {
                items.add(item("GeneSetMatrixViewer2", "Gmx.png", () -> openFile(file, open, true)));
                items.add(new SeparatorMenuItem());
                items.add(item("=> Extract GeneSets from the GeneSetMatrix", null,
                        () -> extractGeneSets(file, refresh)));
                items.add(item("=> Convert the GeneSetMatrix into a single GeneSet", null,
                        () -> convertMatrixToGeneSet(file, refresh)));
            }
            case Constants.CHIP ->
                    items.add(item("View Chip Annotation", "Chip16.png", () -> openFile(file, open, true)));
            case Constants.RPT ->
                    items.add(item("ReportViewer", "past_analysis16.gif", () -> openFile(file, open, true)));
            default -> {
                // html/htm/tsv/csv/xls and unknown extensions use FileBrowser + OsExplorer only.
            }
        }
        return items;
    }

    private static List<MenuItem> viewerAndExtraItemsForObject(PersistentObject pob, File src,
            Consumer<ViewPage> open, Runnable refresh) {
        List<MenuItem> items = new ArrayList<>();
        if (pob instanceof edu.mit.broad.genome.objects.Dataset) {
            items.add(item("DatasetViewer", "Res16.gif", () -> openObject(pob, open)));
        } else if (pob instanceof edu.mit.broad.genome.objects.RankedList) {
            items.add(item("RankedListViewer", "Rnk.png", () -> openObject(pob, open)));
        } else if (pob instanceof edu.mit.broad.genome.objects.Template) {
            items.add(item("Phenotype viewer", "Cls.gif", () -> openObject(pob, open)));
        } else if (pob instanceof GeneSet) {
            items.add(item("GeneSetViewer", "Grp.gif", () -> openObject(pob, open)));
            if (src != null) {
                items.add(new SeparatorMenuItem());
                items.add(item("=> Remove duplicates from the GeneSet", null,
                        () -> removeGeneSetDuplicates(src, refresh)));
            }
        } else if (pob instanceof GeneSetMatrix gm) {
            items.add(item("GeneSetMatrixViewer2", "Gmx.png", () -> openObject(pob, open)));
            items.add(new SeparatorMenuItem());
            items.add(item("=> Extract GeneSets from the GeneSetMatrix", null,
                    () -> extractGeneSets(gm, refresh)));
            items.add(item("=> Convert the GeneSetMatrix into a single GeneSet", null,
                    () -> convertMatrixToGeneSet(gm, refresh)));
        } else if (pob instanceof edu.mit.broad.vdb.chip.Chip) {
            items.add(item("View Chip Annotation", "Chip16.png", () -> openObject(pob, open)));
        } else if (pob instanceof edu.mit.broad.genome.reports.api.Report) {
            items.add(item("ReportViewer", "past_analysis16.gif", () -> openObject(pob, open)));
        }
        return items;
    }

    private static MenuItem launchFileItem(File file) {
        String ext = extensionOf(file);
        String label = "Launch File";
        String icon = null;
        if (Constants.HTML.equals(ext)) {
            label = "Web Browser";
            icon = "Htm.gif";
        } else if (Constants.CSV.equals(ext) || Constants.TSV.equals(ext) || Constants.XLS.equals(ext)) {
            label = "Launch in Excel";
            icon = "Xls.gif";
        }
        return item(label, icon, () -> {
            try {
                xapps.gsea.fx.FxDesktopUtil.openUri(file.toURI());
            } catch (Exception ex) {
                AppServices.require().dialogs().showError("Trouble launching File on path '" + file.getPath() + "'", ex);
            }
        });
    }

    private static MenuItem fileExplorerItem(File file) {
        return item("File Explorer", "OsExplorer16.gif", () -> revealInOs(file));
    }

    private static MenuItem item(String label, String iconResource, Runnable action) {
        MenuItem mi = new MenuItem(label);
        if (iconResource != null) {
            mi.setGraphic(FxFileIcons.forResource(iconResource));
        }
        mi.setOnAction(e -> action.run());
        return mi;
    }

    private static void openObject(PersistentObject pob, Consumer<ViewPage> openPage) {
        viewObject(pob, openPage);
    }

    /** Open the default viewer for a cached object (primary action). */
    public static void viewObject(PersistentObject pob, FeatureHost host) {
        if (pob == null || host == null) {
            return;
        }
        try {
            host.openPage(FxViewerFactory.open(pob, host));
        } catch (Exception ex) {
            AppServices.require().dialogs().showError("Could not open viewer", ex);
        }
    }

    public static void viewObject(PersistentObject pob, Consumer<ViewPage> openPage) {
        if (pob == null || openPage == null) {
            return;
        }
        try {
            // Non-Report objects do not need FeatureHost.
            openPage.accept(FxViewerFactory.open(pob));
        } catch (Exception ex) {
            AppServices.require().dialogs().showError("Could not open viewer", ex);
        }
    }

    public static void openFile(File file, Consumer<ViewPage> openPage, boolean openViewer) {
        openFile(file, openPage, openViewer, true);
    }

    public static void openFile(File file, Consumer<ViewPage> openPage, boolean openViewer, boolean useCache) {
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            try {
                xapps.gsea.fx.FxDesktopUtil.openUri(file.toURI());
            } catch (Exception e) {
                AppServices.require().dialogs().showError("Could not open file", e);
            }
            return;
        }
        if (lower.endsWith(".tsv") && useCache) {
            try {
                xapps.gsea.fx.FxDesktopUtil.openUri(file.toURI());
            } catch (Exception e) {
                AppServices.require().dialogs().showError("Could not open file", e);
            }
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                Object obj = ParserFactory.read(file, useCache);
                if (useCache) {
                    AppServices.require().files().registerRecentlyOpenedFile(file);
                } else {
                    AppServices.require().files().getRecentFilesStore().refresh(file.getPath());
                }
                if (openViewer && obj != null && openPage != null) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (obj instanceof edu.mit.broad.genome.reports.api.Report) {
                                AppServices.require().dialogs().showMessage(
                                        "Open this report from Analysis History or Jobs.");
                            } else {
                                openPage.accept(FxViewerFactory.open(obj));
                            }
                        } catch (Exception ex) {
                            klog.debug("No viewer for {}", obj.getClass().getName());
                        }
                    });
                } else if (!useCache && !openViewer) {
                    final Object reloaded = obj;
                    javafx.application.Platform.runLater(() -> {
                        if (reloaded instanceof PersistentObject pob) {
                            AppServices.require().dialogs().showMessage(
                                    "<html><body><b>Successfully reloaded: " + pob.getName()
                                            + "</b><br>From file: " + file + "</body></html>");
                        } else {
                            AppServices.require().dialogs().showMessage(
                                    "Force reload", "Reloaded: " + file.getName());
                        }
                    });
                }
            } catch (Throwable t) {
                klog.error("Failed to open {}", file, t);
                javafx.application.Platform.runLater(() ->
                        AppServices.require().dialogs().showError("Could not open file", t));
            }
        }, "gsea-open-file");
        worker.setDaemon(true);
        worker.start();
    }

    private static void removeGeneSetDuplicates(File file, Runnable afterChange) {
        if (file == null || !file.getName().toLowerCase(Locale.ROOT).endsWith(".grp")) {
            AppServices.require().dialogs().showError(
                    "Only .grp files allowed - cannot perform this action on file: " + file);
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                int before = ParseUtils.countLines(file, true);
                GeneSet gset = (GeneSet) ParserFactory.read(file);
                ParserFactory.save(gset, file);
                javafx.application.Platform.runLater(() -> {
                    afterChange.run();
                    AppServices.require().dialogs().showMessage(
                            "Successfully removed duplicates from the GeneSet. Before: "
                                    + before + " after: " + gset.getNumMembers());
                });
            } catch (Throwable t) {
                klog.error("Remove duplicates failed", t);
                javafx.application.Platform.runLater(() ->
                        AppServices.require().dialogs().showError("Error removing duplicates from GeneSet", t));
            }
        }, "gsea-remove-dups");
        worker.setDaemon(true);
        worker.start();
    }

    private static void extractGeneSets(Object fileOrMatrix, Runnable afterChange) {
        Thread worker = new Thread(() -> {
            try {
                GeneSetMatrix gm;
                if (fileOrMatrix instanceof File file) {
                    Object obj = ParserFactory.read(file);
                    if (!(obj instanceof GeneSetMatrix)) {
                        throw new IllegalArgumentException("Not a gene-set matrix: " + file.getName());
                    }
                    gm = (GeneSetMatrix) obj;
                } else if (fileOrMatrix instanceof GeneSetMatrix matrix) {
                    gm = matrix;
                } else {
                    throw new IllegalArgumentException(
                            "Only GeneSetMatrix or File Objects allowed. Got: " + fileOrMatrix);
                }
                ParserFactory.extractGeneSets(gm);
                final GeneSetMatrix done = gm;
                javafx.application.Platform.runLater(() -> {
                    afterChange.run();
                    AppServices.require().dialogs().showMessage(
                            "Successfully created " + done.getNumGeneSets()
                                    + " GeneSets from the GeneSetMatrix " + done.getName());
                });
            } catch (Throwable t) {
                klog.error("Extract GeneSets failed", t);
                javafx.application.Platform.runLater(() ->
                        AppServices.require().dialogs().showError("Error creating GeneSets from GeneSetMatrix", t));
            }
        }, "gsea-extract-gsets");
        worker.setDaemon(true);
        worker.start();
    }

    private static void convertMatrixToGeneSet(Object fileOrMatrix, Runnable afterChange) {
        Thread worker = new Thread(() -> {
            try {
                GeneSetMatrix gm;
                if (fileOrMatrix instanceof File file) {
                    Object obj = ParserFactory.read(file);
                    if (!(obj instanceof GeneSetMatrix)) {
                        throw new IllegalArgumentException("Not a gene-set matrix: " + file.getName());
                    }
                    gm = (GeneSetMatrix) obj;
                } else if (fileOrMatrix instanceof GeneSetMatrix matrix) {
                    gm = matrix;
                } else {
                    throw new IllegalArgumentException(
                            "Only GeneSetMatrix or File Objects allowed. Got: " + fileOrMatrix);
                }
                GeneSet gset = ParserFactory.combineIntoOne(gm);
                File tmp = new File(AppServices.require().vdb().getTmpDir(), gset.getName(true));
                ParserFactory.save(gset, tmp);
                final GeneSetMatrix done = gm;
                javafx.application.Platform.runLater(() -> {
                    afterChange.run();
                    AppServices.require().dialogs().showMessage(
                            "Successfully created a GeneSet from the GeneSetMatrix " + done.getName()
                                    + " into: " + tmp.getPath());
                });
            } catch (Throwable t) {
                klog.error("Convert to GeneSet failed", t);
                javafx.application.Platform.runLater(() ->
                        AppServices.require().dialogs().showError("Error creating a GeneSet from GeneSetMatrix", t));
            }
        }, "gsea-convert-gset");
        worker.setDaemon(true);
        worker.start();
    }

    private static void revealInOs(File file) {
        try {
            xapps.gsea.fx.FxDesktopUtil.openInOsExplorer(file);
        } catch (Exception e) {
            AppServices.require().dialogs().showError("Trouble launching File Explorer on path '" + file.getPath() + "'", e);
        }
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
