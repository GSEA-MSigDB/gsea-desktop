package xapps.gsea;

import java.awt.Component;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.genepattern.uiutil.UIUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

public class UpdateChecker {

    private static final String GSEA_UPDATE_CHECK_URL = GseaWebResources.getGseaBaseURL() + "/gseaUpdate";

    private static final transient Logger klog = XLogger.getLogger(UpdateChecker.class);

    private static boolean MAKE_GSEA_UPDATE_CHECK = BooleanUtils.toBoolean(System.getProperty("MAKE_GSEA_UPDATE_CHECK", "true"))
            && XPreferencesFactory.kMakeGseaUpdateCheck.getBoolean();

    private static final String UPDATE_CHECK_EXTRA_PROJECT_INFO = System.getProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GSEA");

    public static final void oneTimeGseaUpdateCheck(Component parent) {
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            klog.info("Currently running disconnected from the internet: skipping GSEA update check.");
        } else if (MAKE_GSEA_UPDATE_CHECK) {

            try {
                String versionQueryString = GSEA_UPDATE_CHECK_URL + "?currentVersion="
                        + GseaFijiTabsApplicationFrame.buildProps.getProperty("build.version", "not_available")
                        + "&extraProjectInfo=" + UPDATE_CHECK_EXTRA_PROJECT_INFO;

                URL url = new URL(versionQueryString);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(20000);
                String versionCheckInfo = IOUtils.toString(connection.getInputStream(), (Charset) null);

                int currMajor = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.major", "3"), 3);
                int currMinor = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.minor", "0"), 0);
                int currPatch = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.patchLevel", "0"), 0);
                String currVersion = GseaFijiTabsApplicationFrame.buildProps.getProperty("build.version", "3.0");
                Properties latestGseaVersionProps = parseGseaVersionInfo(versionCheckInfo);
                int latestMajor = NumberUtils.toInt(latestGseaVersionProps.getProperty("build.major", "3"), 3);
                int latestMinor = NumberUtils.toInt(latestGseaVersionProps.getProperty("build.minor", "0"), 0);
                int latestPatch = NumberUtils.toInt(latestGseaVersionProps.getProperty("build.patchLevel", "0"), 0);

                // Compare the current website version info to the local info and if so display/log a message.
                if (newerVersionExists(currMajor, latestMajor, currMinor, latestMinor, currPatch, latestPatch)) {
                    String latestVersion = latestGseaVersionProps.getProperty("build.version");
                    String latestTimestamp = latestGseaVersionProps.getProperty("build.timestamp");
                    String message = "Your current version of GSEA is " + currVersion + ". A newer version (" + latestVersion
                            + ") is available." + IOUtils.LINE_SEPARATOR
                            + "To update, please download from http://gsea-msigdb.org/gsea/downloads.jsp" + IOUtils.LINE_SEPARATOR
                            + "(build date: " + latestTimestamp + ")";

                    klog.info(message);
                    klog.info("Note: GenePattern users should update through GenePattern.");
                    
                    // 
                    
                    if (parent != null) {
                        UIUtil.showMessageDialog(parent, message);
                    }
                }
            } catch (Throwable t) {
                // Silently swallow exceptions. Update check failure should not affect normal operation.
            }
        }

        // Set flag to avoid future update checks.
        MAKE_GSEA_UPDATE_CHECK = false;
    }

    @SuppressWarnings("unchecked")
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
