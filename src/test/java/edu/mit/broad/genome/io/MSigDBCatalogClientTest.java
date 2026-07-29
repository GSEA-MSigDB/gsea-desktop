/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;

/**
 * Exercises {@link MSigDBCatalogClient} end-to-end against a local, in-process HTTP server
 * (no fixtures/mocking library needed beyond the JDK's own {@code com.sun.net.httpserver}), since
 * the class's only real behavior of interest -- JSON parsing and the session cache -- is only
 * observable through its public HTTP-fetching methods.
 */
class MSigDBCatalogClientTest {
    private static final String VALID_RELEASES_JSON = "{"
            + "\"catalogFormatVersion\":\"1.0\","
            + "\"releases\":["
            + "{\"species\":\"Human\",\"releaseName\":\"MSigDB 2023.2.Hs\",\"versionId\":\"2023.2.Hs\","
            + "\"description\":\"Human collections\",\"releaseDate\":\"2023-09-19\","
            + "\"geneSetsCatalogUrl\":\"http://example.invalid/genesets.json\","
            + "\"chipCatalogUrl\":\"http://example.invalid/chips.json\"},"
            + "{\"species\":\"Mouse\",\"releaseName\":\"MSigDB 2023.2.Mm\",\"versionId\":\"2023.2.Mm\","
            + "\"description\":\"Mouse collections\",\"releaseDate\":\"2023-09-19\","
            + "\"geneSetsCatalogUrl\":\"http://example.invalid/genesets-mm.json\","
            + "\"chipCatalogUrl\":\"http://example.invalid/chips-mm.json\"}"
            + "]}";

    private static final String VALID_FILES_JSON = "{"
            + "\"versionId\":\"2023.2.Hs\",\"species\":\"Human\","
            + "\"files\":["
            + "{\"name\":\"h.all.v2023.2.Hs.symbols.gmt\",\"description\":\"Hallmark gene sets\","
            + "\"url\":\"http://example.invalid/h.all.v2023.2.Hs.symbols.gmt\"}"
            + "]}";

    private static final String MISSING_VERSION_ID_JSON = "{"
            + "\"catalogFormatVersion\":\"1.0\","
            + "\"releases\":[{\"species\":\"Human\",\"releaseName\":\"Bad Release\","
            + "\"description\":\"d\",\"releaseDate\":\"2023-01-01\","
            + "\"geneSetsCatalogUrl\":\"http://example.invalid/g\",\"chipCatalogUrl\":\"http://example.invalid/c\"}]}";

    private static final String MALFORMED_JSON = "not valid json {{";

    private static final String NON_OBJECT_ROOT_JSON = "[1, 2, 3]";

    private static HttpServer server;
    private static int port;
    private static final AtomicInteger countingHits = new AtomicInteger(0);

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        registerJson("/releases.json", VALID_RELEASES_JSON);
        registerJson("/genesets.json", VALID_FILES_JSON);
        registerJson("/missing-version.json", MISSING_VERSION_ID_JSON);
        registerJson("/malformed.json", MALFORMED_JSON);
        registerJson("/non-object-root.json", NON_OBJECT_ROOT_JSON);
        server.createContext("/counting.json", (HttpExchange exchange) -> {
            countingHits.incrementAndGet();
            writeResponse(exchange, VALID_RELEASES_JSON);
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @BeforeEach
    void resetState() {
        MSigDBCatalogClient.clearCache();
        countingHits.set(0);
    }

    private static void registerJson(String path, String json) {
        server.createContext(path, (HttpExchange exchange) -> writeResponse(exchange, json));
    }

    private static void writeResponse(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String urlFor(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void fetchReleaseCatalog_parsesAllFieldsForBothSpecies() throws IOException {
        List<MSigDBRelease> releases = MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/releases.json"));
        assertEquals(2, releases.size());

        MSigDBRelease human = releases.get(0);
        assertEquals(MSigDBSpecies.Human, human.getSpecies());
        assertEquals("MSigDB 2023.2.Hs", human.getReleaseName());
        assertEquals("2023.2.Hs", human.getMSigDBVersion().getVersionString());
        assertEquals("Human collections", human.getDescription());
        assertEquals("2023-09-19", human.getReleaseDate());
        assertEquals("http://example.invalid/genesets.json", human.getGeneSetsCatalogUrl());
        assertEquals("http://example.invalid/chips.json", human.getChipCatalogUrl());

        assertEquals(MSigDBSpecies.Mouse, releases.get(1).getSpecies());
    }

    @Test
    void fetchFileCatalog_parsesFilesWithSharedReleaseVersion() throws IOException {
        List<MSigDBCatalogFile> files = MSigDBCatalogClient.fetchFileCatalog(urlFor("/genesets.json"));
        assertEquals(1, files.size());

        MSigDBCatalogFile file = files.get(0);
        assertEquals("h.all.v2023.2.Hs.symbols.gmt", file.getName());
        assertEquals("Hallmark gene sets", file.getDescription());
        assertEquals("http://example.invalid/h.all.v2023.2.Hs.symbols.gmt", file.getPath());
        assertEquals("2023.2.Hs", file.getMSigDBVersion().getVersionString());
        assertEquals(MSigDBSpecies.Human, file.getMSigDBVersion().getMsigDBSpecies());
    }

    @Test
    void fetchReleaseCatalog_missingRequiredFieldThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/missing-version.json")));
    }

    @Test
    void fetchReleaseCatalog_malformedJsonThrowsIOException() {
        assertThrows(IOException.class, () -> MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/malformed.json")));
    }

    @Test
    void fetchReleaseCatalog_nonObjectRootThrowsIOException() {
        assertThrows(IOException.class, () -> MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/non-object-root.json")));
    }

    @Test
    void fetchReleaseCatalog_secondCallForSameUrlUsesSessionCacheNotANewRequest() throws IOException {
        MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/counting.json"));
        MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/counting.json"));
        assertEquals(1, countingHits.get());
    }

    @Test
    void clearCache_forcesReFetchOnNextCall() throws IOException {
        MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/counting.json"));
        MSigDBCatalogClient.clearCache();
        MSigDBCatalogClient.fetchReleaseCatalog(urlFor("/counting.json"));
        assertEquals(2, countingHits.get());
    }
}
