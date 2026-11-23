/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.ui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.xbench.RendererFactory2;

/**
 * Renders the two node kinds shown in the MSigDB gene set / CHIP chooser trees: release nodes
 * ({@link MSigDBRelease}) and file leaf nodes ({@link MSigDBCatalogFile}), plus the transient
 * placeholder child shown while a release's file catalog is still being lazily fetched. Bolds the
 * current/latest release, replacing the old per-file bolding in {@code FTPFileListCellRenderer}
 * now that "latest" is a release-level concept.
 *
 * @author David Eby
 */
public class MSigDBCatalogTreeCellRenderer extends DefaultTreeCellRenderer {
    public static final String LOADING_PLACEHOLDER = "Loading...";

    private final String highestVersionId;

    public MSigDBCatalogTreeCellRenderer(String highestVersionId) {
        this.highestVersionId = StringUtils.lowerCase(highestVersionId);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        // Reset to the tree's plain font before applying any per-node styling below, since this
        // renderer component is reused across cells and must not let bold/italic styling leak
        // from one cell's render call onto the next.
        setFont(tree.getFont());

        Object userObject = (value instanceof DefaultMutableTreeNode)
                ? ((DefaultMutableTreeNode) value).getUserObject() : value;

        if (userObject instanceof MSigDBRelease) {
            MSigDBRelease release = (MSigDBRelease) userObject;
            setText(release.getReleaseName());
            setToolTipText(buildReleaseTooltip(release));
            String versionId = StringUtils.lowerCase(release.getMSigDBVersion().getVersionString());
            if (highestVersionId != null && highestVersionId.equals(versionId)) {
                setFont(getFont().deriveFont(Font.BOLD));
            }
        } else if (userObject instanceof MSigDBCatalogFile) {
            MSigDBCatalogFile file = (MSigDBCatalogFile) userObject;
            setText(file.getName());
            setToolTipText(StringUtils.isNotBlank(file.getDescription()) ? file.getDescription() : null);
            setIcon(RendererFactory2.MSIGDB_FILE_ICON);
        } else if (LOADING_PLACEHOLDER.equals(userObject)) {
            setFont(getFont().deriveFont(Font.ITALIC));
            setToolTipText(null);
        }

        return this;
    }

    private static String buildReleaseTooltip(MSigDBRelease release) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotBlank(release.getDescription())) { buf.append(release.getDescription()); }
        if (StringUtils.isNotBlank(release.getReleaseDate())) {
            if (buf.length() > 0) { buf.append(" -- "); }
            buf.append("released ").append(release.getReleaseDate());
        }
        return (buf.length() > 0) ? buf.toString() : null;
    }
}
