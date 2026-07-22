/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.canvas.PixelCanvas;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import xapps.gsea.fx.FxTheme;

/**
 * Dock leaf with GSEA-themed drop/snap hints (accent blue; matches bento.css -color-accent-*).
 */
final class GseaDockContainerLeaf extends DockContainerLeaf {

    /** Light: -color-accent-emphasis (#0b6bcb) with alpha. */
    private static final int LIGHT_FILL = 0x330B6BCB;
    private static final int LIGHT_BORDER = 0xCC0B6BCB;
    /** Dark: -color-accent-emphasis (#4c8bf5) / -color-accent-3 (#8ab4f8). */
    private static final int DARK_FILL = 0x444C8BF5;
    private static final int DARK_BORDER = 0xDD8AB4F8;

    GseaDockContainerLeaf(Bento bento, String identifier) {
        super(bento, identifier);
    }

    @Override
    public void drawCanvasHint(Region target, Side side) {
        double ox = 0;
        double oy = 0;
        Parent parent = target.getParent();
        while (parent != null && parent != this) {
            ox += parent.getLayoutX();
            oy += parent.getLayoutY();
            parent = parent.getParent();
        }

        PixelCanvas canvas = getCanvas();
        canvas.clear();

        boolean dark = FxTheme.isDarkEffective();
        int color = dark ? DARK_FILL : LIGHT_FILL;
        int borderColor = dark ? DARK_BORDER : LIGHT_BORDER;
        final int borderWidth = 2;
        final double x = ox + target.getLayoutX();
        final double y = oy + target.getLayoutY();
        final double w = target.getWidth();
        final double h = target.getHeight();
        if (side == null) {
            canvas.fillBorderedRect(x, y, w, h, borderWidth, color, borderColor);
        } else {
            switch (side) {
                case TOP -> canvas.fillBorderedRect(x, y, w, h / 2, borderWidth, color, borderColor);
                case BOTTOM -> canvas.fillBorderedRect(x, y + h / 2, w, h / 2, borderWidth, color, borderColor);
                case LEFT -> canvas.fillBorderedRect(x, y, w / 2, h, borderWidth, color, borderColor);
                case RIGHT -> canvas.fillBorderedRect(x + w / 2, y, w / 2, h, borderWidth, color, borderColor);
            }
        }
        canvas.commit();
    }
}
