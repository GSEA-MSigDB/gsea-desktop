/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.tui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import xtools.api.Tool;
import xtools.api.ToolCategory;
import xtools.chip2chip.Chip2Chip;
import xtools.gsea.Gsea;
import xtools.gsea.GseaPreranked;
import xtools.gsea.LeadingEdgeTool;
import xtools.gsea.SsGsea;
import xtools.munge.CollapseDataset;

/**
 * Tool tree organized by {@link ToolCategory} (Swing {@code ToolSelectorTree} parity).
 * Used when a catalog browser is needed; the main GSEA shell uses the vertical toolbar.
 */
public class FxToolSelectorTree {

    private final BorderPane root = new BorderPane();
    private final TreeView<Object> tree;
    private final Consumer<Tool> onToolSelected;

    public FxToolSelectorTree(Consumer<Tool> onToolSelected) {
        this.onToolSelected = onToolSelected != null ? onToolSelected : t -> { };
        TreeItem<Object> rootItem = new TreeItem<>("Tools");
        rootItem.setExpanded(true);

        List<Tool> tools = List.of(
                new Gsea(),
                new GseaPreranked(),
                new SsGsea(),
                new LeadingEdgeTool(),
                new CollapseDataset(),
                new Chip2Chip()
        );
        Map<ToolCategory, List<Tool>> byCategory = new LinkedHashMap<>();
        for (Tool tool : tools) {
            byCategory.computeIfAbsent(tool.getCategory(), k -> new ArrayList<>()).add(tool);
        }
        List<ToolCategory> categories = new ArrayList<>(byCategory.keySet());
        categories.sort(Comparator.comparing(ToolCategory::getName, String.CASE_INSENSITIVE_ORDER));
        for (ToolCategory cat : categories) {
            TreeItem<Object> catItem = new TreeItem<>(cat);
            catItem.setExpanded(true);
            for (Tool tool : byCategory.get(cat)) {
                catItem.getChildren().add(new TreeItem<>(tool));
            }
            rootItem.getChildren().add(catItem);
        }

        tree = new TreeView<>(rootItem);
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    setStyle("");
                    return;
                }
                if (item instanceof ToolCategory cat) {
                    setText(cat.getName());
                    setStyle("-fx-font-weight: bold;");
                    setTooltip(new Tooltip(cat.getDesc()));
                    setGraphic(iconView(cat.getIconResource()));
                } else if (item instanceof Tool tool) {
                    setText(tool.getName());
                    setStyle("");
                    setTooltip(tool.getDesc() != null ? new Tooltip(tool.getDesc()) : null);
                    setGraphic(iconView("Tool16.gif"));
                } else {
                    setText(String.valueOf(item));
                    setGraphic(null);
                    setTooltip(null);
                    setStyle("");
                }
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null && sel.getValue() instanceof Tool) {
                this.onToolSelected.accept((Tool) sel.getValue());
            }
        });
        root.setCenter(tree);
        root.setPrefWidth(220);

        initToLastToolRun();
    }

    public boolean selectTool(String toolClassName) {
        if (toolClassName == null || toolClassName.isBlank()) {
            return false;
        }
        TreeItem<Object> match = findToolItem(tree.getRoot(), toolClassName);
        if (match == null) {
            return false;
        }
        tree.getSelectionModel().select(match);
        tree.scrollTo(tree.getRow(match));
        return true;
    }

    private void initToLastToolRun() {
        try {
            String last = Application.getToolManager().getLastToolName();
            if (last != null) {
                selectTool(last);
            }
        } catch (Throwable t) {
            // Safe init — ignore like Swing.
        }
    }

    private static TreeItem<Object> findToolItem(TreeItem<Object> node, String toolClassName) {
        if (node == null) {
            return null;
        }
        Object v = node.getValue();
        if (v instanceof Tool && toolClassName.equals(v.getClass().getName())) {
            return node;
        }
        for (TreeItem<Object> child : node.getChildren()) {
            TreeItem<Object> found = findToolItem(child, toolClassName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static ImageView iconView(String resource) {
        if (resource == null) {
            return null;
        }
        try {
            var url = JarResources.toURL(resource);
            if (url == null) {
                return null;
            }
            ImageView iv = new ImageView(new Image(url.toExternalForm(), 16, 16, true, true));
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            return iv;
        } catch (Exception e) {
            return null;
        }
    }

    public BorderPane getNode() {
        return root;
    }
}
