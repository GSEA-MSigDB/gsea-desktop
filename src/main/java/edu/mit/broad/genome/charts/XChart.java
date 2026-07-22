/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.charts;

import java.io.File;
import java.io.IOException;

/**
 * Simple chart wrapper for report PNG/SVG export.
 */
public interface XChart {

    // Name is NOT the same as Title - name is simple and file name safe. Title is short but can be 'English'.
    public String getName();

    public String getTitle();

    // Caption is a more verbose form of Title
    public String getCaption();

    public void saveAsPNG(File inFile, int width, int height) throws IOException;

    public void saveAsSVG(File toFile, int width, int height) throws IOException;
}
