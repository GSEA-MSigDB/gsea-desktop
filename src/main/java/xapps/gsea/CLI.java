/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea;

import java.util.Arrays;

/**
 * Headless CLI entry. Dispatches to {@code xtools.*} tool mains (no GenePattern wrappers).
 */
public class CLI {

    private static final String USAGE_MESSAGE = "Usage: operationName followed by operation-specific arguments "
            + "where operationName is one of GSEA, GSEAPreranked, ssGSEA, CollapseDataset, Chip2Chip, or LeadingEdgeTool";

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.err.println(USAGE_MESSAGE);
            System.exit(1);
        }
        String operationName = args[0];
        String[] operationArgs = (args.length == 1) ? new String[]{} : Arrays.copyOfRange(args, 1, args.length);

        if ("GSEA".equalsIgnoreCase(operationName)) {
            xtools.gsea.Gsea.main(operationArgs);
        } else if ("GSEAPreranked".equalsIgnoreCase(operationName)) {
            xtools.gsea.GseaPreranked.main(operationArgs);
        } else if ("ssGSEA".equalsIgnoreCase(operationName) || "SsGsea".equalsIgnoreCase(operationName)) {
            xtools.gsea.SsGsea.main(operationArgs);
        } else if ("CollapseDataset".equalsIgnoreCase(operationName)) {
            xtools.munge.CollapseDataset.main(operationArgs);
        } else if ("Chip2Chip".equalsIgnoreCase(operationName)) {
            xtools.chip2chip.Chip2Chip.main(operationArgs);
        } else if ("LeadingEdgeTool".equalsIgnoreCase(operationName)) {
            xtools.gsea.LeadingEdgeTool.main(operationArgs);
        } else {
            throw new Exception("Usage: unrecognized operationName " + operationName + "\n" + USAGE_MESSAGE);
        }
    }
}
