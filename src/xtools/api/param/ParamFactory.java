/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.Headers;
import edu.mit.broad.genome.alg.DatasetMetrics;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.strucs.CollapsedDetails;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Class ParamFactory
 * collection of commonly used Param objects
 * Factory for commonly used parameters
 * Instantiation through this class helps keep the name and descs etc consistent
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ParamFactory {

    /**
     * Class constructor
     */
    private ParamFactory() {
    }

    public static BooleanParam createZipReportParam(final boolean reqd) {
        return new BooleanParam("zip_report", "Make a zipped file with all reports",
                "Create a zipped file with all files made by the report. This can be emailed to share results",
                false, reqd, Param.ADVANCED);
    }

    public static IntegerParam createNumPermParam(boolean reqd) {
        return createNumPermParam(1000, reqd);
    }

    public static IntegerParam createNumPermParam(final int nperm, boolean reqd) {
        return new IntegerParam("nperm", "Number of permutations", "The number of permutations", nperm, new int[]{0, 1, 10, 100, 1000}, reqd);
    }

    public static BooleanParam createMedianParam(final boolean reqd) {
        return new BooleanParam("median", "Median for class  metrics", "Use the median of each class instead of the mean for the class seperation metrics", XPreferencesFactory.kMedian.getBoolean(), reqd);
    }

    public static IntegerParam createNumMarkersParam(int def, boolean reqd) {
        return new IntegerParam("num", "Number of markers", "Number of markers", def, reqd);
    }

    public static IntegerParam createGeneSetMinSizeParam(int def, boolean reqd) {
        return new IntegerParam("set_min", "Min size: exclude smaller sets", "Gene sets smaller than this number are EXLCUDED from the analysis", def, reqd);
    }

    public static IntegerParam createGeneSetMaxSizeParam(int def, boolean reqd) {
        return new IntegerParam("set_max", "Max size: exclude larger sets", "Gene sets larger than this number are EXLCUDED from the analysis", def, reqd);
    }

    public static Map getMetricParams(BooleanParam medianParam) {
        Map map = DatasetMetrics.getDefaultMetricParams();
        map.put(Headers.USE_MEDIAN, medianParam.getValue());
        return Collections.unmodifiableMap(map);
    }

    // barfs if size is zero or content is zero for all sets
    public static void checkAndBarfIfZeroSets(final GeneSet[] qual_gsets) {

        boolean wasError = false;
        if (qual_gsets.length == 0) {
            wasError = true;
        } else {
            boolean at_least_one_non_empty_set = false;
            for (int i = 0; i < qual_gsets.length; i++) {
                if (qual_gsets[i].getNumMembers() > 0) {
                    at_least_one_non_empty_set = true;
                    break;
                }
            }
            if (!at_least_one_non_empty_set) {
                wasError = true;
            }
        }

        if (wasError) {
            throw new BadParamException("After pruning, none of the gene sets passed size thresholds.", 1001);
        }
    }

    public static void checkIfCollapsedIsEmpty(final CollapsedDetails cd) {

        if (!cd.wasCollapsed) {
            return;
        }

        if (cd.getNumRow_orig() == 0) { // huh
            return;
        }

        if (cd.getNumRow_collapsed() != 0) {
            return;
        }

        throw new BadParamException("The collapsed dataset was empty when used with chip:" + cd.getChipName(), 1005);
    }

    public static ChipChooserMultiParam createChipsTargetParam(boolean reqd) {
        return new ChipChooserMultiParam("chip_target", "Target chip", "The destination chip - to which orthology/homology mappings are converted to", reqd);
    }

    public static BooleanParam createShowEtiologyParam(boolean def, boolean reqd) {
        return new BooleanParam("show_etiology", "Output verbose mapping details", "Show the etiology for the features", def, reqd);
    }

}    // End ParamFactory
