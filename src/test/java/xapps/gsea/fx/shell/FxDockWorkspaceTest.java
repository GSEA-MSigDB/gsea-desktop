/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Smoke tests for document open / reselect identity in {@link FxDockWorkspace}.
 */
public class FxDockWorkspaceTest {

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
    void openPageReselectsSameViewPageInstance() throws Exception {
        AtomicReference<AssertionError> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FxDockWorkspace workspace = new FxDockWorkspace();
                workspace.buildLayout(new Region(), new Region(), new Region());
                ViewPage page = stubPage("Doc A");
                workspace.openPage(page);
                DockContainerLeaf docs = workspace.getDocumentLeaf();
                assertEquals(1, docs.getDockables().size());
                Dockable first = docs.getSelectedDockable();
                workspace.openOrReselect(page);
                assertEquals(1, docs.getDockables().size());
                assertSame(first, docs.getSelectedDockable());

                ViewPage other = stubPage("Doc B");
                workspace.openPage(other);
                assertEquals(2, docs.getDockables().size());
                assertNotSame(first, docs.getSelectedDockable());
                workspace.openOrReselect(page);
                assertSame(first, docs.getSelectedDockable());
            } catch (AssertionError e) {
                failure.set(e);
            } catch (Throwable t) {
                failure.set(new AssertionError(t));
            } finally {
                done.countDown();
            }
        });
        if (!done.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX test timed out");
        }
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    void leftColumnNotResizableWithParent() throws Exception {
        AtomicReference<AssertionError> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FxDockWorkspace workspace = new FxDockWorkspace();
                workspace.buildLayout(new Region(), new Region(), new Region());
                assertEquals(Boolean.FALSE, javafx.scene.control.SplitPane.isResizableWithParent(workspace.getLeftColumn()));
            } catch (AssertionError e) {
                failure.set(e);
            } catch (Throwable t) {
                failure.set(new AssertionError(t));
            } finally {
                done.countDown();
            }
        });
        if (!done.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX test timed out");
        }
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    private static ViewPage stubPage(String title) {
        Label content = new Label(title);
        return new ViewPage() {
            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getIconResourceId() {
                return null;
            }

            @Override
            public javafx.scene.Node getContent() {
                return content;
            }
        };
    }
}
