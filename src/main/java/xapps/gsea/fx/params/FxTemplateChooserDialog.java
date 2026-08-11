/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateDerivative;
import edu.mit.broad.genome.objects.TemplateDerivatives;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.TemplateImplFromSampleNames;
import edu.mit.broad.genome.objects.TemplateMode;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * JavaFX phenotype (CLS) chooser with options: browse a .cls,
 * on-the-fly sample-name phenotypes, gene-as-phenotype, and all-sources list.
 * Selection is single (matches {@code TemplateSingleChooserParam).
 */
public final class FxTemplateChooserDialog {
    private static final Logger klog = LoggerFactory.getLogger(FxTemplateChooserDialog.class);

    private FxTemplateChooserDialog() {
    }

    /**
     * @return template path string (may include {@code #aux), or empty if cancelled
     */
    public static Optional<String> show(Window owner) {
        return show(owner, TemplateMode.ALL, null);
    }

    public static Optional<String> show(Window owner, TemplateMode mode) {
        return show(owner, mode, null);
    }

    /**
     * @param mode restricts which phenotype shapes are offered (matches
     * {@code TemplateSingleChooserParam}'s mode); {@code null} means no restriction.
     * @param previousSelection prior chooser result to restore, or null
     * @return template path string (may include {@code #aux), or empty if cancelled
     */
    public static Optional<String> show(Window owner, TemplateMode mode, String previousSelection) {
        final TemplateMode effectiveMode = mode != null ? mode : TemplateMode.ALL;
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select phenotype comparison");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResizable(true);
        FxFtpChooserSupport.addHelpButton(dialog, "#cls");
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        TextField clsPath = new TextField();
        clsPath.setManaged(false);
        clsPath.setVisible(false);
        if (previousSelection != null && !previousSelection.isBlank()) {
            String pathOnly = previousSelection;
            int hash = previousSelection.indexOf('#');
            if (hash > 0) {
                pathOnly = previousSelection.substring(0, hash);
            }
            clsPath.setText(pathOnly);
        }

        ListView<TemplateDerivative> options = new ListView<>();
        options.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        final boolean[] comboSourceMode = { false };
        options.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TemplateDerivative item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("gsea-phenotype-combo-source");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName(comboSourceMode[0], false));
                    setGraphic(null);
                    if (comboSourceMode[0]) {
                        getStyleClass().add("gsea-phenotype-combo-source");
                    }
                }
            }
        });
        FxFtpChooserSupport.enableDoubleClickToFire(options, okButton);

        ComboBox<Template> cachedTemplateCombo = new ComboBox<>();
        cachedTemplateCombo.setMaxWidth(Double.MAX_VALUE);
        cachedTemplateCombo.setItems(FXCollections.observableArrayList(nonAuxCachedTemplates()));
        dialog.setOnShowing(e -> refreshCachedTemplateCombo(cachedTemplateCombo));
        cachedTemplateCombo.setOnShowing(e -> refreshCachedTemplateCombo(cachedTemplateCombo));
        cachedTemplateCombo.setCellFactory(lv -> xapps.gsea.fx.widgets.FxPobListCells.pobCell());
        cachedTemplateCombo.setButtonCell(xapps.gsea.fx.widgets.FxPobListCells.pobCell());
        cachedTemplateCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                clsPath.clear();
                comboSourceMode[0] = false;
                options.refresh();
                loadOptionsFromCachedTemplate(selected, options, dialog, effectiveMode);
            }
        });

        Button bShowAll = new Button("Show phenotypes from all source files");
        bShowAll.setMaxWidth(Double.MAX_VALUE);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(bShowAll);
        bShowAll.setOnAction(e -> {
            try {
                @SuppressWarnings("unchecked")
                List<Template> all = AppServices.require().cache().getCachedObjectsL(Template.class);
                Template[] qualified = qualifyByTypeAndMode(
                        all.toArray(new Template[0]), true, effectiveMode);
                List<TemplateDerivative> tds = new ArrayList<>();
                for (Template template : qualified) {
                    tds.add(new TemplateDerivatives.PseudoTemplateDerivative(template));
                }
                clsPath.clear();
                cachedTemplateCombo.getSelectionModel().clearSelection();
                options.setItems(FXCollections.observableArrayList(tds));
                options.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                comboSourceMode[0] = true;
                options.refresh();
                if (!tds.isEmpty()) {
                    options.getSelectionModel().selectFirst();
                }
            } catch (Exception ex) {
                klog.error(ex.getMessage(), ex);
                showError(dialog, "Could not list cached phenotypes", ex.getMessage());
            }
        });

        Button bOnTheFly = new Button("Create an on-the-fly phenotype ...");
        bOnTheFly.setMaxWidth(Double.MAX_VALUE);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(bOnTheFly);
        bOnTheFly.setOnAction(e -> {
            Window w = dialog.getDialogPane().getScene().getWindow();
            Optional<File> created = showOnTheFlyFromSampleNames(w);
            created.ifPresent(file -> {
                FxFileChooserUtil.registerOpened(file);
                applyClsSourceFile(file, clsPath, cachedTemplateCombo, comboSourceMode, options, dialog, effectiveMode);
            });
        });

        Button bFromGene = new Button("Use a gene as the phenotype ...");
        bFromGene.setMaxWidth(Double.MAX_VALUE);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(bFromGene);
        bFromGene.setOnAction(e -> {
            Window w = dialog.getDialogPane().getScene().getWindow();
            Optional<File> created = showGenePhenotype(w);
            created.ifPresent(file -> {
                FxFileChooserUtil.registerOpened(file);
                applyClsSourceFile(file, clsPath, cachedTemplateCombo, comboSourceMode, options, dialog, effectiveMode);
            });
        });

        Runnable browseClsAction = () -> {
            Window w = dialog.getDialogPane().getScene().getWindow();
            FxFtpChooserSupport.browseLocalFiles(
                    w,
                    "Open phenotype file",
                    FxFtpChooserSupport.clsFileFilters(),
                    false,
                    clsPath.getText(),
                    files -> FxFtpChooserSupport.loadLocalFilesAsync(
                            files,
                            Template.class,
                            loaded -> {
                                try {
                                    applyClsSourceFile(
                                            AppServices.require().cache().getSourceFile(loaded.get(0)),
                                            clsPath,
                                            cachedTemplateCombo,
                                            comboSourceMode,
                                            options,
                                            dialog,
                                            effectiveMode);
                                } catch (Exception ex) {
                                    klog.error(ex.getMessage(), ex);
                                    showError(dialog, "Could not load phenotype file", ex.getMessage());
                                }
                            }));
        };

        Button browseCls = xapps.gsea.fx.widgets.FxEllipsisButton.create("Browse");
        browseCls.setOnAction(e -> browseClsAction.run());

        HBox sourceRow = new HBox(6, cachedTemplateCombo, browseCls);
        sourceRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cachedTemplateCombo, Priority.ALWAYS);
        cachedTemplateCombo.setMaxWidth(Double.MAX_VALUE);

        VBox sourceCol = new VBox(4,
                sectionLabel("Select source file"),
                sourceRow);

        VBox optionsButtons = new VBox(6, bShowAll, bOnTheFly, bFromGene);
        optionsButtons.setPadding(new Insets(8));
        TitledPane optionsPane = new TitledPane("Options", optionsButtons);
        optionsPane.setCollapsible(false);
        optionsPane.setExpanded(true);

        options.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox phenotypeCol = new VBox(4, sectionLabel("Select phenotype comparison"), options);
        VBox.setVgrow(options, Priority.ALWAYS);
        VBox.setVgrow(phenotypeCol, Priority.ALWAYS);

        VBox center = new VBox(10, sourceCol, phenotypeCol);
        VBox.setVgrow(phenotypeCol, Priority.ALWAYS);
        center.setFillWidth(true);
        center.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setCenter(center);
        root.setBottom(optionsPane);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(550, 450);
        xapps.gsea.fx.FxTheme.apply(dialog);
        xapps.gsea.fx.widgets.FxButtons.stylePrimary(okButton);
        xapps.gsea.fx.widgets.FxButtons.styleSecondary(
                (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));

        dialog.setResultConverter(btn -> {
            if (btn == null || btn.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            }
            TemplateDerivative sel = options.getSelectionModel().getSelectedItem();
            if (sel != null) {
                return sel.getName(false, true);
            }
            String path = clsPath.getText();
            return StringUtils.isBlank(path) ? "" : path.trim();
        });

        if (previousSelection != null && !previousSelection.isBlank()) {
            File prior = StringUtils.isNotBlank(clsPath.getText())
                    ? new File(clsPath.getText().trim()) : null;
            if (prior != null && prior.isFile()) {
                loadOptions(prior, options, dialog, effectiveMode);
                selectMatchingOption(options, previousSelection);
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    List<Template> all = AppServices.require().cache().getCachedObjectsL(Template.class);
                    for (Template t : all) {
                        if (t == null) {
                            continue;
                        }
                        if (previousSelection.equals(t.getName())
                                || previousSelection.endsWith("#" + t.getName())
                                || previousSelection.contains(t.getName())) {
                            loadOptionsFromCachedTemplate(t, options, dialog, effectiveMode);
                            selectMatchingOption(options, previousSelection);
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return dialog.showAndWait().filter(StringUtils::isNotBlank);
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private static void selectMatchingOption(ListView<TemplateDerivative> options, String previousSelection) {
        for (TemplateDerivative td : options.getItems()) {
            String name = td.getName(false, true);
            if (previousSelection.equals(name)) {
                options.getSelectionModel().select(td);
                return;
            }
        }
    }

    private static void applyClsSourceFile(
            File file,
            TextField clsPath,
            ComboBox<Template> cachedTemplateCombo,
            boolean[] comboSourceMode,
            ListView<TemplateDerivative> options,
            Dialog<?> dialog,
            TemplateMode mode) {
        clsPath.setText(file.getAbsolutePath());
        cachedTemplateCombo.getSelectionModel().clearSelection();
        comboSourceMode[0] = false;
        options.refresh();
        loadOptions(file, options, dialog, mode);
    }

    private static Optional<File> showOnTheFlyFromSampleNames(Window owner) {
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("On-the-fly phenotype by sample names");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.setResizable(true);
        FxFtpChooserSupport.addUserGuideHelpButton(dialog, "#Phenotype-Select-Window");

        TextArea taClassA = new TextArea();
        taClassA.setPrefRowCount(12);
        TextField tfClassA = new TextField("ClassA");

        TextArea taClassB = new TextArea();
        taClassB.setPrefRowCount(12);
        TextField tfClassB = new TextField("ClassB");

        TitledPane samplesA = new TitledPane("Samples for class A (one per line)", taClassA);
        samplesA.setCollapsible(false);
        TitledPane nameA = new TitledPane("Enter a brief name for class A", tfClassA);
        nameA.setCollapsible(false);
        TitledPane samplesB = new TitledPane("Samples for class B (one per line)", taClassB);
        samplesB.setCollapsible(false);
        TitledPane nameB = new TitledPane("Enter a brief name for class B", tfClassB);
        nameB.setCollapsible(false);

        VBox colA = new VBox(6,
                new Label("Class A (sample names must match the dataset)"),
                samplesA,
                nameA);
        VBox.setVgrow(samplesA, Priority.ALWAYS);
        VBox colB = new VBox(6,
                new Label("Class B (and sample names cant be reused)"),
                samplesB,
                nameB);
        VBox.setVgrow(samplesB, Priority.ALWAYS);
        HBox classes = new HBox(15, colA, colB);
        HBox.setHgrow(colA, Priority.ALWAYS);
        HBox.setHgrow(colB, Priority.ALWAYS);

        ComboBox<Dataset> cbDataset = datasetCombo();
        TitledPane datasetPane = new TitledPane("Dataset", cbDataset);
        datasetPane.setCollapsible(false);
        Button apply = new Button("Apply to dataset");
        xapps.gsea.fx.widgets.FxButtons.stylePrimary(apply);
        final File[] created = { null };
        apply.setOnAction(e -> {
            try {
                if (StringUtils.isBlank(taClassA.getText())) {
                    showMessage(dialog, "No sample names specified in class A");
                    return;
                }
                if (StringUtils.isBlank(taClassB.getText())) {
                    showMessage(dialog, "No sample names specified in class B");
                    return;
                }
                if (StringUtils.isBlank(tfClassA.getText())) {
                    showMessage(dialog, "Invalid (empty) name specified for class A");
                    return;
                }
                if (StringUtils.isBlank(tfClassB.getText())) {
                    showMessage(dialog, "Invalid (empty) name specified for class B");
                    return;
                }
                Dataset ds = cbDataset.getSelectionModel().getSelectedItem();
                if (ds == null) {
                    showMessage(dialog, "No dataset available. First import a dataset and then apply this phenotype");
                    return;
                }

                File out = AppServices.require().vdb().getDefaultOutputDir();
                String classAName = tfClassA.getText().trim();
                String classBName = tfClassB.getText().trim();
                String tn = classAName + "_vs_" + classBName + ".cls";
                File file = NamingConventions.createSafeFile(out, tn);

                String[] classASampleNames = ParseUtils.string2strings(taClassA.getText(), "\n\t");
                String[] classBSampleNames = ParseUtils.string2strings(taClassB.getText(), "\n\t");

                TemplateImplFromSampleNames tsn = new TemplateImplFromSampleNames(
                        tn, classAName, classASampleNames, classBName, classBSampleNames);
                Template createdTemplate = tsn.createTemplate(ds);
                // Must save tsn (keeps sample names); saving createdTemplate strips them
                ParserFactory.save(tsn, file);
                created[0] = file;
                showMessage(dialog, "Successfully made template: " + createdTemplate.getName());
            } catch (Throwable t) {
                klog.error(t.getMessage(), t);
                showError(dialog, "Trouble making template", t.getMessage());
            }
        });

        HBox south = new HBox(8, datasetPane, apply);
        HBox.setHgrow(datasetPane, Priority.ALWAYS);
        cbDataset.setMaxWidth(Double.MAX_VALUE);

        VBox root = new VBox(10, classes, south);
        root.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(550, 400);
        xapps.gsea.fx.FxTheme.apply(dialog);
        dialog.setResultConverter(btn -> created[0]);
        return dialog.showAndWait().filter(f -> f != null);
    }

    private static Optional<File> showGenePhenotype(Window owner) {
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("Use a gene as the phenotype");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.setResizable(true);
        FxFtpChooserSupport.addUserGuideHelpButton(dialog, "#Phenotype-Select-Window");

        ComboBox<Dataset> cbDataset = datasetCombo();
        TextField geneFilter = new TextField();
        ListView<String> geneList = new ListView<>();
        geneList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        ObservableList<String> allGenes = FXCollections.observableArrayList();
        FilteredList<String> filtered = new FilteredList<>(allGenes, s -> true);
        geneList.setItems(filtered);

        Runnable reloadGenes = () -> {
            allGenes.clear();
            Dataset ds = cbDataset.getSelectionModel().getSelectedItem();
            if (ds != null) {
                allGenes.addAll(ds.getRowNames());
            }
            String filter = geneFilter.getText();
            filtered.setPredicate(name -> StringUtils.isBlank(filter)
                    || name.toLowerCase().contains(filter.trim().toLowerCase()));
            if (!filtered.isEmpty()) {
                geneList.getSelectionModel().selectFirst();
            }
        };

        cbDataset.setOnAction(e -> reloadGenes.run());
        geneFilter.textProperty().addListener((obs, o, n) -> {
            String filter = n == null ? "" : n.trim().toLowerCase();
            filtered.setPredicate(name -> filter.isEmpty() || name.toLowerCase().contains(filter));
        });
        if (cbDataset.getSelectionModel().getSelectedItem() != null) {
            reloadGenes.run();
        }

        Button apply = new Button("Apply to dataset");
        xapps.gsea.fx.widgets.FxButtons.stylePrimary(apply);
        final File[] created = { null };
        apply.setOnAction(e -> {
            try {
                String gene = geneList.getSelectionModel().getSelectedItem();
                if (StringUtils.isBlank(gene)) {
                    String typed = geneFilter.getText();
                    if (StringUtils.isNotBlank(typed) && allGenes.contains(typed.trim())) {
                        gene = typed.trim();
                    } else {
                        showMessage(dialog, "First select a gene, then apply");
                        return;
                    }
                }
                if (!allGenes.contains(gene)) {
                    showMessage(dialog, "Chosen gene is not in the feature list");
                    return;
                }
                Dataset ds = cbDataset.getSelectionModel().getSelectedItem();
                if (ds == null) {
                    showMessage(dialog, "No dataset available. First import a dataset and then apply this phenotype");
                    return;
                }

                File out = AppServices.require().vdb().getDefaultOutputDir();
                String tn = gene + "_profile_in_" + ds.getName() + ".cls";
                File file = NamingConventions.createSafeFile(out, tn);
                Template createdTemplate = TemplateFactory.createContinuousTemplate(gene, ds);
                ParserFactory.save(createdTemplate, file);
                created[0] = file;
                showMessage(dialog, "Successfully made template: " + createdTemplate.getName());
            } catch (Throwable t) {
                klog.error(t.getMessage(), t);
                showError(dialog, "Trouble making template", t.getMessage());
            }
        });

        TitledPane selectedGene = new TitledPane("Selected Gene", geneFilter);
        selectedGene.setCollapsible(false);
        TitledPane featureList = new TitledPane("Feature List", geneList);
        featureList.setCollapsible(false);
        TitledPane datasetPane = new TitledPane("Dataset", cbDataset);
        datasetPane.setCollapsible(false);

        HBox south = new HBox(8, datasetPane, apply);
        HBox.setHgrow(datasetPane, Priority.ALWAYS);
        cbDataset.setMaxWidth(Double.MAX_VALUE);

        VBox root = new VBox(8, selectedGene, featureList, south);
        VBox.setVgrow(featureList, Priority.ALWAYS);
        root.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(550, 400);
        xapps.gsea.fx.FxTheme.apply(dialog);
        dialog.setResultConverter(btn -> created[0]);
        return dialog.showAndWait().filter(f -> f != null);
    }

    private static ComboBox<Dataset> datasetCombo() {
        ComboBox<Dataset> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        try {
            @SuppressWarnings("unchecked")
            List<Dataset> datasets = AppServices.require().cache().getCachedObjectsL(Dataset.class);
            cb.setItems(FXCollections.observableArrayList(datasets));
            if (!datasets.isEmpty()) {
                cb.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            klog.debug("Could not list cached datasets", e);
        }
        cb.setCellFactory(lv -> xapps.gsea.fx.widgets.FxPobListCells.pobCell());
        cb.setButtonCell(xapps.gsea.fx.widgets.FxPobListCells.pobCell());
        return cb;
    }

    private static void loadOptions(File clsFile, ListView<TemplateDerivative> options, Dialog<?> dialog,
            TemplateMode mode) {
        options.getItems().clear();
        if (clsFile == null || !clsFile.exists()) {
            return;
        }
        try {
            Template main = ParserFactory.readTemplate(clsFile, true, true, true);
            Template[] tss = qualifyByTypeAndMode(
                    TemplateFactory.extractAllPossibleTemplates(main, true), true, mode);
            List<TemplateDerivative> tds = new ArrayList<>();
            for (Template t : tss) {
                tds.add(new TemplateDerivatives.AuxTemplateDerivative(t.getName(), main));
            }
            try {
                Template[] conts = qualifyByTypeAndMode(ParserFactory.readTemplates(clsFile), false, mode);
                for (Template ct : conts) {
                    if (ct.isContinuous()) {
                        tds.add(new TemplateDerivatives.ContTemplateDerivative(ct.getName(), clsFile));
                    }
                }
            } catch (Exception contEx) {
                klog.debug("No additional templates from {}: {}", clsFile, contEx.toString());
            }
            options.setItems(FXCollections.observableArrayList(tds));
            options.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            if (!tds.isEmpty()) {
                options.getSelectionModel().selectFirst();
            }
        } catch (Exception ex) {
            klog.error(ex.getMessage(), ex);
            showError(dialog, "Could not parse phenotype file", ex.getMessage());
        }
    }

    private static List<Template> nonAuxCachedTemplates() {
        List<Template> result = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Template> all = AppServices.require().cache().getCachedObjectsL(Template.class);
            for (Template t : all) {
                if (t.isContinuous() || !t.isAux()) {
                    result.add(t);
                }
            }
        } catch (Exception e) {
            klog.debug("Could not list cached templates", e);
        }
        return result;
    }

    private static void refreshCachedTemplateCombo(ComboBox<Template> combo) {
        Template selected = combo.getSelectionModel().getSelectedItem();
        List<Template> items = nonAuxCachedTemplates();
        combo.setItems(FXCollections.observableArrayList(items));
        if (selected != null && items.contains(selected)) {
            combo.getSelectionModel().select(selected);
        }
    }

    private static void loadOptionsFromCachedTemplate(Template selected, ListView<TemplateDerivative> options,
            Dialog<?> dialog, TemplateMode mode) {
        options.getItems().clear();
        if (selected == null) {
            return;
        }
        try {
            List<TemplateDerivative> tds = new ArrayList<>();
            if (selected.isContinuous()) {
                File file = AppServices.require().cache().getSourceFile(selected);
                Set<Template> uniq = new LinkedHashSet<>(Arrays.asList(ParserFactory.readTemplates(file)));
                Template[] cts = qualifyByTypeAndMode(uniq.toArray(new Template[0]), false, mode);
                for (Template ct : cts) {
                    tds.add(new TemplateDerivatives.ContTemplateDerivative(ct.getName(), file));
                }
            } else {
                Template[] tss = qualifyByTypeAndMode(
                        TemplateFactory.extractAllPossibleTemplates(selected, true), true, mode);
                for (Template t : tss) {
                    tds.add(new TemplateDerivatives.AuxTemplateDerivative(t.getName(), selected));
                }
            }
            options.setItems(FXCollections.observableArrayList(tds));
            options.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            if (!tds.isEmpty()) {
                options.getSelectionModel().selectFirst();
            }
        } catch (Exception ex) {
            klog.error(ex.getMessage(), ex);
            showError(dialog, "Could not build phenotype options", ex.getMessage());
        }
    }

    private static Template[] qualifyByTypeAndMode(Template[] tss, boolean onlyHashOnesForBiphasic, TemplateMode mode) {
        List<Template> list = new ArrayList<>();
        TemplateMode effective = mode != null ? mode : TemplateMode.ALL;

        if (effective == TemplateMode.CONTINUOUS_ONLY) {
            for (Template t : tss) {
                if (t.isContinuous()) {
                    list.add(t);
                }
            }
        } else if (effective == TemplateMode.CATEGORICAL_2_CLASS_ONLY) {
            for (Template t : tss) {
                if (onlyHashOnesForBiphasic) {
                    if (!t.isContinuous() && t.getNumClasses() == 2 && t.getName().indexOf('#') != -1) {
                        list.add(t);
                    }
                } else {
                    if (!t.isContinuous() && t.getNumClasses() == 2) {
                        list.add(t);
                    }
                }
            }
        } else if (effective == TemplateMode.CATEGORICAL_ONLY) {
            for (Template t : tss) {
                if (!t.isContinuous()) {
                    list.add(t);
                }
            }
        } else if (effective == TemplateMode.UNIPHASE_ONLY) {
            for (Template t : tss) {
                if (t.getNumClasses() == 1) {
                    list.add(t);
                }
            }
        } else if (effective == TemplateMode.ALL) {
            for (Template t : tss) {
                // Skip the non-aux parent template (typically the source file name).
                if (!t.isContinuous() && !t.isAux() && t.getName().indexOf('#') == -1) {
                    continue;
                }
                list.add(t);
            }
            return list.toArray(new Template[0]);
        } else if (effective == TemplateMode.CATEGORICAL_2_CLASS_AND_NUMERIC) {
            for (Template t : tss) {
                if (t.isContinuous()) {
                    list.add(t);
                }
            }
            for (Template t : tss) {
                if (onlyHashOnesForBiphasic) {
                    if (!t.isContinuous() && t.getNumClasses() == 2 && t.getName().indexOf('#') != -1) {
                        list.add(t);
                    }
                } else {
                    if (!t.isContinuous() && t.getNumClasses() == 2) {
                        list.add(t);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown mode: " + effective);
        }
        return list.toArray(new Template[0]);
    }

    private static void showMessage(Dialog<?> ownerDialog, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (ownerDialog.getDialogPane().getScene() != null) {
            alert.initOwner(ownerDialog.getDialogPane().getScene().getWindow());
        }
        alert.setTitle("Phenotype");
        alert.setHeaderText(null);
        alert.setContentText(message);
        xapps.gsea.fx.FxTheme.apply(alert);
        alert.showAndWait();
    }

    private static void showError(Dialog<?> ownerDialog, String header, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (ownerDialog.getDialogPane().getScene() != null) {
            alert.initOwner(ownerDialog.getDialogPane().getScene().getWindow());
        }
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(detail);
        xapps.gsea.fx.FxTheme.apply(alert);
        alert.showAndWait();
    }
}
