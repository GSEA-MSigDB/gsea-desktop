/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import java.io.File;
import java.io.IOException;

import edu.mit.broad.genome.charts.XChart;

/** {@link XChart} backed by a {@link PlotSpec} (no JFreeChart). */
public final class PlotChart implements XChart {

    private final PlotSpec spec;

    public PlotChart(PlotSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("PlotSpec cannot be null");
        }
        this.spec = spec;
    }

    public PlotSpec getSpec() {
        return spec;
    }

    @Override
    public String getName() {
        return spec.name();
    }

    @Override
    public String getTitle() {
        return spec.title();
    }

    @Override
    public String getCaption() {
        return spec.caption();
    }

    @Override
    public void saveAsPNG(File inFile, int width, int height) throws IOException {
        PlotRasterExporter.savePng(spec, inFile, width, height);
    }

    @Override
    public void saveAsSVG(File toFile, int width, int height) throws IOException {
        PlotRasterExporter.saveSvg(spec, toFile, width, height);
    }
}
