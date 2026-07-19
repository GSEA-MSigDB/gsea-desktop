/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.File;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import xapps.gsea.GseaWebResources;
import xapps.gsea.fx.params.FxFileChooserUtil;

/**
 * JavaFX preferences form backed by {@link XPreferencesFactory}.
 * Shown as a modal OK/Cancel dialog (Swing {@code GseaPreferencesDialog} parity).
 */
public class FxPreferencesPane implements ViewPage {

    private final BorderPane root = new BorderPane();
    // Swing Application/Algorithm panels: JLabel + bare checkbox (not checkbox-with-name).
    private final CheckBox askBeforeShutdown = new CheckBox();
    private final CheckBox onlineMode = new CheckBox();
    private final CheckBox updateCheck = new CheckBox();
    private final ComboBox<String> appearance = new ComboBox<>();
    private final TextField outputDirField = new TextField();
    private final TextField cytoscapePortField = new TextField();
    private final CheckBox median = new CheckBox();
    private final CheckBox fixLowVar = new CheckBox();
    private final CheckBox biasedVar = new CheckBox();

    public FxPreferencesPane() {
        loadFromPrefs();

        // Swing GseaPreferencesDialog.OptionsPanel order:
        // Report settings → Program settings → Application preferences → Algorithm defaults.
        GridPane reportForm = new GridPane();
        reportForm.setHgap(12);
        reportForm.setVgap(10);
        reportForm.setPadding(new Insets(8));
        Label outLabel = new Label("Default output folder");
        // Swing GDirFieldPlusChooser: icon-only Ellipsis button (no "Browse…" text).
        Button browse = xapps.gsea.fx.FxEllipsisButton.create("Browse for folder");
        browse.setOnAction(e -> chooseOutputDir());
        HBox outRow = new HBox(8, outputDirField, browse);
        HBox.setHgrow(outputDirField, Priority.ALWAYS);
        reportForm.add(outLabel, 0, 0);
        reportForm.add(outRow, 1, 0);
        TitledPane reportPane = new TitledPane(" Report settings ", reportForm);
        reportPane.setCollapsible(false);

        GridPane programForm = new GridPane();
        programForm.setHgap(12);
        programForm.setVgap(10);
        programForm.setPadding(new Insets(8));
        Label portLabel = new Label("Cytoscape REST port (for Enrichment Map Visualization)");
        programForm.add(portLabel, 0, 0, 2, 1);
        programForm.add(cytoscapePortField, 0, 1, 2, 1);
        TitledPane programPane = new TitledPane(" Program settings ", programForm);
        programPane.setCollapsible(false);

        GridPane appPrefs = new GridPane();
        appPrefs.setHgap(12);
        appPrefs.setVgap(10);
        appPrefs.setPadding(new Insets(8));
        appPrefs.add(new Label("Prompt before closing application"), 0, 0);
        appPrefs.add(askBeforeShutdown, 1, 0);
        appPrefs.add(new Label("Connect over the Internet"), 0, 1);
        appPrefs.add(onlineMode, 1, 1);
        appPrefs.add(new Label("Check for new GSEA version on startup"), 0, 2);
        appPrefs.add(updateCheck, 1, 2);
        appearance.getItems().setAll(
                xapps.gsea.fx.FxTheme.APPEARANCE_SYSTEM,
                xapps.gsea.fx.FxTheme.APPEARANCE_LIGHT,
                xapps.gsea.fx.FxTheme.APPEARANCE_DARK);
        appearance.setMaxWidth(Double.MAX_VALUE);
        appPrefs.add(new Label("Appearance"), 0, 3);
        appPrefs.add(appearance, 1, 3);
        TitledPane appPane = new TitledPane(" Application preferences ", appPrefs);
        appPane.setCollapsible(false);

        GridPane algoPrefs = new GridPane();
        algoPrefs.setHgap(12);
        algoPrefs.setVgap(10);
        algoPrefs.setPadding(new Insets(8));
        algoPrefs.add(new Label(XPreferencesFactory.kMedian.getName()), 0, 0);
        algoPrefs.add(median, 1, 0);
        algoPrefs.add(new Label(XPreferencesFactory.kFixLowVar.getName()), 0, 1);
        algoPrefs.add(fixLowVar, 1, 1);
        algoPrefs.add(new Label(XPreferencesFactory.kBiasedVar.getName()), 0, 2);
        algoPrefs.add(biasedVar, 1, 2);
        TitledPane algoPane = new TitledPane("Algorithm: These are 'Defaults' ", algoPrefs);
        algoPane.setCollapsible(false);

        VBox center = new VBox(10, reportPane, programPane, appPane, algoPane);
        center.setPadding(new Insets(12));
        root.setCenter(center);
    }

