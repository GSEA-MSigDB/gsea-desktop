/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableDragDropBehavior;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import xapps.gsea.fx.viewers.FxHomePane;

/**
 * BentoFX-backed workspace: IDE-style in-app docking for tool panels and {@link ViewPage} documents.
 * Does not float dockables into separate OS windows.
 */
public final class FxDockWorkspace {

    /** Document tabs (Home, tools, reports, viewers). */
    public static final int GROUP_DOCUMENTS = 1;
    /** Tools rail, Jobs, Application messages. */
    public static final int GROUP_TOOLS = 2;

    private static final int DEFAULT_MESSAGES_HEIGHT_PX = 180;
    private static final String ID_MESSAGES = "gsea-messages";

    private final Bento bento = new Bento() {
        @Override
        protected DockableDragDropBehavior newDragDropBehavior() {
            // Center drops: same group only (tools stay with tools, documents with documents).
            // Corner/edge drops: allow splitting any leaf so users can create new subpanels.
            return new DockableDragDropBehavior() {
                @Override
                public boolean canReceiveDockable(DockContainerLeaf targetContainer,
                        Side targetSide, Dockable dockable) {
                    if (targetSide != null) {
                        return true;
                    }
                    return targetContainer.getDockables().stream()
                            .anyMatch(d -> d.getDragGroupMask() == dockable.getDragGroupMask());
                }
            };
        }

        @Override
        protected DockBuilding newDockBuilding() {
            Bento self = this;
            return new DockBuilding(self) {
                @Override
                public DockContainerLeaf leaf(String identifier) {
                    return new GseaDockContainerLeaf(self, identifier);
                }
            };
        }
    };
    private final Map<ViewPage, Dockable> pageDockables = new IdentityHashMap<>();

    private DockContainerRootBranch root;
    private DockContainerBranch leftColumn;
    private DockContainerLeaf toolsLeaf;
    private DockContainerLeaf jobsLeaf;
    private DockContainerLeaf documentLeaf;
    private DockContainerLeaf messagesLeaf;
    private Dockable messagesDockable;

    public FxDockWorkspace() {
        bento.placeholderBuilding().setDockablePlaceholderFactory(d -> emptyPane());
        bento.placeholderBuilding().setContainerPlaceholderFactory(c -> emptyPane());
        bento.events().addEventListener(event -> {
            if ((event instanceof DockEvent.DockableRemoved removed && isBottomShelf(removed.container()))
                    || (event instanceof DockEvent.DockableAdded added && isBottomShelf(added.container()))) {
                Platform.runLater(this::syncBottomShelf);
            }
        });
    }

    public DockContainerLeaf getDocumentLeaf() {
        return documentLeaf;
    }

    public DockContainerBranch getLeftColumn() {
        return leftColumn;
    }

