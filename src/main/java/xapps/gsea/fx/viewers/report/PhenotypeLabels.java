/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.AuxUtils;

/**
 * Shared phenotype short/long labels from an {@link EnrichmentDb} template
 * (mirrors EnrichmentReports classA/classB naming).
 */
public record PhenotypeLabels(String shortName, String longName, boolean positive) {

    public static PhenotypeLabels from(EnrichmentDb edb, boolean positive) {
        Template template = null;
        try {
            template = edb != null ? edb.getTemplate() : null;
        } catch (Throwable ignored) {
            // preranked / missing template
        }
        if (template == null) {
            String shortName = Constants.NA + (positive ? "_pos" : "_neg");
            return new PhenotypeLabels(shortName,
                    positive ? "positive correlation with profile" : "negative correlation with profile",
                    positive);
        }
        if (template.isContinuous()) {
            String nn = AuxUtils.getAuxNameOnlyNoHash(template);
            return new PhenotypeLabels(nn + (positive ? "_pos" : "_neg"),
                    positive ? "positive correlation with profile" : "negative correlation with profile",
                    positive);
        }
        try {
            int idx = positive ? 0 : 1;
            String shortName = template.getClassName(idx);
            return new PhenotypeLabels(shortName,
                    shortName + " (" + template.getClass(idx).getSize() + " samples)",
                    positive);
        } catch (Throwable t) {
            String shortName = Constants.NA + (positive ? "_pos" : "_neg");
            return new PhenotypeLabels(shortName, shortName, positive);
        }
    }
}
