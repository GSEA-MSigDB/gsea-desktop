/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

/**
 * @author Aravind Subramanian
 */
public class FeatureSpaceReqdParam extends StringReqdParam {
    private static String[] MODES = new String[] { "Remap_Only", "Collapse", "No_Collapse" };

    public FeatureSpaceReqdParam() {
        super(FEATURE_SPACE, FEATURE_SPACE_ENGLISH, FEATURE_SPACE_DESC, MODES[1], MODES);
    }

    public FeatureSpaceReqdParam(String def) {
        super(FEATURE_SPACE, FEATURE_SPACE_ENGLISH, FEATURE_SPACE_DESC, def, MODES);
    }

    @Override
    public void setValue(Object value) {
        if ("true".equals(value)) {
            value = "Collapse";
        }
        if ("false".equals(value)) {
            value = "No_Collapse";
        }
        super.setValue(value);
    }

    public boolean isSymbols() {
        int index = getStringIndexChoosen();
        return (index <= 1);
    }

    public boolean isRemap() {
        int index = getStringIndexChoosen();
        return (index == 0);
    }
}
