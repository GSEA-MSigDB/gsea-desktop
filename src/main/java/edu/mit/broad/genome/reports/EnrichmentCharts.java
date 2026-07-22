/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.charts.XChart;

/**
 * Enrichment mountain combo chart for a gene set (PlotSpec-backed {@link XChart}).
 */
public class EnrichmentCharts {

    public final XChart comboChart;

    public EnrichmentCharts(XChart comboChart) {
        this.comboChart = comboChart;
    }
}
