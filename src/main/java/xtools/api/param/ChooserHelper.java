/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.io.MSigDBCatalogClient;
import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import xtools.api.ui.MSigDBCatalogTreeCellRenderer;

/**
 * @author Aravind Subramanian, David Eby
 */
public class ChooserHelper {
    private static final Logger klog = LoggerFactory.getLogger(ChooserHelper.class);

    /**
     * Populates (in place) a JTree with the MSigDB releases for one species, taken from an
     * already-fetched MSigDB release catalog. Releases are sorted newest-first; the single most
     * recent release has its file catalog (a GMT file catalog or a CHIP catalog, depending on
     * {@code fileCatalogUrlExtractor}) fetched eagerly, while every older release gets a
     * placeholder child that is lazily replaced the first time that release's node is expanded
     * (see {@link #createLazyFileFetchListener}). {@code MSigDBCatalogClient}'s own session cache
     * means neither the eager nor a lazy fetch ever repeats an HTTP call for a catalog URL already
     * fetched once in this process.
     *
     * @param fileCatalogUrlExtractor {@code MSigDBRelease::getGeneSetsCatalogUrl} or
     *        {@code MSigDBRelease::getChipCatalogUrl}, selecting which of a release's two file
     *        catalogs this tree should show.
     * @param selectionMode a {@code TreeSelectionModel} selection mode constant.
     */
    public static void populateCatalogTree(JTree tree, List<MSigDBRelease> allReleases, MSigDBSpecies species,
            Function<MSigDBRelease, String> fileCatalogUrlExtractor, DialogDescriptor desc, int selectionMode)
            throws IOException {
        List<MSigDBRelease> releases = new ArrayList<MSigDBRelease>();
        for (MSigDBRelease release : allReleases) {
            if (release.getSpecies() == species) { releases.add(release); }
        }

        ComparatorFactory.MSigDBReleaseByVersionComparator comp = new ComparatorFactory.MSigDBReleaseByVersionComparator();
        Collections.sort(releases, comp);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (int i = 0; i < releases.size(); i++) {
            MSigDBRelease release = releases.get(i);
            DefaultMutableTreeNode releaseNode = new DefaultMutableTreeNode(release);
            if (i == 0) {
                // Eager-fetch only the single most recent release for this species. An error here
                // is allowed to propagate (same as a release-catalog fetch failure) rather than be
                // handled per-node, since without the top item the tab has little practical use.
                addFileChildren(releaseNode, MSigDBCatalogClient.fetchFileCatalog(fileCatalogUrlExtractor.apply(release)));
            } else {
                releaseNode.add(new DefaultMutableTreeNode(MSigDBCatalogTreeCellRenderer.LOADING_PLACEHOLDER, false));
            }
            root.add(releaseNode);
        }

        tree.setModel(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new MSigDBCatalogTreeCellRenderer(comp.getHighestVersionId()));
        tree.getSelectionModel().setSelectionMode(selectionMode);
        tree.addTreeWillExpandListener(createLazyFileFetchListener(fileCatalogUrlExtractor));
        // Selecting a release (non-leaf) node is not a meaningful choice -- getSelectedFiles()
        // already ignores one if somehow selected, but immediately reverting it here avoids the
        // more confusing appearance of a "selected" row that silently does nothing on OK.
        tree.addTreeSelectionListener((TreeSelectionEvent event) -> {
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) { return; }
            for (TreePath path : paths) {
                Object last = path.getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode && !((DefaultMutableTreeNode) last).isLeaf()) {
                    tree.removeSelectionPath(path);
                }
            }
        });
        desc.enableDoubleClickableJTree(tree);
    }

    /**
     * Returns the {@link MSigDBCatalogFile} leaf nodes currently selected in the tree, silently
     * ignoring any selected release (non-leaf) node -- selecting a release itself does not mean
     * "select all of its files," matching how the old flat file list never offered that either.
     */
    public static List<MSigDBCatalogFile> getSelectedFiles(JTree tree) {
        List<MSigDBCatalogFile> selected = new ArrayList<MSigDBCatalogFile>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                Object last = path.getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) last).getUserObject();
                    if (userObject instanceof MSigDBCatalogFile) {
                        selected.add((MSigDBCatalogFile) userObject);
                    }
                }
            }
        }
        return selected;
    }

    private static void addFileChildren(DefaultMutableTreeNode releaseNode, List<MSigDBCatalogFile> files) {
        if (files.isEmpty()) {
            releaseNode.add(new DefaultMutableTreeNode("No files available", false));
            return;
        }
        for (MSigDBCatalogFile file : withHallmarkFirst(files)) {
            releaseNode.add(new DefaultMutableTreeNode(file, false));
        }
    }

    /**
     * Moves the Hallmark collection (h.all for Human, mh.all for Mouse) to the front of the list,
     * ahead of every other file; all other files keep the order the catalog gave them in. Never
     * matches a CHIP file name, so this has no effect when populating the CHIP chooser's tree.
     */
    static List<MSigDBCatalogFile> withHallmarkFirst(List<MSigDBCatalogFile> files) {
        List<MSigDBCatalogFile> hallmarkFirst = new ArrayList<MSigDBCatalogFile>(files.size());
        List<MSigDBCatalogFile> rest = new ArrayList<MSigDBCatalogFile>(files.size());
        for (MSigDBCatalogFile file : files) {
            (isHallmark(file) ? hallmarkFirst : rest).add(file);
        }
        hallmarkFirst.addAll(rest);
        return hallmarkFirst;
    }

    private static boolean isHallmark(MSigDBCatalogFile file) {
        String name = file.getName().toLowerCase();
        switch (file.getMSigDBVersion().getMsigDBSpecies()) {
            case Human: return name.startsWith("h.all.");
            case Mouse: return name.startsWith("mh.all.");
            default: return false;
        }
    }

    // Lazily fetches an older release's file catalog the first time its node is expanded, only
    // ever replacing the single LOADING_PLACEHOLDER child put there by populateCatalogTree above
    // -- so re-expanding an already-populated (or already-failed) node is a no-op, not a re-fetch.
    private static TreeWillExpandListener createLazyFileFetchListener(Function<MSigDBRelease, String> fileCatalogUrlExtractor) {
        return new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object last = event.getPath().getLastPathComponent();
                if (!(last instanceof DefaultMutableTreeNode)) { return; }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
                if (!(node.getUserObject() instanceof MSigDBRelease)) { return; }
                if (node.getChildCount() != 1 || !MSigDBCatalogTreeCellRenderer.LOADING_PLACEHOLDER.equals(
                        ((DefaultMutableTreeNode) node.getFirstChild()).getUserObject())) {
                    return;
                }

                MSigDBRelease release = (MSigDBRelease) node.getUserObject();
                node.removeAllChildren();
                try {
                    addFileChildren(node, MSigDBCatalogClient.fetchFileCatalog(fileCatalogUrlExtractor.apply(release)));
                } catch (IOException ioe) {
                    klog.error(ioe.getMessage(), ioe);
                    node.add(new DefaultMutableTreeNode("Error: " + ioe.getMessage(), false));
                }

                Object source = event.getSource();
                if (source instanceof JTree) {
                    ((DefaultTreeModel) ((JTree) source).getModel()).nodeStructureChanged(node);
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) { }
        };
    }

    public static String DESELECT_INSTRUCTIONS = (SystemUtils.IS_OS_MAC) ?
            "Use command-click to select/deselect items." :
            "Use control-click to select/deselect items.";

    public static JTextArea createOfflineMessageDisplay() {
        String message = "Offline mode" + SystemUtils.LINE_SEPARATOR +
          "Change this in Menu=>Preferences" + SystemUtils.LINE_SEPARATOR +
          "Use 'Load Data' to access local files." + SystemUtils.LINE_SEPARATOR +
          "Choose gene sets from other tabs.";
        JTextArea offlineMsgDisplay = new JTextArea();
        offlineMsgDisplay.setText(message);
        offlineMsgDisplay.setEditable(false);
        offlineMsgDisplay.setBackground(Color.WHITE);
        return offlineMsgDisplay;
    }

    public static JTextArea createErrorMessageDisplay(Exception e) {
        String message = "Error listing MSigDB files:" + SystemUtils.LINE_SEPARATOR +
          e.getMessage() + SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR +
          "This might be due to your network's firewall rules." + SystemUtils.LINE_SEPARATOR +
          "MSigDB files can be manually downloaded from www.gsea-msigdb.org/gsea/downloads.jsp" + SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR +
          "Use 'Load Data' to provide access to local files." + SystemUtils.LINE_SEPARATOR +
          "Choose gene sets from other tabs.";
        JTextArea errorMsgDisplay = new JTextArea();
        errorMsgDisplay.setText(message);
        errorMsgDisplay.setEditable(false);
        errorMsgDisplay.setBackground(Color.WHITE);
        return errorMsgDisplay;
    }
}
