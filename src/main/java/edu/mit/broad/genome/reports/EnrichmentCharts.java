/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.charts.XComboChart;

/**
 * @author Aravind Subramanian
 */
public class EnrichmentCharts {

    public XChart esProfileChart;
    public XChart hitProfileChart;
    public XChart hitProfileWithBgChart;
    public XChart rlProfileChart;

    public XComboChart comboChart;

    protected EnrichmentCharts(XChart esProfileChart,
                               XChart hitProfileChart,
                               XChart hitProfileWithBgChart,
                               XChart rlProfileChart,
                               XComboChart comboChart
    ) {

        this.esProfileChart = esProfileChart;
        this.hitProfileChart = hitProfileChart;
        this.hitProfileWithBgChart = hitProfileWithBgChart;
        this.rlProfileChart = rlProfileChart;
        this.comboChart = comboChart;
    }

}