    /**
     * Build the default IDE layout and return the scene-graph root.
     *
     * @param toolsContent left Tools rail content
     * @param jobsContent Jobs panel content
     * @param messagesContent Application messages content
     */
    public Node buildLayout(Node toolsContent, Node jobsContent, Node messagesContent) {
        DockBuilding builder = bento.dockBuilding();
        root = builder.root("gsea-dock");
        DockContainerBranch workspaceBranch = builder.branch("gsea-workspace");
        leftColumn = builder.branch("gsea-left");
        toolsLeaf = builder.leaf("gsea-tools");
        jobsLeaf = builder.leaf("gsea-jobs");
        documentLeaf = builder.leaf("gsea-documents");
        messagesLeaf = builder.leaf("gsea-messages");

        root.setOrientation(Orientation.VERTICAL);
        workspaceBranch.setOrientation(Orientation.HORIZONTAL);
        leftColumn.setOrientation(Orientation.VERTICAL);

        toolsLeaf.setSide(Side.LEFT);
        jobsLeaf.setSide(Side.LEFT);
        messagesLeaf.setSide(Side.BOTTOM);
        // Keep a durable center document area and full-width bottom shelf. Side tool panels
        // prune when emptied (recreate via corner splits); the bottom shelf cannot be
        // recreated that way because it lives on the root split, so it stays put.
        documentLeaf.setPruneWhenEmpty(false);
        messagesLeaf.setPruneWhenEmpty(false);

        root.addContainers(workspaceBranch, messagesLeaf);
        workspaceBranch.addContainers(leftColumn, documentLeaf);
        leftColumn.addContainers(toolsLeaf, jobsLeaf);

        int toolsWidth = clampPx(XPreferencesFactory.kShellToolsPanelWidth.getInt(),
                FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX, 600, FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX);
        int messagesHeight = clampPx(XPreferencesFactory.kShellMessagesPanelHeight.getInt(), 80, 500,
                DEFAULT_MESSAGES_HEIGHT_PX);
        int toolsSplit = clampPx(XPreferencesFactory.kShellLeftVerticalDivider.getInt(), 20, 80, 55);

        // SplitPane compress floor for the tools+jobs column (includes side-tab chrome).
        leftColumn.setMinWidth(FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX);
        SplitPane.setResizableWithParent(leftColumn, false);
        SplitPane.setResizableWithParent(messagesLeaf, false);
        root.setContainerSizePx(messagesLeaf, messagesHeight);
        workspaceBranch.setContainerSizePx(leftColumn, toolsWidth);
        leftColumn.setContainerSizePercent(toolsLeaf, toolsSplit / 100.0);

        Dockable tools = toolDockable("Tools", null, toolsContent);
        Dockable jobs = toolDockable("Jobs", "History16_v2.gif", jobsContent);
        messagesDockable = toolDockable("Application messages", "expandall.png", messagesContent);
        toolsLeaf.addDockable(tools);
        jobsLeaf.addDockable(jobs);
        messagesLeaf.addDockable(messagesDockable);
        toolsLeaf.selectDockable(tools);
        jobsLeaf.selectDockable(jobs);
        // Collapsed until the user opens Application messages (or expands the divider).
        root.setContainerCollapsed(messagesLeaf, true);

        bento.registerRoot(root);
        return root;
    }

    public void openPage(ViewPage page) {
        Objects.requireNonNull(page, "page");
        Dockable existing = pageDockables.get(page);
        if (existing != null && existing.getContainer() != null) {
            selectExisting(existing);
            return;
        }
        Dockable dockable = documentDockable(page);
        pageDockables.put(page, dockable);
        // Close (not drag-move) clears the registry entry.
        dockable.addCloseListener((path, d) -> pageDockables.remove(page, d));
        DockContainerLeaf target = preferredDocumentLeaf();
        target.addDockable(dockable);
        target.selectDockable(dockable);
    }

    public void openOrReselect(ViewPage page) {
        Objects.requireNonNull(page, "page");
        Dockable existing = pageDockables.get(page);
        if (existing != null && existing.getContainer() != null) {
            selectExisting(existing);
            return;
        }
        // Stale map entry (closed elsewhere) — drop and reopen.
        pageDockables.remove(page);
        openPage(page);
    }

    public void showMessages() {
        if (messagesDockable == null) {
            return;
        }
        ensureBottomShelfAttached();
        DockContainerLeaf leaf = messagesDockable.getContainer();
        if (leaf == null) {
            messagesLeaf.addDockable(messagesDockable);
            leaf = messagesLeaf;
        }
        if (leaf.isCollapsed() && leaf.getParentContainer() != null) {
            leaf.getParentContainer().setContainerCollapsed(leaf, false);
        }
        leaf.selectDockable(messagesDockable);
        if (!leaf.isFocusWithin()) {
            leaf.requestFocus();
        }
    }

    /** Persist tool / messages panel sizes into preferences. */
    public void savePanelSizes() {
        try {
            if (leftColumn != null && leftColumn.getWidth() > 1) {
                XPreferencesFactory.kShellToolsPanelWidth.setValue(
                        clampPx((int) Math.round(leftColumn.getWidth()),
                                FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX, 600,
                                FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX));
            }
            if (messagesLeaf != null && !messagesLeaf.isCollapsed()
                    && !messagesLeaf.getDockables().isEmpty()
                    && messagesLeaf.getHeight() > 80) {
                XPreferencesFactory.kShellMessagesPanelHeight.setValue(
                        clampPx((int) Math.round(messagesLeaf.getHeight()), 80, 500, DEFAULT_MESSAGES_HEIGHT_PX));
            }
            if (leftColumn != null && toolsLeaf != null && leftColumn.getHeight() > 1 && toolsLeaf.getHeight() > 1) {
                int pct = (int) Math.round(100.0 * toolsLeaf.getHeight() / leftColumn.getHeight());
                XPreferencesFactory.kShellLeftVerticalDivider.setValue(Math.max(20, Math.min(80, pct)));
            }
        } catch (Exception ignored) {
            // Preference writes are best-effort on quit.
        }
    }

