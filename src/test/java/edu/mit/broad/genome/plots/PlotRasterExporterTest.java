/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.reports.EnrichmentReports;

class PlotRasterExporterTest {

    @TempDir
    File tmp;

    @Test
    void rendersHistogramAndDualWritesJson() throws Exception {
        HistogramPlotSpec hist = PlotBuilders.histogram(
                "test_hist", "Histogram", "caption",
                "x", "y",
                new double[]{1, 3, 2, 5},
                new String[]{"a", "b", "c", "d"},
                new int[]{0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF0D9488},
                1);
        BufferedImage img = PlotRasterExporter.render(hist, 320, 200);
        assertEquals(320, img.getWidth());
        assertEquals(200, img.getHeight());

        File png = new File(tmp, "hist.png");
        PlotRasterExporter.savePng(hist, png, 320, 200);
        assertTrue(png.isFile());
        File json = new File(tmp, "hist.plot.json");
        assertTrue(json.isFile());
        String body = Files.readString(json.toPath());
        assertTrue(body.contains("\"type\":\"histogram\""));
        assertTrue(body.contains("test_hist"));
    }

    @Test
    void rendersXyAndBubble() {
        XyPlotSpec xy = XyPlotSpec.builder("xy", "Scatter")
                .xLabel("x").yLabel("y")
                .addSeries("s", new double[]{0, 1, 2}, new double[]{0, 1, 0}, true, true, 0xFF0D9488)
                .build();
        assertNotNull(PlotRasterExporter.render(xy, 200, 150));

        BubblePlotSpec bubble = new BubblePlotSpec("bub", "Bubbles", "", "x", "y",
                new BubblePlotSpec.Point[]{
                        new BubblePlotSpec.Point(1, 0, 8, 0xFFFF0000, "GeneSetA"),
                        new BubblePlotSpec.Point(2, 1, 12, 0xFF0000FF, "GeneSetB")
                },
                180, 3.0);
        BufferedImage img = PlotRasterExporter.render(bubble, 500, 200);
        assertTrue(hasNonWhiteInLeftBand(img, 160), "bubble labels should paint in left margin");
    }

    @Test
    void bubbleAllowsNegativeXAndAbsoluteDiameters() {
        BubblePlotSpec bubble = new BubblePlotSpec("ss", "ssGSEA", "", "score", "sets",
                new BubblePlotSpec.Point[]{
                        new BubblePlotSpec.Point(-2.0, 1, 20, 0xFFFF0000, "PosLike"),
                        new BubblePlotSpec.Point(1.5, 0, 10, 0xFF0000FF, "NegLike")
                },
                200, 2.5,
                BubblePlotSpec.Options.ssgsea(-2.5));
        BufferedImage img = PlotRasterExporter.render(bubble, 640, 360);
        assertNotNull(img);
        // Negative domain should still paint bubbles (not clipped off-canvas).
        assertTrue(hasNearColor(img, Color.RED, 80) || hasNearColor(img, new Color(255, 0, 0), 100));
    }

    @Test
    void gseaBubbleDrawsThresholdMarkersAndLegends() {
        BubblePlotSpec.Options opt = BubblePlotSpec.Options.gsea(true, 2.0, 0,
                new double[]{-Math.log10(0.25), -Math.log10(0.05)},
                new String[]{"NOM p = 0.25", "NOM p = 0.05"});
        BubblePlotSpec bubble = new BubblePlotSpec("gsea", "Bubble plot", "",
                "-log10(NOM p-value)", "Gene set",
                new BubblePlotSpec.Point[]{
                        new BubblePlotSpec.Point(2.0, 0, 16, 0xFFFF1414, "SET_A", "***")
                },
                260, 2.5, opt);
        BufferedImage img = PlotRasterExporter.render(bubble, 900, 500);
        assertTrue(hasNearColor(img, new Color(255, 20, 20), 40), "FDR-colored bubble");
        // Legend heading ink
        assertTrue(hasNonWhiteInLeftBand(img, 250), "gene-set label");
    }

    @Test
    void nesVsSignificanceHasYellowBand() {
        XyPlotSpec xy = XyPlotSpec.builder("pvalues_vs_nes_plot", "NES vs. Significance")
                .xLabel("NES").yLabel("FDR q-value").y2Label("Nominal P-value")
                .yBand(0, 0.25, 0xFFFFFFD2)
                .legend(true).legendBottom(true).marginR(88)
                .addSeries("FDR q-value", new double[]{-1, 0, 1}, new double[]{0.4, 0.1, 0.2}, false, true, 0xFFFF00FF, 0)
                .addSeries("nominal p-value", new double[]{-1, 0, 1}, new double[]{0.3, 0.05, 0.2}, false, true, 0xFF000000, 1)
                .build();
        BufferedImage img = PlotRasterExporter.render(xy, 500, 500);
        assertTrue(hasNearColor(img, new Color(255, 255, 210), 30), "expected FDR<=0.25 yellow band");
    }

