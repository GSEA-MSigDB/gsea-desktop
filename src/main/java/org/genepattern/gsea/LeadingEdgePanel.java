/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.IExpressionDataUtil;
import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.HeatMapComponent;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;

/**
 * @author Joshua Gould
 */
public class LeadingEdgePanel {

    private HeatMapComponent heatMap;

    private File resultDirectory;

    /**
     * @param parent         the parent for the leading edge panel
     */
    public LeadingEdgePanel(Frame parent) {
        heatMap = new HeatMapComponent(parent, IExpressionDataUtil
                .createRandomData(1, 1), // XXX set dummy data initially
                null);
        heatMap.setFeatureUIString("Gene Set");
        heatMap.setSampleUIString("Gene");
        heatMap.setShowFeatureAnnotator(false);
        heatMap.setShowRowDescriptions(false);
        heatMap.setShowSampleAnnotator(false);
        heatMap.setShowColorSchemeOptions(false);

        final JTable featureTable = heatMap.getFeatureTable();
        featureTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (resultDirectory == null) {
                    return;
                }
                int row = featureTable.rowAtPoint(e.getPoint());
                int column = featureTable.columnAtPoint(e.getPoint());
                if (row != -1 && column == 0) {
                    String value = (String) featureTable
                            .getValueAt(row, column);
                    value = value.replaceAll("_signal", "");
                    File gsetGseaFile = new File(resultDirectory, value
                            + ".html");
                    URI uri = gsetGseaFile.toURI();
                    try {
                        Desktop.getDesktop().browse(uri);
                    } catch (Throwable x) {
                        x.printStackTrace();
                    }
                }
            }
        });

    }

    public void setResultDirectory(File resultDirectory) {
        this.resultDirectory = resultDirectory;
    }

    public void setData(IExpressionData data, ColorScheme cs) {
        heatMap.setColorConverter(cs);
        heatMap.setExpressionData(data);
    }

    public JMenuBar getMenuBar() {
        heatMap.setOptionsDialogOptions(false, false, false);
        return heatMap.createMenuBar(false, false, false, false);
    }

    public HeatMapComponent getHeatMapComponent() {
        return heatMap;
    }

}
