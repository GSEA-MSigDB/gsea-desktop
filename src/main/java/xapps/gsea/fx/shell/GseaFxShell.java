/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.lang3.SystemUtils;
import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.FileManager;
import edu.mit.broad.xbench.core.api.ToolManager;
import edu.mit.broad.xbench.core.api.ToolManagerImpl;
import edu.mit.broad.xbench.core.api.VdbManager;
import edu.mit.broad.xbench.core.api.WindowManager;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.runtime.WorkspaceSession;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.ui.api.PageId;
import org.gsea_msigdb.gsea.ui.api.PageRegistry;
import xapps.gsea.UpdateChecker;
import xapps.gsea.VdbManagerForGsea;
import xapps.gsea.fx.jobs.ApplicationLog;
import xapps.gsea.fx.jobs.FxJobsPane;
import xapps.gsea.fx.jobs.JobRuntime;
import xapps.gsea.fx.tui.FxToolLauncherPane;
import xapps.gsea.fx.viewers.FxAnalysisHistoryPane;
import xapps.gsea.fx.viewers.FxConsoleViewer;
import xapps.gsea.fx.viewers.FxEnrichmentMapPane;
import xapps.gsea.fx.viewers.FxHomePane;
import xapps.gsea.fx.viewers.FxLeadingEdgePane;
import xapps.gsea.fx.viewers.FxLoadDataPane;
import xapps.gsea.fx.viewers.coremap.FxCoreMapPane;
import xapps.gsea.fx.viewers.FxPreferencesPane;
import org.broad.gsea.ui.DesktopIntegration;
import xtools.api.ToolBootstrap;
import xtools.chip2chip.Chip2Chip;
import xtools.gsea.Gsea;
import xtools.gsea.GseaPreranked;
import xtools.gsea.SsGsea;
import xtools.munge.CollapseDataset;

/**
 * Primary JavaFX application shell.
 */
public class GseaFxShell implements Application.Handler {

    private static final Logger klog = LoggerFactory.getLogger(GseaFxShell.class);
    private static final Properties buildProps = JarResources.getBuildInfo();
    private static final String RPT_CACHE_BUILD_DATE = "April4_2006_build";

    private final Stage stage;
    private final FxDockWorkspace dockWorkspace = new FxDockWorkspace();
    private final ToolManager toolManager = new ToolManagerImpl();
    private final VdbManager vdbManager = new VdbManagerForGsea(RPT_CACHE_BUILD_DATE);
    private final FileManager fileManager;
    private final FxWorkspaceWindowManager windowManager;
    private final ApplicationLog applicationLog;
    private final JobRuntime jobRuntime;
    private final WorkspaceSession workspaceSession;
    private final AppServices appServices;
    private final PageRegistry pageRegistry = new PageRegistry();
    private final FeatureHost featureHost;
    private final FxJobsPane jobsPane;
    private final FxConsoleViewer consoleViewer;
    private final ShellWindowPrefs windowPrefs = new ShellWindowPrefs();

    public GseaFxShell(Stage stage) {
        System.setProperty("GSEA", Boolean.TRUE.toString());
        this.stage = stage;
        ToolBootstrap.setUpdateCheckFromToolsEnabled(false);
        this.windowManager = new FxWorkspaceWindowManager(this);
        this.workspaceSession = new WorkspaceSession();
        this.workspaceSession.bindAsCurrent();
        // Register before FileManager: its ctor touches ParserFactory, which needs VdbManager via Application.
        Application.registerHandler(this);
        this.fileManager = new FileManager();
        this.applicationLog = new ApplicationLog();
        this.jobRuntime = new JobRuntime(applicationLog);
        this.appServices = new AppServices(windowManager, fileManager, vdbManager, toolManager,
                jobRuntime, workspaceSession);
        this.appServices.bindAsCurrent();
        this.featureHost = new FeatureHost(appServices, pageRegistry, dockWorkspace::openPage);
        this.jobsPane = new FxJobsPane(featureHost);
        this.consoleViewer = new FxConsoleViewer(applicationLog);
        this.jobRuntime.installCapture();
        registerPages();
    }

