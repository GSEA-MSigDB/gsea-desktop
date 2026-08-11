/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.runtime.AppServices;
import org.gsea_msigdb.gsea.runtime.WorkspaceSession;

import edu.mit.broad.genome.parsers.ObjectCache;
import edu.mit.broad.xbench.core.api.FileManager;
import edu.mit.broad.xbench.core.api.ToolManager;
import edu.mit.broad.xbench.core.api.VdbManager;
import edu.mit.broad.xbench.core.api.WindowManager;
import javafx.concurrent.Task;
import javafx.stage.Window;
import xapps.gsea.fx.FxWorkers;
import xapps.gsea.fx.jobs.JobRuntime;

/**
 * Host API for feature panes: navigation, dialogs, services, background work.
 */
public final class FeatureHost implements Workspace {

    private final AppServices services;
    private final PageRegistry pages;
    private final Consumer<ViewPage> openPage;
    private final java.util.function.Supplier<Window> ownerWindow;

    public FeatureHost(AppServices services, PageRegistry pages, Consumer<ViewPage> openPage,
            java.util.function.Supplier<Window> ownerWindow) {
        this.services = Objects.requireNonNull(services, "services");
        this.pages = Objects.requireNonNull(pages, "pages");
        this.openPage = Objects.requireNonNull(openPage, "openPage");
        this.ownerWindow = ownerWindow != null ? ownerWindow : () -> null;
    }

    public AppServices services() {
        return services;
    }

    public PageRegistry pages() {
        return pages;
    }

    public JobRuntime jobs() {
        return services.jobs();
    }

    public WorkspaceSession session() {
        return services.session();
    }

    public ObjectCache cache() {
        return services.cache();
    }

    public WindowManager dialogs() {
        return services.dialogs();
    }

    public FileManager files() {
        return services.files();
    }

    public VdbManager vdb() {
        return services.vdb();
    }

    public ToolManager tools() {
        return services.tools();
    }

    public Window ownerWindow() {
        return ownerWindow.get();
    }

    public void openPage(PageId id) {
        openPage(pages.get(id));
    }

    @Override
    public void openPage(ViewPage page) {
        openPage.accept(page);
    }

    @Override
    public void showError(String message) {
        dialogs().showError(message);
    }

    @Override
    public void showError(String message, Throwable t) {
        dialogs().showError(message, t);
    }

    @Override
    public void showMessage(String title, String message) {
        dialogs().showMessage(title, message);
    }

    @Override
    public boolean showConfirm(String title, String message) {
        return dialogs().showConfirm(title, message);
    }

    /**
     * Run work off the FX thread; failures surface via dialogs.
     */
    public <T> void runBackground(String threadName, Callable<T> work, Consumer<T> onSuccess,
            String errorTitle) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            if (t != null) {
                dialogs().showError(errorTitle != null ? errorTitle : "Error", t);
            }
        });
        FxWorkers.start(task, threadName != null ? threadName : "gsea-feature");
    }
}
