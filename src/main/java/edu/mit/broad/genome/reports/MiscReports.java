/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.alg.*;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.reports.pages.HtmlFormat;
import edu.mit.broad.genome.reports.pages.HtmlPage;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;

import java.io.File;
import java.util.*;

import org.apache.ecs.StringElement;
import org.apache.ecs.html.Div;
import org.genepattern.heatmap.image.HeatMap;

/**
 * More packaged than the base chart things
 * Canned Reports
 */
public class MiscReports {

    /**
     * Privatized constructor
     */
    private MiscReports() {
    }

    public static HtmlPage createDatasetHeatMapAndCorrelationPlots(final Dataset fullDs_opt,
                                                                   final Template template,
                                                                   final RankedList rl,
                                                                   final int topBotXGenes,
                                                                   final File saveInDir,
                                                                   final boolean createSvgs,
                                                                   final boolean createGcts) {

        HtmlPage htmlPage = new HtmlPage("heat_map_corr_plot", "Heat map and correlation plot for " + rl.getName());

        htmlPage.addBreak();
        htmlPage.addHtml("&nbsp&nbsp");
        
        if (fullDs_opt != null) {
            try {
                List<String> useNames = rl.getNamesOfUpOrDnXRanks(topBotXGenes, true);
                useNames.addAll(rl.getNamesOfUpOrDnXRanks(topBotXGenes, false));
                Dataset ds = new DatasetGenerators().extractRows(fullDs_opt, useNames);
                HeatMap heatMap = new GramImagerImpl().createBpogHeatMap(ds, template);
                htmlPage.addHeatMap("heat_map", "Heat Map of the top " + topBotXGenes + " features for each phenotype in " + fullDs_opt.getName(), heatMap, saveInDir, createSvgs);

                if (createGcts) {
                    GctParser gctExporter = new GctParser();
                    File heatMapCorrGctFilename = new File(saveInDir, "heat_map_Top_" + topBotXGenes + "_Features.gct");
                    gctExporter.export(ds, heatMapCorrGctFilename);

                    StringElement gctLink = HtmlFormat.Links.hyper("GCT file  ", heatMapCorrGctFilename , " for the data backing the heatmap (for use in external visualization)", saveInDir);
                    Div div = new Div();
                    htmlPage.addBlock(div, true);
                    div.addElement(gctLink);
                }

            } catch (Throwable t) {
                htmlPage.addError("Trouble making heat map", t);
                t.printStackTrace();
            }
        }

        htmlPage.addBreak();
        htmlPage.addHtml("&nbsp&nbsp");

        String classAName;
        String classBName;

        if (template == null) {
            classAName = "classA";
            classBName = "classB";
        } else {
            classAName = template.getClassName(0);
            classBName = template.getClassName(1);
        }

        XChart xc = RankedListCharts.createRankedListChart(rl, classAName, classBName, true);
        xc.getFreeChart().setBackgroundPaint(EnrichmentReports.CHART_FRAME_COLOR);
        htmlPage.addChart(xc, 500, 400, saveInDir, createSvgs);

        htmlPage.addBreak();
        return htmlPage;
    }

    public static StringDataframe createRankOrderedGeneList(final String name, final RankedList rl, final FeatureAnnot fann_opt) {
        final String[] colnames = new String[]{"TITLE", "SCORE"};
        StringMatrix sm = new StringMatrix(rl.getSize(), colnames.length);
        for (int r = 0; r < rl.getSize(); r++) {
            int coln = 0;
            String probeName = rl.getRankName(r);

            if (fann_opt != null) {
                String title = fann_opt.getNativeDesc(probeName);
                if (title == null) title = "";
				sm.setElement(r, coln++, title);
            } else {
                sm.setElement(r, coln++, "");
            }

            sm.setElement(r, coln, Float.toString(rl.getScore(r)));
        }

        return new StringDataframe(name, sm, rl.getRankedNames(), colnames);
    }
}