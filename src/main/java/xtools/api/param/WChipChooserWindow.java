/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.io.MSigDBCatalogClient;
import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.actions.ext.BrowserAction;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import xapps.gsea.GseaWebResources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.tree.TreeSelectionModel;

/**
 * @author Aravind Subramanian, David Eby
 */
public class WChipChooserWindow {
    private static final Logger klog = LoggerFactory.getLogger(WChipChooserWindow.class);
    
    public WChipChooserWindow() { }

    public String[] showDirectlyWithModels() {
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel wrapper = new JPanel(new BorderLayout()); // @note needed else the input widget comes up real small in the dd
        wrapper.add(tabbedPane, BorderLayout.CENTER);
        
        Action helpAction = JarResources.createDataFormatAction("#chip");
        Action infoAction = new BrowserAction("MSigDB Chips", "MSigDB Chips Info",
                GuiHelper.ICON_HELP16, GseaWebResources.getGseaChipInfoHelpURL());
        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor("Select a chip", wrapper, helpAction, infoAction, false);
        
        final JTree humanFileJTree = new JTree();
        final JTree mouseFileJTree = new JTree();
        if (XPreferencesFactory.kOnlineMode.getBoolean()) {
            try {
                List<MSigDBRelease> allReleases =
                        MSigDBCatalogClient.fetchReleaseCatalog(XPreferencesFactory.kMSigDBCatalogURL.getString());
                ChooserHelper.populateCatalogTree(humanFileJTree, allReleases, MSigDBSpecies.Human,
                        MSigDBRelease::getChipCatalogUrl, desc, TreeSelectionModel.SINGLE_TREE_SELECTION);
                ChooserHelper.populateCatalogTree(mouseFileJTree, allReleases, MSigDBSpecies.Mouse,
                        MSigDBRelease::getChipCatalogUrl, desc, TreeSelectionModel.SINGLE_TREE_SELECTION);
                tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(humanFileJTree));
                tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(mouseFileJTree));
            } catch (Exception ex) {
                klog.error(ex.getMessage(), ex);
                tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(ChooserHelper.createErrorMessageDisplay(ex)));
                tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(ChooserHelper.createErrorMessageDisplay(ex)));
            }
        } else {
            tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(ChooserHelper.createOfflineMessageDisplay()));
            tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(ChooserHelper.createOfflineMessageDisplay()));
        }

        // TODO: strong typing, should be JList<Chip> (or POB) but need to verify and make changes elsewhere
        final JList localChipJList = new JList(ObjectBindery.getModel(Chip.class));
        localChipJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabbedPane.addTab("Local Chips", new JScrollPane(localChipJList));
        desc.enableDoubleClickableJList(localChipJList);

        BooleanSupplier errorChecker = () -> {
            // Note: based on selected *files* specifically, not raw tree selection, since a
            // selected release (non-leaf) node is not itself a valid choice -- see
            // ChooserHelper.getSelectedFiles().
            boolean haveHuman = !ChooserHelper.getSelectedFiles(humanFileJTree).isEmpty();
            boolean noMouse = ChooserHelper.getSelectedFiles(mouseFileJTree).isEmpty();
            if (haveHuman) { return noMouse && localChipJList.isSelectionEmpty(); }
            if (!noMouse) { return localChipJList.isSelectionEmpty(); }
            return true;
        };
        Supplier<Errors> errorMsgBuilder = () -> {
            Errors errors = new Errors("Multiple CHIPs selected");
            errors.add("Multiple CHIP selections are not allowed.\n");
            errors.add("Is there a selection on another tab?");
            errors.add(ChooserHelper.DESELECT_INSTRUCTIONS);
            return errors;
        };
        Validator errorValidator = new Validator(errorChecker, errorMsgBuilder);
        desc.setErrorValidator(errorValidator);

        desc.setDisplayWider();
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            List<MSigDBCatalogFile> humanSelected = ChooserHelper.getSelectedFiles(humanFileJTree);
            if (!humanSelected.isEmpty()) { return new String[]{ humanSelected.get(0).getPath() }; }

            List<MSigDBCatalogFile> mouseSelected = ChooserHelper.getSelectedFiles(mouseFileJTree);
            if (!mouseSelected.isEmpty()) { return new String[]{ mouseSelected.get(0).getPath() }; }

            // TODO: always Chip/POB, or refactored to String
            Object selectedObj = localChipJList.getSelectedValue();
            if (selectedObj != null) { return new String[]{ ParserFactory.getCache().getSourcePath(selectedObj) }; }

            return new String[] {};
        }
    }
}
