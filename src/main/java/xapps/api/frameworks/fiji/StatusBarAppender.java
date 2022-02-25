/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.api.frameworks.fiji;

import com.jidesoft.popup.JidePopup;
import com.jidesoft.status.*;
import com.jidesoft.swing.JideBoxLayout;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.viewers.SystemConsoleViewer;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class StatusBarAppender {
    private StatusBar fJideStatusBar;
    private LabelStatusBarItem fStatusBarLabelItem;
    private SystemConsoleViewer fSystemConsoleComp;
    
    public StatusBarAppender(String name) {
        this.fJideStatusBar = new StatusBar();
        this.fSystemConsoleComp = new SystemConsoleViewer();
        fSystemConsoleComp.setBorder(BorderFactory.createTitledBorder("Application messages"));

        fJideStatusBar.add(new TimeStatusBarItem(), JideBoxLayout.FIX);

        // Status bar component to display logging messages in the UI
        this.fStatusBarLabelItem = new LabelStatusBarItem();
        fStatusBarLabelItem.setIcon(JarResources.getIcon("expandall.png"));
        fStatusBarLabelItem.setToolTipText("Click for application messages (such as # of permutations complete)");
        // Create a Logging Handler wrapped around this label object and register it with the Root logger
        LabelStatusBarLoggingHandler handler = new LabelStatusBarLoggingHandler(fStatusBarLabelItem);
        LogManager.getLogManager().getLogger("").addHandler(handler);

        fJideStatusBar.add(fStatusBarLabelItem, JideBoxLayout.FLEXIBLE);

        final MemoryStatusBarItem gc = new MemoryStatusBarItem();
        gc.setPreferredWidth(75);
        fJideStatusBar.add(gc, JideBoxLayout.FIX);

        final ResizeStatusBarItem resize = new ResizeStatusBarItem();
        resize.setPreferredWidth(20);
        resize.setBorder(BorderFactory.createEmptyBorder());
        fJideStatusBar.add(resize, JideBoxLayout.FIX);

        this.fStatusBarLabelItem.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showPopup(fStatusBarLabelItem); }
            public void mouseEntered(MouseEvent e) { fStatusBarLabelItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            public void mouseExited(MouseEvent e) {  fStatusBarLabelItem.setCursor(Cursor.getDefaultCursor()); }
        });
    }

    public JComponent getAsComponent() { return fJideStatusBar; }

    private void showPopup(final JComponent owner) {
        final JidePopup popup = new JidePopup();
        popup.setMovable(true);
        popup.getContentPane().setLayout(new BorderLayout());

        fSystemConsoleComp.setPreferredSize(new Dimension(700, 350));
        popup.getContentPane().add(fSystemConsoleComp);
        popup.setDefaultFocusComponent(fSystemConsoleComp);
        popup.setOwner(owner);
        popup.setResizable(true);
        popup.setMovable(true);
        if (popup.isPopupVisible()) {
            popup.hidePopup();
        } else {
            popup.showPopup();
        }
    }
    
    private class LabelStatusBarLoggingHandler extends Handler {
        final LabelStatusBarItem labelStatusBarItem;
        public LabelStatusBarLoggingHandler(LabelStatusBarItem labelStatusBarItem) {
            this.labelStatusBarItem = labelStatusBarItem;
            this.setLevel(Level.INFO);
            this.setFormatter(new SimpleFormatter());
        }

        @Override
        public void close() throws SecurityException { }

        @Override
        public void flush() { }

        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) { return; }
            try {
                String message = getFormatter().format(record);
                labelStatusBarItem.setText(message);
            } catch (Exception ex) {
                reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            }
        }
        
        @Override
        public boolean isLoggable(LogRecord record) {
            return this.labelStatusBarItem != null && super.isLoggable(record);
        }
    }
}
