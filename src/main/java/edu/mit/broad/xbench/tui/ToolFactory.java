/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import java.lang.reflect.Constructor;
import java.util.Properties;

import xtools.api.Tool;

/**
 * Constructs tool instances for launch and relaunch.
 */
public final class ToolFactory {

    private ToolFactory() {
    }

    /**
     * Clone a tool via its {@code Properties} constructor. Copies and strips launch-only
     * keys; does not mutate {@code params}.
     */
    public static Tool createTool(final String toolClassName, final Properties params) throws Exception {
        Class<?> toolClass = Class.forName(toolClassName);
        Constructor<?> ctor = toolClass.getConstructor(Properties.class);
        Properties prp = snapshotForCtor(params);
        return (Tool) ctor.newInstance(prp);
    }

    public static Tool createTool(final String toolClassName) throws Exception {
        Class<?> toolClass = Class.forName(toolClassName);
        return (Tool) toolClass.getConstructor().newInstance();
    }

    /** Copy + strip launch-only keys; does not mutate the caller's {@link Properties}. */
    public static Properties snapshotForCtor(Properties src) {
        Properties prp = new Properties();
        if (src != null) {
            prp.putAll(src);
        }
        prp.remove("help");
        return prp;
    }
}
