/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.io.File;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * List/combo cells matching Swing {@code RendererFactory2.CommonLookListRenderer}
 * and {@code GeneSetMatrixChooserWindow.NonFTPGeneSetsRenderer}.
 */
public final class FxPobListCells {

    private FxPobListCells() {
    }

    public static void applyPob(ListCell<?> cell, PersistentObject pob) {
        String qi = pob.getQuickInfo();
        if (qi != null && !qi.isBlank()) {
            Label name = new Label(pob.getName());
            Label info = new Label(" [" + qi + "]");
            info.getStyleClass().add("gsea-muted");
            cell.setText(null);
            cell.setGraphic(new HBox(name, info));
        } else {
            cell.setGraphic(null);
            cell.setText(pob.getName());
        }
        try {
            if (ParserFactory.getCache().isCached(pob)) {
                File f = ParserFactory.getCache().getSourceFile(pob);
                cell.setTooltip(f != null ? new Tooltip(f.getAbsolutePath()) : new Tooltip("Unknown origins of file"));
            } else {
                cell.setTooltip(new Tooltip("Unknown origins of file"));
            }
        } catch (Throwable t) {
            cell.setTooltip(new Tooltip("Unknown origins of file"));
        }
    }

    /** Combo button/list cell: name + muted {@code [quickInfo]} for PersistentObject items. */
    public static ListCell<Object> pobNameQuickInfoCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                if (item instanceof PersistentObject pob) {
                    applyPob(this, pob);
                } else {
                    setText(String.valueOf(item));
                    setGraphic(null);
                    setTooltip(null);
                }
            }
        };
    }

    /** Typed PersistentObject combo cell (name + muted quick-info). */
    public static <T extends PersistentObject> ListCell<T> pobCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                applyPob(this, item);
            }
        };
    }
}
