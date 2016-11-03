/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.uiutil;

import javax.swing.*;
import java.awt.*;

public class UIUtil {
    private static MessageHandler messageHandler = new DefaultMessageHandler();

    private UIUtil() {
    }

    public static boolean showConfirmDialog(Component parent, String message) {
        if (JOptionPane.showOptionDialog(parent, message, "",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Yes", "No"}, "Yes") == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }

    public static void showMessageDialog(Component parent, String message) {
        messageHandler.showMessageDialog(parent, message);
    }

    public static void showErrorDialog(Component parent, String message) {
        messageHandler.showErrorDialog(parent, message);
    }

    public static void sizeToScreen(Component c) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        c.setSize(screenSize);
    }

    public static void setMessageHandler(MessageHandler m) {
        messageHandler = m;
    }

    public static interface MessageHandler {
        public void showMessageDialog(Component parent, String message);

        public void showErrorDialog(Component parent, String message);
    }

    private static class DefaultMessageHandler implements MessageHandler {
        public void showMessageDialog(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "",
                    JOptionPane.INFORMATION_MESSAGE, null);
        }

        public void showErrorDialog(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "",
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }
}