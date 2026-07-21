/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.SystemUtils;
import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.gsea_msigdb.gsea.ui.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.StandardException;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.FileManager;
import edu.mit.broad.xbench.core.api.ToolManager;
import edu.mit.broad.xbench.core.api.ToolManagerImpl;
import edu.mit.broad.xbench.core.api.VdbManager;
import edu.mit.broad.xbench.core.api.WindowManager;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import xapps.gsea.GseaWebResources;
import xapps.gsea.UpdateChecker;
import xapps.gsea.VdbManagerForGsea;
import xapps.gsea.fx.FxFileIcons;
import xapps.gsea.fx.tui.FxTaskTablePane;
import xapps.gsea.fx.tui.FxToolLauncherPane;
import xapps.gsea.fx.viewers.FxAnalysisHistoryPane;
import xapps.gsea.fx.viewers.FxConsoleViewer;
import xapps.gsea.fx.viewers.FxEnrichmentMapPane;
import xapps.gsea.fx.viewers.FxHomePane;
import xapps.gsea.fx.viewers.FxLeadingEdgePane;
import xapps.gsea.fx.viewers.FxLoadDataPane;
import xapps.gsea.fx.viewers.FxPreferencesPane;
import org.broad.gsea.ui.DesktopIntegration;
import xtools.chip2chip.Chip2Chip;
import xtools.gsea.Gsea;
import xtools.gsea.GseaPreranked;
import xtools.gsea.LeadingEdgeTool;
import xtools.gsea.SsGsea;
import xtools.munge.CollapseDataset;

/**
 * Primary JavaFX application shell.
 */
public class GseaFxShell implements Workspace, Application.Handler {

    private static final Logger klog = LoggerFactory.getLogger(GseaFxShell.class);
    private static final Properties buildProps = JarResources.getBuildInfo();
    private static final String RPT_CACHE_BUILD_DATE = "April4_2006_build";

    private final Stage stage;
    private final TabPane tabPane = new TabPane();
    private final Label statusLabel = new Label("");
    private final ToolManager toolManager = new ToolManagerImpl();
    private final VdbManager vdbManager = new VdbManagerForGsea(RPT_CACHE_BUILD_DATE);
    private final FileManager fileManager;
    private final FxWorkspaceWindowManager windowManager;
    private final FxTaskTablePane taskTablePane;

    private FxLoadDataPane loadDataPage;
    private FxLeadingEdgePane leadingEdgePage;
    private FxEnrichmentMapPane enrichmentMapPage;
    private FxAnalysisHistoryPane analysisHistoryPage;
    private FxToolLauncherPane gseaPage;
    private FxToolLauncherPane gseaPrerankedPage;
    private FxToolLauncherPane ssGseaPage;
    private FxToolLauncherPane collapsePage;
    private FxToolLauncherPane chip2ChipPage;