    @Test
    void histogramMarkerPaintsNonBackground() {
        HistogramPlotSpec hist = PlotBuilders.histogram(
                "null_es", "Random ES", "Observed ES=0.5",
                "ES", "P(ES)",
                new double[]{1, 4, 2, 1},
                new String[]{"-1", "-0.5", "0", "0.5"},
                new int[]{0xFF0D9488, 0xFF0D9488, 0xFF0D9488, 0xFF0D9488},
                -1, 2.5);
        BufferedImage img = PlotRasterExporter.render(hist, 320, 200);
        assertTrue(hist.hasMarker());
        assertTrue(hasNearColor(img, new Color(0xDC, 0x26, 0x26), 40), "expected red marker line");
    }

    @Test
    void mountainV2UsesTealAndColorBarSegments() {
        MountainPlotSpec mountain = sampleMountain(MountainPlotSpec.Style.V2, EnrichmentReports.ENPLOT2_ + "SET");
        BufferedImage img = PlotRasterExporter.render(mountain, 640, 720);
        assertTrue(hasNearColor(img, new Color(0x0D, 0x94, 0x88), 50), "v2 ES curve should be teal");
        assertTrue(hasNearColor(img, Color.RED, 60) || hasNearColor(img, new Color(255, 0, 0), 80),
                "expected red color-bar segment");
        assertTrue(hasNearColor(img, Color.BLUE, 60) || hasNearColor(img, new Color(0, 0, 255), 80),
                "expected blue color-bar segment");
        assertTrue(mountain.name().startsWith(EnrichmentReports.ENPLOT2_));
        assertEquals(MountainPlotSpec.Style.V2, mountain.style());
    }

    @Test
    void mountainClassicUsesGreenEsNotTeal() {
        MountainPlotSpec mountain = sampleMountain(MountainPlotSpec.Style.CLASSIC, EnrichmentReports.ENPLOT_ + "SET");
        BufferedImage img = PlotRasterExporter.render(mountain, 640, 720);
        assertTrue(hasNearColor(img, Color.GREEN, 55), "classic ES curve should be Color.GREEN");
        assertFalse(hasNearColor(img, new Color(0x0D, 0x94, 0x88), 25),
                "classic ES should not use v2 teal");
        assertTrue(mountain.name().startsWith(EnrichmentReports.ENPLOT_));
        assertFalse(mountain.name().startsWith(EnrichmentReports.ENPLOT2_));
        assertEquals(MountainPlotSpec.Style.CLASSIC, mountain.style());
        assertNotNull(img);
    }

    @Test
    void mountainDualWritesJson() throws Exception {
        MountainPlotSpec mountain = sampleMountain(MountainPlotSpec.Style.V2, EnrichmentReports.ENPLOT2_ + "G");
        File png = new File(tmp, "enplot2_G.png");
        PlotRasterExporter.savePng(mountain, png, 400, 500);
        File json = new File(tmp, "enplot2_G.plot.json");
        assertTrue(json.isFile());
        String body = Files.readString(json.toPath());
        assertTrue(body.contains("\"type\":\"mountain\""));
        assertTrue(body.contains("\"style\":\"V2\""));
    }

    @Test
    void hitRanksFromMembership() {
        Vector membership = new Vector(new float[]{0, 1, 0, 1, 1});
        int[] hits = PlotBuilders.hitRanksFromMembership(membership);
        assertEquals(3, hits.length);
        assertEquals(1, hits[0]);
        assertEquals(3, hits[1]);
        assertEquals(4, hits[2]);
        assertEquals(0, PlotBuilders.hitRanksFromMembership(null).length);
    }

    private static MountainPlotSpec sampleMountain(MountainPlotSpec.Style style, String name) {
        ColorBarSegment[] segs = new ColorBarSegment[]{
                new ColorBarSegment(0, 50, 0xFFFF0000),
                new ColorBarSegment(50, 100, 0xFF0000FF)
        };
        double[] esX = new double[100];
        double[] esY = new double[100];
        double[] metric = new double[100];
        for (int i = 0; i < 100; i++) {
            esX[i] = i;
            esY[i] = Math.sin(i / 10.0) * 0.5;
            metric[i] = 1.0 - i / 50.0;
        }
        int[] hits = new int[]{10, 20, 30, 70};
        boolean[] le = new boolean[]{true, true, true, false};
        return new MountainPlotSpec(
                name,
                "Enrichment plot: SET",
                style == MountainPlotSpec.Style.V2
                        ? "ES=0.5 NES=1.2 NOM pVal=0.05 FDR=0.01"
                        : "",
                style,
                100,
                esX, esY,
                hits, le,
                metric, "Signal2Noise",
                "Tumor", "Normal",
                segs,
                20, 0.5,
                0.5f, 1.2f, 0.05f, 0.01f,
                50);
    }

    private static boolean hasNonWhiteInLeftBand(BufferedImage img, int maxX) {
        for (int y = 40; y < img.getHeight() - 20; y++) {
            for (int x = 0; x < Math.min(maxX, img.getWidth()); x++) {
                int rgb = img.getRGB(x, y) & 0x00FFFFFF;
                if (rgb != 0xFFFFFF && rgb != 0x000000) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNearColor(BufferedImage img, Color target, int tol) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (Math.abs(r - target.getRed()) <= tol
                        && Math.abs(g - target.getGreen()) <= tol
                        && Math.abs(b - target.getBlue()) <= tol) {
                    return true;
                }
            }
        }
        return false;
    }
}
