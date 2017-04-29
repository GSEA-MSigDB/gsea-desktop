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

    public static final void oneTimeGseaUpdateCheck(Component parent) {
        if (!XPreferencesFactory.kOnlineMode.getBoolean()) {
            klog.info("Currently running disconnected from the internet: skipping GSEA update check.");
        } else if (MAKE_GSEA_UPDATE_CHECK) {

            // Run the update check here. This will give us a bundle (Props? JSON?) with info on the latest version available
            // from the website. Suppress exceptions; this check should be silent and not hinder the program.
            try {
                String versionQueryString = GSEA_UPDATE_CHECK_URL + "?currentVersion="
                        + GseaFijiTabsApplicationFrame.buildProps.getProperty("build.version", "not_available");

                URL url = new URL(versionQueryString);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(20000);
                String versionCheckInfo = IOUtils.toString(connection.getInputStream(), (Charset) null);

                // Parse this info (Props? JSON?). We're just pretending for now.
                int thisMajor = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.major", "3"), 3);
                int thisMinor = NumberUtils.toInt(GseaFijiTabsApplicationFrame.buildProps.getProperty("build.minor", "0"), 0);
                Properties currentGseaVersionProps = parseGseaVersionInfo(versionCheckInfo);
                int currMajor = NumberUtils.toInt(currentGseaVersionProps.getProperty("build.major", "3"), 3);
                int currMinor = NumberUtils.toInt(currentGseaVersionProps.getProperty("build.minor", "0"), 0);

                // Compare the current website version info to the local info and if so display/log a message.
                if (compareThisVersionToCurrent(thisMajor, currMajor, thisMinor, currMinor)) {
                    String currentVersion = currentGseaVersionProps.getProperty("build.version");
                    String currentTimestamp = currentGseaVersionProps.getProperty("build.timestamp");
                    String message = "A newer version of GSEA (" + currentVersion + ") is available " + IOUtils.LINE_SEPARATOR
                            + "from http://gsea-msigdb.org/gsea/downloads.jsp" + IOUtils.LINE_SEPARATOR + "(build date: "
                            + currentTimestamp + ")";

                    klog.info(message);
                    if (parent != null) {
                        UIUtil.showMessageDialog(parent, message);
                    }
                }
            } catch (Throwable t) {
                // Silently swallow exceptions.
            }
        }

        // Set flag to avoid future update checks.
        MAKE_GSEA_UPDATE_CHECK = false;
    }

    @SuppressWarnings("unchecked")
    private static final Properties parseGseaVersionInfo(String versionCheckInfo) throws ParseException {
        klog.info("Version info:");
        klog.info(versionCheckInfo);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = (JSONObject) (jsonParser.parse(versionCheckInfo));

        Properties currentGseaVersionProps = new Properties();
        currentGseaVersionProps.putAll(jsonObj);
        return currentGseaVersionProps;
    }

    private static final boolean compareThisVersionToCurrent(int thisMajor, int currMajor, int thisMinor, int currMinor) {
        return (currMajor > thisMajor) || (currMajor == thisMajor && currMinor > thisMinor);
    }
}
