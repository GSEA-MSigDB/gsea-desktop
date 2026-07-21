/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import java.lang.reflect.Constructor;
import java.util.Properties;

import xtools.api.Tool;
import xtools.api.param.ParamSet;

/**
 * Constructs tool instances for launch and relaunch.
 */
public final class ToolFactory {

    private ToolFactory() {
    }

    public static Tool createTool(final Tool tool, final ParamSet pset) throws Exception {
        Class<?> toolClass = Class.forName(tool.getClass().getName());
        Constructor<?> ctor = toolClass.getConstructor(Properties.class);
        Properties prp = pset.toProperties();
        prp.remove("help");
        return (Tool) ctor.newInstance(prp);
    }

    public static Tool createTool(final String toolClassName) throws Exception {
        Class<?> toolClass = Class.forName(toolClassName);
        return (Tool) toolClass.getConstructor().newInstance();
    }
}
