/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public final class FxEllipsisButton {

    private FxEllipsisButton() {
    }

    public static Button create() {
        return create(null);
    }

    public static Button create(String tooltip) {
        Button but = new Button();
        but.setGraphic(FxFileIcons.forResource("Ellipsis.png"));
        FxButtons.styleIcon(but);
        if (tooltip != null && !tooltip.isBlank()) {
            but.setTooltip(new Tooltip(tooltip));
        }
        return but;
    }
}
