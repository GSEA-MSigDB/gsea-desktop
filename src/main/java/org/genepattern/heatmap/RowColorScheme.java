/*
 * Decompiled with CFR 0.152.
 */
package org.genepattern.heatmap;

import java.awt.Color;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.ColorScheme;

public class RowColorScheme
implements ColorScheme {
    public static int COLOR_RESPONSE_LOG = 0;
    public static int COLOR_RESPONSE_LINEAR = 1;
    int response = COLOR_RESPONSE_LINEAR;
    IExpressionData dataset;
    int lastRow = -1;
    static Color missingColor = Color.BLACK;
    private double min = 0.0;
    private double max = 8000.0;
    private double mean = Double.NEGATIVE_INFINITY;
    private final Color[] colors;
    private final double[] slots;
    private static int[] defaultColorMap = new int[]{4522157, 2556113, 7035119, 0x8888FF, 13091327, 0xD5D5FF, 16761061, 0xFF8989, 16740480, 0xFF5A5A, 15679552, 14027776};
    private boolean globalScale;

    public static RowColorScheme getRowInstance(Color[] colors) {
        return new RowColorScheme(colors, COLOR_RESPONSE_LINEAR, false);
    }

    public static Color[] getDefaultColorMap() {
        return RowColorScheme.getColorMap(defaultColorMap);
    }

    public Color[] getColorMap() {
        return this.colors;
    }

    private static Color[] getColorMap(int[] colormap) {
        Color[] colors = new Color[colormap.length];
        for (int i = 0; i < colormap.length; ++i) {
            colors[i] = new Color(colormap[i]);
        }
        return colors;
    }

    public RowColorScheme(int[] colormap, int response) {
        this(RowColorScheme.getColorMap(colormap), response, false);
    }

    public RowColorScheme(Color[] colors, int response, boolean global) {
        this.colors = colors;
        this.slots = new double[colors.length];
        if (response != COLOR_RESPONSE_LINEAR && response != COLOR_RESPONSE_LOG) {
            throw new IllegalArgumentException("Unkown color response");
        }
        this.response = response;
        this.globalScale = global;
    }

    public RowColorScheme(int response) {
        this(defaultColorMap, response);
    }

    @Override
    public void setDataset(IExpressionData d) {
        this.dataset = d;
        this.setGlobalScale(this.globalScale);
        this.lastRow = -1;
    }

    public final void setGlobalScale(boolean b) {
        this.globalScale = b;
        if (this.globalScale) {
            this.max = -1.7976931348623157E308;
            this.min = Double.MAX_VALUE;
            this.mean = 0.0;
            int rows = this.dataset.getRowCount();
            for (int i = 0; i < rows; ++i) {
                int columns = this.dataset.getColumnCount();
                for (int j = 0; j < columns; ++j) {
                    double d = this.dataset.getValue(i, j);
                    this.max = d > this.max ? d : this.max;
                    this.min = d < this.min ? d : this.min;
                    this.mean += d;
                }
            }
            int num = this.dataset.getColumnCount() * this.dataset.getRowCount();
            this.mean /= (double)num;
            this.calculateSlots(this.min, this.max, this.mean, this.slots);
        }
    }

    @Override
    public Color getColor(int row, int column) {
        double val;
        if (!this.globalScale && this.lastRow != row) {
            this.calculateRowStats(row);
            this.lastRow = row;
        }
        if (Double.isNaN(val = this.dataset.getValue(row, column))) {
            return missingColor;
        }
        int num = this.slots.length - 1;
        if (val >= this.slots[num]) {
            return this.colors[num];
        }
        for (int i = num; i > 0; --i) {
            if (!(this.slots[i] > val) || !(val > this.slots[i - 1])) continue;
            return this.colors[i];
        }
        return this.colors[0];
    }

    public int getColorCount() {
        return this.colors.length;
    }

    public double[] getSlots() {
        double[] new_slots = new double[this.slots.length + 1];
        new_slots[0] = this.min;
        System.arraycopy(this.slots, 0, new_slots, 1, this.slots.length);
        return new_slots;
    }

    public void calculateSlots(double min, double max, double mean, double[] values) {
        if (this.response == COLOR_RESPONSE_LOG) {
            this.computeLogScaleSlots(min, max, mean, values);
        } else {
            this.computeLinearSlots(min, max, mean, values);
        }
    }

    private void calculateRowStats(int rowNumber) {
        int num = this.dataset.getColumnCount();
        double theMin = Double.POSITIVE_INFINITY;
        double theMax = Double.NEGATIVE_INFINITY;
        double theMean = 0.0;
        int numDataPoints = 0;
        for (int i = 0; i < num; ++i) {
            double tmpVal = this.dataset.getValue(rowNumber, i);
            if (Double.isNaN(tmpVal)) continue;
            if (tmpVal < theMin) {
                theMin = tmpVal;
            }
            if (tmpVal > theMax) {
                theMax = tmpVal;
            }
            theMean += tmpVal;
            ++numDataPoints;
        }
        this.min = theMin;
        this.max = theMax;
        this.mean = theMean /= (double)numDataPoints;
        this.calculateSlots(this.min, this.max, this.mean, this.slots);
    }

    private void computeLogScaleSlots(double real_min, double real_max, double real_mean, double[] slots) {
        double inc;
        double min = 1.0;
        double max = real_max - real_min + 1.0;
        double mean = real_mean - real_min + 1.0;
        double range = Math.log(max) - Math.log(1.0);
        int num = slots.length;
        double log_val = inc = range / (double)num;
        double adjustment = real_min - 1.0;
        for (int i = 0; i < num; ++i) {
            slots[i] = Math.exp(log_val) + adjustment;
            log_val += inc;
        }
    }

    private void computeLinearSlots(double min, double max, double mean, double[] slots) {
        double ave = mean == Double.NEGATIVE_INFINITY ? (max - min) / 2.0 : mean;
        int num = slots.length;
        int halfway = slots.length / 2;
        double inc2 = (ave - min) / (double)halfway;
        double lin_val = min;
        for (int i = 0; i < halfway; ++i) {
            slots[i] = lin_val += inc2;
        }
        double inc = (max - ave) / (double)(num - halfway);
        lin_val = ave;
        for (int i = halfway; i < num; ++i) {
            slots[i] = lin_val += inc;
        }
    }
}
