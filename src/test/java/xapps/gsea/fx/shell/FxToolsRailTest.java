/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Layout / preference smoke tests for {@link FxToolsRail}.
 */
public class FxToolsRailTest {

    @BeforeAll
    static void initFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException already) {
            latch.countDown();
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start");
        }
    }

    @Test
    void widthConstantsEnforceSingleColumnAtDefault() {
        assertTrue(FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX < FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX);
        // icon + gap + wrapped text + button pad = 32+8+64+14
        assertEquals(118, FxToolsRail.MIN_ITEM_WIDTH_PX);
        assertEquals(132, FxToolsRail.MIN_SECTION_WIDTH_PX);
        // section + rail + side tab = 132+16+24
        assertEquals(172, FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX);
        assertEquals(320, FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX);
        assertEquals(200, FxToolsRail.COLUMN_BREAK_WIDTH_PX);
        assertEquals(16, FxToolsRail.RAIL_INSETS_H);
        assertEquals(14, FxToolsRail.CARD_INSETS_H);
        assertEquals(10, FxToolsRail.COL_GAP);
        // Even if the viewport were as wide as the default tools column, stay at 1.
        assertEquals(1, FxToolsRail.columnsForViewportWidth(
                FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX, 8));
        assertEquals(1, FxToolsRail.columnsForViewportWidth(
                FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX, 8));
    }

    @Test
    void layoutKeepsStructuralMinsWhilePrefTracksViewport() throws Exception {
        AtomicReference<FxToolsRail.RailLayout> layout = new AtomicReference<>();
        AtomicReference<Double> widePref = new AtomicReference<>();
        runFx(() -> {
            ScrollPane scroll = (ScrollPane) FxToolsRail.build(sampleSections());
            FxToolsRail.RailLayout rail = (FxToolsRail.RailLayout) scroll.getUserData();
            layout.set(rail);
            FxToolsRail.applyLayout(rail, FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX);
            VBox card = rail.sectionOrder.get(0);
            Button button = (Button) findItems(card).getChildren().get(0);
            assertEquals(FxToolsRail.MIN_SECTION_WIDTH_PX, card.getMinWidth());
            assertEquals(FxToolsRail.MIN_ITEM_WIDTH_PX, button.getMinWidth());
            assertTrue(card.getPrefWidth() > FxToolsRail.MIN_SECTION_WIDTH_PX);
            assertEquals(card.getPrefWidth(), card.getMaxWidth());
            assertEquals(button.getPrefWidth(), button.getMaxWidth());
            widePref.set(card.getPrefWidth());
        });

        runFx(() -> {
            FxToolsRail.RailLayout rail = layout.get();
            // Still single-column, but tighter than default — pref must drop; min must not.
            FxToolsRail.applyLayout(rail, FxToolsRail.MIN_TOOLS_COLUMN_WIDTH_PX);
            VBox card = rail.sectionOrder.get(0);
            Button button = (Button) findItems(card).getChildren().get(0);
            assertEquals(FxToolsRail.MIN_SECTION_WIDTH_PX, card.getMinWidth());
            assertEquals(FxToolsRail.MIN_ITEM_WIDTH_PX, button.getMinWidth());
            assertTrue(card.getPrefWidth() < widePref.get());
            assertEquals(card.getPrefWidth(), card.getMaxWidth());
            assertEquals(button.getPrefWidth(), button.getMaxWidth());
        });
    }

    @Test
    void columnBreakNeedsNearlyTwoBudgets() {
        double twoColMin = FxToolsRail.viewportWidthForColumns(2);
        assertEquals(1, FxToolsRail.columnsForViewportWidth(twoColMin - 1, 8));
        assertEquals(2, FxToolsRail.columnsForViewportWidth(twoColMin, 8));
        assertEquals(3, FxToolsRail.columnsForViewportWidth(
                FxToolsRail.viewportWidthForColumns(3), 8));
    }

    @Test
    void usableWidthMatchesRailInsets() {
        assertEquals(0, FxToolsRail.usableWidth(0));
        assertEquals(0, FxToolsRail.usableWidth(FxToolsRail.RAIL_INSETS_H - 1));
        assertEquals(304, FxToolsRail.usableWidth(FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX));
    }

    @Test
    void defaultWidthStaysSingleColumn() throws Exception {
        AtomicReference<FxToolsRail.RailLayout> layout = new AtomicReference<>();
        runFx(() -> {
            ScrollPane scroll = (ScrollPane) FxToolsRail.build(sampleSections());
            layout.set((FxToolsRail.RailLayout) scroll.getUserData());
            FxToolsRail.applyLayout(layout.get(), FxToolsRail.DEFAULT_TOOLS_COLUMN_WIDTH_PX);
        });
        assertEquals(1, layout.get().columnCount);

        runFx(() -> FxToolsRail.applyLayout(layout.get(),
                FxToolsRail.viewportWidthForColumns(2) - 1));
        assertEquals(1, layout.get().columnCount);
    }

    @Test
    void expandingOpensIndependentColumns() throws Exception {
        AtomicReference<FxToolsRail.RailLayout> layout = new AtomicReference<>();
        runFx(() -> {
            ScrollPane scroll = (ScrollPane) FxToolsRail.build(sampleSections());
            layout.set((FxToolsRail.RailLayout) scroll.getUserData());
            FxToolsRail.applyLayout(layout.get(), FxToolsRail.viewportWidthForColumns(2));
        });
        assertEquals(2, layout.get().columnCount);
        assertEquals(2, layout.get().host.getChildren().size());
        assertEquals(1, ((VBox) layout.get().host.getChildren().get(0)).getChildren().size());
        assertEquals(1, ((VBox) layout.get().host.getChildren().get(1)).getChildren().size());
    }

    @Test
    void buildsRailStructure() throws Exception {
        AtomicReference<Node> root = new AtomicReference<>();
        runFx(() -> root.set(FxToolsRail.build(sampleSections())));
        assertInstanceOf(ScrollPane.class, root.get());
        ScrollPane scroll = (ScrollPane) root.get();
        assertInstanceOf(HBox.class, scroll.getContent());
        assertEquals(ScrollPane.ScrollBarPolicy.AS_NEEDED, scroll.getVbarPolicy());
        FxToolsRail.RailLayout layout = (FxToolsRail.RailLayout) scroll.getUserData();
        assertTrue(layout.host.getStyleClass().contains("gsea-tools-rail"));
        assertEquals(2, layout.sectionOrder.size());
        assertTrue(findItems(layout.sectionOrder.get(0)).getChildren().get(0) instanceof Button);
    }

    @Test
    void restoresSavedSectionAndItemOrder() throws Exception {
        String previous = XPreferencesFactory.kShellToolsRailOrder.getString();
        try {
            XPreferencesFactory.kShellToolsRailOrder.setValue("b:b2,b1;a:a1");
            AtomicReference<FxToolsRail.RailLayout> layoutRef = new AtomicReference<>();
            runFx(() -> {
                ScrollPane scroll = (ScrollPane) FxToolsRail.build(sampleSections());
                layoutRef.set((FxToolsRail.RailLayout) scroll.getUserData());
            });
            FxToolsRail.RailLayout layout = layoutRef.get();
            assertEquals("b", layout.sectionOrder.get(0).getUserData());
            assertEquals("a", layout.sectionOrder.get(1).getUserData());
            VBox items = findItems(layout.sectionOrder.get(0));
            assertEquals("b2", items.getChildren().get(0).getUserData());
            assertEquals("b1", items.getChildren().get(1).getUserData());
        } finally {
            XPreferencesFactory.kShellToolsRailOrder.setValue(previous == null ? "" : previous);
        }
    }

    @Test
    void persistsSectionReorder() throws Exception {
        String previous = XPreferencesFactory.kShellToolsRailOrder.getString();
        try {
            XPreferencesFactory.kShellToolsRailOrder.setValue("");
            AtomicReference<FxToolsRail.RailLayout> layoutRef = new AtomicReference<>();
            runFx(() -> {
                ScrollPane scroll = (ScrollPane) FxToolsRail.build(sampleSections());
                FxToolsRail.RailLayout layout = (FxToolsRail.RailLayout) scroll.getUserData();
                layoutRef.set(layout);
                FxToolsRail.moveInList(layout.sectionOrder, 0, 2);
                FxToolsRail.applyLayout(layout, FxToolsRail.COLUMN_BREAK_WIDTH_PX);
                FxToolsRail.persistOrder(layout);
            });
            assertEquals("b", layoutRef.get().sectionOrder.get(0).getUserData());
            assertEquals("a", layoutRef.get().sectionOrder.get(1).getUserData());
            assertEquals("b:b1,b2;a:a1", XPreferencesFactory.kShellToolsRailOrder.getString());
        } finally {
            XPreferencesFactory.kShellToolsRailOrder.setValue(previous == null ? "" : previous);
        }
    }

    @Test
    void ignoresUnknownSavedIdsAndAppendsNewSections() throws Exception {
        String previous = XPreferencesFactory.kShellToolsRailOrder.getString();
        try {
            XPreferencesFactory.kShellToolsRailOrder.setValue("gone:x;b:b2,unknown,b1");
            AtomicReference<FxToolsRail.RailLayout> layoutRef = new AtomicReference<>();
            runFx(() -> {
                ScrollPane scroll = (ScrollPane) FxToolsRail.build(sampleSections());
                layoutRef.set((FxToolsRail.RailLayout) scroll.getUserData());
            });
            FxToolsRail.RailLayout layout = layoutRef.get();
            assertEquals("b", layout.sectionOrder.get(0).getUserData());
            assertEquals("a", layout.sectionOrder.get(1).getUserData());
            VBox items = findItems(layout.sectionOrder.get(0));
            assertEquals("b2", items.getChildren().get(0).getUserData());
            assertEquals("b1", items.getChildren().get(1).getUserData());
            assertEquals(2, items.getChildren().size());
        } finally {
            XPreferencesFactory.kShellToolsRailOrder.setValue(previous == null ? "" : previous);
        }
    }

    @Test
    void toolButtonsStillFireActions() throws Exception {
        AtomicInteger clicks = new AtomicInteger();
        runFx(() -> {
            ScrollPane scroll = (ScrollPane) FxToolsRail.build(List.of(
                    new FxToolsRail.ToolSection("a", "A", List.of(
                            new FxToolsRail.ToolItem("a1", "One", null, clicks::incrementAndGet)))));
            FxToolsRail.RailLayout layout = (FxToolsRail.RailLayout) scroll.getUserData();
            ((Button) findItems(layout.sectionOrder.get(0)).getChildren().get(0)).fire();
        });
        assertEquals(1, clicks.get());
    }

    private static VBox findItems(VBox card) {
        for (Node n : card.getChildren()) {
            if (n instanceof VBox vb && vb.getStyleClass().contains("gsea-tool-group-items")) {
                return vb;
            }
        }
        return null;
    }

    private static List<FxToolsRail.ToolSection> sampleSections() {
        return List.of(
                new FxToolsRail.ToolSection("a", "A", List.of(
                        new FxToolsRail.ToolItem("a1", "One", null, () -> {
                        }))),
                new FxToolsRail.ToolSection("b", "B", List.of(
                        new FxToolsRail.ToolItem("b1", "First", null, () -> {
                        }),
                        new FxToolsRail.ToolItem("b2", "Second", null, () -> {
                        }))));
    }

    private static void runFx(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX action timed out");
        }
        if (error.get() != null) {
            if (error.get() instanceof Exception ex) {
                throw ex;
            }
            if (error.get() instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(error.get());
        }
    }
}
