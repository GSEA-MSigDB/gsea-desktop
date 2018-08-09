/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import java.io.File;
import java.io.Serializable;
import java.util.Properties;

/**
 * @author Aravind Subramanian
 */
public interface ParamSet extends Serializable {

    public static final String PARAM_FILE = "param_file";

    public GuiParam getGuiParam();

    public ReportDirParam getAnalysisDirParam();

    public ReportLabelParam getReportLabelParam();

    public FoundMissingFile fileCheckingFill(Properties prop);

    public boolean isRequiredAllSet();

    public int getNumParams();

    public Param getParam(int pos);

    public Param[] getParams(Param.Type thisType, boolean excludeCommonOnes);

    public Properties toProperties();

    public String getAsCommand(boolean includecp, boolean fullfilepaths, boolean ignoreguiparam);

    /**
     * fill params but dont fill those that have missing files
     * instead put them into a FoundMissingFile[] missing object and return that
     *
     * @param prop
     */

    public static class FoundMissingFile {

        public File[] foundFiles;
        public File[] missingFiles;
        public String[] foundFilesParamNames;

    }

} // End interface ParamSet
