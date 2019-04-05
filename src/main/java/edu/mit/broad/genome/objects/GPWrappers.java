/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.ColorSchemes;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.ScaleMode;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.image.FeatureAnnotator;
import org.genepattern.heatmap.image.SampleAnnotator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Aravind Subramanian
 */
public class GPWrappers {

    private GPWrappers() {
    }

    public static ColorScheme createColorScheme_for_lev_with_score(final Dataset ds) {

        return new ColorScheme() {

            ColorDataset cds = new ColorDatasetImpl(ds, ScaleMode.REL_MEAN_ZERO_OMITTED, new ColorSchemes.BroadCancer());

            public Color getColor(int row, int column) {
                return cds.getColor(row, column);
            }

            public void setDataset(final IExpressionData d) {
                //cds = new ColorDatasetImpl(cr)
                //throw new NotImplementedException();
            }

            public Component getLegend() {
                return null;
            }
        };

    }

    public static ColorScheme createColorScheme(final Dataset ds,
                                                final edu.mit.broad.genome.math.ColorSchemes.ColorScheme csIn) {

        return new ColorScheme() {
            ColorDataset cds = new ColorDatasetImpl(ds, csIn);

            public Color getColor(int row, int column) {
                return cds.getColor(row, column);
            }

            public void setDataset(final IExpressionData d) {
                //cds = new ColorDatasetImpl(cr)
                //throw new NotImplementedException();
            }

            JComponent legend;

            public Component getLegend() {
                if (legend == null) {
                    legend = new JScrollPane(new LegendTable(csIn));
                }

                return legend;
            }
        };
    }

    static class LegendTable extends JTable {

