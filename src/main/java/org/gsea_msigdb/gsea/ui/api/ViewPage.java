/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package org.gsea_msigdb.gsea.ui.api;

import javafx.scene.Node;

/**
 * Page that can be opened in the application workspace (tab / window).
 */
public interface ViewPage {

    String getTitle();

    /**
     * Optional classpath resource id for an icon (e.g. {@code "Gsea_app16_v2.png"}), or null.
     */
    String getIconResourceId();

    /**
     * JavaFX content node for this page.
     */
    Node getContent();
}
