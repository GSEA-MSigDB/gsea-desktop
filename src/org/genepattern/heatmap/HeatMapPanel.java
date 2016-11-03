/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap;

import org.genepattern.data.expr.IExpressionData;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;

/**
 * This class is used to draw a heat map.
 *
 * @author Joshua Gould
 */
public class HeatMapPanel extends JPanel {

    public static int COLOR_RESPONSE_LOG = 0;

    public static int COLOR_RESPONSE_LINEAR = 1;

    public static int NORMALIZATION_ROW = 0;

    public static int NORMALIZATION_GLOBAL = 1;

    private IExpressionData data;

    /**
     * width and height of one 'cell' in the heatmap
     */
    int columnSize = 8;

    int rowSize = 8;

    private boolean drawGrid = true;

    private Color borderColor = Color.black;

    private ColorScheme colorConverter;

    private int normalization = NORMALIZATION_ROW;

    private boolean showToolTip = true;

    private PixelConverter pixelConverter;

    private PropertyChangeSupport changeSupport;

    private boolean upperTriangular = false;

    private boolean showNamesInToolTipText = true;

    private NumberFormat numberFormat;

    private ToolTipProvider toolTipProvider;

    /**
     * Constructs an <code>HeatMapPanel</code> with specified data
     */
    public HeatMapPanel(IExpressionData data) {
        this(data, RowColorScheme.getDefaultColorMap());
    }

