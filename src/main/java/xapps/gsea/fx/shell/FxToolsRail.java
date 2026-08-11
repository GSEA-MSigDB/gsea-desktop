/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Left-rail tool catalog: independent column stacks, drag-reorder, persisted order.
 * Item reorder is section-local (items do not move across sections).
 *
 * <h2>Width contract</h2>
 * Dock / prefs use the Bento <em>tools column</em> width (side-tab chrome included):
 * <ul>
 *   <li>{@link #MIN_TOOLS_COLUMN_WIDTH_PX} — SplitPane compress floor</li>
 *   <li>{@link #DEFAULT_TOOLS_COLUMN_WIDTH_PX} — startup width; stays single-column</li>
 * </ul>
 * Rail reflow uses the {@link ScrollPane} viewport only. Cards and buttons keep a fixed
 * structural {@code minWidth} (wrapped-button geometry); {@code pref}/{@code max} track the
 * live viewport so the divider can shrink below the last laid-out size. Scroll policy is
 * {@code AS_NEEDED}.
 * <p>
 * Columns: {@code floor((usable + gap) / (COLUMN_BREAK + gap))} with
 * {@code usable = max(0, viewport − rail padding)}.
 */
public final class FxToolsRail {

    private static final DataFormat SECTION_DND = new DataFormat("application/x-gsea-tool-section");
    private static final DataFormat ITEM_DND = new DataFormat("application/x-gsea-tool-item");

    private static final double RAIL_PAD_H = 8;
    private static final double RAIL_PAD_V = 10;
    /** Host padding left+right ({@link #RAIL_PAD_H} × 2). */
    static final double RAIL_INSETS_H = RAIL_PAD_H * 2;
    /** {@code .gsea-tool-group} padding left+right. */
    private static final double CARD_PAD_H = 12;
    /** {@code .gsea-tool-group} border left+right. */
    private static final double CARD_BORDER_H = 2;
    static final double CARD_INSETS_H = CARD_PAD_H + CARD_BORDER_H;
    static final double COL_GAP = 10;
    private static final double ROW_GAP = 10;

    // Keep in sync with .gsea-rail-button in gsea-fx.css.
    private static final double RAIL_ICON_PX = 32;
    private static final double RAIL_GRAPHIC_TEXT_GAP_PX = 8;
    private static final double RAIL_BUTTON_PAD_H = 6 + 8;
    private static final double RAIL_MIN_WRAPPED_TEXT_PX = 64;

    /** Icon + gap + wrapped text column + button padding. */
    static final double MIN_ITEM_WIDTH_PX =
            RAIL_ICON_PX + RAIL_GRAPHIC_TEXT_GAP_PX + RAIL_MIN_WRAPPED_TEXT_PX + RAIL_BUTTON_PAD_H;

    static final double MIN_SECTION_WIDTH_PX = MIN_ITEM_WIDTH_PX + CARD_INSETS_H;

    /** Left tab strip in the tools leaf ({@code bento.css} {@code .header:left}). */
    private static final double DOCK_SIDE_TAB_PX = 24;

    /**
     * Tools-column compress floor for the dock SplitPane: section + rail padding + side tab.
     */
    public static final int MIN_TOOLS_COLUMN_WIDTH_PX = (int) Math.ceil(
            MIN_SECTION_WIDTH_PX + RAIL_INSETS_H + DOCK_SIDE_TAB_PX);

    /**
     * Startup tools-column width. Sized so {@code Leading Edge Analysis (Classic LEA)}
     * fits on one line at rest. Keep {@code XPreferencesFactory.kShellToolsPanelWidth}
     * default in sync.
     */
    public static final int DEFAULT_TOOLS_COLUMN_WIDTH_PX = 320;

    /**
     * Usable width that buys one column. Chosen so a viewport as wide as
     * {@link #DEFAULT_TOOLS_COLUMN_WIDTH_PX} still yields one column.
     */
    public static final int COLUMN_BREAK_WIDTH_PX = 200;

    private static final String[] SECTION_DROP = {
            "gsea-tool-group-drop-before", "gsea-tool-group-drop-after" };
    private static final String[] ITEM_DROP = {
            "gsea-rail-button-drop-before", "gsea-rail-button-drop-after" };

    static {
        if (MIN_TOOLS_COLUMN_WIDTH_PX >= DEFAULT_TOOLS_COLUMN_WIDTH_PX) {
            throw new IllegalStateException("min tools width must be < default");
        }
        // Worst case: viewport as wide as the default tools column → still one column.
        if (columnsForViewportWidth(DEFAULT_TOOLS_COLUMN_WIDTH_PX, Integer.MAX_VALUE) != 1) {
            throw new IllegalStateException("default tools width must lay out as one column");
        }
    }

    private FxToolsRail() {
    }

    public record ToolItem(String id, String label, String iconResource, Runnable action) {
        public ToolItem {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(action, "action");
        }
    }

    public record ToolSection(String id, String title, List<ToolItem> items) {
        public ToolSection {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(items, "items");
            items = List.copyOf(items);
        }

        boolean hasTitle() {
            return title != null && !title.isBlank();
        }
    }

    /** Scroll content state used by layout / DnD / tests. */
    static final class RailLayout {
        final HBox host = new HBox(COL_GAP);
        final List<VBox> sectionOrder = new ArrayList<>();
        final Map<String, VBox> sectionNodes = new HashMap<>();
        int columnCount = 1;

        RailLayout() {
            host.setPadding(new Insets(RAIL_PAD_V, RAIL_PAD_H, RAIL_PAD_V, RAIL_PAD_H));
            host.getStyleClass().add("gsea-tools-rail");
            host.setFillHeight(false);
        }
    }

    public static Node build(List<ToolSection> defaults) {
        Objects.requireNonNull(defaults, "defaults");
        Map<String, ToolSection> byId = new LinkedHashMap<>();
        for (ToolSection s : defaults) {
            byId.put(s.id(), s);
        }

        RailLayout layout = new RailLayout();
        for (ToolSection section : applySavedOrder(byId, defaults)) {
            VBox card = buildSectionCard(section, layout);
            layout.sectionNodes.put(section.id(), card);
            layout.sectionOrder.add(card);
        }

        ScrollPane scroll = new ScrollPane(layout.host);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Do not let content mins inflate the ScrollPane's reported minimum.
        scroll.setMinWidth(0);
        layout.host.setMinWidth(0);
        scroll.getStyleClass().add("gsea-tools-scroll");
        scroll.setUserData(layout);

        applyLayout(layout, COLUMN_BREAK_WIDTH_PX);
        scroll.viewportBoundsProperty().addListener((obs, o, bounds) -> {
            if (bounds.getWidth() > 0) {
                applyLayout(layout, bounds.getWidth());
            }
        });
        return scroll;
    }

    /** Content width inside the rail host after horizontal padding. */
    static double usableWidth(double viewportWidth) {
        return Math.max(0, viewportWidth - RAIL_INSETS_H);
    }

    /**
     * Minimum viewport width that yields {@code columns} (before capping by section count).
     */
    static double viewportWidthForColumns(int columns) {
        if (columns <= 1) {
            return 0;
        }
        double usable = columns * COLUMN_BREAK_WIDTH_PX + (columns - 1) * COL_GAP;
        return usable + RAIL_INSETS_H;
    }

    static int columnsForViewportWidth(double viewportWidth, int sectionCount) {
        if (sectionCount <= 1) {
            return 1;
        }
        double usable = usableWidth(viewportWidth);
        int cols = Math.max(1, (int) Math.floor((usable + COL_GAP) / (COLUMN_BREAK_WIDTH_PX + COL_GAP)));
        return Math.min(sectionCount, cols);
    }

    /**
     * Size cards/buttons from the viewport. Structural mins stay at the wrapped-button
     * floor; pref/max follow the available width so SplitPane can compress.
     */
    static void applyLayout(RailLayout layout, double viewportWidth) {
        int cols = columnsForViewportWidth(viewportWidth, layout.sectionOrder.size());
        double usable = usableWidth(viewportWidth);
        double sectionW = Math.max(0, Math.floor((usable - (cols - 1) * COL_GAP) / cols));
        double itemW = Math.max(0, sectionW - CARD_INSETS_H);

        boolean rebuild = cols != layout.columnCount || layout.host.getChildren().size() != cols;
        for (VBox card : layout.sectionOrder) {
            card.setMinWidth(MIN_SECTION_WIDTH_PX);
            card.setPrefWidth(sectionW);
            card.setMaxWidth(sectionW);
            for (Node child : itemsOf(card).getChildren()) {
                if (child instanceof Button b) {
                    b.setMinWidth(MIN_ITEM_WIDTH_PX);
                    b.setPrefWidth(itemW);
                    b.setMaxWidth(itemW);
                }
            }
        }
        layout.columnCount = cols;
        if (rebuild) {
            rebuildColumns(layout);
        }
    }

    private static void rebuildColumns(RailLayout layout) {
        layout.host.getChildren().clear();
        List<VBox> columns = new ArrayList<>(layout.columnCount);
        for (int i = 0; i < layout.columnCount; i++) {
            VBox col = new VBox(ROW_GAP);
            col.setFillWidth(true);
            col.setMinWidth(0);
            col.setMaxHeight(Region.USE_PREF_SIZE);
            columns.add(col);
            layout.host.getChildren().add(col);
        }
        int n = layout.sectionOrder.size();
        int perCol = Math.max(1, (int) Math.ceil(n / (double) layout.columnCount));
        for (int i = 0; i < n; i++) {
            columns.get(Math.min(layout.columnCount - 1, i / perCol))
                    .getChildren()
                    .add(layout.sectionOrder.get(i));
        }
    }

    private static VBox buildSectionCard(ToolSection section, RailLayout layout) {
        Label grip = new Label("⠿");
        grip.getStyleClass().add("gsea-tool-group-grip");
        Tooltip.install(grip, new Tooltip("Drag to reorder section"));

        HBox titleRow = new HBox(grip);
        titleRow.getStyleClass().add("gsea-tool-group-title");
        if (section.hasTitle()) {
            Label header = new Label(section.title());
            header.getStyleClass().add("gsea-section-header");
            header.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(header, Priority.ALWAYS);
            titleRow.getChildren().add(header);
        } else {
            titleRow.getStyleClass().add("gsea-tool-group-title-untitled");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            titleRow.getChildren().add(spacer);
        }

        VBox items = new VBox();
        items.getStyleClass().add("gsea-tool-group-items");
        Map<String, Button> byId = new LinkedHashMap<>();
        for (ToolItem item : section.items()) {
            Button b = toolButton(item);
            byId.put(item.id(), b);
            installItemDnD(b, item.id(), section.id(), items, byId, layout);
            items.getChildren().add(b);
        }
        installItemsPaneDnD(items, section.id(), byId, layout);

        VBox card = new VBox(titleRow, items);
        card.getStyleClass().add("gsea-tool-group");
        card.setUserData(section.id());
        card.setMaxHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(card, Priority.NEVER);
        installSectionDnD(card, section.id(), layout, titleRow);
        return card;
    }

    private static Button toolButton(ToolItem item) {
        Button b = new Button(item.label());
        xapps.gsea.fx.widgets.FxButtons.styleRail(b);
        b.setUserData(item.id());
        b.setMaxHeight(Region.USE_PREF_SIZE);
        if (item.iconResource() != null) {
            try {
                var url = JarResources.toURL(item.iconResource());
                if (url != null) {
                    b.setGraphic(new ImageView(
                            new Image(url.toExternalForm(), 32, 32, true, true)));
                }
            } catch (Exception ignored) {
            }
        }
        b.setOnAction(e -> item.action().run());
        return b;
    }

    private static void installSectionDnD(VBox card, String sectionId, RailLayout layout,
            Node dragSource) {
        dragSource.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(SECTION_DND, sectionId);
            db.setContent(content);
            setDragPreview(db, card, e.getX(), e.getY());
            card.getStyleClass().add("gsea-tool-group-dragging");
            e.consume();
        });
        card.setOnDragDone(e -> {
            card.getStyleClass().remove("gsea-tool-group-dragging");
            clearHints(layout.sectionOrder, SECTION_DROP);
            e.consume();
        });
        card.setOnDragOver(e -> {
            Object raw = e.getDragboard().getContent(SECTION_DND);
            if (raw instanceof String src && !src.equals(sectionId)) {
                e.acceptTransferModes(TransferMode.MOVE);
                setDropHint(card, SECTION_DROP, dropBefore(card, e));
                e.consume();
            }
        });
        card.setOnDragExited(e -> clearHint(card, SECTION_DROP));
        card.setOnDragDropped(e -> {
            boolean ok = false;
            Object raw = e.getDragboard().getContent(SECTION_DND);
            if (raw instanceof String sourceId && !sourceId.equals(sectionId)) {
                VBox source = layout.sectionNodes.get(sourceId);
                int from = source == null ? -1 : layout.sectionOrder.indexOf(source);
                int to = layout.sectionOrder.indexOf(card);
                if (from >= 0 && to >= 0) {
                    if (!dropBefore(card, e)) {
                        to++;
                    }
                    moveInList(layout.sectionOrder, from, to);
                    rebuildColumns(layout);
                    persistOrder(layout);
                    ok = true;
                }
            }
            clearHints(layout.sectionOrder, SECTION_DROP);
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private static void installItemDnD(Button button, String itemId, String sectionId,
            VBox items, Map<String, Button> byId, RailLayout layout) {
        button.setOnDragDetected(e -> {
            Dragboard db = button.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(ITEM_DND, sectionId + '\n' + itemId);
            db.setContent(content);
            setDragPreview(db, button, e.getX(), e.getY());
            button.getStyleClass().add("gsea-rail-button-dragging");
            e.consume();
        });
        button.setOnDragDone(e -> {
            button.getStyleClass().remove("gsea-rail-button-dragging");
            clearHints(items.getChildren(), ITEM_DROP);
            e.consume();
        });
        button.setOnDragOver(e -> {
            String sourceId = itemSourceId(e, sectionId);
            if (sourceId != null && !sourceId.equals(itemId)) {
                e.acceptTransferModes(TransferMode.MOVE);
                setDropHint(button, ITEM_DROP, dropBefore(button, e));
                e.consume();
            }
        });
        button.setOnDragExited(e -> clearHint(button, ITEM_DROP));
        button.setOnDragDropped(e -> {
            boolean ok = false;
            String sourceId = itemSourceId(e, sectionId);
            Button source = sourceId == null ? null : byId.get(sourceId);
            if (source != null && !sourceId.equals(itemId)) {
                int to = items.getChildren().indexOf(button);
                if (!dropBefore(button, e)) {
                    to++;
                }
                ok = moveChild(items.getChildren(), source, to);
                if (ok) {
                    persistOrder(layout);
                }
            }
            clearHints(items.getChildren(), ITEM_DROP);
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private static void installItemsPaneDnD(VBox items, String sectionId,
            Map<String, Button> byId, RailLayout layout) {
        items.setOnDragOver(e -> {
            if (itemSourceId(e, sectionId) != null) {
                e.acceptTransferModes(TransferMode.MOVE);
                clearHints(items.getChildren(), ITEM_DROP);
                e.consume();
            }
        });
        items.setOnDragDropped(e -> {
            boolean ok = false;
            String sourceId = itemSourceId(e, sectionId);
            Button source = sourceId == null ? null : byId.get(sourceId);
            if (source != null) {
                ok = moveChild(items.getChildren(), source, items.getChildren().size());
                if (ok) {
                    persistOrder(layout);
                }
            }
            clearHints(items.getChildren(), ITEM_DROP);
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private static void setDragPreview(Dragboard db, Node node, double x, double y) {
        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage image = node.snapshot(params, null);
            db.setDragView(image, Math.max(0, x), Math.max(0, y));
        } catch (RuntimeException ignored) {
        }
    }

    /** Move {@code from} to insertion index {@code to} (pre-remove index). */
    static <T> void moveInList(List<T> list, int from, int to) {
        if (from < 0 || from >= list.size()) {
            return;
        }
        T item = list.remove(from);
        if (from < to) {
            to--;
        }
        list.add(Math.max(0, Math.min(to, list.size())), item);
    }

    private static boolean moveChild(ObservableList<Node> children, Node node, int to) {
        int from = children.indexOf(node);
        if (from < 0) {
            return false;
        }
        children.remove(from);
        if (from < to) {
            to--;
        }
        children.add(Math.max(0, Math.min(to, children.size())), node);
        return true;
    }

    static void persistOrder(RailLayout layout) {
        StringBuilder sb = new StringBuilder();
        for (VBox card : layout.sectionOrder) {
            if (!(card.getUserData() instanceof String sectionId)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(sectionId).append(':');
            boolean first = true;
            for (Node child : itemsOf(card).getChildren()) {
                if (child.getUserData() instanceof String itemId) {
                    if (!first) {
                        sb.append(',');
                    }
                    sb.append(itemId);
                    first = false;
                }
            }
        }
        XPreferencesFactory.kShellToolsRailOrder.setValue(sb.toString());
    }

    private static VBox itemsOf(VBox card) {
        return (VBox) card.getChildren().get(1);
    }

    private static List<ToolSection> applySavedOrder(Map<String, ToolSection> byId,
            List<ToolSection> defaults) {
        String saved = XPreferencesFactory.kShellToolsRailOrder.getString();
        if (saved == null || saved.isBlank()) {
            return defaults;
        }

        Map<String, List<String>> itemOrder = new LinkedHashMap<>();
        List<String> sectionIds = new ArrayList<>();
        for (String part : saved.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            int colon = part.indexOf(':');
            String sectionId = colon >= 0 ? part.substring(0, colon) : part;
            if (!byId.containsKey(sectionId) || sectionIds.contains(sectionId)) {
                continue;
            }
            sectionIds.add(sectionId);
            List<String> ids = new ArrayList<>();
            if (colon >= 0 && colon + 1 < part.length()) {
                for (String itemId : part.substring(colon + 1).split(",")) {
                    if (!itemId.isBlank()) {
                        ids.add(itemId);
                    }
                }
            }
            itemOrder.put(sectionId, ids);
        }
        for (ToolSection s : defaults) {
            if (!sectionIds.contains(s.id())) {
                sectionIds.add(s.id());
            }
        }

        List<ToolSection> result = new ArrayList<>();
        for (String sectionId : sectionIds) {
            ToolSection def = byId.get(sectionId);
            if (def == null) {
                continue;
            }
            Map<String, ToolItem> remaining = new LinkedHashMap<>();
            for (ToolItem item : def.items()) {
                remaining.put(item.id(), item);
            }
            List<ToolItem> ordered = new ArrayList<>();
            for (String id : itemOrder.getOrDefault(sectionId, List.of())) {
                ToolItem item = remaining.remove(id);
                if (item != null) {
                    ordered.add(item);
                }
            }
            ordered.addAll(remaining.values());
            result.add(new ToolSection(def.id(), def.title(), ordered));
        }
        return result;
    }

    private static String itemSourceId(DragEvent e, String sectionId) {
        Object raw = e.getDragboard().getContent(ITEM_DND);
        if (!(raw instanceof String payload)) {
            return null;
        }
        String[] parts = payload.split("\n", 2);
        return parts.length == 2 && sectionId.equals(parts[0]) ? parts[1] : null;
    }

    private static boolean dropBefore(Node target, DragEvent e) {
        return e.getY() < target.getLayoutBounds().getHeight() / 2;
    }

    private static void setDropHint(Node node, String[] beforeAfter, boolean before) {
        clearHint(node, beforeAfter);
        node.getStyleClass().add(beforeAfter[before ? 0 : 1]);
    }

    private static void clearHint(Node node, String[] beforeAfter) {
        node.getStyleClass().removeAll(beforeAfter);
    }

    private static void clearHints(Iterable<? extends Node> nodes, String[] beforeAfter) {
        for (Node n : nodes) {
            clearHint(n, beforeAfter);
        }
    }
}
