/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import xapps.gsea.GseaWebResources;
import xapps.gsea.fx.widgets.FxFileIcons;

/**
 * File, Downloads, and Help menus for the JavaFX shell.
 */
final class GseaFxMenus {

    interface Host {
        void openPreferences();

        void clearRecentFileHistory();

        void exitApplication();

        void showApplicationMessages();

        void browse(String url);

        void showError(String message, Throwable error);

        File runtimeHome();

        File defaultOutputDir();

        String version();

        String buildNumber();

        String buildTimestamp();
    }

    private GseaFxMenus() {
    }

    static MenuBar build(Host host) {
        MenuBar bar = new MenuBar();

        Menu file = new Menu("File");
        MenuItem prefs = item("Preferences ...",
                "View and modify application wide preferences", "Preferences16.gif");
        prefs.setOnAction(e -> host.openPreferences());
        MenuItem clearHistory = item("Clear recent file history",
                "Clear recent file history (from the 'Load Data' panel)", null);
        clearHistory.setOnAction(e -> host.clearRecentFileHistory());
        file.getItems().addAll(prefs, new SeparatorMenuItem(), clearHistory);
        if (!SystemUtils.IS_OS_MAC) {
            MenuItem exit = item("Exit", "Quit the GSEA application", null);
            exit.setOnAction(e -> host.exitApplication());
            file.getItems().addAll(new SeparatorMenuItem(), exit);
        }

        Menu downloads = new Menu("Downloads");
        downloads.getItems().addAll(
                link(host, "Download Human chip annotations",
                        "Download Human array annotation files - useful to annotate GSEA reports",
                        GseaWebResources.getHumanArrayAnnotationsURL(), false),
                link(host, "Download Mouse chip annotations",
                        "Download Mouse array annotation files - useful to annotate GSEA reports",
                        GseaWebResources.getMouseArrayAnnotationsURL(), false),
                link(host, "Download example datasets",
                        "Download example datasets for GSEA - expression data, phenotype labels and gene sets files",
                        GseaWebResources.getGseaExamplesURL(), false));

        Menu help = new Menu("Help");
        MenuItem buildInfo = new MenuItem("GSEA v" + host.version() + " [build: " + host.buildNumber() + "]");
        MenuItem showHome = item("Show GSEA home folder",
                "Show runtime home directory of this application", null);
        showHome.setOnAction(e -> openFolder(host, host.runtimeHome()));
        MenuItem showOut = item("Show GSEA output folder (default location)",
                "Show output directory of this application", null);
        showOut.setOnAction(e -> openFolder(host, host.defaultOutputDir()));
        MenuItem appMessages = item("Application messages",
                "Show unattributed application messages (not tied to a job)", null);
        appMessages.setOnAction(e -> host.showApplicationMessages());
        help.getItems().addAll(
                link(host, "GSEA web site", "Open the GSEA website in a web browser",
                        GseaWebResources.getGseaBaseURL(), true),
                link(host, "GSEA documentation",
                        "Online documentation of the GSEA algorithm and software",
                        GseaWebResources.getGseaHelpURL(), true),
                link(host, "GSEA & MSigDB License Terms", "GSEA & MSigDB License Terms",
                        GseaWebResources.getGseaBaseURL() + "/license_terms_list.jsp", true),
                new SeparatorMenuItem(),
                showHome,
                showOut,
                appMessages,
                new SeparatorMenuItem(),
                link(host, "Contact Us", "Contact Us", GseaWebResources.getGseaContactURL(), false),
                new SeparatorMenuItem(),
                buildInfo);
        String ts = host.buildTimestamp();
        if (ts == null || ts.isBlank()) {
            help.getItems().add(new SeparatorMenuItem());
        } else {
            help.getItems().add(new MenuItem("Built: " + ts));
        }

        bar.getMenus().addAll(file, downloads, help);
        return bar;
    }

    private static void openFolder(Host host, File dir) {
        if (dir == null) {
            return;
        }
        try {
            xapps.gsea.fx.FxDesktopUtil.openInOsExplorer(dir);
        } catch (Exception ex) {
            host.showError("Trouble launching File Explorer on path '" + dir.getPath() + "'", ex);
        }
    }

    private static MenuItem item(String name, String tooltip, String iconResource) {
        Label label = new Label(name);
        if (iconResource != null && !iconResource.isBlank()) {
            label.setGraphic(FxFileIcons.forResource(iconResource));
        }
        if (tooltip != null && !tooltip.isBlank()) {
            label.setTooltip(new Tooltip(tooltip));
        }
        CustomMenuItem menuItem = new CustomMenuItem(label);
        menuItem.setHideOnClick(true);
        return menuItem;
    }

    private static MenuItem link(Host host, String name, String tooltip, String url, boolean helpIcon) {
        MenuItem menuItem = item(name, tooltip, helpIcon ? "Help16_v2.gif" : null);
        menuItem.setOnAction(e -> host.browse(url));
        return menuItem;
    }
}
