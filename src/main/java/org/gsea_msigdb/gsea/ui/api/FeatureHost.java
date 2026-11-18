/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import java.util.Objects;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * Navigation handle for feature panes. Dialogs, files, jobs, and the cache
 * come from {@link #services()} rather than a second set of forwarding methods.
 */
public final class FeatureHost {

    private final AppServices services;
    private final PageRegistry pages;
    private final Consumer<ViewPage> openPage;

    public FeatureHost(AppServices services, PageRegistry pages, Consumer<ViewPage> openPage) {
        this.services = Objects.requireNonNull(services, "services");
        this.pages = Objects.requireNonNull(pages, "pages");
        this.openPage = Objects.requireNonNull(openPage, "openPage");
    }

    public AppServices services() {
        return services;
    }

    public PageRegistry pages() {
        return pages;
    }

    public void openPage(ViewPage page) {
        openPage.accept(page);
    }
}
