/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

public class UpdateChecker {
    private static final String GSEA_UPDATE_CHECK_URL = GseaWebResources.getGseaBaseURL() + "/gseaUpdate";
    private static final transient Logger klog = LoggerFactory.getLogger(UpdateChecker.class);
    private static boolean MAKE_GSEA_UPDATE_CHECK = BooleanUtils.toBoolean(System.getProperty("MAKE_GSEA_UPDATE_CHECK", "true"))
            && XPreferencesFactory.kMakeGseaUpdateCheck.getBoolean();
    private static final String UPDATE_CHECK_EXTRA_PROJECT_INFO = System.getProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GSEA");
    private static final Properties buildProps = JarResources.getBuildInfo();

    /**
     * One-time startup check: logs any update message and disables further automatic checks.
     */
    public static final void oneTimeGseaUpdateCheck() {
        Optional<String> message = oneTimeGseaUpdateCheckMessage();
        message.ifPresent(klog::info);
    }

    /**
     * One-time startup check that returns a UI-ready message when a newer version exists.
     */
    public static Optional<String> oneTimeGseaUpdateCheckMessage() {
        Optional<String> message = Optional.empty();
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            klog.info("Currently running disconnected from the internet: skipping GSEA update check.");
        } else if (MAKE_GSEA_UPDATE_CHECK) {
            message = checkForUpdateMessage(false);
        }
        MAKE_GSEA_UPDATE_CHECK = false;
        return message;
    }

    /**
     * Manual / forced update check (Help → Check for updates). Honors online mode.
     */
    public static Optional<String> checkForUpdateMessage() {
        return checkForUpdateMessage(true);
    }

    private static Optional<String> checkForUpdateMessage(boolean force) {
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            if (force) {
                return Optional.of("Currently running disconnected from the internet. "
                        + "Enable \"Connect over the Internet\" in Preferences to check for updates.");
            }
            return Optional.empty();
        }

        try {
            int currMajor = NumberUtils.toInt(buildProps.getProperty("build.major", "not_found"), -1);
            int currMinor = NumberUtils.toInt(buildProps.getProperty("build.minor", "not_found"), -1);
            int currPatch = NumberUtils.toInt(buildProps.getProperty("build.patchLevel", "not_found"), -1);
            String currVersion = buildProps.getProperty("build.version", "not_found");

            if (currMajor < 0 || currMinor < 0 || currPatch < 0 || StringUtils.equals(currVersion, "not_found")) {
                klog.debug("Current version not recognized; skipping update check.");
                return Optional.empty();
            }

            String versionQueryString = GSEA_UPDATE_CHECK_URL + "?currentVersion="
                    + buildProps.getProperty("build.version", "not_available")
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
                return Optional.of(message);
            }

            if (force) {
                return Optional.of("You are running the latest version of GSEA (" + currVersion + ").");
            }
        } catch (Throwable t) {
            // Update check failure should not affect normal operation.
            if (force) {
                return Optional.of("Could not check for updates: " + t.getMessage());
            }
        }
        return Optional.empty();
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
