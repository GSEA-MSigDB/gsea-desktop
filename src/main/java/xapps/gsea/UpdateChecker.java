/*
 *  Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import java.awt.Component;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.genepattern.uiutil.UIUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.mit.broad.xbench.prefs.XPreferencesFactory;

public class UpdateChecker {
    private static final String GSEA_UPDATE_CHECK_URL = GseaWebResources.getGseaBaseURL() + "/gseaUpdate";
    private static final transient Logger klog = LoggerFactory.getLogger(UpdateChecker.class);
    private static boolean MAKE_GSEA_UPDATE_CHECK = BooleanUtils.toBoolean(System.getProperty("MAKE_GSEA_UPDATE_CHECK", "true"))
            && XPreferencesFactory.kMakeGseaUpdateCheck.getBoolean();
    private static final String UPDATE_CHECK_EXTRA_PROJECT_INFO = System.getProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GSEA");

    public static final void oneTimeGseaUpdateCheck(Component parent) {
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            klog.info("Currently running disconnected from the internet: skipping GSEA update check.");
        } else if (MAKE_GSEA_UPDATE_CHECK) {
            try {
                int currMajor = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.major", "not_found"), -1);
                int currMinor = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.minor", "not_found"), -1);
                int currPatch = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.patchLevel", "not_found"), -1);
                String currVersion = GseaFijiTabsApplicationFrame.buildProps.getProperty("build.version", "not_found");
                
                // Don't make the update check if the current version is in an unrecognizable form.  This is primarily
                // for development builds.
                if (currMajor < 0 || currMinor < 0 || currPatch < 0 || StringUtils.equals(currVersion, "not_found")) {
                    klog.debug("Current version not recognized; skipping update check.");
                } else {
                    String versionQueryString = GSEA_UPDATE_CHECK_URL + "?currentVersion="
                            + GseaFijiTabsApplicationFrame.buildProps.getProperty("build.version", "not_available")
                            + "&extraProjectInfo=" + UPDATE_CHECK_EXTRA_PROJECT_INFO;
    
                    URL url = URI.create(versionQueryString).toURL();
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(20000);
                    String versionCheckInfo = IOUtils.toString(connection.getInputStream(), (Charset) null);
    
                    Properties latestGseaVersionProps = parseGseaVersionInfo(versionCheckInfo);
                    int latestMajor = NumberUtils.toInt(latestGseaVersionProps.getProperty("build.major", ""), currMajor);
                    int latestMinor = NumberUtils.toInt(latestGseaVersionProps.getProperty("build.minor", ""), currMinor);
                    int latestPatch = NumberUtils.toInt(latestGseaVersionProps.getProperty("build.patchLevel", ""), currPatch);
    
                    // Compare the current website version info to the local info and display/log a message if there's a new version.
                    if (newerVersionExists(currMajor, latestMajor, currMinor, latestMinor, currPatch, latestPatch)) {
                        String latestVersion = latestGseaVersionProps.getProperty("build.version", "");
                        String latestTimestamp = latestGseaVersionProps.getProperty("build.timestamp", "");
                        String updateMessage = latestGseaVersionProps.getProperty("build.updateMessage", "");
                        String message = "Your current version of GSEA is " + currVersion + ". A newer version";
                        if (StringUtils.isNotBlank(latestVersion)) {
                            message += " (" + latestVersion + ")";
                        }
                        message += " is available." + IOUtils.LINE_SEPARATOR
                                + "To update, please download from "
                                + GseaWebResources.getGseaBaseURL() + "/gsea/downloads.jsp";
                        if (StringUtils.isNotBlank(latestTimestamp)) {
                                message += IOUtils.LINE_SEPARATOR + "(build date: " + latestTimestamp + ")";
                        }
                        if (StringUtils.isNotBlank(updateMessage)) {
                            message += IOUtils.LINE_SEPARATOR + " " + updateMessage;
                        }
                        
                        klog.info(message);
                        klog.info("Note: GenePattern users should update through GenePattern.");
                        
                        if (parent != null) {
                            UIUtil.showMessageDialog(parent, message);
                        }
                    }
                }
            } catch (Throwable t) {
                // Silently swallow exceptions. Update check failure should not affect normal operation.
            }
        }

        // Set flag to avoid future update checks.
        MAKE_GSEA_UPDATE_CHECK = false;
    }

    private static final Properties parseGseaVersionInfo(String versionCheckInfo) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = (JSONObject) (jsonParser.parse(versionCheckInfo));

        Properties currentGseaVersionProps = new Properties();
        currentGseaVersionProps.putAll(jsonObj);
        return currentGseaVersionProps;
    }

    private static final boolean newerVersionExists(int currMajor, int latestMajor, int currMinor, int latestMinor, int currPatch,
            int latestPatch) {
        return (latestMajor > currMajor) || (latestMajor == currMajor && latestMinor > currMinor)
                || (latestMajor == currMajor && latestMinor == currMinor && latestPatch > currPatch);
    }
}
