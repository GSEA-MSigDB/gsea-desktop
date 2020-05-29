/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.menu.jfree;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.genepattern.io.ImageUtil;
import org.genepattern.menu.PlotAction;
import org.genepattern.uiutil.UIUtil;
import org.jfree.chart.ChartPanel;

import edu.mit.broad.genome.StandardException;
import xapps.gsea.GseaFileFilter;

/**
 * @author Joshua Gould
 * @author David Eby
 */
public class JFreeSaveImageAction extends PlotAction {

    private final Frame parent;

    public JFreeSaveImageAction(Frame parent) {
        if (parent == null) {
            throw new NullPointerException("Null parent not allowed");
        }

        putValue(AbstractAction.NAME, "Save Image");
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
//        try {
//            if (!SystemUtils.IS_OS_MAC_OSX) {
//                File outputFile = null;
//                String outputFileFormat = null;
//                JFileChooser fc = new JFileChooser();
//                fc.setAcceptAllFileFilterUsed(false);
//                fc.addChoosableFileFilter(new JFreeSaveImageAction.SaveImageFileFilter(new String[]{"jpeg",
//                        "jpg"}, "JPEG image", "JPEG"));
//                fc.addChoosableFileFilter(new JFreeSaveImageAction.SaveImageFileFilter(new String[]{"png"},
//                        "PNG image", "PNG"));
//                fc.addChoosableFileFilter(new JFreeSaveImageAction.SaveImageFileFilter(new String[]{"svg"},
//                        "SVG image", "SVG"));
//                if (fc.showSaveDialog(getPlot().getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
//                    outputFile = fc.getSelectedFile();
//                    outputFileFormat = ((JFreeSaveImageAction.SaveImageFileFilter) fc.getFileFilter())
//                            .getFileFormat();
//        
//                    if (!FileChooser.overwriteFile(getPlot().getTopLevelAncestor(),
//                            outputFile)) {
//                        return;
//                    }
//        
//                }
//                if (outputFile != null) {
//                    save(outputFile, outputFileFormat);
//                }
//            }
//        } catch (Exception x) {
//            showError(x, "An error occurred while saving the plot.");
//        }
    }

    public JMenuItem[] getSubMenuItems() {
        // TODO: possible bug if this method is actually used.  Do we really disallow on non-Mac?
        // Or, is it that we use this on Mac and the one above on non-Mac?
//        if (!SystemUtils.IS_OS_MAC_OSX) {
//            return null;
//        }
        final JMenuItem jpegItem = new JMenuItem("jpeg...");
        final JMenuItem pngItem = new JMenuItem("png...");
        final JMenuItem svgItem = new JMenuItem("svg...");
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == jpegItem) {
                    save("jpeg", "jpg");
                } else if (e.getSource() == pngItem) {
                    save("png");
                } else if (e.getSource() == svgItem) {
                    save("svg");
                }
//                File outputFile = FileChooser.showSaveDialog(parent);
//                if (outputFile != null) {
//                    save(outputFile, fileFormat);
//                }
            }
        };
        jpegItem.addActionListener(l);
        pngItem.addActionListener(l);
        svgItem.addActionListener(l);
        return new JMenuItem[]{jpegItem, pngItem, svgItem};
    }

    private void save(final String... formats) {
        final String outputFileFormat = formats[0];
        FileDialog fileDialog = new FileDialog(parent, "Save as " + outputFileFormat, FileDialog.SAVE);
        fileDialog.setMultipleMode(false);
        fileDialog.setModal(true);
        
        StringBuilder sb = new StringBuilder("*.").append(outputFileFormat);
        for (int i = 1; i < formats.length; i++) {
            sb.append(";*.").append(formats[0]);
        }
        fileDialog.setFile("*." + formats);
        fileDialog.setFilenameFilter(new GseaFileFilter(formats, outputFileFormat + " image files"));
        fileDialog.setVisible(true);
        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            final File outputFile = files[0];
            final ChartPanel plot = (ChartPanel) getPlot();
            SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        ImageUtil.savePlotImage(plot, outputFile, outputFileFormat);
                    }
                    catch (StandardException se) {
                        UIUtil.showErrorDialog(getPlot().getTopLevelAncestor(), se.getMessage());
                    }
                    return null;
                }
            };
            worker.execute();
        }
    }

    private void showError(Exception e, String msg) {
        if (e.getMessage() != null) {
            msg += "\nCause: " + e.getMessage();
        }
        UIUtil.showErrorDialog(getPlot().getTopLevelAncestor(), msg);
    }

    /**
     * @author Joshua Gould
     */
    public static class SaveImageFileFilter extends FileFilter {
        private final java.util.List<String> extensions;
    
        private final String fileFormat, description;
    
        public SaveImageFileFilter(String[] extensions, String description,
                            String fileFormat) {
            this.extensions = java.util.Arrays.asList(extensions);
            this.description = description;
            this.fileFormat = fileFormat;
    
        }
    
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String name = f.getName();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex > 0) {
                String ext = name.substring(dotIndex + 1, name.length());
                return extensions.contains(ext.toLowerCase());
            }
            return false;
        }
    
        public String getDescription() {
            return description;
        }
    
        public String getFileFormat() {
            return fileFormat;
        }
    }
}