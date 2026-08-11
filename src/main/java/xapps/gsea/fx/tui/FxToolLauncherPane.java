/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.tui;

import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;
import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.xbench.tui.ReportStub;
import edu.mit.broad.xbench.tui.ToolFactory;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.jobs.JobRuntime;
import xapps.gsea.fx.params.FxParamSetForm;
import xapps.gsea.fx.params.ParamDependency;
import xtools.api.Tool;
import xtools.api.param.Param;
import xtools.api.param.ParamSet;
import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.ui.api.FeatureHost;

/**
 * JavaFX tool launcher: param form + Run / Help / Last / Reset / Command line.
 */
public class FxToolLauncherPane implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxToolLauncherPane.class);

    private final Tool tool;
    private final String title;
    private final String iconResourceId;
    private final boolean showInitializedBanner;
    private final JobRuntime jobRuntime;
    private final AppServices svc;
    private final BorderPane root = new BorderPane();
    private final FxParamSetForm form;
    private final Button run = new Button("Run");
    private final Button cmd = new Button("Command");
    private final Button last = new Button("Last");

    private FxToolLauncherPane(Tool tool, String title, String iconResourceId,
            boolean showInitializedBanner, AppServices svc, JobRuntime jobRuntime) {
        this.tool = tool;
        this.title = title != null ? title : tool.getTitle();
        this.iconResourceId = iconResourceId != null && !iconResourceId.isBlank()
                ? iconResourceId : "Gsea_app16_v2.png";
        this.showInitializedBanner = showInitializedBanner;
        this.svc = Objects.requireNonNull(svc, "svc");
        this.jobRuntime = Objects.requireNonNull(jobRuntime, "jobRuntime");
        this.form = new FxParamSetForm(tool.getParamSet());
        // Preserve historical tool rule: altDelim must be applied before gene-set parse.
        form.addDependency(ParamDependency.whenSpecified("altDelim→gene_sets", "altDelim", source -> {
            Param geneSets = ParamDependency.find(tool.getParamSet(), Param.GMX);
            if (geneSets instanceof xtools.api.param.GeneSetMatrixMultiChooserParam gmx) {
                Object v = source.getValue();
                if (v != null && !v.toString().isBlank()) {
                    gmx.setAlternateDelimiter(v.toString());
                }
            }
        }));

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 10, 12));
        header.getStyleClass().add("gsea-tool-launcher-header");
        try {
            var url = edu.mit.broad.genome.JarResources.toURL(this.iconResourceId);
            if (url == null) {
                url = edu.mit.broad.genome.JarResources.toURL("Tool16.gif");
            }
            if (url != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(url.toExternalForm(), 20, 20, true, true));
                header.getChildren().add(iv);
            }
        } catch (Exception ignored) {
        }
        Label titleLabel = new Label();
        titleLabel.setText(stripSimpleHtml(tool.getTitle()));
        titleLabel.getStyleClass().add("gsea-tool-launcher-title");
        header.getChildren().add(titleLabel);

        run.setDefaultButton(true);
        run.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Run16.png"));
        run.setOnAction(e -> runTool());

        // Help icon left; Reset / Last / Command / Run clustered beside it.
        Button help = new Button();
        help.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Help16_v2.gif"));
        help.setTooltip(new javafx.scene.control.Tooltip("Online HELP!! for this tool"));
        xapps.gsea.fx.widgets.FxButtons.styleIcon(help);
        help.setOnAction(e -> {
            try {
                xapps.gsea.fx.FxDesktopUtil.openUrl(tool.getHelpURL());
            } catch (Exception ex) {
                svc.dialogs().showError("Could not open help URL", ex);
            }
        });

        last.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("History16_v2.gif"));
        last.setTooltip(new javafx.scene.control.Tooltip("Set to the previous run"));
        last.setOnAction(e -> loadLastRunParams());

        Button reset = new Button("Reset");
        reset.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Reset16.gif"));
        reset.setTooltip(new javafx.scene.control.Tooltip("Reset to default parameters"));
        reset.setOnAction(e -> resetDefaults());

        cmd.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("CommandLine16_v2.gif"));
        cmd.setTooltip(new javafx.scene.control.Tooltip(
                "Commandline representation (for running from a unix terminal, dos window etc)"));
        cmd.setOnAction(e -> showCommandLine());

        run.setTooltip(new javafx.scene.control.Tooltip("Execute the tool with specified parameters"));
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(reset);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(last);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(cmd);
        xapps.gsea.fx.widgets.FxButtons.stylePrimary(run);
        xapps.gsea.fx.widgets.FxButtons.sizeToContent(reset, last, cmd, run);

        HBox rhs = xapps.gsea.fx.widgets.FxButtons.row(reset, last, cmd, run);
        HBox actions = xapps.gsea.fx.widgets.FxButtons.actionBar(help, rhs);

        VBox top = new VBox(8);
        if (showInitializedBanner) {
            Label initPrefix = new Label("Initialized to: ");
            Label initName = new Label(tool.getName());
            initName.setStyle("-fx-font-weight: bold;");
            HBox init = new HBox(initPrefix, initName);
            init.setPadding(new Insets(6, 12, 0, 12));
            top.getChildren().add(init);
        }
        top.getChildren().add(header);
        top.setPadding(new Insets(0, 0, 0, 0));

        root.setTop(top);
        root.setCenter(form.getScrollPane());
        root.setBottom(actions);
        VBox.setVgrow(form.getScrollPane(), Priority.ALWAYS);

        // Run/Command stay disabled until required params are filled; Help/Last/Reset always work.
        form.addChangeListener(this::refreshRunEnabled);
        refreshRunEnabled();
    }

    private void refreshRunEnabled() {
        boolean ready;
        try {
            form.commitAll();
            ready = tool.getParamSet().isRequiredAllSet();
        } catch (RuntimeException ex) {
            ready = false;
        }
        run.setDisable(!ready);
        cmd.setDisable(!ready);
    }

    public static FxToolLauncherPane forTool(Tool tool, String title, String iconResourceId,
            FeatureHost host) {
        return forTool(tool, title, iconResourceId, false, host);
    }

    public static FxToolLauncherPane forTool(Tool tool, String title, String iconResourceId,
            boolean showInitializedBanner, FeatureHost host) {
        Objects.requireNonNull(host, "host");
        return new FxToolLauncherPane(tool, title, iconResourceId, showInitializedBanner,
                host.services(), host.jobs());
    }

    private void runTool() {
        try {
            form.commitAll();
            form.applyDependencies();
            ParamSet pset = form.getParamSet();
            if (!pset.isRequiredAllSet()) {
                svc.dialogs().showError("Please fill all required parameters before running.");
                return;
            }
            String runId = jobRuntime.start(tool, pset, Thread.NORM_PRIORITY);
            jobRuntime.showParamErrorIfNeeded(runId);
        } catch (Exception ex) {
            klog.error("Failed to run tool {}", tool.getName(), ex);
            svc.dialogs().showError("Failed to run " + tool.getName(), ex);
        }
    }

    /**
     * confirm, then {@link FxLoadToolTask} on a throwaway tool clone with {@code launchANewToolWindow=false} (cache side-effects only — live form unchanged).
     */
    private void loadLastRunParams() {
        last.setDisable(true);
        Thread worker = new Thread(() -> {
            try {
                ReportStub rs = svc.tools().getLastReportStub(tool.getClass().getName());
                if (rs == null) {
                    javafx.application.Platform.runLater(() -> {
                        last.setDisable(false);
                        svc.dialogs().showMessage("Last run",
                                "No history available for: " + tool.getName());
                    });
                    return;
                }
                javafx.application.Platform.runLater(() -> {
                    boolean proceed = svc.dialogs().showConfirm(
                            "Load data files from the last analysis: " + rs.getName());
                    if (!proceed) {
                        last.setDisable(false);
                        return;
                    }
                    Thread parseWorker = new Thread(() -> {
                        try {
                            Properties props = rs.getReport(true).getParametersUsed();
                            Tool fillTool = ToolFactory.createTool(tool.getClass().getName());
                            FxLoadToolTask.create(fillTool, rs.getName(), true, props, false).run();
                            javafx.application.Platform.runLater(() -> {
                                refreshRunEnabled();
                                last.setDisable(false);
                                svc.dialogs().showMessage(
                                        "Data from the last run of this tool was automagically loaded in. "
                                                + "They are now available as parameter options");
                            });
                        } catch (Exception ex) {
                            klog.error("Failed to load last run params", ex);
                            javafx.application.Platform.runLater(() -> {
                                last.setDisable(false);
                                svc.dialogs().showError(
                                        "Could not load last run parameters", ex);
                            });
                        }
                    }, "gsea-last-run-parse");
                    parseWorker.setDaemon(true);
                    parseWorker.start();
                });
            } catch (Exception ex) {
                klog.error("Failed to load last run params", ex);
                javafx.application.Platform.runLater(() -> {
                    last.setDisable(false);
                    svc.dialogs().showError("Could not load last run parameters", ex);
                });
            }
        }, "gsea-last-run");
        worker.setDaemon(true);
        worker.start();
    }

    private void resetDefaults() {
        if (!svc.dialogs().showConfirm("Confirm reset",
                "Confirm reset parameters to defaults")) {
            return;
        }
        for (int i = 0; i < tool.getParamSet().getNumParams(); i++) {
            Param p = tool.getParamSet().getParam(i);
            Object def = p.getDefault();
            if (def instanceof Object[] arr) {
                StringBuilder buf = new StringBuilder();
                for (int j = 0; j < arr.length; j++) {
                    buf.append(arr[j].toString());
                    if (j != arr.length - 1) {
                        buf.append(',');
                    }
                }
                p.setValue(buf.toString());
            } else {
                p.setValue(def);
            }
        }
        form.syncEditorsFromParams();
        refreshRunEnabled();
    }

    /** Show the launcher script command (tool name + args) with a Copy action. */
    private void showCommandLine() {
        form.commitAll();
        String launcherCmd = SystemUtils.IS_OS_WINDOWS ? "gsea-cli.bat" : "gsea-cli.sh";
        String cmdLine = launcherCmd + " " + tool.getName() + " " + tool.getParamSet().getAsCommand(true).trim();

        TextArea area = new TextArea(cmdLine);
        area.setEditable(true);
        area.setWrapText(true);
        area.setPrefRowCount(8);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Command Line for: " + tool.getName());
        alert.setHeaderText(null);
        alert.getButtonTypes().setAll(ButtonType.APPLY, ButtonType.CANCEL);
        Button copyButton = (Button) alert.getDialogPane().lookupButton(ButtonType.APPLY);
        copyButton.setText("Copy");
        copyButton.setGraphic(xapps.gsea.fx.widgets.FxFileIcons.forResource("Copy16.gif"));
        copyButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(area.getText());
            Clipboard.getSystemClipboard().setContent(content);
            e.consume();
        });
        alert.getDialogPane().setContent(area);
        alert.setResizable(true);
        xapps.gsea.fx.FxTheme.apply(alert);
        alert.showAndWait();
    }

    /** Strip simple HTML tags from {@link Tool#getTitle()} for a plain JavaFX Label. */
    private static String stripSimpleHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("(?i)<br\\s*/?>", " — ")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getIconResourceId() {
        return iconResourceId;
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
