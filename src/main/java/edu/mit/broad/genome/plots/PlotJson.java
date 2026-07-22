/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.plots;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONObject;

/** JSON helpers for {@link PlotSpec}. */
public final class PlotJson {

    private PlotJson() {
    }

    public static String toJsonString(PlotSpec spec) {
        if (spec == null) {
            return "{}";
        }
        JSONObject o = spec.toJson();
        return o.toJSONString();
    }

    public static void write(PlotSpec spec, File file) throws IOException {
        if (spec == null || file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (FileWriter w = new FileWriter(file, StandardCharsets.UTF_8)) {
            w.write(toJsonString(spec));
        }
    }

    /** Write {@code foo.plot.json} next to {@code foo.png}. */
    public static File writeAlongsidePng(PlotSpec spec, File pngFile) throws IOException {
        if (spec == null || pngFile == null) {
            return null;
        }
        String path = pngFile.getPath();
        File json = path.toLowerCase().endsWith(".png")
                ? new File(path.substring(0, path.length() - 4) + ".plot.json")
                : new File(pngFile.getParentFile(), pngFile.getName() + ".plot.json");
        write(spec, json);
        return json;
    }
}
