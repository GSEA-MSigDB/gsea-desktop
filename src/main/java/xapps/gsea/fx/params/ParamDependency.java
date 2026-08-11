/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import xtools.api.param.Param;
import xtools.api.param.ParamSet;

/**
 * Declares cross-param rules enforced by the FX form before a run (CLI keys unchanged).
 */
public final class ParamDependency {

    @FunctionalInterface
    public interface Rule {
        /** Apply side effects / validation before run. */
        void apply(ParamSet paramSet) throws Exception;
    }

    private final String name;
    private final Rule rule;

    public ParamDependency(String name, Rule rule) {
        this.name = Objects.requireNonNull(name, "name");
        this.rule = Objects.requireNonNull(rule, "rule");
    }

    public String name() {
        return name;
    }

    public void apply(ParamSet paramSet) throws Exception {
        rule.apply(paramSet);
    }

    public static void applyAll(List<ParamDependency> deps, ParamSet paramSet) throws Exception {
        if (deps == null) {
            return;
        }
        for (ParamDependency dep : deps) {
            dep.apply(paramSet);
        }
    }

    /**
     * When {@code source} is specified, invoke {@code onSource} (e.g. set alternate delimiter).
     */
    public static ParamDependency whenSpecified(String name, String sourceParamName,
            Consumer<Param> onSource) {
        return new ParamDependency(name, paramSet -> {
            Param source = find(paramSet, sourceParamName);
            if (source != null && source.isSpecified() && onSource != null) {
                onSource.accept(source);
            }
        });
    }

    public static Param find(ParamSet paramSet, String name) {
        if (paramSet == null || name == null) {
            return null;
        }
        for (int i = 0; i < paramSet.getNumParams(); i++) {
            Param p = paramSet.getParam(i);
            if (p != null && name.equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

    public static List<ParamDependency> listOf(ParamDependency... deps) {
        List<ParamDependency> list = new ArrayList<>();
        if (deps != null) {
            for (ParamDependency d : deps) {
                if (d != null) {
                    list.add(d);
                }
            }
        }
        return list;
    }
}
