/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class FtpResultInputStream extends InputStream {
    private File file;
    private InputStream inputStream;

    public FtpResultInputStream(File file, boolean gzipCompressed) throws IOException {
        super();
        if (file == null) throw new IOException("file cannot be null");
        this.file = file;

        this.inputStream = new BufferedInputStream(new FileInputStream(file));
        if (gzipCompressed) {
            this.inputStream = new GzipCompressorInputStream(this.inputStream);
        }
    }

    public File getFile() {
        return file;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}
