/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import edu.mit.broad.genome.JarResources;

import javax.swing.*;

/**
 * Several awt components support placement of an icon and a string.
 * Thsi class defines several precanned icon-text combinations for resuse.
 * <p/>
 * as well as mechanism to formute new text-icon associations.
 * <p/>
 * Client code can directly access icon and strinfgg as they are immutable once cretaed
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TextIconPair {

    public static final TextIconPair NA_COMPONENT = new TextIconPair("<No Available Component>",
            JarResources.getIcon("NAComponent.gif"));
    public static final TextIconPair WAITING_FOR_TASK =
            new TextIconPair("<Waiting for input>", JarResources.getIcon("WaitingPlaceholder.gif"));
    public static final TextIconPair DISABLED_COMPONENT =
            new TextIconPair("<Disabled Component>", JarResources.getIcon("DisabledComponent.gif"));
    public static final TextIconPair ERROR_COMPONENT =
            new TextIconPair("<Error Component>", JarResources.getIcon("ErrorComponent.gif"));

    /**
     * The icon of the text-icon pair
     */
    public Icon icon;

    /**
     * The text of the text-icon pair
     */
    public String text;

    /**
     * Class Constructor.
     */
    public TextIconPair() {
    }

    /**
     * Class Constructor.
     */
    public TextIconPair(String text, Icon icon) {
        this.text = text;
        this.icon = icon;
    }
}    // End TextIconPair