    public AppServices getAppServices() {
        return appServices;
    }

    public FeatureHost getFeatureHost() {
        return featureHost;
    }

    private void registerPages() {
        pageRegistry.registerSingleton(PageId.LOAD_DATA,
                () -> new FxLoadDataPane(featureHost));
        pageRegistry.registerSingleton(PageId.TOOLS_GSEA,
                () -> FxToolLauncherPane.forTool(new Gsea(), "Run Gsea", "Gsea_app16_v2.png",
                        featureHost));
        pageRegistry.registerSingleton(PageId.TOOLS_GSEA_PRERANKED,
                () -> FxToolLauncherPane.forTool(new GseaPreranked(),
                        "Run Gsea on a Pre-Ranked gene list", "Gsea_app16_v2.png", featureHost));
        pageRegistry.registerSingleton(PageId.TOOLS_SSGSEA,
                () -> FxToolLauncherPane.forTool(new SsGsea(),
                        "ssGSEA — single-sample GSEA (gene-set scores per sample)", "Gsea_app16_v2.png",
                        featureHost));
        pageRegistry.registerSingleton(PageId.TOOLS_COLLAPSE,
                () -> FxToolLauncherPane.forTool(new CollapseDataset(),
                        "Collapse Dataset from Probes to Symbols", "ProjectSpecific16.png",
                        featureHost));
        pageRegistry.registerSingleton(PageId.TOOLS_CHIP2CHIP,
                () -> FxToolLauncherPane.forTool(new Chip2Chip(), "Chip2Chip", "Chip2Chip16.gif",
                        featureHost));
        pageRegistry.registerSingleton(PageId.LEADING_EDGE,
                () -> new FxLeadingEdgePane(featureHost));
        pageRegistry.registerSingleton(PageId.ENRICHMENT_MAP,
                () -> new FxEnrichmentMapPane(featureHost));
        pageRegistry.registerSingleton(PageId.COREMAP,
                () -> new FxCoreMapPane(featureHost));
        pageRegistry.registerSingleton(PageId.ANALYSIS_HISTORY,
                () -> new FxAnalysisHistoryPane(featureHost));
    }

