/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import org.json.simple.JSONObject;

/**
 * Versioned, toolkit-agnostic plot description used by FX viewers and headless exporters.
 */
public interface PlotSpec {

    String type();

    int version();

    String name();

    String title();

    String caption();

    JSONObject toJson();
}
