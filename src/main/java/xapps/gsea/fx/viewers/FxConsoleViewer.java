/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * System console that retains shared log history across reopenings.
 * Captures JUL logs and redirects {@link System#out}/{@link System#err}.
 */
public class FxConsoleViewer implements ViewPage {

    private static final StringBuilder HISTORY = new StringBuilder();
    private static final List<TextArea> LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Runnable> STATUS_LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile String lastStatusLine = "";
    private static volatile boolean lastStatusWarning;
    private static volatile boolean handlerInstalled;
    private static volatile boolean streamsInstalled;

    static {
        HISTORY.append("< Process output will appear below >\n\n");
    }

    private final BorderPane root = new BorderPane();
    private final TextArea area = new TextArea();

    public FxConsoleViewer() {
        installLogHandler();
        installSystemStreamCapture();
        area.setEditable(false);
        area.setWrapText(true);
        area.setText(HISTORY.toString());
        LISTENERS.add(area);
        root.setCenter(area);

        Button clear = new Button("Clear All Output");
        xapps.gsea.fx.FxButtons.styleSecondary(clear);
        clear.setOnAction(e -> clearConsole());
        Button copy = new Button("Copy");
        xapps.gsea.fx.FxButtons.styleSecondary(copy);
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(area.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        // Swing SystemConsoleViewer: FlowLayout.RIGHT for Clear/Copy.
        HBox actions = xapps.gsea.fx.FxButtons.row(clear, copy);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8));
        root.setBottom(actions);
    }

    /**
     * Attach a JUL handler so SLF4J-to-JUL log lines appear in the console.
     * Safe to call multiple times.
     */
    public static synchronized void installLogHandler() {
        if (handlerInstalled) {
            return;
        }
        Handler handler = new Handler() {
            private final SimpleFormatter formatter = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                // Swing StatusBarAppender: JUL → status bar only (not SystemConsole).
                // Magenta only for Level.WARNING (not SEVERE). Do not trim — Swing shows full formatter text.
                String line = formatter.format(record);
                publishStatus(line, record.getLevel() == Level.WARNING);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(edu.mit.broad.genome.Conf.isDebugMode()
                ? Level.FINE : Level.INFO);
        Logger.getLogger("").addHandler(handler);
        handlerInstalled = true;
    }

    /**
     * Redirect System.out / System.err into the console (Swing SystemConsole parity).
     * Safe to call multiple times.
     */
    public static synchronized void installSystemStreamCapture() {
        if (streamsInstalled) {
            return;
        }
        PrintStream shared = new PrintStream(new ConsoleOutputStream(), true, StandardCharsets.UTF_8);
        System.setOut(shared);
        System.setErr(shared);
        streamsInstalled = true;
    }

    public static void addStatusListener(Runnable listener) {
        if (listener != null) {
            STATUS_LISTENERS.add(listener);
        }
    }

    public static String getLastStatusLine() {
        return lastStatusLine;
    }

    public static boolean isLastStatusWarning() {
        return lastStatusWarning;
    }

    private static void publishStatus(String line, boolean warning) {
        if (line == null || line.isBlank()) {
            return;
        }
        // Status footer is single-line; JUL SimpleFormatter is multi-line — keep the last line.
        String display = line.trim();
        int nl = display.lastIndexOf('\n');
        if (nl >= 0 && nl + 1 < display.length()) {
            String last = display.substring(nl + 1).trim();
            if (!last.isEmpty()) {
                display = last;
            }
        }
        lastStatusWarning = warning;
        lastStatusLine = display;
        for (Runnable r : STATUS_LISTENERS) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Append raw stream text (Swing {@code SystemConsole} appends chunks as read — no retention cap,
     * no forced newlines between chunks).
     */
    public static void appendText(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        Runnable r = () -> {
            HISTORY.append(chunk);
            for (TextArea listener : LISTENERS) {
                listener.appendText(chunk);
                listener.positionCaret(listener.getLength());
            }
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public static void appendLine(String line) {
        if (line == null) {
            return;
        }
        appendText(line.endsWith("\n") ? line : line + "\n");
    }

    public static void clearConsole() {
        Runnable r = () -> {
            HISTORY.setLength(0);
            for (TextArea listener : LISTENERS) {
                listener.clear();
            }
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    private static final class ConsoleOutputStream extends OutputStream {
        // Swing SystemConsole: InputStreamReader decodes with a persistent charset state.
        private final java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE);
        private final java.nio.ByteBuffer leftover = java.nio.ByteBuffer.allocate(4);

        @Override
        public synchronized void write(int b) {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            if (b == null || len <= 0) {
                return;
            }
            java.nio.ByteBuffer in = java.nio.ByteBuffer.allocate(leftover.position() + len);
            leftover.flip();
            in.put(leftover);
            leftover.clear();
            in.put(b, off, len);
            in.flip();
            java.nio.CharBuffer out = java.nio.CharBuffer.allocate(in.remaining() + 1);
            java.nio.charset.CoderResult cr = decoder.decode(in, out, false);
            if (cr.isUnderflow() && in.hasRemaining()) {
                leftover.put(in);
            }
            out.flip();
            if (out.hasRemaining()) {
                appendText(out.toString());
            }
        }

        @Override
        public synchronized void flush() {
            // Publish any complete characters; incomplete UTF-8 sequences stay in leftover/decoder.
            java.nio.CharBuffer out = java.nio.CharBuffer.allocate(8);
            java.nio.ByteBuffer empty = java.nio.ByteBuffer.allocate(0);
            decoder.decode(empty, out, false);
            out.flip();
            if (out.hasRemaining()) {
                appendText(out.toString());
            }
        }
    }

    @Override
    public String getTitle() {
        return "Application messages";
    }

    @Override
    public String getIconResourceId() {
        return "expandall.png";
    }

    @Override
    public Object getContent() {
        return root;
    }
}
