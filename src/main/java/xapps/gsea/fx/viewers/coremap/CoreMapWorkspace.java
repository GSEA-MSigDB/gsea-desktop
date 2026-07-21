/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.xbench.core.api.Application;
import javafx.scene.control.ChoiceDialog;

/**
 * Bridges the shell Steps-rail CoreMap workspace and Report Explorer opens so
 * both layers land on one pane.
 */
public final class CoreMapWorkspace {

    public static final String LIVE_API_CONFIRM =
            "CoreMap will contact SIGNOR and/or STRING over the network, and may use UniProt for "
                    + "ID mapping. Continue?";

    private static volatile Supplier<FxCoreMapPane> workspace;

    private CoreMapWorkspace() {
    }

    /** Registered by {@code GseaFxShell}: ensure singleton pane, show it, return it. */
    public static void registerWorkspace(Supplier<FxCoreMapPane> ensureOpen) {
        workspace = ensureOpen;
    }

    /**
     * Ask mech/pheno (preferring an empty slot), confirm overwrite if needed, load directory.
     */
    public static void openFromReport(File dir, Supplier<FxCoreMapPane> fallbackPane,
            Consumer<ViewPage> openPage) {
        if (dir == null) {
            return;
        }
        FxCoreMapPane pane = resolvePane(fallbackPane, openPage);
        if (pane == null) {
            return;
        }
        Boolean asMechanistic = chooseLayerRole(pane);
        if (asMechanistic == null) {
            return;
        }
        if (pane.isLayerLoaded(asMechanistic)
                && !Application.getWindowManager().showConfirm(
                        "Replace the existing "
                                + (asMechanistic ? "mechanistic" : "phenotypic")
                                + " CoreMap layer with this report?")) {
            return;
        }
        pane.loadFromDirectory(dir, asMechanistic);
    }

    /** Open a saved CoreMap job folder into the Steps-rail workspace. */
    public static void openJob(File jobDir, Consumer<ViewPage> openPage) {
        if (jobDir == null) {
            return;
        }
        FxCoreMapPane pane = resolvePane(FxCoreMapPane::new, openPage);
        if (pane == null) {
            Application.getWindowManager().showMessage("Could not open CoreMap workspace.");
            return;
        }
        pane.loadJob(jobDir);
    }

    private static FxCoreMapPane resolvePane(Supplier<FxCoreMapPane> fallbackPane,
            Consumer<ViewPage> openPage) {
        Supplier<FxCoreMapPane> ensure = workspace;
        if (ensure != null) {
            return ensure.get();
        }
        if (fallbackPane == null || openPage == null) {
            return null;
        }
        FxCoreMapPane pane = fallbackPane.get();
        openPage.accept(pane);
        return pane;
    }

    /**
     * @return true = mechanistic, false = phenotypic, null = cancelled
     */
    static Boolean chooseLayerRole(FxCoreMapPane pane) {
        String def = "Mechanistic";
        if (pane != null) {
            if (pane.isLayerLoaded(true) && !pane.isLayerLoaded(false)) {
                def = "Phenotypic";
            } else if (!pane.isLayerLoaded(true)) {
                def = "Mechanistic";
            }
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(def, "Mechanistic", "Phenotypic");
        dialog.setTitle("CoreMap layer");
        dialog.setHeaderText("Load this GSEA result into CoreMap");
        dialog.setContentText("Layer:");
        if (pane != null) {
            dialog.initOwner(xapps.gsea.fx.params.FxFileChooserUtil.windowOf(pane.getContent()));
        }
        xapps.gsea.fx.FxTheme.apply(dialog);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return null;
        }
        return "Mechanistic".equals(result.get());
    }
}
