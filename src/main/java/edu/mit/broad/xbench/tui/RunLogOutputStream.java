/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * UTF-8 {@link PrintStream} that forwards decoded text to a {@code (runId, chunk)} sink.
 */
public final class RunLogOutputStream extends OutputStream {

    private final String runId;
    private final BiConsumer<String, String> sink;
    private final CharsetDecoder decoder = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    private final ByteBuffer leftover = ByteBuffer.allocate(4);

    public RunLogOutputStream(String runId, BiConsumer<String, String> sink) {
        this.runId = runId;
        this.sink = sink;
    }

    public static PrintStream printStream(String runId, BiConsumer<String, String> sink) {
        return new PrintStream(new RunLogOutputStream(runId, sink), true, StandardCharsets.UTF_8);
    }

    @Override
    public synchronized void write(int b) {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        if (b == null || len <= 0 || sink == null) {
            return;
        }
        ByteBuffer in = ByteBuffer.allocate(leftover.position() + len);
        leftover.flip();
        in.put(leftover);
        leftover.clear();
        in.put(b, off, len);
        in.flip();
        CharBuffer out = CharBuffer.allocate(in.remaining() + 1);
        decoder.decode(in, out, false);
        if (in.hasRemaining()) {
            leftover.put(in);
        }
        out.flip();
        if (out.hasRemaining()) {
            sink.accept(runId, out.toString());
        }
    }
}
