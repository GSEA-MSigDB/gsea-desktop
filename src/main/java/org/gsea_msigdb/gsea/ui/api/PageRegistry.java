/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Lazy page factory registry (singleton-per-id by default).
 */
public final class PageRegistry {

    @FunctionalInterface
    public interface PageFactory {
        ViewPage create();
    }

    private final ConcurrentHashMap<PageId, PageFactory> factories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<PageId, ViewPage> singletons = new ConcurrentHashMap<>();

    public void register(PageId id, PageFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        factories.put(id, factory);
    }

    public void registerSingleton(PageId id, Supplier<ViewPage> factory) {
        register(id, () -> singletons.computeIfAbsent(id, k -> factory.get()));
    }

    public boolean isRegistered(PageId id) {
        return factories.containsKey(id);
    }

    public ViewPage get(PageId id) {
        PageFactory factory = factories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("No page registered for " + id);
        }
        return factory.create();
    }

    /** Returns the singleton instance if already created; does not invoke the factory. */
    public ViewPage peekSingleton(PageId id) {
        return singletons.get(id);
    }

    public ViewPage getSingleton(PageId id) {
        return get(id);
    }
}
