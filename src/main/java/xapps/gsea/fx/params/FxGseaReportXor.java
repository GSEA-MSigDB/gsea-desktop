/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared GSEA report cache XOR folder-field resolution used by Leading Edge,
 * Enrichment Map, and CoreMap load UIs.
 */
public final class FxGseaReportXor {

    public static final String CONFLICT_MESSAGE =
            "Both cache and a browsed directory were specified. Only 1 can be specified. Delete one and try again";
    public static final String EMPTY_MESSAGE =
            "No GSEA result folder was specified. Specify one and try again";

    public enum Kind {
        EMPTY,
        CONFLICT,
        RESOLVED
    }

    public static final class SingleResult {
        public final Kind kind;
        public final File dir;

        private SingleResult(Kind kind, File dir) {
            this.kind = kind;
            this.dir = dir;
        }

        public static SingleResult empty() {
            return new SingleResult(Kind.EMPTY, null);
        }

        public static SingleResult conflict() {
            return new SingleResult(Kind.CONFLICT, null);
        }

        public static SingleResult resolved(File dir) {
            return new SingleResult(Kind.RESOLVED, dir);
        }
    }

    public static final class MultiResult {
        public final Kind kind;
        public final List<File> dirs;

        private MultiResult(Kind kind, List<File> dirs) {
            this.kind = kind;
            this.dirs = dirs != null ? List.copyOf(dirs) : List.of();
        }

        public static MultiResult empty() {
            return new MultiResult(Kind.EMPTY, List.of());
        }

        public static MultiResult conflict() {
            return new MultiResult(Kind.CONFLICT, List.of());
        }

        public static MultiResult resolved(List<File> dirs) {
            return new MultiResult(Kind.RESOLVED, dirs);
        }
    }

    private FxGseaReportXor() {
    }

    /** Prefer nested {@code edb/} when present (Leading Edge / CoreMap). */
    public static File edbPreferring(File reportOrEdb) {
        if (reportOrEdb == null) {
            return null;
        }
        File nested = new File(reportOrEdb, "edb");
        return nested.isDirectory() ? nested : reportOrEdb;
    }

    /**
     * Resolve a single GSEA result folder. Cache selections prefer {@code edbDir}
     * when available; typed cache paths and browsed folders use {@link #edbPreferring}.
     */
    public static SingleResult resolveSingle(FxReportCacheChooser chooser, String folderText,
            File rememberedDir) {
        boolean cacheSpecified = chooser != null && chooser.isSpecified();
        boolean dirSpecified = folderText != null && !folderText.isBlank();
        if (cacheSpecified && dirSpecified) {
            return SingleResult.conflict();
        }
        if (!cacheSpecified && !dirSpecified) {
            return SingleResult.empty();
        }
        if (cacheSpecified) {
            FxReportCacheSupport.CachedReport sel = chooser.getSelectedOne();
            if (sel != null && sel.edbDir != null) {
                return SingleResult.resolved(sel.edbDir);
            }
            List<File> dirs = chooser.getReportDirs();
            if (dirs.isEmpty()) {
                return SingleResult.empty();
            }
            return SingleResult.resolved(edbPreferring(dirs.get(0)));
        }
        String path = folderText.trim();
        File dir = rememberedDir != null && rememberedDir.getAbsolutePath().equals(path)
                ? rememberedDir
                : new File(path);
        return SingleResult.resolved(edbPreferring(dir));
    }

    /**
     * Resolve one or more report directories for Enrichment Map. Cache selections use
     * {@code reportDir} paths (not edb); typed folder text may be comma-separated.
     */
    public static MultiResult resolveMulti(FxReportCacheChooser chooser, String folderText) {
        boolean cacheSpecified = chooser != null && chooser.isSpecified();
        boolean dirSpecified = folderText != null && !folderText.isBlank();
        if (cacheSpecified && dirSpecified) {
            return MultiResult.conflict();
        }
        if (!cacheSpecified && !dirSpecified) {
            return MultiResult.empty();
        }
        List<File> out = new ArrayList<>();
        if (cacheSpecified) {
            List<FxReportCacheSupport.CachedReport> selected = chooser.getSelected();
            if (!selected.isEmpty()) {
                for (FxReportCacheSupport.CachedReport c : selected) {
                    if (c.reportDir != null) {
                        out.add(c.reportDir);
                    }
                }
            } else {
                out.addAll(chooser.getReportDirs());
            }
        } else {
            for (String part : folderText.trim().split(",")) {
                String p = part.trim();
                if (!p.isEmpty()) {
                    out.add(new File(p));
                }
            }
        }
        if (out.isEmpty()) {
            return MultiResult.empty();
        }
        return MultiResult.resolved(out);
    }
}
