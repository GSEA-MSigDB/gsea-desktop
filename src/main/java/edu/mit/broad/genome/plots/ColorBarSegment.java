/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.alg.distrib.RangeFactory;
import edu.mit.broad.genome.math.ColorSchemes;
import edu.mit.broad.genome.math.ColorSchemes.ColorScheme;
import edu.mit.broad.genome.math.Range;
import edu.mit.broad.genome.math.ScoreMode;
import edu.mit.broad.genome.objects.ColorDataset;
import edu.mit.broad.genome.objects.RankedList;

/** Color-bar segment for enrichment mountain plots. */
public final class ColorBarSegment {

    public final double start;
    public final double end;
    public final int argb;

    public ColorBarSegment(double start, double end, int argb) {
        this.start = start;
        this.end = end;
        this.argb = argb;
    }

    public Color paint() {
        return new Color(argb, true);
    }

    /** CSS {@code #rrggbb} without allocating an AWT {@link Color}. */
    public String toCssRgb() {
        return String.format("#%02x%02x%02x", (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    private static final ColorScheme RED = new ColorSchemes.BroadCancerRed();
    private static final ColorScheme BLUE = new ColorSchemes.BroadCancerBlue();

    /** Default positive/negative color-bar bin count used in enrichment mountains. */
    public static final int DEFAULT_RANGES = 100;

    public static ColorBarSegment[] forRankedList(final RankedList rl) {
        int numRanges = (rl.getSize() < DEFAULT_RANGES) ? rl.getSize() : DEFAULT_RANGES;
        return forRankedList(numRanges, rl);
    }

    public static ColorBarSegment[] forRankedList(final int numRanges, final RankedList rl) {
        List<ColorBarSegment> list = new ArrayList<>();
        RankedList rl_pos = rl.extractRanked(ScoreMode.POS_ONLY);
        Range[] ranges_on_full_list = RangeFactory.createRanges(numRanges, 0, rl_pos.getSize());
        list.addAll(Arrays.asList(segments(numRanges, rl_pos, ranges_on_full_list, 0, RED)));
        RankedList rl_neg = rl.extractRanked(ScoreMode.NEG_ONLY);
        ranges_on_full_list = RangeFactory.createRanges(numRanges, rl_pos.getSize(), rl.getSize());
        list.addAll(Arrays.asList(segments(numRanges, rl_neg, ranges_on_full_list, rl_pos.getSize() + 1, BLUE)));
        return list.toArray(new ColorBarSegment[0]);
    }

    private static ColorBarSegment[] segments(final int numRanges, final RankedList rl, final Range[] rangesForMarkers,
            final int startX, final ColorScheme cs) {
        final ColorDataset cds = new DatasetGenerators().createColorDataset(numRanges, rl, cs);
        final ColorBarSegment[] markers = new ColorBarSegment[numRanges];
        double prev_start = startX;
        for (int c = 0; c < rangesForMarkers.length; c++) {
            Color paint = cds.getColor(0, c);
            markers[c] = new ColorBarSegment(prev_start, rangesForMarkers[c].getMax(), paint.getRGB());
            prev_start = rangesForMarkers[c].getMin();
        }
        return markers;
    }
}