    /**
     * Constructs an <code>HeatMapPanel</code> with specified data and color
     * map
     */
    public HeatMapPanel(IExpressionData data, Color[] colorMap) {
        this.data = data;
        colorConverter = RowColorScheme.getRowInstance(colorMap);
        colorConverter.setDataset(data);
        ToolTipManager.sharedInstance().registerComponent(this);
        pixelConverter = new PixelConverter(this);
        pixelConverter.rowSize = rowSize;
        pixelConverter.columnSize = columnSize;
        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(4);
    }

    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list. The
     * listener is registered for all properties.
     * <p/>
     * A <code>PropertyChangeEvent</code> will get fired in response to
     * setting a bound property, such as <code>setRowSize</code>,
     * <code>setColumnSize</code>.
     * <p/>
     * Note that if the current component is inheriting its foreground,
     * background, or font from its container, then no event will be fired in
     * response to a change in the inherited property.
     *
     * @param listener the <code>PropertyChangeListener</code> to be added
     */
    public synchronized void addPropertyChangeListener(
            PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a <code>PropertyChangeListener</code> for a specific property. The
     * listener will be invoked only when a call on
     * <code>firePropertyChange</code> names that specific property.
     * <p/>
     * If listener is <code>null</code>, no exception is thrown and no action
     * is performed.
     *
     * @param propertyName the name of the property to listen on
     * @param listener     the <code>PropertyChangeListener</code> to be added
     */
    public synchronized void addPropertyChangeListener(String propertyName,
                                                       PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties.
     *
     * @param listener the <code>PropertyChangeListener</code> to be removed
     */
    public synchronized void removePropertyChangeListener(
            PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Removes a <code>PropertyChangeListener</code> for a specific property.
     * If listener is <code>null</code>, no exception is thrown and no action
     * is performed.
     *
     * @param propertyName the name of the property that was listened on
     * @param listener     the <code>PropertyChangeListener</code> to be removed
     */
    public synchronized void removePropertyChangeListener(String propertyName,
                                                          PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (changeSupport == null) {
            return;
        }
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public void setExpressionData(IExpressionData data) {
        this.data = data;
        colorConverter.setDataset(data);
        repaint();
    }

    public void setToolTipProvider(ToolTipProvider t) {
        toolTipProvider = t;
    }

    public String getToolTipText(MouseEvent e) {
        if (!showToolTip) {
            return null;
        }

        int col = pixelConverter.columnAtPoint(e.getPoint());
        int row = pixelConverter.rowAtPoint(e.getPoint());
        if (col >= 0 && col < data.getColumnCount() && row >= 0
                && row < data.getRowCount()) {
            if (toolTipProvider != null) {
                return toolTipProvider.getToolTipText(row, col);
            }
            String value = numberFormat.format(data.getValue(row, col));
            return showNamesInToolTipText ? "<html>" + value + "<br>"
                    + data.getRowName(row) + "<br>" + data.getColumnName(col)
                    : value;
        }
        return null;
    }

    public static interface ToolTipProvider {
        public String getToolTipText(int row, int column);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        final int columns = data.getColumnCount();
        Rectangle bounds = g.getClipBounds();
        int left = 0;
        int right = columns;
        int top = 0;
        int bottom = data.getRowCount();

        if (bounds != null) {
            top = pixelConverter.getTopIndex(bounds.y);
            bottom = pixelConverter.getBottomIndex(bounds.y + bounds.height,
                    data.getRowCount());
            left = pixelConverter.getLeftIndex(bounds.x);
            right = pixelConverter.getRightIndex(bounds.x + bounds.width,
                    columns);
        }

        // draw rectangles
        for (int row = top; row < bottom; row++) {
            for (int column = left; column < right; column++) {
                int x = column * columnSize + getInsets().left;
                int y = row * rowSize + getInsets().top;

                if (upperTriangular && column < row) {
                    continue;
                }
                g.setColor(colorConverter.getColor(row, column));
                g.fillRect(x, y, columnSize, rowSize);
            }
        }

        if (drawGrid) {
            g.setColor(borderColor);

            int leftx = left * columnSize + getInsets().left;
            int rightx = right * columnSize + getInsets().left;

            for (int row = top; row <= bottom; row++) { // draw horizontal lines
                int y = row * rowSize + getInsets().top;
                if (upperTriangular) {
                    int leftDiag = (row - 1) * columnSize + getInsets().left;
                    g.drawLine(leftDiag, y, rightx, y);
                } else {
                    g.drawLine(leftx, y, rightx, y);
                }
            }

            int topy = getInsets().top + rowSize * top;
            int bottomy = getInsets().top + rowSize * bottom;
            for (int column = left; column <= right; column++) { // draw
                // vertical
                // lines
                int x = column * columnSize + getInsets().left;
                if (upperTriangular) {
                    int bottomDiag = rowSize * (column + 1) + getInsets().top;
                    if (column == columns) {
                        bottomDiag = bottomDiag - rowSize;
                    }
                    g.drawLine(x, topy, x, bottomDiag);
                } else {
                    g.drawLine(x, topy, x, bottomy);
                }
            }
        }
    }

    /**
     * Calls the <code>configureEnclosingScrollPane</code> method.
     *
     * @see #configureEnclosingScrollPane
     */
    public void addNotify() {
        super.addNotify();
        configureEnclosingScrollPane();
    }

    /**
     * Calls the <code>unconfigureEnclosingScrollPane</code> method.
     *
     * @see #unconfigureEnclosingScrollPane
     */
    public void removeNotify() {
        unconfigureEnclosingScrollPane();
        super.removeNotify();
    }

    /**
     * If this <code>HeatMap</code> is the <code>viewportView</code> of an
     * enclosing <code>JScrollPane</code> (the usual situation), configure
     * this <code>ScrollPane</code> by, amongst other things, installing the
     * heat map's <code>header</code> as the <code>columnHeaderView</code>
     * of the scroll pane. When a <code>HeatMap</code> is added to a
     * <code>JScrollPane</code> in the usual way, using
     * <code>new JScrollPane(myHeatMap)</code>, <code>addNotify</code> is
     * called in the <code>HeatMap</code> (when the heat map is added to the
     * viewport). <code>HeatMap</code>'s <code>addNotify</code> method in
     * turn calls this method, which is protected so that this default
     * installation procedure can be overridden by a subclass.
     *
     * @see #addNotify
     */
    protected void configureEnclosingScrollPane() {
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                // scrollPane.setColumnHeaderView(header);
                // header.updateSize(contentWidth, elementWidth);
                Border border = scrollPane.getBorder();
                if (border == null || border instanceof UIResource) {
                    scrollPane.setBorder(UIManager
                            .getBorder("Table.scrollPaneBorder"));
                }
            }
        }
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Reverses the effect of <code>configureEnclosingScrollPane</code> by
     * replacing the <code>columnHeaderView</code> of the enclosing scroll
     * pane with <code>null</code>. <code>HeatMap</code>'s
     * <code>removeNotify</code> method calls this method, which is protected
     * so that this default uninstallation procedure can be overridden by a
     * subclass.
     *
     * @see #removeNotify
     * @see #configureEnclosingScrollPane
     */
    protected void unconfigureEnclosingScrollPane() {
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                // scrollPane.setColumnHeaderView(null);
            }
        }
    }

    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {
        if ((changeSupport != null) && (oldValue != null)
                && !(oldValue.equals(newValue))) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(columnSize * data.getColumnCount() + 1, rowSize
                * (data.getRowCount() + 1));
    }

    public int getRowSize() {
        return rowSize;
    }

    public void setRowSize(int elementHeight) {
        this.rowSize = elementHeight;
        pixelConverter.rowSize = rowSize;
        firePropertyChange("rowSize", new Integer(elementHeight), new Integer(
                rowSize));
    }

    public int getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(int elementWidth) {
        this.columnSize = elementWidth;
        pixelConverter.columnSize = columnSize;
        firePropertyChange("columnSize", new Integer(elementWidth),
                new Integer(columnSize));
    }

    public boolean isDrawGrid() {
        return drawGrid;
    }

    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
    }

    public int getNormalization() {
        return normalization;
    }

    public void setNormalization(int normalization) {
        this.normalization = normalization;
        if (colorConverter instanceof RowColorScheme) { // FIXME
            ((RowColorScheme) colorConverter)
                    .setGlobalScale(normalization == NORMALIZATION_GLOBAL);
        }
    }

    public void setColorConverter(ColorScheme colorConverter) {
        this.colorConverter = colorConverter;
        colorConverter.setDataset(data);
    }

    public ColorScheme getColorConverter() {
        return this.colorConverter;
    }

    public boolean isUpperTriangular() {
        return upperTriangular;
    }

    public void setUpperTriangular(boolean upperTriangular) {
        this.upperTriangular = upperTriangular;
    }

}
