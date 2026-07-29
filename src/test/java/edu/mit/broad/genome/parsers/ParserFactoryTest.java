/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.mit.broad.xbench.core.api.Application;
import xtools.api.XToolsApplication;

class ParserFactoryTest {
    @BeforeAll
    static void registerHeadlessApplicationHandler() {
        // ParserFactory's static init reaches Application.getVdbManager(), which requires a
        // handler to already be registered. XToolsApplication is the same headless handler real
        // command-line GSEA tools register (see xtools.api.AbstractTool) -- reused here rather
        // than inventing a test-only stub, since it's already the established headless path.
        if (!Application.isHandlerSet()) {
            Application.registerHandler(new XToolsApplication());
        }
    }

    @Test
    void readGeneSetMatrix_ftpUrlIsRejectedRatherThanDownloaded() {
        Exception thrown = assertThrows(Exception.class, () -> ParserFactory.readGeneSetMatrix(
                "ftp://ftp.broadinstitute.org/pub/gsea/msigdb/human/gene_sets/h.all.v2023.2.Hs.symbols.gmt", false));

        assertInstanceOf(IOException.class, thrown);
        assertTrue(thrown.getMessage().contains("FTP"), "Expected a clear FTP-not-supported message, got: " + thrown.getMessage());
    }

    @Test
    void readGeneSetMatrix_ftpUrlRejectedRegardlessOfHost() {
        // The rejection must be protocol-based, not tied to a specific hostname -- otherwise an
        // ftp:// URL for some other host would silently fall through to the JVM's own built-in
        // FTP handler instead of being rejected.
        Exception thrown = assertThrows(Exception.class,
                () -> ParserFactory.readGeneSetMatrix("ftp://some.other.host/some/file.gmt", false));

        assertInstanceOf(IOException.class, thrown);
        assertTrue(thrown.getMessage().contains("FTP"), "Expected a clear FTP-not-supported message, got: " + thrown.getMessage());
    }
}
