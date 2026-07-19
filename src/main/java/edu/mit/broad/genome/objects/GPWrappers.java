/*
 * Decompiled with CFR 0.152.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.ColorSchemes;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.ScaleMode;
import edu.mit.broad.genome.objects.Annot;
import edu.mit.broad.genome.objects.ColorDataset;
import edu.mit.broad.genome.objects.ColorDatasetImpl;
import edu.mit.broad.genome.objects.ColorMap;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.FeatureAnnot;
import edu.mit.broad.genome.objects.SampleAnnot;
import edu.mit.broad.genome.objects.Template;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.image.FeatureAnnotator;
import org.genepattern.heatmap.image.SampleAnnotator;

public class GPWrappers {
    private GPWrappers() {
    }

    public static ColorScheme createColorScheme_for_lev_with_score(final Dataset ds) {
        return new ColorScheme(){
            ColorDataset cds;
            {
                this.cds = new ColorDatasetImpl(ds, ScaleMode.REL_MEAN_ZERO_OMITTED, new ColorSchemes.BroadCancer());
            }

            @Override
            public Color getColor(int row, int column) {
                return this.cds.getColor(row, column);
            }

            @Override
            public void setDataset(IExpressionData d) {
            }
        };
    }

    public static ColorScheme createColorScheme(final Dataset ds, final ColorSchemes.ColorScheme csIn) {
        return new ColorScheme(){
            ColorDataset cds;
            {
                this.cds = new ColorDatasetImpl(ds, csIn);
            }

            @Override
            public Color getColor(int row, int column) {
                return this.cds.getColor(row, column);
            }

            @Override
            public void setDataset(IExpressionData d) {
            }
        };
    }

    public static IExpressionData createIExpressionData(Dataset ds) {
        return new IExpressionDataAdaptor(ds);
    }

    public static Dataset createDataset(IExpressionData ied, Annot annot_opt) {
        Matrix m = new Matrix(ied.getRowCount(), ied.getColumnCount());
        String[] rowNames = new String[ied.getRowCount()];
        for (int r = 0; r < ied.getRowCount(); ++r) {
            rowNames[r] = ied.getRowName(r);
        }
        String[] colNames = new String[ied.getColumnCount()];
        for (int c = 0; c < ied.getColumnCount(); ++c) {
            colNames[c] = ied.getColumnName(c);
        }
        for (int r = 0; r < ied.getRowCount(); ++r) {
            for (int c = 0; c < ied.getColumnCount(); ++c) {
                m.setElement(r, c, (float)ied.getValue(r, c));
            }
        }
        Annot synched_annot = null;
        if (annot_opt != null) {
            synched_annot = new Annot(annot_opt.getFeatureAnnot(), annot_opt.getSampleAnnot_synched(colNames));
        }
        return new DefaultDataset("conv", m, rowNames, colNames, synched_annot);
    }

    public static FeatureAnnotator createFeatureAnnotator(Dataset ds) {
        return GPWrappers.createFeatureAnnotator(ds.getAnnot().getFeatureAnnot());
    }

    public static FeatureAnnotator createFeatureAnnotator(final FeatureAnnot fa) {
        if (fa == null) {
            throw new IllegalArgumentException("Param fa cannot be null");
        }
        return new FeatureAnnotator(){

            @Override
            public String getAnnotation(String feature, int j) {
                String s = j == 0 ? fa.getGeneSymbol(feature) : fa.getGeneTitle(feature);
                if (s == null || s.equalsIgnoreCase("NULL")) {
                    return "";
                }
                return s.trim();
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public List<Color> getColors(String featureName) {
                return Collections.emptyList();
            }
        };
    }

    public static SampleAnnotator createSampleAnnotator(final Dataset ds, final Template t_opt) {
        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }
        return new SampleAnnotator(){

            @Override
            public Color getPhenotypeColor(String sampleName) {
                if (t_opt != null) {
                    return t_opt.getItemColor(ds.getColumnIndex(sampleName));
                }
                return Color.WHITE;
            }

            @Override
            public boolean hasPhenotypeColors() {
                return true;
            }

            @Override
            public String getLabel(int i) {
                if (ds.getAnnot() == null || ds.getAnnot().getSampleAnnot_global() == null) {
                    return "";
                }
                return ds.getAnnot().getSampleAnnot_global().getColorMap().getRowName(i);
            }

            @Override
            public List<Color> getColors(String sampleName) {
                if (ds.getAnnot() == null || ds.getAnnot().getSampleAnnot_global() == null) {
                    return new ArrayList<Color>();
                }
                ArrayList<Color> list = new ArrayList<Color>();
                SampleAnnot sa = ds.getAnnot().getSampleAnnot_global();
                ColorMap.Columns cm = sa.getColorMap();
                for (int r = 0; r < cm.getNumRow(); ++r) {
                    list.add(cm.getColor(cm.getRowName(r), sampleName));
                }
                return list;
            }
        };
    }

    static class IExpressionDataAdaptor
    implements IExpressionData {
        private Dataset fDataset;

        @Override
        public String getDataName(int index) {
            return null;
        }

        @Override
        public int getDataCount() {
            return 0;
        }

        public IExpressionDataAdaptor(Dataset ds) {
            if (ds == null) {
                throw new IllegalArgumentException("Param ds cannot be null");
            }
            this.fDataset = ds;
        }

        @Override
        public double getValue(int i, int i1) {
            return this.fDataset.getElement(i, i1);
        }

        @Override
        public String getRowName(int i) {
            return this.fDataset.getRowName(i);
        }

        @Override
        public int getRowCount() {
            return this.fDataset.getNumRow();
        }

        @Override
        public int getColumnCount() {
            return this.fDataset.getNumCol();
        }

        @Override
        public String getColumnName(int i) {
            return this.fDataset.getColumnName(i);
        }

        @Override
        public int getRowIndex(String pmid) {
            return this.fDataset.getRowIndex(pmid);
        }

        @Override
        public int getColumnIndex(String pmid) {
            return this.fDataset.getColumnIndex(pmid);
        }

        @Override
        public String getValueAsString(int i, int i1) {
            return Double.toString(this.getValue(i, i1));
        }

        @Override
        public Object getData(int row, int column, String name) {
            throw new NotImplementedException();
        }

        @Override
        public String getRowMetadata(int row, String name) {
            return null;
        }

        @Override
        public String getColumnMetadata(int column, String name) {
            return null;
        }
    }
}