        LegendTable(final edu.mit.broad.genome.math.ColorSchemes.ColorScheme colorScheme) {

            DefaultTableModel model = new DefaultTableModel(2, colorScheme.getNumColors());
            String[] ss = new String[colorScheme.getNumColors()];
            String[] ss_values = new String[colorScheme.getNumColors()];


            for (int i = 0; i < colorScheme.getNumColors(); i++) {
                ss[i] = "";
                ss_values[i] = colorScheme.getValue(i);

            }

            model.addRow(ss);
            model.addRow(ss_values);
            this.setModel(model);

            this.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(final JTable table,
                                                               final Object value,
                                                               final boolean isSelected,
                                                               final boolean hasFocus,
                                                               final int row,
                                                               final int col) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                    if (row == 0) {
                        this.setBackground(colorScheme.getColor(col));
                    } else {
                        this.setBackground(Color.WHITE);
                    }
                    return this;
                }
            });
        }

    } // End class LegendTable


    public static IExpressionData createIExpressionData(final Dataset ds) {
        return new IExpressionDataAdaptor(ds);
    }

    // @todo improve to use an adaptor rather than cloning
    public static Dataset createDataset(final IExpressionData ied, final Annot annot_opt) {

        //klog.debug(">>> " + ied.getRowCount() + " " + ied.getColumnCount());

        final Matrix m = new Matrix(ied.getRowCount(), ied.getColumnCount());

        final String[] rowNames = new String[ied.getRowCount()];
        for (int r = 0; r < ied.getRowCount(); r++) {
            rowNames[r] = ied.getRowName(r);
        }

        final String[] colNames = new String[ied.getColumnCount()];
        for (int c = 0; c < ied.getColumnCount(); c++) {
            colNames[c] = ied.getColumnName(c);
        }

        for (int r = 0; r < ied.getRowCount(); r++) {
            for (int c = 0; c < ied.getColumnCount(); c++) {
                m.setElement(r, c, (float) ied.getValue(r, c));
            }
        }

        Annot synched_annot = null;
        if (annot_opt != null) {
            synched_annot = new Annot(annot_opt.getFeatureAnnot(), annot_opt.getSampleAnnot_synched(colNames));
        }

        return new DefaultDataset("conv", m, rowNames, colNames, true, synched_annot);
    }

    public static FeatureAnnotator createFeatureAnnotator(final Dataset ds) {
        return createFeatureAnnotator(ds.getAnnot().getFeatureAnnot());
    }

    public static FeatureAnnotator createFeatureAnnotator(final FeatureAnnot fa) {

        if (fa == null) {
            throw new IllegalArgumentException("Param fa cannot be null");
        }

        return new FeatureAnnotator() {

            public String getAnnotation(final String feature, final int j) {

                String s;
                if (j == 0) {
                    s = fa.getGeneSymbol(feature);
                } else {
                    s = fa.getGeneTitle(feature);
                }

                if (s == null || s.equalsIgnoreCase("NULL")) {
                    return "";
                } else {
                    return s.trim();
                }
            }

            // This is the count for the getAnnotation method  and not the GIN grid)
            // // The count is NOT including the feature name field
            public int getColumnCount() {
                return 2; // symbol and title
            }

            public java.util.List getColors(final String featureName) {
                return Collections.EMPTY_LIST;
            }
        };
    }

    public static SampleAnnotator createSampleAnnotator(final Dataset ds, final Template t_opt) {

        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        return new SampleAnnotator() {

            public Color getPhenotypeColor(final String sampleName) {
                if (t_opt != null) {
                    return t_opt.getItemColor(ds.getColumnIndex(sampleName));
                } else {
                    return Color.WHITE;
                    //return Color.GRAY; // @todo fix
                }
            }

            public boolean hasPhenotypeColors() {
                return true;
                //return t_opt != null;
            }

            public String getLabel(int i) {
                if (ds.getAnnot() == null || ds.getAnnot().getSampleAnnot_global() == null) {
                    return "";
                }

                return ds.getAnnot().getSampleAnnot_global().getColorMap().getRowName(i);
            }

            // the colors for the sample at the given column
            public java.util.List getColors(final String sampleName) {

                if (ds.getAnnot() == null || ds.getAnnot().getSampleAnnot_global() == null) {
                    return new ArrayList();
                }

                java.util.List list = new ArrayList();
                SampleAnnot sa = ds.getAnnot().getSampleAnnot_global();

                ColorMap.Columns cm = sa.getColorMap();

                for (int r = 0; r < cm.getNumRow(); r++) {
                    list.add(cm.getColor(cm.getRowName(r), sampleName));
                }

                return list;
            }
        };
    }

    /**
     * Inner class implementing a IExpressionData
     */
    static class IExpressionDataAdaptor implements IExpressionData {

        private Dataset fDataset;


        public String getDataName(int index) {
            return null;
        }

        public int getDataCount() {
            return 0;
        }

        /**
         * Class constructor
         *
         * @param ds
         */
        public IExpressionDataAdaptor(final Dataset ds) {
            if (ds == null) {
                throw new IllegalArgumentException("Param ds cannot be null");
            }

            this.fDataset = ds;
        }

        public double getValue(final int i, final int i1) {
            return fDataset.getElement(i, i1);
        }

        public java.lang.String getRowName(final int i) {
            return fDataset.getRowName(i);
        }

        public int getRowCount() {
            return fDataset.getNumRow();
        }

        public int getColumnCount() {
            return fDataset.getNumCol();
        }

        public java.lang.String getColumnName(final int i) {
            return fDataset.getColumnName(i);
        }

        public int getRowIndex(java.lang.String pmid) {
            return fDataset.getRowIndex(pmid);
        }

        public int getColumnIndex(final java.lang.String pmid) {
            return fDataset.getColumnIndex(pmid);
        }

        public java.lang.String getValueAsString(final int i, final int i1) {
            return Double.toString(getValue(i, i1));
        }

        public Object getData(final int row, final int column, final String name) {
            throw new NotImplementedException();
        }

        public String getRowMetadata(final int row, final String name) {
            return null;
        }

        public String getColumnMetadata(final int column, final String name) {
            return null;
        }
    }
}