    public void show() {
        normalizeBuildProps();
        String version = buildProps.getProperty("build.version");
        stage.setTitle("GSEA " + version + " (Gene set enrichment analysis)");
        installStageIcons();

        MenuBar menuBar = buildMenuBar();
        menuBar.getStyleClass().add("gsea-menu-bar");
        if (SystemUtils.IS_OS_MAC) {
            menuBar.setUseSystemMenuBar(true);
        }

        BorderPane root = new BorderPane();
        // Scene root (toast StackPane) owns gsea-root / gsea-dark — keep this pane unclassed.
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setTop(menuBar);
        Node workspace = buildCenter();
        if (workspace instanceof Region region) {
            region.getStyleClass().add("gsea-workspace");
        }
        root.setCenter(workspace);

        javafx.scene.layout.StackPane sceneRoot = new javafx.scene.layout.StackPane(root);
        xapps.gsea.fx.widgets.FxToast.setHost(sceneRoot);
        // gsea-dark must be present before Scene construction so dock Text titles
        // resolve -color-fg-* on the first CSS pass.
        xapps.gsea.fx.FxTheme.prepareRoot(sceneRoot);

        int width = Math.max(1, XPreferencesFactory.kAppWidth.getInt());
        int height = Math.max(1, XPreferencesFactory.kAppHeight.getInt());
        Scene scene = new Scene(sceneRoot, width, height);
        xapps.gsea.fx.FxTheme.apply(scene);
        stage.setScene(scene);
        windowPrefs.restore(stage);
        stage.setOnCloseRequest(e -> {
            e.consume();
            exitApplication();
        });
        windowPrefs.installTracking(stage);
        stage.show();
        bringStageToFront();

        DesktopIntegration.installHandlers(this::showAbout, this::requestQuit);

        dockWorkspace.openPage(new FxHomePane(featureHost, this::openLoadData, this::openGsea, this::openAnalysisHistory));

        klog.info("JavaFX GSEA shell ready");

        Thread updateThread = new Thread(() -> {
            Optional<String> updateMsg = UpdateChecker.oneTimeGseaUpdateCheckMessage();
            updateMsg.ifPresent(msg -> Platform.runLater(() -> windowManager.showMessage(null, msg)));
        }, "gsea-update-check");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void installStageIcons() {
        String[] names = {
                "icon_16x16.png", "icon_16x16@2x.png",
                "icon_32x32.png", "icon_32x32@2x.png",
                "icon_48x48.png",
                "icon_64x64.png",
                "icon_128x128.png", "icon_128x128@2x.png",
                "icon_256x256.png", "icon_256x256@2x.png",
                "icon_512x512.png", "icon_512x512@2x.png"};
        // One icon before show so the window has a taskbar/title identity; rest load off the critical path.
        Image primary = loadStageIcon(names[0]);
        if (primary != null) {
            stage.getIcons().setAll(primary);
        }
        Thread iconThread = new Thread(() -> {
            List<Image> icons = new ArrayList<>();
            for (String name : names) {
                Image img = loadStageIcon(name);
                if (img != null) {
                    icons.add(img);
                }
            }
            if (!icons.isEmpty()) {
                Platform.runLater(() -> stage.getIcons().setAll(icons));
            }
        }, "gsea-stage-icons");
        iconThread.setDaemon(true);
        iconThread.start();
    }

    private Image loadStageIcon(String name) {
        try {
            var url = getClass().getResource("/" + name);
            if (url == null) {
                url = getClass().getResource("/edu/mit/broad/genome/resources/" + name);
            }
            if (url == null) {
                url = JarResources.toURL(name);
            }
            if (url != null) {
                return new Image(url.toExternalForm());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void bringStageToFront() {
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
        // Theme/native chrome may apply on a later pulse; re-raise once so we stay on top.
        Platform.runLater(() -> {
            if (!stage.isShowing()) {
                return;
            }
            stage.setIconified(false);
            stage.toFront();
            stage.requestFocus();
        });
    }

    private MenuBar buildMenuBar() {
        return GseaFxMenus.build(new GseaFxMenus.Host() {
            @Override
            public void openPreferences() {
                FxPreferencesPane.showDialog(stage, appServices);
            }

            @Override
            public void clearRecentFileHistory() {
                GseaFxShell.this.clearRecentFileHistory();
            }

            @Override
            public void exitApplication() {
                GseaFxShell.this.exitApplication();
            }

            @Override
            public void showApplicationMessages() {
                GseaFxShell.this.showApplicationMessages();
            }

            @Override
            public void browse(String url) {
                browseUrl(url);
            }

            @Override
            public void showError(String message, Throwable error) {
                klog.error(message, error);
                windowManager.showError(message, error);
            }

            @Override
            public File runtimeHome() {
                return XPreferencesFactory.kAppRuntimeHomeDir;
            }

            @Override
            public File defaultOutputDir() {
                return appServices.vdb().getDefaultOutputDir();
            }

            @Override
            public String version() {
                return buildProps.getProperty("build.version", "[NO BUILD VERSION FOUND]");
            }

            @Override
            public String buildNumber() {
                return buildProps.getProperty("build.number", "Error loading build.properties!");
            }

            @Override
            public String buildTimestamp() {
                return buildProps.getProperty("build.timestamp", "");
            }
        });
    }

    /** Tool groups: Load Data, Enrichment Methods, Post-Analysis, Additional Tools, Analysis History. */
    private Node buildLeftToolRail() {
        return FxToolsRail.build(List.of(
                new FxToolsRail.ToolSection("loadData", null, List.of(
                        new FxToolsRail.ToolItem("loadData", "Load Data", "Open16.gif",
                                this::openLoadData))),
                new FxToolsRail.ToolSection("enrichment", "Enrichment Methods", List.of(
                        new FxToolsRail.ToolItem("gsea", "GSEA", "GseaApp24.gif", this::openGsea),
                        new FxToolsRail.ToolItem("gseaPreranked", "GSEAPreranked", "GseaApp24.gif",
                                this::openGseaPreranked),
                        new FxToolsRail.ToolItem("ssGsea", "single-sample GSEA", "GseaApp24.gif",
                                this::openSsGsea))),
                new FxToolsRail.ToolSection("postAnalysis", "Post-Analysis", List.of(
                        new FxToolsRail.ToolItem("coreMap", "CoreMap (Network Integration)",
                                "coremap_logo.png", this::openCoreMap),
                        new FxToolsRail.ToolItem("leadingEdge",
                                "Leading Edge Analysis (Classic LEA)", "Lev32.gif",
                                this::openLeadingEdge),
                        new FxToolsRail.ToolItem("enrichmentMap", "Enrichment Map Visualization",
                                "enrichmentmap_logo.gif", this::openEnrichmentMap))),
                new FxToolsRail.ToolSection("additional", "Additional Tools", List.of(
                        new FxToolsRail.ToolItem("collapseDataset", "Collapse Dataset",
                                "ProjectSpecific16.png", this::openCollapseDataset),
                        new FxToolsRail.ToolItem("chip2Chip", "Chip2Chip", "Chip2Chip24_b.gif",
                                this::openChip2Chip))),
                new FxToolsRail.ToolSection("history", null, List.of(
                        new FxToolsRail.ToolItem("analysisHistory", "Analysis History",
                                "past_analysis32.gif", this::openAnalysisHistory)))));
    }

    private void openLoadData() {
        openOrReselect(pageRegistry.get(PageId.LOAD_DATA));
    }

    private void openGsea() {
        openOrReselect(pageRegistry.get(PageId.TOOLS_GSEA));
    }

    private void openGseaPreranked() {
        openOrReselect(pageRegistry.get(PageId.TOOLS_GSEA_PRERANKED));
    }

    private void openSsGsea() {
        openOrReselect(pageRegistry.get(PageId.TOOLS_SSGSEA));
    }

    private void openCollapseDataset() {
        openOrReselect(pageRegistry.get(PageId.TOOLS_COLLAPSE));
    }

    private void openChip2Chip() {
        openOrReselect(pageRegistry.get(PageId.TOOLS_CHIP2CHIP));
    }

    private void openLeadingEdge() {
        openOrReselect(pageRegistry.get(PageId.LEADING_EDGE));
    }

    private void openEnrichmentMap() {
        if (pageRegistry.peekSingleton(PageId.ENRICHMENT_MAP) == null) {
            if (!windowManager.showConfirm("Please confirm this action", FxEnrichmentMapPane.LAUNCH_MSG)) {
                return;
            }
        }
        openOrReselect(pageRegistry.get(PageId.ENRICHMENT_MAP));
    }

    private void openCoreMap() {
        openOrReselect(pageRegistry.get(PageId.COREMAP));
    }

    private void openAnalysisHistory() {
        openOrReselect(pageRegistry.get(PageId.ANALYSIS_HISTORY));
    }

    /**
     * If the page is already docked, reselect it; otherwise open a new document dockable.
     */
    private void openOrReselect(ViewPage page) {
        dockWorkspace.openOrReselect(page);
    }

    private Node buildCenter() {
        return dockWorkspace.buildLayout(
                buildLeftToolRail(),
                jobsPane.getNode(),
                consoleViewer.getContent());
    }

    private void showApplicationMessages() {
        dockWorkspace.showMessages();
    }

    private void clearRecentFileHistory() {
        if (!windowManager.showConfirm("Clear file history",
                "Are you sure you want to erase all file history")) {
            return;
        }
        try {
            appServices.files().getRecentFilesStore().clearAll();
            refreshOpenLoadDataPanes();
        } catch (Exception e) {
            klog.error("Could not clear recent file history", e);
            windowManager.showError("Could not clear recent file history", e);
        }
    }

    private void refreshOpenLoadDataPanes() {
        ViewPage page = pageRegistry.peekSingleton(PageId.LOAD_DATA);
        if (page instanceof FxLoadDataPane loadData) {
            loadData.refreshRecentFiles();
        }
    }

    private void normalizeBuildProps() {
        String ver = buildProps.getProperty("build.version");
        if (ver == null || ver.isBlank()) {
            buildProps.setProperty("build.version", "[NO BUILD VERSION FOUND]");
        }
        String num = buildProps.getProperty("build.number");
        if (num == null || num.isBlank()) {
            buildProps.setProperty("build.number", "Error loading build.properties!");
        }
    }

    private void showAbout() {
        normalizeBuildProps();
        String version = buildProps.getProperty("build.version");
        String buildNo = buildProps.getProperty("build.number");
        String ts = buildProps.getProperty("build.timestamp", "");

        Label title = new Label("Gene Set Enrichment Analysis (GSEA) v" + version);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label copy1 = new Label(
                "Copyright (c) 2003-2025 Broad Institute, Inc., Massachusetts Institute of Technology, ");
        Label copy2 = new Label("and Regents of the University of California.  All rights reserved.");
        String infoText = "GSEA v" + version + " [build: " + buildNo + "]";
        if (!ts.isEmpty()) {
            infoText += ", Built: " + ts;
        }
        Label info = new Label(infoText);
        info.setWrapText(true);

        VBox body = new VBox(8, title, copy1, copy2, info);
        body.setPadding(new Insets(8, 4, 4, 4));

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("About GSEA");
        alert.setHeaderText(null);
        alert.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
        alert.getDialogPane().setContent(body);
        alert.getDialogPane().setGraphic(null);
        alert.initOwner(stage);
        alert.setResizable(true);
        xapps.gsea.fx.FxTheme.apply(alert);
        alert.showAndWait();
    }

    private void browseUrl(String url) {
        try {
            xapps.gsea.fx.FxDesktopUtil.openUrl(url);
            try {
            appServices.files().registerRecentlyOpenedURL(url);
            } catch (Throwable ignored) {
            }
        } catch (Exception e) {
            klog.error("Could not launch browser", e);
            windowManager.showError("Could not launch browser", e);
        }
    }

    /**
     * @return true if quit proceeded (for macOS {@code performQuit}).
     */
    private boolean requestQuit() {
        if (XPreferencesFactory.kAskBeforeAppShutdown.getBoolean()) {
            if (!windowManager.showConfirm("Please confirm this action", "Exit the application?")) {
                return false;
            }
        }
        persistWindowAndDock();
        fileManager.flushRecentWrites();
        jobsPane.dispose();
        jobRuntime.dispose();
        appServices.unbindIfCurrent();
        workspaceSession.unbindIfCurrent();
        Platform.exit();
        if (!Conf.isDebugMode()) {
            Conf.exitSystem(false);
        }
        return true;
    }

    private void exitApplication() {
        requestQuit();
    }

    private void persistWindowAndDock() {
        try {
            windowPrefs.persist(stage);
            dockWorkspace.savePanelSizes();
            XPreferencesFactory.save();
        } catch (Exception e) {
            klog.warn("Could not save window preferences", e);
        }
    }

    @Override
    public ToolManager getToolManager() {
        return toolManager;
    }

    @Override
    public FileManager getFileManager() {
        return fileManager;
    }

    @Override
    public VdbManager getVdbManager() {
        return vdbManager;
    }

    @Override
    public WindowManager getWindowManager() {
        return windowManager;
    }

    public Stage getStage() {
        return stage;
    }

    public JobRuntime getJobRuntime() {
        return jobRuntime;
    }
}
