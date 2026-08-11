/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.List;

import org.gsea_msigdb.gsea.runtime.AppServices;

import edu.mit.broad.genome.objects.PersistentObject;
import javafx.scene.control.TextField;
import xtools.api.param.Param;

final class ParamEditorSupport {

    private ParamEditorSupport() {
    }

    /**
     * Display text for path-ish param values.
     * Matches : {@code Object[]} is joined with commas
     * (empty array → blank). Never use {@code Object.toString} on arrays
     */
    static String displayText(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof File) {
            return ((File) v).getPath();
        }
        if (v instanceof Object[]) {
            Object[] arr = (Object[]) v;
            if (arr.length == 0) {
                return "";
            }
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    continue;
                }
                if (buf.length() > 0) {
                    buf.append(',');
                }
                buf.append(elementDisplayText(arr[i]));
            }
            return buf.toString();
        }
        return String.valueOf(v);
    }

    static boolean containsByString(List<Object> items, Object value) {
        String vs = displayText(value);
        if (vs.isEmpty() || items == null) {
            return false;
        }
        for (Object item : items) {
            if (item != null && vs.equals(displayText(item))) {
                return true;
            }
        }
        return false;
    }

    static String elementDisplayText(Object el) {
        if (el instanceof File) {
            return ((File) el).getPath();
        }
        if (el instanceof PersistentObject) {
            try {
                String path = AppServices.require().cache().getSourcePath(el);
                if (path != null && !path.isBlank()) {
                    return path;
                }
            } catch (Throwable ignored) {
            }
            return ((PersistentObject) el).getName();
        }
        return el.toString().trim();
    }

    /**
     * Prefer each Param's own string form (e.g. {@code GeneSetMatrixMultiChooserParam} path list);
     * fall back to {@link #displayText}.
     */
    static String paramDisplayText(Param param) {
        try {
            String rep = param.getValueStringRepresentation(false);
            if (rep != null) {
                return rep;
            }
        } catch (Throwable ignored) {
        }
        Object v = param.getValue();
        if (v == null) {
            v = param.getDefault();
        }
        return displayText(v);
    }

    /** Empty arrays are common Param defaults; treat like unset for combo/text seeding. */
    static boolean isEmptyArray(Object v) {
        return v != null && v.getClass().isArray() && java.lang.reflect.Array.getLength(v) == 0;
    }

    /** Null-out empty-array defaults so they are not stringified as {@code [L…;@hash]}. */
    static Object unsetIfEmptyArray(Object v) {
        return isEmptyArray(v) ? null : v;
    }

    /**
     * Path coloring: blue = good, red = bad;
     * URL schemes and comma-separated multi-paths supported.
     * Dir/ReportDir fields also get {@code gsea-dir-field} (light green).
     */
    static void attachPathColorListener(TextField field) {
        attachPathColorListener(field, false);
    }

    static void attachPathColorListener(TextField field, boolean dirField) {
        if (dirField) {
            if (!field.getStyleClass().contains("gsea-dir-field")) {
                field.getStyleClass().add("gsea-dir-field");
            }
        }
        FxPathFieldColors.attach(field);
    }

    static void attachFileOrPobDrop(TextField field) {
        field.setOnDragOver(e -> {
            if (e.getGestureSource() != field
                    && (e.getDragboard().hasFiles()
                    || xapps.gsea.fx.widgets.FxPobTransferSupport.hasPobList(e.getDragboard()))) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            e.consume();
        });
        field.setOnDragDropped(e -> {
            javafx.scene.input.Dragboard db = e.getDragboard();
            String path = null;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                path = db.getFiles().get(0).getAbsolutePath();
            } else if (xapps.gsea.fx.widgets.FxPobTransferSupport.hasPobList(db)) {
                List<PersistentObject> pobs = xapps.gsea.fx.widgets.FxPobTransferSupport.takeDragPobs(e);
                if (!pobs.isEmpty()) {
                    try {
                        File src = AppServices.require().cache().getSourceFile(pobs.get(0));
                        if (src != null) {
                            path = src.getAbsolutePath();
                        }
                    } catch (Throwable ignored) {
                        // leave path null
                    }
                }
            }
            boolean ok = path != null;
            if (ok) {
                field.setText(path);
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }
}
