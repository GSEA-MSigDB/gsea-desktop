/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.awt.image.BufferedImage;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/**
 * AWT {@link BufferedImage} → JavaFX {@link Image} via {@link SwingFXUtils}.
 */
public final class FxImages {

    private FxImages() {
    }

    public static Image toFxImage(BufferedImage buffered) {
        if (buffered == null) {
            return null;
        }
        WritableImage fx = SwingFXUtils.toFXImage(buffered, null);
        return fx;
    }
}
