/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.widgets;

import java.awt.image.BufferedImage;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * AWT {@link BufferedImage} → JavaFX {@link Image} without {@code javafx.swing}.
 */
public final class FxImages {

    private FxImages() {
    }

    public static Image toFxImage(BufferedImage buffered) {
        if (buffered == null) {
            return null;
        }
        int width = buffered.getWidth();
        int height = buffered.getHeight();
        int[] pixels = buffered.getRGB(0, 0, width, height, null, 0, width);
        WritableImage fx = new WritableImage(width, height);
        fx.getPixelWriter().setPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return fx;
    }
}
