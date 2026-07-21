/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/**
 * FX drag payload:
 * pob-list (in-JVM) + file list + path string.
 */
public final class FxPobTransferSupport {

    /** Marker format analogous to . */
    public static final DataFormat POB_LIST_FORMAT =
            new DataFormat("application/x-gsea-pob-list");

    private static List<PersistentObject> dragPobs = List.of();

    private FxPobTransferSupport() {
    }

    public static void startPobDrag(Node source, List<PersistentObject> pobs, MouseEvent event) {
        if (source == null || event == null || pobs == null || pobs.isEmpty()) {
            return;
        }
        List<PersistentObject> snapshot = List.copyOf(pobs);
        List<File> files = new ArrayList<>();
        for (PersistentObject pob : snapshot) {
            try {
                File f = ParserFactory.getCache().getSourceFile(pob);
                if (f != null) {
                    files.add(f);
                }
            } catch (Throwable ignored) {
                // omit when no source path
            }
        }

        Dragboard db = source.startDragAndDrop(TransferMode.COPY);
        ClipboardContent content = new ClipboardContent();
        content.put(POB_LIST_FORMAT, Integer.toString(snapshot.size()));
        if (!files.isEmpty()) {
            content.putString(pathsString(files));
            content.putFiles(files);
        } else {
            StringBuilder names = new StringBuilder();
            for (PersistentObject pob : snapshot) {
                names.append(pob.getName()).append('\n');
            }
            content.putString(names.toString());
        }
        dragPobs = snapshot;
        db.setContent(content);
        event.consume();
    }

    public static boolean hasPobList(Dragboard db) {
        return db != null && db.hasContent(POB_LIST_FORMAT);
    }

    public static List<PersistentObject> getDragPobs() {
        return dragPobs != null ? dragPobs : List.of();
    }

    public static List<PersistentObject> takeDragPobs(DragEvent event) {
        if (event == null || !hasPobList(event.getDragboard())) {
            return List.of();
        }
        List<PersistentObject> out = dragPobs;
        clear();
        return out != null ? out : List.of();
    }

    public static void clear() {
        dragPobs = List.of();
    }

    private static String pathsString(List<File> files) {
        StringBuilder buf = new StringBuilder();
        for (File f : files) {
            buf.append(f.getAbsolutePath()).append('\n');
        }
        return buf.toString();
    }
}
