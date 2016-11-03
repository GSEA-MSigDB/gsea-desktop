/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.heatmap;

import org.genepattern.heatmap.ColorScheme;

import java.awt.*;

public class DisplayState {

    public static final Color DEFAULT_HEADER_BG_COLOR = Color.WHITE;
    public static final Color DEFAULT_HEADER_FG_COLOR = Color.WHITE;
    public static final int DEFAULT_HEADER_HEIGHT = 150;
    public static final int DEFAULT_CELL_WIDTH = 150;
    public static final int DEFAULT_CELL_HEIGHT = 12;

    // seems to work better than others when coupled with anti-aiasing and when things are zoomed a lot
    public static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 10);
    public static final int MAX_CELL_HEIGHT = 50;

    public static final int MAX_FONT_SIZE = 10;

    public static final int MAX_HEADER_HEIGHT = 400;

    public static final Color DEFAULT_SELECTION_COLOR = Color.decode("#FFFF00"); // yellow

    protected int fRowHeight = DEFAULT_CELL_HEIGHT;
    protected int fHeaderHeight = DEFAULT_HEADER_HEIGHT;
    protected Color fHeaderFgColor = DEFAULT_HEADER_FG_COLOR;
    protected Color fHeaderBgColor = DEFAULT_HEADER_BG_COLOR;
    protected final Color fSelectionColor = DEFAULT_SELECTION_COLOR;

    protected Font fFont = DEFAULT_FONT;

    protected boolean fDisplayGrid = true;

    private ColorScheme fColorScheme;

    /**
     * Class Constructor.
     * Does nothing, use the registerGrams method to add grams that
     * will be synched
     */
    public DisplayState() {
    }

    public DisplayState(final ColorScheme cs) {
        this.fColorScheme = cs;
    }

    public ColorScheme getColorScheme() {
        return fColorScheme;
    }

} // End DisplayState
