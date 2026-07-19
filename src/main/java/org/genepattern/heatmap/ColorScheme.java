/*
 * Decompiled with CFR 0.152.
 */
package org.genepattern.heatmap;

import java.awt.Color;
import org.genepattern.data.expr.IExpressionData;

public interface ColorScheme {
    public Color getColor(int var1, int var2);

    public void setDataset(IExpressionData var1);
}
