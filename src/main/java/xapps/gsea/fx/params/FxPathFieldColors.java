/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.StringTokenizer;

import edu.mit.broad.genome.parsers.AuxUtils;
import javafx.css.PseudoClass;
import javafx.scene.control.TextInputControl;

/**
 * Path-field good/bad coloring matching Swing {@code GFieldUtils}
 * (blue = good, red = bad; URL schemes; comma-separated multi-paths; AuxUtils).
 */
public final class FxPathFieldColors {

    private static final PseudoClass GOOD = PseudoClass.getPseudoClass("gsea-path-good");
    private static final PseudoClass BAD = PseudoClass.getPseudoClass("gsea-path-bad");

    private FxPathFieldColors() {
    }

    /** Apply validity pseudo-classes; keeps other style classes (e.g. {@code gsea-dir-field}). */
    public static void apply(TextInputControl field, String pathOrPaths) {
        if (field == null) {
            return;
        }
        boolean good = isGood(pathOrPaths);
        field.pseudoClassStateChanged(GOOD, good);
        field.pseudoClassStateChanged(BAD, !good);
    }

    public static void attach(TextInputControl field) {
        if (field == null) {
            return;
        }
        field.textProperty().addListener((obs, o, n) -> apply(field, n));
        apply(field, field.getText());
    }

    public static boolean isGood(String pathOrPaths) {
        if (pathOrPaths == null) {
            return false;
        }
        // Match Swing getFileFieldColor outer checks (no trim before scheme check).
        if (pathOrPaths.startsWith("http") || pathOrPaths.startsWith("ftp")
                || pathOrPaths.startsWith("gseaftp")) {
            return true;
        }
        pathOrPaths = pathOrPaths.trim();
        if (pathOrPaths.isEmpty()) {
            return false;
        }
        if (pathOrPaths.indexOf(',') == -1) {
            return isGoodSingle(pathOrPaths);
        }
        // Swing does not trim multipath tokens.
        StringTokenizer tok = new StringTokenizer(pathOrPaths, ",");
        while (tok.hasMoreTokens()) {
            if (!isGoodSingle(tok.nextToken())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGoodSingle(String pathMaybeWithAux) {
        if (pathMaybeWithAux == null || pathMaybeWithAux.length() == 0) {
            return false;
        }
        String lower = pathMaybeWithAux.toLowerCase();
        if (lower.startsWith("http") || lower.startsWith("www") || lower.startsWith("ftp")
                || lower.startsWith("gseaftp")) {
            return true;
        }
        File file = AuxUtils.getBaseFileFromFullPath(pathMaybeWithAux);
        return file != null && file.exists();
    }
}
