/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.reports;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.plots.BubblePlotSpec;
import edu.mit.broad.genome.plots.PlotChart;
import edu.mit.broad.genome.plots.XyPlotSpec;

/**
 * Per-sample bubble plot for ssGSEA scores.
 */
public final class SsGseaBubblePlot {

    private static final int MAX_DEFAULT_TOP = 200;

    private SsGseaBubblePlot() {
    }

    public static XChart createPerSampleChart(final String sampleLabel, final List<String> geneSetNames,
                                              final float[] scores, final int topN) {
        if (geneSetNames == null || scores == null || geneSetNames.size() != scores.length) {
            return emptyChart(sampleLabel, "ssGSEA bubble plot (invalid input)");
        }

        final List<Integer> finite = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            if (Float.isFinite(scores[i])) {
                finite.add(i);
            }
        }
        if (finite.isEmpty()) {
            return emptyChart(sampleLabel, "ssGSEA bubble plot (no finite scores)");
        }

        Collections.sort(finite, Comparator.comparing((Integer a) -> Math.abs(scores[a])).reversed());

        final int cap = topN > 0 ? topN : MAX_DEFAULT_TOP;
        final int n = Math.min(cap, finite.size());
        double maxAbs = 1e-12d;
        int maxLabelLen = 8;
        for (int i = 0; i < n; i++) {
            maxAbs = Math.max(maxAbs, Math.abs(scores[finite.get(i)]));
            String name = geneSetNames.get(finite.get(i));
            if (name != null) {
                maxLabelLen = Math.max(maxLabelLen, name.length());
            }
        }
        List<BubblePlotSpec.Point> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int ix = finite.get(i);
            float s = scores[ix];
            double t = Math.min(1.0d, Math.abs(s) / maxAbs);
            double sizePx = 8.0d + 14.0d * t;
            int argb = paintForSignedScore(s, maxAbs).getRGB();
            points.add(new BubblePlotSpec.Point(s, (n - 1) - i, sizePx, argb, geneSetNames.get(ix)));
        }
        double pad = Math.max(0.08 * maxAbs, 1e-6);
        double xMin = -maxAbs - pad;
        double xMax = maxAbs + pad;
        int leftMargin = (int) Math.min(760, Math.max(260, maxLabelLen * 8.2 + 70));
        String safeName = fileSafeName(sampleLabel);
        return new PlotChart(new BubblePlotSpec(
                "ssgsea_bubble_" + safeName,
                "ssGSEA bubble plot: " + sampleLabel,
                "Top " + n + " gene sets by |score|",
                "ssGSEA enrichment score",
                "Gene sets (ranked)",
                points.toArray(new BubblePlotSpec.Point[0]),
                leftMargin,
                xMax,
                BubblePlotSpec.Options.ssgsea(xMin)));
    }

    /** Historic JFree shading: white→red / white→blue with channel floor at 40. */
    private static Color paintForSignedScore(float score, double maxAbs) {
        float tNorm = (float) Math.min(1.0, Math.abs(score) / Math.max(maxAbs, 1e-12));
        int c = (int) (220 - 175 * tNorm);
        c = Math.max(40, c);
        if (score >= 0) {
            return new Color(255, c, c);
        }
        return new Color(c, c, 255);
    }

    private static XChart emptyChart(String sampleLabel, String caption) {
        return new PlotChart(XyPlotSpec.builder("ssgsea_bubble_" + fileSafeName(sampleLabel), caption)
                .xLabel("ssGSEA enrichment score")
                .yLabel("Gene sets")
                .caption(caption)
                .build());
    }

    private static String fileSafeName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "sample";
        }
        String t = raw.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        return t.length() > 80 ? t.substring(0, 80) : t;
    }
}