    /** Modal preferences dialog with Help / OK / Cancel (Swing parity). @return true if OK saved. */
    public static boolean showDialog(javafx.stage.Window owner) {
        FxPreferencesPane pane = new FxPreferencesPane();
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Preferences");
        dialog.setResizable(true);
        dialog.getDialogPane().setContent((javafx.scene.Node) pane.getContent());
        xapps.gsea.fx.FxTheme.apply(dialog);
        // Swing createButtonPanel: Help west; Cancel then OK east.
        javafx.scene.control.ButtonType helpType =
                new javafx.scene.control.ButtonType("Help", javafx.scene.control.ButtonBar.ButtonData.LEFT);
        javafx.scene.control.ButtonType cancelType = javafx.scene.control.ButtonType.CANCEL;
        javafx.scene.control.ButtonType okType = javafx.scene.control.ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().setAll(helpType, cancelType, okType);
        javafx.scene.control.ButtonBar buttonBar =
                (javafx.scene.control.ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setButtonOrder("L+CO");
        }
        javafx.scene.control.Button helpBtn =
                (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(helpType);
        helpBtn.setGraphic(xapps.gsea.fx.FxFileIcons.forResource("Help16_v2.gif"));
        xapps.gsea.fx.FxButtons.styleSecondary(helpBtn);
        javafx.scene.control.Button okBtn =
                (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(okType);
        xapps.gsea.fx.FxButtons.stylePrimary(okBtn);
        javafx.scene.control.Button cancelBtn =
                (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(cancelType);
        xapps.gsea.fx.FxButtons.styleSecondary(cancelBtn);
        helpBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            openPrefsHelp();
            e.consume();
        });
        // Swing GDirFieldPlusChooser / GFileField: path validity color on the output dir field.
        pane.wireOutputDirColors();
        dialog.setResultConverter(bt -> bt == okType);
        boolean[] saved = { false };
        dialog.showAndWait().ifPresent(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                pane.savePreferences(true);
                saved[0] = true;
            }
        });
        return saved[0];
    }

    private void wireOutputDirColors() {
        xapps.gsea.fx.params.FxPathFieldColors.attach(outputDirField);
    }

    private static void openPrefsHelp() {
        String url = GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/#Prefs-Window";
        try {
            xapps.gsea.fx.FxDesktopUtil.openUrl(url);
        } catch (Exception ex) {
            Application.getWindowManager().showError("Could not open preferences help", ex);
        }
    }

    private void loadFromPrefs() {
        askBeforeShutdown.setSelected(XPreferencesFactory.kAskBeforeAppShutdown.getBoolean());
        onlineMode.setSelected(XPreferencesFactory.kOnlineMode.getBoolean());
        updateCheck.setSelected(XPreferencesFactory.kMakeGseaUpdateCheck.getBoolean());
        appearance.getSelectionModel().select(xapps.gsea.fx.FxTheme.appearancePref());
        File dir = XPreferencesFactory.kDefaultReportsOutputDir.getDir(false);
        outputDirField.setText(dir != null ? dir.getAbsolutePath() : "");
        cytoscapePortField.setText(Integer.toString(XPreferencesFactory.kCytoscapeRESTPort.getInt()));
        median.setSelected(XPreferencesFactory.kMedian.getBoolean());
        fixLowVar.setSelected(XPreferencesFactory.kFixLowVar.getBoolean());
        biasedVar.setSelected(XPreferencesFactory.kBiasedVar.getBoolean());
    }

    private void chooseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        // Swing GDirFieldPlusChooser → chooseDirByDialog (platform default title).
        FxFileChooserUtil.seedInitialDirectory(chooser, outputDirField.getText());
        File selected = chooser.showDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (selected != null) {
            outputDirField.setText(selected.getAbsolutePath());
            FxFileChooserUtil.registerOpenedDir(selected);
        }
    }

    private void savePreferences(boolean toast) {
        try {
            boolean algoChanged =
                    median.isSelected() != XPreferencesFactory.kMedian.getBoolean()
                    || fixLowVar.isSelected() != XPreferencesFactory.kFixLowVar.getBoolean()
                    || biasedVar.isSelected() != XPreferencesFactory.kBiasedVar.getBoolean();

            XPreferencesFactory.kAskBeforeAppShutdown.setValue(askBeforeShutdown.isSelected());
            XPreferencesFactory.kOnlineMode.setValue(onlineMode.isSelected());
            XPreferencesFactory.kMakeGseaUpdateCheck.setValue(updateCheck.isSelected());
            String appearanceSel = appearance.getSelectionModel().getSelectedItem();
            if (appearanceSel == null || appearanceSel.isBlank()) {
                appearanceSel = xapps.gsea.fx.FxTheme.APPEARANCE_SYSTEM;
            }
            XPreferencesFactory.kUiAppearance.setValue(appearanceSel);
            String outPath = outputDirField.getText().trim();
            // Swing DirPreference saves the chooser value unconditionally (including blank).
            XPreferencesFactory.kDefaultReportsOutputDir.setValue(new File(outPath));
            XPreferencesFactory.kCytoscapeRESTPort.setValue(cytoscapePortField.getText().trim());
            XPreferencesFactory.kMedian.setValue(median.isSelected());
            XPreferencesFactory.kFixLowVar.setValue(fixLowVar.isSelected());
            XPreferencesFactory.kBiasedVar.setValue(biasedVar.isSelected());
            XPreferencesFactory.save();
            xapps.gsea.fx.FxTheme.refreshAll();
            if (toast) {
                Application.getWindowManager().showMessage(algoChanged
                        ? "Preferences saved. Algorithm defaults apply to newly opened tool windows."
                        : "Preferences saved.");
            }
        } catch (Exception e) {
            Application.getWindowManager().showError("Trouble saving preferences", e);
        }
    }

    @Override
    public String getTitle() {
        return "Preferences";
    }

    @Override
    public String getIconResourceId() {
        return null;
    }

    @Override
    public Object getContent() {
        return root;
    }
}