    /**
     * Keep the root-level bottom shelf attached when empty (collapsed, no chrome).
     * Unlike side panels, it cannot be recreated by splitting a nested leaf.
     */
    private void syncBottomShelf() {
        if (messagesLeaf == null || root == null) {
            return;
        }
        ensureBottomShelfAttached();
        if (messagesLeaf.getDockables().isEmpty()) {
            if (!messagesLeaf.isCollapsed()) {
                root.setContainerCollapsed(messagesLeaf, true);
            }
            return;
        }
        if (!messagesLeaf.isCollapsed() && messagesLeaf.getHeight() < 80) {
            int messagesHeight = clampPx(XPreferencesFactory.kShellMessagesPanelHeight.getInt(), 80, 500,
                    DEFAULT_MESSAGES_HEIGHT_PX);
            root.setContainerSizePx(messagesLeaf, messagesHeight);
        }
    }

    private void ensureBottomShelfAttached() {
        if (root == null || messagesLeaf == null) {
            return;
        }
        if (messagesLeaf.getParentContainer() == root) {
            return;
        }
        if (messagesLeaf.getParentContainer() != null) {
            messagesLeaf.removeFromParent();
        }
        root.addContainers(messagesLeaf);
        messagesLeaf.setSide(Side.BOTTOM);
        messagesLeaf.setPruneWhenEmpty(false);
    }

    private boolean isBottomShelf(DockContainerLeaf leaf) {
        return leaf != null && ID_MESSAGES.equals(leaf.getIdentifier());
    }

    private DockContainerLeaf preferredDocumentLeaf() {
        // Prefer the leaf that currently holds a selected document, else the original center leaf.
        for (Dockable d : pageDockables.values()) {
            DockContainerLeaf leaf = d.getContainer();
            if (leaf != null && leaf.getSelectedDockable() == d) {
                return leaf;
            }
        }
        for (Dockable d : pageDockables.values()) {
            DockContainerLeaf leaf = d.getContainer();
            if (leaf != null) {
                return leaf;
            }
        }
        return documentLeaf;
    }

    private void selectExisting(Dockable dockable) {
        DockContainerLeaf leaf = dockable.getContainer();
        if (leaf == null) {
            return;
        }
        if (leaf.isCollapsed() && leaf.getParentContainer() != null) {
            leaf.getParentContainer().setContainerCollapsed(leaf, false);
        }
        leaf.selectDockable(dockable);
    }

    private Dockable documentDockable(ViewPage page) {
        Dockable dockable = bento.dockBuilding().dockable("page:" + UUID.randomUUID());
        dockable.setTitle(page.getTitle() != null ? page.getTitle() : "Untitled");
        dockable.setNode((Node) page.getContent());
        dockable.setDragGroupMask(GROUP_DOCUMENTS);
        dockable.setCanBeDroppedToNewWindow(false);
        dockable.setClosable(!(page instanceof FxHomePane));
        String iconId = page.getIconResourceId();
        if (iconId != null && !iconId.isBlank()) {
            dockable.setIconFactory(d -> iconNode(iconId, 16));
        }
        return dockable;
    }

    private Dockable toolDockable(String title, String iconResource, Node content) {
        Dockable dockable = bento.dockBuilding().dockable("tool:" + title.replace(' ', '_'));
        dockable.setTitle(title);
        dockable.setNode(content);
        dockable.setDragGroupMask(GROUP_TOOLS);
        dockable.setCanBeDroppedToNewWindow(false);
        dockable.setClosable(false);
        if (iconResource != null && !iconResource.isBlank()) {
            dockable.setIconFactory(d -> iconNode(iconResource, 16));
        }
        return dockable;
    }

    private static Node iconNode(String resourceId, int size) {
        try {
            var url = JarResources.toURL(resourceId);
            if (url != null) {
                return new ImageView(new Image(url.toExternalForm(), size, size, true, true));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Region emptyPane() {
        Region r = new Region();
        r.setMinSize(0, 0);
        return r;
    }

    private static int clampPx(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }
}
