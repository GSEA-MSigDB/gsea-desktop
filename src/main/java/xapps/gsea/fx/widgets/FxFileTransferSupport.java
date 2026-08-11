/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/**
 * FX file drag-out and clipboard support
 * ({@code javaFileListFlavor} + path string).
 */
public final class FxFileTransferSupport {

    private FxFileTransferSupport() {
    }

    public static void copyFilesToClipboard(List<File> files) {
        List<File> all = nonNullFiles(files);
        ClipboardContent content = new ClipboardContent();
        content.putString(pathsString(all));
        content.putFiles(all);
        Clipboard.getSystemClipboard().setContent(content);
    }

    public static void startFileDrag(Node source, List<File> files, MouseEvent event) {
        List<File> all = nonNullFiles(files);
        if (all.isEmpty() || source == null || event == null) {
            return;
        }
        Dragboard db = source.startDragAndDrop(TransferMode.COPY);
        ClipboardContent content = new ClipboardContent();
        content.putString(pathsString(all));
        content.putFiles(all);
        db.setContent(content);
        event.consume();
    }

    /**
     * Resolve drop payload preferring the path string when it lists more entries than the
     * file-list flavor (OS / FX may strip missing files from {@code DataFormat.FILES}).
     */
    public static List<File> filesFromDragboard(javafx.scene.input.Dragboard db) {
        if (db == null) {
            return Collections.emptyList();
        }
        List<File> fromFiles = db.hasFiles() ? nonNullFiles(db.getFiles()) : Collections.emptyList();
        List<File> fromString = Collections.emptyList();
        if (db.hasString() && db.getString() != null && !db.getString().isBlank()) {
            List<File> parsed = new ArrayList<>();
            for (String line : db.getString().split("\\R")) {
                String path = line.trim();
                if (!path.isEmpty()) {
                    parsed.add(new File(path));
                }
            }
            fromString = parsed;
        }
        if (fromString.size() > fromFiles.size()) {
            return fromString;
        }
        if (!fromFiles.isEmpty()) {
            return fromFiles;
        }
        return fromString;
    }

    private static List<File> nonNullFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<File> out = new ArrayList<>();
        for (File f : files) {
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    private static String pathsString(List<File> files) {
        StringBuilder buf = new StringBuilder();
        for (File f : files) {
            buf.append(f.getAbsolutePath()).append('\n');
        }
        return buf.toString();
    }
}