    public GseaFxShell(Stage stage) {
        System.setProperty("GSEA", Boolean.TRUE.toString());
        this.stage = stage;
        this.windowManager = new FxWorkspaceWindowManager(this);
        // Register before FileManager: its ctor touches ParserFactory, which needs VdbManager via Application.
        Application.registerHandler(this);
        this.fileManager = new FileManager();
        this.taskTablePane = new FxTaskTablePane(this::openPage);
        FxConsoleViewer.installLogHandler();
        FxConsoleViewer.installSystemStreamCapture();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    public void show() {
        normalizeBuildProps();
        String version = buildProps.getProperty("build.version");
        stage.setTitle("GSEA " + version + " (Gene set enrichment analysis)");
        installStageIcons();

        FxConsoleViewer.addStatusListener(() -> {
            String line = FxConsoleViewer.getLastStatusLine();
            boolean warn = FxConsoleViewer.isLastStatusWarning();
            Platform.runLater(() -> {
                statusLabel.setText(line);
                statusLabel.setStyle(warn ? "-fx-text-fill: #FF00FF;" : "");
            });
        });

        MenuBar menuBar = buildMenuBar();
        if (SystemUtils.IS_OS_MAC) {
            menuBar.setUseSystemMenuBar(true);
        }

        BorderPane root = new BorderPane();
        // Scene root (toast StackPane) owns gsea-root / gsea-dark — keep this pane unclassed.
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setTop(menuBar);
        root.setCenter(buildCenter());
        root.setBottom(buildBottom());

        javafx.scene.layout.StackPane sceneRoot = new javafx.scene.layout.StackPane(root);
        xapps.gsea.fx.FxToast.setHost(sceneRoot);

        int width = Math.max(1, XPreferencesFactory.kAppWidth.getInt());
        int height = Math.max(1, XPreferencesFactory.kAppHeight.getInt());
        Scene scene = new Scene(sceneRoot, width, height);
        xapps.gsea.fx.FxTheme.apply(scene);
        stage.setScene(scene);
        restoreWindowBounds();
        stage.setOnCloseRequest(e -> {
            e.consume();
            exitApplication();
        });
        stage.show();

        DesktopIntegration.installHandlers(this::showAbout, this::requestQuit);

        openPage(new FxHomePane(this::openPage, this::openLoadData, this::openGsea, this::openAnalysisHistory));

        klog.info("JavaFX GSEA shell ready");

        Thread updateThread = new Thread(() -> {
            Optional<String> updateMsg = UpdateChecker.oneTimeGseaUpdateCheckMessage();
            updateMsg.ifPresent(msg -> Platform.runLater(() -> showMessage(null, msg)));
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

    private void restoreWindowBounds() {
        try {
            double x = XPreferencesFactory.kAppXPosition.getInt();
            double y = XPreferencesFactory.kAppYPosition.getInt();
            double w = stage.getWidth();
            double h = stage.getHeight();
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            if (w > bounds.getWidth() || h > bounds.getHeight()) {
                w = bounds.getWidth() * 3.0 / 4.0;
                h = bounds.getHeight() * 3.0 / 4.0;
                stage.setWidth(w);
                stage.setHeight(h);
            }
            if (x > bounds.getWidth() || x < 0 || y > bounds.getHeight() || y < 0) {
                stage.setX((bounds.getWidth() - stage.getWidth()) / 2.0);
                stage.setY((bounds.getHeight() - stage.getHeight()) / 2.0);
            } else {
                stage.setX(x);
                stage.setY(y);
            }
            if (XPreferencesFactory.kAppMaximized.getBoolean()) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            klog.debug("Window restore: {}", e.toString());
            stage.centerOnScreen();
        }
    }

    private MenuBar buildMenuBar() {
        MenuBar bar = new MenuBar();

        Menu file = new Menu("File");
        MenuItem prefs = menuItemWithTooltip("Preferences ...",
                "View and modify application wide preferences", "Preferences16.gif");
        prefs.setOnAction(e -> FxPreferencesPane.showDialog(stage));
        MenuItem clearHistory = menuItemWithTooltip("Clear recent file history",
                "Clear recent file history (from the 'Load Data' panel)");
        clearHistory.setOnAction(e -> clearRecentFileHistory());
        file.getItems().addAll(prefs, new SeparatorMenuItem(), clearHistory);
        if (!SystemUtils.IS_OS_MAC) {
            MenuItem exit = menuItemWithTooltip("Exit", "Quit the GSEA application");
            exit.setOnAction(e -> exitApplication());
            file.getItems().addAll(new SeparatorMenuItem(), exit);
        }

        Menu downloads = new Menu("Downloads");
        downloads.getItems().addAll(
                browseMenuItem("Download Human chip annotations",
                        "Download Human array annotation files - useful to annotate GSEA reports",
                        GseaWebResources.getHumanArrayAnnotationsURL(), false),
                browseMenuItem("Download Mouse chip annotations",
                        "Download Mouse array annotation files - useful to annotate GSEA reports",
                        GseaWebResources.getMouseArrayAnnotationsURL(), false),
                browseMenuItem("Download example datasets",
                        "Download example datasets for GSEA - expression data, phenotype labels and gene sets files",
                        GseaWebResources.getGseaExamplesURL(), false));

        Menu help = new Menu("Help");
        String version = buildProps.getProperty("build.version", "[NO BUILD VERSION FOUND]");
        String buildNo = buildProps.getProperty("build.number", "Error loading build.properties!");
        String ts = buildProps.getProperty("build.timestamp", "");
        MenuItem buildInfo = new MenuItem("GSEA v" + version + " [build: " + buildNo + "]");
        MenuItem showHome = menuItemWithTooltip("Show GSEA home folder",
                "Show runtime home directory of this application");
        showHome.setOnAction(e -> {
            File dir = XPreferencesFactory.kAppRuntimeHomeDir;
            if (dir == null) {
                return;
            }
            try {
                // Open in explorer only — do not register folders as "recent files".
                xapps.gsea.fx.FxDesktopUtil.openInOsExplorer(dir);
            } catch (Exception ex) {
                showError("Trouble launching File Explorer on path '" + dir.getPath() + "'", ex);
            }
        });
        MenuItem showOut = menuItemWithTooltip("Show GSEA output folder (default location)",
                "Show output directory of this application");
        showOut.setOnAction(e -> {
            File dir = Application.getVdbManager().getDefaultOutputDir();
            if (dir == null) {
                return;
            }
            try {
                // Open in explorer only — do not register folders as "recent files".
                xapps.gsea.fx.FxDesktopUtil.openInOsExplorer(dir);
            } catch (Exception ex) {
                showError("Trouble launching File Explorer on path '" + dir.getPath() + "'", ex);
            }
        });
        help.getItems().addAll(
                browseMenuItem("GSEA web site", "Open the GSEA website in a web browser",
                        GseaWebResources.getGseaBaseURL(), true),
                browseMenuItem("GSEA documentation",
                        "Online documentation of the GSEA algorithm and software",
                        GseaWebResources.getGseaHelpURL(), true),
                browseMenuItem("GSEA & MSigDB License Terms", "GSEA & MSigDB License Terms",
                        GseaWebResources.getGseaBaseURL() + "/license_terms_list.jsp", true),
                new SeparatorMenuItem(),
                showHome,
                showOut,
                new SeparatorMenuItem(),
                browseMenuItem("Contact Us", "Contact Us",
                        GseaWebResources.getGseaContactURL(), false),
                new SeparatorMenuItem(),
                buildInfo);
        if (ts == null || ts.isBlank()) {
            help.getItems().add(new SeparatorMenuItem());
        } else {
            help.getItems().add(new MenuItem("Built: " + ts));
        }

        bar.getMenus().addAll(file, downloads, help);
        return bar;
    }

    private MenuItem menuItemWithTooltip(String name, String tooltip) {
        return menuItemWithTooltip(name, tooltip, null);
    }

    private MenuItem menuItemWithTooltip(String name, String tooltip, String iconResource) {
        Label label = new Label(name);
        if (iconResource != null && !iconResource.isBlank()) {
            label.setGraphic(FxFileIcons.forResource(iconResource));
        }
        if (tooltip != null && !tooltip.isBlank()) {
            label.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        }
        javafx.scene.control.CustomMenuItem item = new javafx.scene.control.CustomMenuItem(label);
        item.setHideOnClick(true);
        return item;
    }

    private MenuItem browseMenuItem(String name, String tooltip, String url, boolean helpIcon) {
        Label label = new Label(name);
        if (helpIcon) {
            label.setGraphic(FxFileIcons.forResource("Help16_v2.gif"));
        }
        if (tooltip != null && !tooltip.isBlank()) {
            label.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        }
        javafx.scene.control.CustomMenuItem item = new javafx.scene.control.CustomMenuItem(label);
        item.setHideOnClick(true);
        item.setOnAction(e -> browseUrl(url));
        return item;
    }

    /** Vertical tool groups: Steps in GSEA analysis, Tools, Analysis history. */
    private Node buildLeftToolRail() {
        VBox steps = titledToolGroup("Steps in GSEA analysis",
                toolButton("Load data", "LocalFileExplorerWidget32.gif",
                        this::openLoadData),
                toolButton("Run GSEA", "GseaApp24.gif",
                        this::openGsea),
                toolButton("Leading edge analysis", "Lev32.gif",
                        this::openLeadingEdge),
                toolButton("Enrichment Map Visualization", "enrichmentmap_logo.gif",
                        this::openEnrichmentMap)
        );
        VBox tools = titledToolGroup("Tools",
                toolButton("Run GSEAPreranked", "GseaApp24.gif",
                        this::openGseaPreranked),
                toolButton("Run ssGSEA", "GseaApp24.gif",
                        this::openSsGsea),
                toolButton("Collapse Dataset", "ProjectSpecific16.png",
                        this::openCollapseDataset),
                toolButton("Chip2Chip mapping", "Chip2Chip24_b.gif",
                        this::openChip2Chip)
        );
        VBox history = new VBox(8,
                toolButton("Analysis history", "past_analysis32.gif",
                        this::openAnalysisHistory));
        history.getStyleClass().add("gsea-tool-group");
        history.setPadding(new Insets(8, 6, 8, 6));

        VBox rail = new VBox(10, steps, tools, history);
        rail.setPadding(new Insets(6));
        // Wide enough for multi-word rail labels + 32px icons without ellipsis.
        rail.setPrefWidth(200);
        rail.setMinWidth(180);
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(rail);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private void openLoadData() {
        openOrReselect(ensureLoadDataPage());
    }

    private void openGsea() {
        openOrReselect(ensureGseaPage());
    }

    private void openGseaPreranked() {
        if (gseaPrerankedPage == null) {
            gseaPrerankedPage = FxToolLauncherPane.forTool(new GseaPreranked(),
                    "Run Gsea on a Pre-Ranked gene list", "Gsea_app16_v2.png");
        }
        openOrReselect(gseaPrerankedPage);
    }

    private void openSsGsea() {
        if (ssGseaPage == null) {
            ssGseaPage = FxToolLauncherPane.forTool(new SsGsea(),
                    "ssGSEA — single-sample GSEA (gene-set scores per sample)", "Gsea_app16_v2.png");
        }
        openOrReselect(ssGseaPage);
    }

    private void openCollapseDataset() {
        if (collapsePage == null) {
            collapsePage = FxToolLauncherPane.forTool(new CollapseDataset(),
                    "Collapse Dataset from Probes to Symbols", "ProjectSpecific16.png");
        }
        openOrReselect(collapsePage);
    }

    private void openChip2Chip() {
        if (chip2ChipPage == null) {
            chip2ChipPage = FxToolLauncherPane.forTool(new Chip2Chip(), "Chip2Chip", "Chip2Chip16.gif");
        }
        openOrReselect(chip2ChipPage);
    }

    private void openLeadingEdge() {
        if (leadingEdgePage == null) {
            leadingEdgePage = new FxLeadingEdgePane();
        }
        openOrReselect(leadingEdgePage);
    }

    private void openEnrichmentMap() {
        if (enrichmentMapPage == null) {
            if (!showConfirm("Please confirm this action", FxEnrichmentMapPane.LAUNCH_MSG)) {
                return;
            }
            enrichmentMapPage = new FxEnrichmentMapPane();
        }
        openOrReselect(enrichmentMapPage);
    }

    private void openAnalysisHistory() {
        if (analysisHistoryPage == null) {
            analysisHistoryPage = new FxAnalysisHistoryPane(this::openPage);
        }
        openOrReselect(analysisHistoryPage);
    }

    private FxLoadDataPane ensureLoadDataPage() {
        if (loadDataPage == null) {
            loadDataPage = new FxLoadDataPane(this::openPage);
        }
        return loadDataPage;
    }

    private FxToolLauncherPane ensureGseaPage() {
        if (gseaPage == null) {
            gseaPage = FxToolLauncherPane.forTool(new Gsea(), "Run Gsea", "Gsea_app16_v2.png");
        }
        return gseaPage;
    }

    /**
     * Swing {@code WindowManagerImplJideTabbedPane.openWindow}: if the widget is already
     * in a tab, reselect it; otherwise add a new tab.
     */
    private void openOrReselect(ViewPage page) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() == page) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        openPage(page);
    }

    private static VBox titledToolGroup(String title, Button... buttons) {
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 4 0;");
        VBox box = new VBox(8);
        box.getChildren().add(header);
        box.getChildren().addAll(buttons);
        box.setPadding(new Insets(8, 6, 8, 6));
        box.getStyleClass().add("gsea-tool-group");
        return box;
    }

    private Button toolButton(String text, String iconResource, Runnable action) {
        Button b = new Button(text);
        xapps.gsea.fx.FxButtons.styleRail(b);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMinHeight(55);
        b.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        b.setWrapText(true);
        b.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        b.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        b.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
        if (iconResource != null) {
            try {
                var url = JarResources.toURL(iconResource);
                if (url != null) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                            new Image(url.toExternalForm(), 32, 32, true, true));
                    b.setGraphic(iv);
                }
            } catch (Exception ignored) {
            }
        }
        b.setOnAction(e -> action.run());
        return b;
    }

    private SplitPane buildCenter() {
        SplitPane left = new SplitPane();
        left.setOrientation(Orientation.VERTICAL);
        left.getItems().addAll(buildLeftToolRail(), taskTablePane.getNode());
        left.setDividerPositions(0.55);

        SplitPane horizontal = new SplitPane();
        horizontal.getItems().addAll(left, tabPane);
        horizontal.setDividerPositions(0.22);
        return horizontal;
    }

    private BorderPane buildBottom() {
        BorderPane bottom = new BorderPane();
        bottom.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.getStyleClass().add("gsea-status");
        statusLabel.setWrapText(false);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setCursor(javafx.scene.Cursor.HAND);
        statusLabel.setTooltip(new javafx.scene.control.Tooltip(
                "Click for application messages (such as # of permutations complete)"));
        statusLabel.setOnMouseClicked(e -> toggleConsolePopup());
        try {
            var url = JarResources.toURL("expandall.png");
            if (url != null) {
                statusLabel.setGraphic(new javafx.scene.image.ImageView(
                        new Image(url.toExternalForm(), 16, 16, true, true)));
            }
        } catch (Exception ignored) {
        }
        bottom.setLeft(statusLabel);
        BorderPane.setAlignment(statusLabel, javafx.geometry.Pos.CENTER_LEFT);
        return bottom;
    }

    private javafx.stage.Stage consolePopup;

    private void toggleConsolePopup() {
        if (consolePopup != null && consolePopup.isShowing()) {
            consolePopup.hide();
            return;
        }
        if (consolePopup == null) {
            consolePopup = new javafx.stage.Stage();
            consolePopup.initOwner(stage);
            consolePopup.setTitle("Application messages");
            FxConsoleViewer viewer = new FxConsoleViewer();
            javafx.scene.Scene consoleScene = new javafx.scene.Scene(
                    (javafx.scene.Parent) viewer.getContent(), 700, 350);
            xapps.gsea.fx.FxTheme.apply(consoleScene);
            consolePopup.setScene(consoleScene);
        }
        consolePopup.show();
        consolePopup.toFront();
    }

    private void clearRecentFileHistory() {
        if (!showConfirm("Clear file history",
                "Are you sure you want to erase all file history")) {
            return;
        }
        try {
            Application.getFileManager().getRecentFilesStore().clearAll();
            refreshOpenLoadDataPanes();
        } catch (Exception e) {
            showError("Could not clear recent file history", e);
        }
    }

    private void refreshOpenLoadDataPanes() {
        if (loadDataPage != null) {
            loadDataPage.refreshRecentFiles();
        }
    }

    @Override
    public void openPage(ViewPage page) {
        Node content = (Node) page.getContent();
        Tab tab = new Tab(page.getTitle(), content);
        tab.setClosable(!(page instanceof FxHomePane));
        tab.setUserData(page);
        String iconId = page.getIconResourceId();
        if (iconId != null && !iconId.isBlank()) {
            try {
                var url = JarResources.toURL(iconId);
                if (url != null) {
                    tab.setGraphic(new javafx.scene.image.ImageView(
                            new Image(url.toExternalForm(), 16, 16, true, true)));
                }
            } catch (Exception ignored) {
            }
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @Override
    public void showError(String message) {
        runOnFxThreadBlocking(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setHeaderText("Error");
            alert.initOwner(stage);
            xapps.gsea.fx.FxTheme.apply(alert);
            alert.showAndWait();
            return null;
        });
    }

    /** Error dialog with an explicit title (used e.g. for a named {@link edu.mit.broad.genome.Errors} bag). */
    public void showError(String title, String message) {
        runOnFxThreadBlocking(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setHeaderText(title);
            alert.initOwner(stage);
            alert.setResizable(true);
            xapps.gsea.fx.FxTheme.apply(alert);
            alert.showAndWait();
            return null;
        });
    }

    @Override
    public void showError(String message, Throwable t) {
        klog.error(message, t);
        runOnFxThreadBlocking(() -> {
            showErrorDialog(message, t);
            return null;
        });
    }

    /**
     * Error dialog with an expandable "Details" stack trace, a Copy button, and (for
     * {@link StandardException}s) a Help button linking to the matching GSEA User Guide anchor.
     */
    private void showErrorDialog(String header, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        String contentText = (t != null && t.getMessage() != null) ? t.getMessage() : header;
        alert.setContentText(contentText);
        alert.setResizable(true);
        xapps.gsea.fx.FxTheme.apply(alert);

        String stackTrace = stackTraceOf(t);
        if (t != null) {
            TextArea details = new TextArea(stackTrace);
            details.setEditable(false);
            details.setWrapText(false);
            details.setMaxWidth(Double.MAX_VALUE);
            details.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(details, Priority.ALWAYS);
            GridPane.setHgrow(details, Priority.ALWAYS);

            GridPane expandable = new GridPane();
            expandable.setMaxWidth(Double.MAX_VALUE);
            expandable.add(new Label("Details:"), 0, 0);
            expandable.add(details, 0, 1);
            alert.getDialogPane().setExpandableContent(expandable);
            alert.getDialogPane().setExpanded(false);
        }

        ButtonType copyType = new ButtonType("Copy Details", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(copyType);
        Button copyButton = (Button) alert.getDialogPane().lookupButton(copyType);
        copyButton.addEventFilter(ActionEvent.ACTION, e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(header + (stackTrace.isEmpty() ? "" : "\n\n" + stackTrace));
            Clipboard.getSystemClipboard().setContent(content);
            e.consume();
        });

        if (t instanceof StandardException) {
            int code = ((StandardException) t).getErrorCode();
            String helpUrl = GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/#error-" + code;
            ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
            alert.getButtonTypes().add(helpType);
            Button helpButton = (Button) alert.getDialogPane().lookupButton(helpType);
            helpButton.addEventFilter(ActionEvent.ACTION, e -> {
                browseUrl(helpUrl);
                e.consume();
            });
        }

        alert.showAndWait();
    }

    private static String stackTraceOf(Throwable t) {
        if (t == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Override
    public void showMessage(String title, String message) {
        windowManager.showMessage(title, message);
    }

    /** Strips a leading {@code <html>...</html>} wrapper down to plain text for a simple Label/Alert body. */
    private static String contentFor(String message) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        if (!trimmed.regionMatches(true, 0, "<html>", 0, Math.min(6, trimmed.length()))) {
            return message;
        }
        String stripped = trimmed.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        return stripped.trim();
    }

    @Override
    public boolean showConfirm(String title, String message) {
        Boolean result = runOnFxThreadBlocking(() -> showConfirmOnFxThread(title, message));
        return Boolean.TRUE.equals(result);
    }

    private boolean showConfirmOnFxThread(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, contentFor(message), ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(title);
        alert.initOwner(stage);
        alert.setResizable(true);
        xapps.gsea.fx.FxTheme.apply(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private <T> T runOnFxThreadBlocking(java.util.concurrent.Callable<T> callable) {
        if (Platform.isFxApplicationThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                klog.warn("FX dialog failed", e);
                return null;
            }
        }
        final java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                klog.warn("FX dialog failed", e);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return result.get();
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
                Application.getFileManager().registerRecentlyOpenedURL(url);
            } catch (Throwable ignored) {
            }
        } catch (Exception e) {
            showError("Could not launch browser", e);
        }
    }

    /**
     * @return true if quit proceeded (for macOS {@code performQuit}).
     */
    private boolean requestQuit() {
        if (XPreferencesFactory.kAskBeforeAppShutdown.getBoolean()) {
            if (!showConfirm("Please confirm this action", "Exit the application?")) {
                return false;
            }
        }
        saveWindowBounds();
        Platform.exit();
        if (!Conf.isDebugMode()) {
            Conf.exitSystem(false);
        }
        return true;
    }

    private void exitApplication() {
        requestQuit();
    }

    private void saveWindowBounds() {
        try {
            XPreferencesFactory.kAppWidth.setValue((int) stage.getWidth());
            XPreferencesFactory.kAppHeight.setValue((int) stage.getHeight());
            XPreferencesFactory.kAppXPosition.setValue((int) stage.getX());
            XPreferencesFactory.kAppYPosition.setValue((int) stage.getY());
            XPreferencesFactory.kAppMaximized.setValue(stage.isMaximized());
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

    public FxTaskTablePane getTaskTablePane() {
        return taskTablePane;
    }
}
