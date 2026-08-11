/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.ui.api.PageId;
import org.gsea_msigdb.gsea.ui.api.ViewPage;

import javafx.scene.control.ChoiceDialog;

/**
 * Opens the Steps-rail CoreMap singleton (via {@link FeatureHost} / {@link PageId#COREMAP})
 * from Report Explorer actions.
 */
public final class CoreMapWorkspace {

    public static final String LIVE_API_CONFIRM =
            "CoreMap will contact SIGNOR and/or STRING over the network, and may use UniProt for "
                    + "ID mapping. Continue?";

    private CoreMapWorkspace() {
    }

    /**
     * Ask mech/pheno (preferring an empty slot), confirm overwrite if needed, load directory.
     */
    public static void openFromReport(File dir, FeatureHost host) {
        if (dir == null || host == null) {
            return;
        }
        FxCoreMapPane pane = resolvePane(host);
        if (pane == null) {
            return;
        }
        Boolean asMechanistic = chooseLayerRole(pane);
        if (asMechanistic == null) {
            return;
        }
        if (pane.isLayerLoaded(asMechanistic)
                && !host.dialogs().showConfirm(
                        "Replace the existing "
                                + (asMechanistic ? "mechanistic" : "phenotypic")
                                + " CoreMap layer with this report?")) {
            return;
        }
        pane.loadFromDirectory(dir, asMechanistic);
    }

    /** Open a saved CoreMap job folder into the Steps-rail workspace. */
    public static void openJob(File jobDir, FeatureHost host) {
        if (jobDir == null || host == null) {
            return;
        }
        FxCoreMapPane pane = resolvePane(host);
        if (pane == null) {
            host.dialogs().showMessage("Could not open CoreMap workspace.");
            return;
        }
        pane.loadJob(jobDir);
    }

    private static FxCoreMapPane resolvePane(FeatureHost host) {
        Objects.requireNonNull(host, "host");
        ViewPage page = host.pages().get(PageId.COREMAP);
        host.openPage(page);
        if (page instanceof FxCoreMapPane coreMap) {
            return coreMap;
        }
        return null;
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
