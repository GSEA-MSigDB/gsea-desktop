/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import java.util.Arrays;

import org.genepattern.modules.Chip2ChipWrapper;
import org.genepattern.modules.CollapseDatasetWrapper;
import org.genepattern.modules.GseaPrerankedWrapper;
import org.genepattern.modules.GseaWrapper;
import org.genepattern.modules.LeadingEdgeToolWrapper;

public class CLI {
    
    // Note that LeadingEdgeTool is *not* given in the usage message as it is not officially supported as a CLI tool.
    private static final String USAGE_MESSAGE = "Usage: operationName followed by operation-specific arguments "
            + "where operationName is one of GSEA, GSEAPreranked, CollapseDataset, or Chip2Chip";

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.err.println(USAGE_MESSAGE);
            System.exit(1);
        }
        String operationName = args[0];
        String[] operationArgs = (args.length == 1) ? new String[]{} : Arrays.copyOfRange(args, 1, args.length);
        
        // TODO: Consider an enum here, if usage ever extends beyond this one location
        if ("GSEA".equalsIgnoreCase(operationName)) {
            GseaWrapper.main(operationArgs);
        } else if ("GSEAPreranked".equalsIgnoreCase(operationName)) {
            GseaPrerankedWrapper.main(operationArgs);
        } else if ("CollapseDataset".equalsIgnoreCase(operationName)) {
            CollapseDatasetWrapper.main(operationArgs);
        } else if ("Chip2Chip".equalsIgnoreCase(operationName)) {
            Chip2ChipWrapper.main(operationArgs);
        } else if ("LeadingEdgeTool".equalsIgnoreCase(operationName)) {
            LeadingEdgeToolWrapper.main(operationArgs);
        } else {
            throw new Exception("Usage: unrecognized operationName " + operationName + "\n" + USAGE_MESSAGE);
        }
    }
}
