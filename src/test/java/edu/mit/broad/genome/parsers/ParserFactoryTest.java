/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;

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

    @Test
    void readClosesStreamAfterFailedGctParse(@TempDir java.nio.file.Path dir) throws Exception {
        java.io.File file = dir.resolve("broken.gct").toFile();
        Files.writeString(file.toPath(), "not a gct file\n", StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> ParserFactory.read(file));

        assertTrue(file.delete(), "parser left the GCT file open");
    }

    @Test
    void secondGctReadClosesTheCacheHitStream(@TempDir java.nio.file.Path dir) throws Exception {
        java.io.File file = dir.resolve("tiny.gct").toFile();
        Files.writeString(file.toPath(), """
                #1.2
                1\t1
                Name\tDescription\tSampleA
                GENE1\tna\t1.5
                """, StandardCharsets.UTF_8);

        ParserFactory.read(file, false);
        ParserFactory.read(file, true);
        ParserFactory.read(file, true);

        assertTrue(file.delete(), "cache-hit read left the GCT file open");
    }

    @Test
    void multiClassClsRereadReturnsTheOriginalPhenotype(@TempDir java.nio.file.Path dir) throws Exception {
        java.io.File file = dir.resolve("three.cls").toFile();
        Files.writeString(file.toPath(), """
                6 3 1
                # A B C
                0 0 1 1 2 2
                """, StandardCharsets.UTF_8);

        Template first = ParserFactory.readTemplate(file);
        Template second = ParserFactory.readTemplate(file);

        assertEquals(3, first.getNumClasses());
        assertEquals(first.getNumClasses(), second.getNumClasses());
        for (int i = 0; i < first.getNumClasses(); i++) {
            assertEquals(first.getClassName(i), second.getClassName(i));
        }
    }

    @Test
    void concurrentReadersSurviveWrites() throws Exception {
        ObjectCache cache = new ObjectCache();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 400; i++) {
                    cache.getCachedObjectsL(GeneSet.class);
                    cache.getCachedRepresentationClasses();
                    cache.getAuxGeneSets();
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        reader.start();
        for (int i = 0; i < 200; i++) {
            cache.add("mem/gs" + i, new GeneSet("gs" + i, new String[] { "A", "B" }), GeneSet.class);
        }
        reader.join();
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        assertEquals(200, cache.getCachedObjectsL(GeneSet.class).size());
    }
}
