/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Finds files inside a GSEA report directory. Tab and viewer construction stays in
 * {@link ReportExplorerSupport}.
 */
public final class ReportFiles {
    private ReportFiles() {
    }

    public static File findFirst(File dir, Predicate<String> nameMatch) {
        List<File> all = findAll(dir, nameMatch);
        return all.isEmpty() ? null : all.get(0);
    }

    public static List<File> findAll(File dir, Predicate<String> nameMatch) {
        List<File> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory() || nameMatch == null) {
            return out;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return out;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            if (f.isFile() && nameMatch.test(f.getName())) {
                out.add(f);
            }
        }
        return out;
    }

    public static List<File> findImages(File dir, Predicate<String> nameMatch) {
        return findAll(dir, name -> isImageName(name) && (nameMatch == null || nameMatch.test(name)));
    }

    public static boolean isImageName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    public static boolean endsWithIgnoreCase(String name, String suffix) {
        return name != null && suffix != null
                && name.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
    }

    public static File resolveEdbDir(File reportDir) {
        if (reportDir == null) {
            return null;
        }
        File nested = new File(reportDir, "edb");
        if (new File(nested, "results.edb").isFile()) {
            return nested;
        }
        if (new File(reportDir, "results.edb").isFile()) {
            return reportDir;
        }
        return nested.isDirectory() ? nested : reportDir;
    }
}
