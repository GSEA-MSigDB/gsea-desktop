/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.genepattern.io.FTPFile;
import org.genepattern.io.FTPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import xapps.gsea.GseaWebResources;

/**
 * Non-UI FTP listing helpers shared by JavaFX gene-set and chip choosers.
 */
public final class FxFtpChooserSupport {
    private static final Logger klog = LoggerFactory.getLogger(FxFtpChooserSupport.class);

    public static final String OFFLINE_MESSAGE =
            "Offline mode" + SystemUtils.LINE_SEPARATOR
                    + "Change this in Menu=>Preferences" + SystemUtils.LINE_SEPARATOR
                    + "Use 'Load Data' to access local files." + SystemUtils.LINE_SEPARATOR
                    + "Choose gene sets from other tabs.";

    public static final String DESELECT_INSTRUCTIONS = SystemUtils.IS_OS_MAC
            ? "Use command-click to select/deselect items."
            : "Use control-click to select/deselect items.";

    private FxFtpChooserSupport() {
    }

    public static boolean isOnline() {
        return XPreferencesFactory.kOnlineMode.getBoolean();
    }

    /**
     * List FTP files ending with {@code suffix}, attaching an {@link MSigDBVersion} parsed from each name.
     */
    public static List<FTPFile> listFtpFiles(String suffix, MSigDBSpecies species, String ftpDir)
            throws Exception {
        FTPList ftpList = null;
        try {
            ftpList = new FTPList(
                    GseaWebResources.getGseaFTPServer(),
                    GseaWebResources.getGseaFTPServerUserName(),
                    GseaWebResources.getGseaFTPServerPassword());
            String[] ftpFileNames = ftpList.getDirectoryListing(ftpDir, null);
            List<FTPFile> files = new ArrayList<>();
            if (ftpFileNames == null) {
                return files;
            }
            for (String ftpFileName : ftpFileNames) {
                String versionId = NamingConventions.extractVersionFromFileName(ftpFileName, suffix);
                files.add(new FTPFile(ftpList.host, ftpDir, ftpFileName, new MSigDBVersion(species, versionId)));
            }
            return files;
        } finally {
            if (ftpList != null) {
                try {
                    ftpList.quit();
                } catch (Exception e) {
                    klog.debug("FTP quit: {}", e.toString());
                }
            }
        }
    }

    public static List<FTPFile> listAndSort(String suffix, MSigDBSpecies species, String ftpDir,
            ComparatorFactory.FTPFileByVersionComparator comparator) throws Exception {
        List<FTPFile> files = listFtpFiles(suffix, species, ftpDir);
        FTPFile[] arr = files.toArray(new FTPFile[0]);
        Arrays.parallelSort(arr, comparator);
        return Arrays.asList(arr);
    }

    /** Style FTP list cells: bold rows whose name contains the highest MSigDB version id. */
    public static void applyLatestVersionBolding(javafx.scene.control.ListView<FTPFile> list,
            ComparatorFactory.FTPFileByVersionComparator comparator) {
        final String highest = comparator != null ? comparator.getHighestVersionId() : null;
        list.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(FTPFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(item.getName());
                setGraphic(xapps.gsea.fx.FxFileIcons.forResource("FTPFile.gif"));
                boolean latest = highest != null && item.getName() != null
                        && item.getName().toLowerCase(java.util.Locale.ROOT)
                        .contains(highest.toLowerCase(java.util.Locale.ROOT));
                setStyle(latest ? "-fx-font-weight: bold;" : "");
            }
        });
    }

    public static String errorListingMessage(Exception e) {
        return "Error listing MSigDB files:" + SystemUtils.LINE_SEPARATOR
                + e.getMessage() + SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR
                + "This might be due to your network's firewall rules." + SystemUtils.LINE_SEPARATOR
                + "MSigDB files can be manually downloaded from www.gsea-msigdb.org/gsea/downloads.jsp"
                + SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR
                + "Use 'Load Data' to provide access to local files." + SystemUtils.LINE_SEPARATOR
                + "Choose gene sets from other tabs.";
    }

    /** Opens {@code url} in the platform browser, reporting failures through the window manager. */
    public static void openUrl(String url) {
        try {
            xapps.gsea.fx.FxDesktopUtil.openUrl(url);
        } catch (Throwable t) {
            Application.getWindowManager().showError(url + ": unable to launch web browser", t);
        }
    }

    /**
     * Adds non-closing Help (data-format anchor) and Info (arbitrary URL) buttons to a dialog's
     * button bar {@code JarResources.createDataFormatAction}
     * with a {@code BrowserAction} info link.
     */
    public static void addHelpAndInfoButtons(Dialog<?> dialog, String dataFormatAnchor,
            String infoLabel, String infoUrl) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        ButtonType infoType = new ButtonType(infoLabel, ButtonBar.ButtonData.HELP);
        dialog.getDialogPane().getButtonTypes().addAll(0, List.of(helpType, infoType));
        wireNonClosingButton(dialog, helpType, () -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL() + dataFormatAnchor));
        wireNonClosingButton(dialog, infoType, () -> openUrl(infoUrl));
    }

    public static void addMsigdbLicenseButton(Dialog<?> dialog) {
        ButtonType licenseType = new ButtonType("MSigDB License", ButtonBar.ButtonData.HELP);
        dialog.getDialogPane().getButtonTypes().add(0, licenseType);
        wireNonClosingButton(dialog, licenseType,
                () -> openUrl(GseaWebResources.getGseaBaseURL() + "/license_terms_list.jsp"));
    }

    /**
     * Adds a single non-closing Help button (data-format anchor) to a dialog's button bar.
     */
    public static void addHelpButton(Dialog<?> dialog, String dataFormatAnchor) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().add(0, helpType);
        wireNonClosingButton(dialog, helpType, () -> openUrl(GseaWebResources.getGseaDataFormatsHelpURL() + dataFormatAnchor));
    }

    /**
     * Adds a single non-closing Help button that opens the User Guide at {@code ugAnchor}
     * (e.g. {@code "#Phenotype-Select-Window")
     */
    public static void addUserGuideHelpButton(Dialog<?> dialog, String ugAnchor) {
        ButtonType helpType = new ButtonType("Help", ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().add(0, helpType);
        wireNonClosingButton(dialog, helpType,
                () -> openUrl(GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/" + ugAnchor));
    }

    private static void wireNonClosingButton(Dialog<?> dialog, ButtonType buttonType, Runnable action) {
        Button button = (Button) dialog.getDialogPane().lookupButton(buttonType);
        button.addEventFilter(ActionEvent.ACTION, e -> {
            action.run();
            e.consume();
        });
    }

    /** Makes double-clicking a list item act like clicking OK */
    public static void enableDoubleClickToFire(ListView<?> list, Button okButton) {
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                okButton.fire();
            }
        });
    }
}
