/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.api.frameworks.fiji;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.viewers.SystemConsoleViewer;

import org.genepattern.uiutil.CenteredDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusBarAppender {
    private JPanel fStatusBar;
    private JLabel fStatusBarLabel;
    private SystemConsoleViewer fSystemConsoleComp;
    private JDialog popup;
    
    public StatusBarAppender(String name) {
        this.fStatusBar = new JPanel(new BorderLayout());
        this.fSystemConsoleComp = new SystemConsoleViewer();
        fSystemConsoleComp.setBorder(BorderFactory.createTitledBorder("Application messages"));

        // Status bar component to display logging messages in the UI
        this.fStatusBarLabel = new JLabel();
        fStatusBarLabel.setIcon(JarResources.getIcon("expandall.png"));
        fStatusBarLabel.setToolTipText("Click for application messages (such as # of permutations complete)");
        // Create a Logging Handler wrapped around this label object and register it with the Root logger
        LabelStatusBarLoggingHandler handler = new LabelStatusBarLoggingHandler(fStatusBarLabel);
        LogManager.getLogManager().getLogger("").addHandler(handler);

        fStatusBar.add(fStatusBarLabel, BorderLayout.WEST);

        this.fStatusBarLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showPopup(); }
            public void mouseEntered(MouseEvent e) { fStatusBarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            public void mouseExited(MouseEvent e) {  fStatusBarLabel.setCursor(Cursor.getDefaultCursor()); }
        });
    }

    public JComponent getAsComponent(JFrame frame) {
        this.popup = new CenteredDialog(frame);
        return fStatusBar;
    }

    private void showPopup() {
        if (popup == null) { return; }
        popup.getContentPane().setLayout(new BorderLayout());
        popup.setSize(new Dimension(700, 350));

        fSystemConsoleComp.setPreferredSize(new Dimension(700, 350));
        popup.getContentPane().add(fSystemConsoleComp);
        popup.setResizable(true);
        if (popup.isVisible()) {
            popup.setVisible(false);
        } else {
            popup.setVisible(true);
        }
    }
    
    private class LabelStatusBarLoggingHandler extends Handler {
        final JLabel statusBarLabel;
        public LabelStatusBarLoggingHandler(JLabel statusBarLabel) {
            this.statusBarLabel = statusBarLabel;
            this.setLevel(Conf.isDebugMode() ? Level.FINE : Level.INFO);
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
                Level level = record.getLevel();
                fStatusBarLabel.setForeground(Color.BLACK);
                if (level == Level.WARNING) { fStatusBarLabel.setForeground(Color.MAGENTA); }
                statusBarLabel.setText(message);
            } catch (Exception ex) {
                reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            }
        }
        
        @Override
        public boolean isLoggable(LogRecord record) {
            return this.statusBarLabel != null && super.isLoggable(record);
        }
    }
}
