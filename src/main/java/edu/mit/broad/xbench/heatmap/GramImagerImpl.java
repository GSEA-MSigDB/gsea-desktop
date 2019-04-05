/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.heatmap;

import edu.mit.broad.genome.objects.*;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.image.DisplaySettings;
import org.genepattern.heatmap.image.HeatMap;

/**
 * @author Aravind Subramanian
 */
public class GramImagerImpl implements GramImager {

    private DisplayState fDisplayState;

    public GramImagerImpl() {
        this(null);
    }

    public GramImagerImpl(final DisplayState state) {
        if (state == null) {
            this.fDisplayState = new DisplayState(); // default
        } else {
            this.fDisplayState = state;
        }

    }

    public HeatMap createBpogHeatMap(final Dataset ds) {
        return _xcoreCreateBpogImage(ds, null);
    }

    public HeatMap createBpogHeatMap(final Dataset ds, final Template t) {
        return _xcoreCreateBpogImage(ds, t);
    }

    private DisplaySettings _createDefaultDisplaySettings(final Dataset origDs) {
        final DisplaySettings set = new DisplaySettings();

        set.drawGrid = true;
        set.rowSize = DisplayState.DEFAULT_CELL_HEIGHT;
        set.columnSize = DisplayState.DEFAULT_CELL_HEIGHT;

        set.sampleAnnonationsHeight = 15;
        set.drawGrid = true; // i prefer this for gsea
        set.showFeatureGridLines = true; // i prefer this for gsea
        set.showSampleGridLines = true; // i prefer this for gsea

        set.drawRowDescriptions = origDs.getAnnot().getFeatureAnnot().hasNativeDescriptions();

        set.drawRowNames = true;

        if (fDisplayState != null && fDisplayState.getColorScheme() != null) {
            set.colorConverter = fDisplayState.getColorScheme();
        }

        return set;
    }

    private HeatMap _xcoreCreateBpogImage(final Dataset origDs,
                                                final Template origT_opt) {
        if (origDs == null) {
            throw new IllegalArgumentException("Param origDs cannot be null");
        }

        DisplaySettings displaySettings_opt = _createDefaultDisplaySettings(origDs);

        final IExpressionData data = GPWrappers.createIExpressionData(origDs);

        return HeatMap.createHeatMap(data, displaySettings_opt, GPWrappers.createFeatureAnnotator(origDs),
                GPWrappers.createSampleAnnotator(origDs, origT_opt));
    }
}