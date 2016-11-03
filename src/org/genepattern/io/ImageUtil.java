/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.XLogger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.genepattern.heatmap.image.HeatMap;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ImageUtil {
    private static final Logger klog = XLogger.getLogger(ImageUtil.class);

    private ImageUtil() {
    }

    /**
     * Saves the image to a file
     *
     * @param bi
     * @param outputFile
     * @param format
     */
    private static final File saveImage(BufferedImage bi, File outputFile, String format) {
        try {
            if (StringUtils.equalsIgnoreCase("svg", format)) {
                throw new IllegalArgumentException("No SVG support for saving a BufferedImage.");
            }

            ImageIO.write(bi, format, outputFile);
            return outputFile;
        } catch (Throwable t) {
            String errMsg = "An error occurred while saving the image '" + outputFile.getName() + "'";
            klog.error(errMsg, t);
            throw new StandardException(errMsg, t, 9002);
        }
    }
 
    public static final File savePlotImage(final ChartPanel plot, File outputFile, final String format) 
            throws IOException {
        // Never use GZ compression for SVG in this case as it doesn't apply to images in general.
        return coreSavePlotImage(plot, outputFile, format, false);
    }

    public static final File saveReportPlotImage(final ChartPanel plot, File outputFile, final String format) 
            throws IOException {
        // Always use GZ compression for SVG when reporting.
        return coreSavePlotImage(plot, outputFile, format, true);
    }

    private static File coreSavePlotImage(final ChartPanel plot, File outputFile,
            final String format, boolean gZipSvg) throws IOException {
        outputFile = correctImageExtensionIfNecessary(outputFile, format, gZipSvg);
        
        // We can't use a BufferedImage for SVG.
        if (StringUtils.equalsIgnoreCase("svg", format)) {
            Dimension prefSize = plot.getPreferredSize();
            return saveAsSVG(plot.getChart(), outputFile, prefSize.width, prefSize.height, gZipSvg);
        } else {
            // GZ compression doesn't apply to these images.
            return saveImage(createImage(plot), outputFile, format);
        }
    }
    
    public static final File savePlotImage(HeatMap heatMap, File outputFile, final String format) 
            throws IOException {
        // Never use GZ compression for SVG in this case as it doesn't apply to images in general.
        return coreSavePlotImage(heatMap, outputFile, format, false);
    }
    
    public static final File saveReportPlotImage(HeatMap heatMap, File outputFile, final String format) 
            throws IOException {
        // Always use GZ compression for SVG when reporting.
        return coreSavePlotImage(heatMap, outputFile, format, true);
    }

    private static File coreSavePlotImage(HeatMap heatMap, File outputFile, final String format,
            boolean gZipSvgs) throws IOException {
        outputFile = correctImageExtensionIfNecessary(outputFile, format, gZipSvgs);
        
        // We can't use a BufferedImage for SVG.
        if (StringUtils.equalsIgnoreCase("svg", format)) {
            return saveAsSVG(heatMap, outputFile, gZipSvgs);
        } else {
            // GZ compression doesn't apply to these images.
            return saveImage(heatMap.snapshot(), outputFile, format);
        }
    }
    
    public static final File saveAsSVG(JFreeChart chart, File outputFile, int width, int height, boolean gZip)
            throws IOException {
        // Make sure we have a ".gz" extension if compressing.
        if (gZip && !StringUtils.endsWithIgnoreCase(outputFile.getName(), ".gz")) {
            outputFile = getSvgFileFromImgFile(outputFile, true);
        }
        SVGGraphics2D graphics = new SVGGraphics2D(width, height);
        drawChartPlot(chart, graphics, width, height);
        SVGUtils.writeToSVG(outputFile, graphics.getSVGElement(), gZip);
        return outputFile;
    }

    private static final File saveAsSVG(HeatMap heatMap, File outputFile, boolean gZip)
            throws IOException {
        // Make sure we have a ".gz" extension if compressing.
        if (gZip && !StringUtils.endsWithIgnoreCase(outputFile.getName(), ".gz")) {
            outputFile = getSvgFileFromImgFile(outputFile, true);
        }
        SVGGraphics2D graphics = new SVGGraphics2D(heatMap.getContentWidth(), heatMap.getHeightWithHeader());
        heatMap.drawSnapshot(graphics);
        SVGUtils.writeToSVG(outputFile, graphics.getSVGElement(), gZip);
        return outputFile;
    }

    private static final void drawChartPlot(JFreeChart chart, Graphics2D graphics, int width, int height) {
        Rectangle rectangle = new Rectangle(width, height);
        chart.draw(graphics, rectangle);
    }

    private static final BufferedImage createImage(ChartPanel plot) {
        Dimension prefSize = plot.getPreferredSize();
        BufferedImage bufferedImage = new BufferedImage(prefSize.width, prefSize.height,
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = bufferedImage.createGraphics();
        drawChartPlot(plot.getChart(), graphics, prefSize.width, prefSize.height);
        graphics.dispose();
        return bufferedImage;
    }

    // TODO: maybe replace with Enums someday...
    private static final Set<String> NON_JPG_IMG_FORMAT_EXTS = new HashSet<String>(
            Arrays.asList(new String[]{ "png", "svg" }));
    private static final Set<String> JPG_FORMAT_EXTS = new HashSet<String>(
            Arrays.asList(new String[]{ "jpg", "jpeg" }));
    
    public static final File correctImageExtensionIfNecessary(File outputFile, String format, boolean gZipSvgs) {
        // Correct the extension if necessary; also checks outputFileFormat validity.
        // Note that we just append the new extension to the old name without cutting off the
        // old extension.  This matches former GSEA behavior.
        format = StringUtils.lowerCase(format);
        String outputFileName = outputFile.getName();
        String ext = FilenameUtils.getExtension(outputFileName).toLowerCase();
        String compExt = "";
        
        // Special case: SVG may be GZ compressed.  Strip "gz" extension (if present) and look for a 
        // secondary extension ahead of the check.
        if (gZipSvgs && StringUtils.equals(format, "svg") && StringUtils.equals(ext, "gz")) {
            compExt = ".gz";
            String baseName = FilenameUtils.getBaseName(outputFileName);
            ext = FilenameUtils.getExtension(baseName);
        }

        if (NON_JPG_IMG_FORMAT_EXTS.contains(format)) {
            if (!StringUtils.equals(format, ext)) {
                return new File(outputFile.getParentFile(),  outputFileName + "." + format + compExt);
            }
        } else if (JPG_FORMAT_EXTS.contains(format)) {
            if (!JPG_FORMAT_EXTS.contains(ext)) {
                return new File(outputFile.getParentFile(), outputFileName + ".jpg");
            }
        } else {
            throw new IllegalArgumentException("Unknown output file format '" + format + "'");
        }

        return outputFile;
    }
    
    public static final File getSvgFileFromImgFile(File imgFile, boolean gZip) {
        String baseName = FilenameUtils.getBaseName(imgFile.getName());
        String ext = (gZip) ? ".svg.gz" : ".svg";
        return new File(imgFile.getParent(), baseName + ext);
    }
}
