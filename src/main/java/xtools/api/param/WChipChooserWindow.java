/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.alg.ComparatorFactory;
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

import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

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
        
        Action helpAction = JarResources.createHelpAction(Param.CHIP);
        Action infoAction = new BrowserAction("MSigDB Chips", "MSigDB Chips Info",
                GuiHelper.ICON_HELP16, GseaWebResources.getGseaChipInfoHelpURL());
        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor("Select a chip", wrapper, helpAction, infoAction, false);
        
        final JList<FTPFile> humanFTPFileJList = new JList<FTPFile>();
        final JList<FTPFile> mouseFTPFileJList = new JList<FTPFile>();
        if (XPreferencesFactory.kOnlineMode.getBoolean()) {
            FTPList ftpList = null;
            try {
                ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                        GseaWebResources.getGseaFTPServerUserName(), GseaWebResources.getGseaFTPServerPassword());
                try {
                    FTPFile[] humanFTPFiles = ChooserHelper.retrieveFTPFiles(ftpList, ".chip",
                            MSigDBSpecies.Human, GseaWebResources.getGseaFTPServerChipDir(MSigDBSpecies.Human));
                    FTPFile[] mouseFTPFiles = ChooserHelper.retrieveFTPFiles(ftpList, ".chip",
                            MSigDBSpecies.Mouse, GseaWebResources.getGseaFTPServerChipDir(MSigDBSpecies.Mouse));
                    ChooserHelper.populateFTPModel(humanFTPFiles, humanFTPFileJList, desc,
                            new ComparatorFactory.FTPFileByVersionComparator(), ListSelectionModel.SINGLE_SELECTION);
                    ChooserHelper.populateFTPModel(mouseFTPFiles, mouseFTPFileJList, desc,
                            new ComparatorFactory.FTPFileByVersionComparator("Mouse"), ListSelectionModel.SINGLE_SELECTION);
                    tabbedPane.addTab("Human Collection Chips (MSigDB)", new JScrollPane(humanFTPFileJList));
                    tabbedPane.addTab("Mouse Collection Chips (MSigDB)", new JScrollPane(mouseFTPFileJList));
                } finally {
                    if (ftpList != null) { ftpList.quit(); }
                }
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
            boolean haveHuman = !humanFTPFileJList.isSelectionEmpty();
            boolean noMouse = mouseFTPFileJList.isSelectionEmpty();
            if (haveHuman) { return noMouse && localChipJList.isSelectionEmpty(); }
            if (!noMouse) { return localChipJList.isSelectionEmpty(); }
            return true;
        };
        Supplier<Errors> errorMsgBuilder = () -> {
            Errors errors = new Errors("Multiple CHIPs selected");
            errors.add("Multiple CHIP selections are not allowed.");
            errors.add("Is there a selection on another tab?");
            return errors;
        };
        Validator errorValidator = new Validator(errorChecker, errorMsgBuilder);
        desc.setErrorValidator(errorValidator);

        desc.setDisplayWider();
        int res = desc.show();
        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {
            FTPFile selected = humanFTPFileJList.getSelectedValue();
            if (selected != null) { return new String[]{ selected.getPath() }; }

            selected = mouseFTPFileJList.getSelectedValue();
            if (selected != null) { return new String[]{ selected.getPath() }; }
            
            // TODO: always Chip/POB, or refactored to String
            Object selectedObj = localChipJList.getSelectedValue();
            if (selectedObj != null) { return new String[]{ ParserFactory.getCache().getSourcePath(selectedObj) }; }

            return new String[] {};
        }
    }
}
