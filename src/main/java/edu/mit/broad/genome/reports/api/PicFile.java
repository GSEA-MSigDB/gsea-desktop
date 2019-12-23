/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports.api;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.reports.pages.HtmlFormat;

import org.apache.ecs.StringElement;
import org.apache.ecs.html.BR;
import org.apache.ecs.html.Div;
import org.apache.ecs.html.IMG;
import org.genepattern.heatmap.image.HeatMap;
import org.genepattern.io.ImageUtil;

import java.io.File;
import java.io.IOException;

/**
 * Inner class to capture a picture and its file
 * Name is not necc unique so we need to generate a name to save the file
 * and not use title by itself
 */
public class PicFile {

    private String srcName;
    private String name;
    private XChart xChart;
    private HeatMap heatMap;
    private String title;
    private String caption;
    private int width;
    private int height;
    private int currPicCnt;
    private boolean createSvgs;
    private File fSavedFile;
    private File fSavedFileSvg;
    private boolean isSaved = false;
    
    public PicFile(final XChart xChart, final int width, final int height, final int currPicCnt, File saveInDir, boolean createSvgs) {
        if (xChart == null) {
            throw new IllegalArgumentException("Parameter xChart cannot be null");
        }

        String srcName = generateName(xChart) + ".png";
        this.xChart = xChart;
        this.heatMap = null;
        init(saveInDir, srcName, xChart.getName(), xChart.getTitle(), xChart.getCaption(), width, height, currPicCnt, createSvgs);
    }

    public PicFile(final String name, final String title, final String caption, final HeatMap heatMap, final int currPicCnt, File saveInDir, boolean createSvgs) {
        if (heatMap == null) {
            throw new IllegalArgumentException("Parameter heatMap cannot be null");
        }

        String srcName = generateNameForImage(name) + ".png";
        this.xChart = null;
        this.heatMap = heatMap;
        init(saveInDir, srcName, name, title, caption, 0, 0, currPicCnt, createSvgs);
    }

    private static int kImageCounter = 1;

    public static String generateName(final XChart xchart) {
        return NamingConventions.createSafeFileName(xchart.getName()) + "_" + kImageCounter++;
    }

    public static String generateNameForImage(final String title) {
        return NamingConventions.createSafeFileName(title) + "_" + kImageCounter++;
    }

    // common init routine
    private void init(final File saveInDir,
                      final String srcName,
                      final String name,
                      final String title,
                      final String caption,
                      final int width,
                      final int height,
                      final int currPicCnt,
                      final boolean createSvgs) {

        if (saveInDir == null) {
            throw new IllegalArgumentException("Parameter saveInDir cannot be null");
        }

        if (srcName == null) {
            throw new IllegalArgumentException("Parameter srcName cannot be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Parameter name cannot be null");
        }

        if (title == null) {
            throw new IllegalArgumentException("Parameter title cannot be null");
        }

        this.fSavedFile = new File(saveInDir, srcName);
        if (createSvgs) {
            this.fSavedFileSvg = ImageUtil.getSvgFileFromImgFile(fSavedFile, true);
        }
        this.srcName = srcName;
        this.name = name;
        this.title = title;
        this.caption = caption;
        this.width = width;
        this.height = height;
        this.currPicCnt = currPicCnt;
        this.createSvgs = createSvgs;
    }

    // core image creation block
    // html properties of the image are set here
    public Div createIMG() throws IOException {
        // First, save the image file
        this.save();
        
        Div image = HtmlFormat.Divs.image();
        IMG img = new IMG(srcName, name);
        img.addElement(new BR()); // 2 breaks makes it look nicer
        img.addElement(new BR());

        String desc = "Fig " + currPicCnt + ": " + title;
        if (caption != null) {
            desc += " &nbsp&nbsp <br> " + caption;
        }

        img.addElement(HtmlFormat.caption(desc));
        image.addElement(img);
        
        if (this.createSvgs) {
            // Add a note regarding compressed SVG format and a link to the file.
            StringElement line = HtmlFormat.Links.hyper("The same image", fSavedFileSvg, "in compressed SVG format", fSavedFileSvg.getParentFile());
            image.addElement(new BR());
            image.addElement(new BR());
            image.addElement(line);
        }
        
        return image;
    }

    private void save() throws IOException {
        if (isSaved) return;
        
        // Note that one or the other can be non-null due to the constructors, but not both.
        if (xChart != null) {
            xChart.saveAsPNG(fSavedFile, width, height);
            if (createSvgs) {
                ImageUtil.saveAsSVG(xChart.getFreeChart(), fSavedFileSvg, width, height, true);
            }
            
            // Clear the reference to free resources - this is necessary because the PicFiles are
            // held in memory for the duration of the report generation process
            xChart = null;
        } else if (heatMap != null) {
            ImageUtil.saveReportPlotImage(heatMap, fSavedFile, "png");
            if (createSvgs) {
                ImageUtil.saveReportPlotImage(heatMap, fSavedFileSvg, "svg");
            }

            // ... as above
            heatMap = null;
        }
        
        isSaved = true;
    }
    
    public File getFile() {
        if (!isSaved) {
            throw new IllegalStateException("Not yet saved");
        }

        return fSavedFile;
    }
    
    public File getSvgFile() {
        if (!isSaved) {
            throw new IllegalStateException("Not yet saved");
        }

        return fSavedFileSvg;
    }

} // End inner class PicFile
