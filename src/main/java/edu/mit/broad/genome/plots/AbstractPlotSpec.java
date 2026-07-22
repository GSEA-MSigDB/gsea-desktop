/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import org.json.simple.JSONObject;

abstract class AbstractPlotSpec implements PlotSpec {

    private final String name;
    private final String title;
    private final String caption;
    private final int version;

    AbstractPlotSpec(String name, String title, String caption, int version) {
        this.name = name != null ? name : "plot";
        this.title = title != null ? title : "";
        this.caption = caption != null ? caption : "";
        this.version = version;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String caption() {
        return caption;
    }

    @SuppressWarnings("unchecked")
    JSONObject baseJson() {
        JSONObject o = new JSONObject();
        o.put("type", type());
        o.put("version", version());
        o.put("name", name());
        o.put("title", title());
        o.put("caption", caption());
        return o;
    }
}
