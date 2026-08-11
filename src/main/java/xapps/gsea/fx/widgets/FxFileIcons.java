/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.widgets;

import java.io.File;
import java.net.URL;
import java.util.Locale;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.vdb.chip.Chip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** File / object type icons */
public final class FxFileIcons {

    private FxFileIcons() {
    }

    public static ImageView forFile(File file) {
        return view(iconResourceForFile(file));
    }

    public static ImageView forObject(Object obj) {
        if (obj instanceof File) {
            return forFile((File) obj);
        }
        if (obj instanceof PersistentObject) {
            return view(iconResourceForPersistentObject((PersistentObject) obj));
        }
        return view("UnknownDataFormat16.gif");
    }

    public static ImageView forResource(String resourceName) {
        return view(resourceName != null ? resourceName : "UnknownDataFormat16.gif");
    }

    public static ImageView reportStubIcon() {
        return view("Rpt15.png");
    }

    private static ImageView view(String resource) {
        URL url = JarResources.toURL(resource);
        if (url == null) {
            url = JarResources.toURL("UnknownDataFormat16.gif");
        }
        if (url == null) {
            return null;
        }
        ImageView iv = new ImageView(new Image(url.toExternalForm(), 16, 16, true, true));
        iv.setFitWidth(16);
        iv.setFitHeight(16);
        return iv;
    }

    private static String iconResourceForFile(File file) {
        if (file == null) {
            return "UnknownDataFormat16.gif";
        }
        String ext = NamingConventions.getExtension(file);
        if (ext == null || ext.isBlank()) {
            return "UnknownDataFormat16.gif";
        }
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "gct" -> "Gct16.gif";
            case "res" -> "Res16.gif";
            case "pcl" -> "Pcl.gif";
            case "txt" -> "Txt.gif";
            case "tsv", "xls" -> "Xls.gif";
            case "xml" -> "Xml.gif";
            case "cls" -> "Cls.gif";
            case "grp" -> "Grp.gif";
            case "gmx" -> "Gmx.png";
            case "gmt" -> "Gmt.png";
            case "rnk" -> "Rnk.png";
            case "chip" -> "Chip16.png";
            case "edb" -> "Edb.png";
            case "rpt" -> "Rpt.gif";
            default -> "UnknownDataFormat16.gif";
        };
    }

    private static String iconResourceForPersistentObject(PersistentObject pob) {
        try {
            Class<?> rep = DataFormat.getRepresentationClass(pob);
            if (rep == Dataset.class) {
                return "Res16.gif";
            }
            if (rep == Template.class) {
                return "Cls.gif";
            }
            if (rep == GeneSet.class) {
                return "Grp.gif";
            }
            if (rep == GeneSetMatrix.class) {
                return "Gmt.png";
            }
            if (rep == RankedList.class) {
                return "Rnk.png";
            }
            if (rep == Chip.class) {
                return "Chip16.png";
            }
            if (rep == EnrichmentDb.class) {
                return "Edb.png";
            }
            if (rep == Report.class) {
                return "Rpt.gif";
            }
        } catch (Throwable ignored) {
            // Fall through.
        }
        return "IconNotFound.gif";
    }
}
