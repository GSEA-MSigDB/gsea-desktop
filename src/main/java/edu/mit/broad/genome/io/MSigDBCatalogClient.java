/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.io;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.objects.MSigDBCatalogFile;
import edu.mit.broad.genome.objects.MSigDBRelease;
import edu.mit.broad.genome.objects.MSigDBSpecies;
import edu.mit.broad.genome.objects.MSigDBVersion;

/**
 * Fetches and parses the MSigDB HTTP/JSON catalogs: the MSigDB release catalog, and the
 * per-release GMT file catalog and CHIP catalog, which share one schema. Follows the same plain
 * {@code java.net.URL} + {@code org.json.simple} approach already used by
 * {@code xapps.gsea.UpdateChecker} for HTTP+JSON round trips elsewhere in this codebase.
 * <p/>
 * Successful fetches are cached in memory, keyed by catalog URL, for as long as this class stays
 * loaded -- i.e. for the lifetime of the running process only. Nothing here is persisted to disk,
 * so a fresh process (a new GSEA Desktop launch, or any CLI tool invocation) always starts with
 * an empty cache; see {@link #clearCache()}.
 *
 * @author David Eby
 */
public class MSigDBCatalogClient {
    private static final Logger klog = LoggerFactory.getLogger(MSigDBCatalogClient.class);

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;

    private static final Map<String, List<MSigDBRelease>> releaseCatalogCache =
            new ConcurrentHashMap<String, List<MSigDBRelease>>();
    private static final Map<String, List<MSigDBCatalogFile>> fileCatalogCache =
            new ConcurrentHashMap<String, List<MSigDBCatalogFile>>();

    private MSigDBCatalogClient() { }

    /**
     * Fetches and parses the MSigDB release catalog, the entry point listing all available
     * MSigDB releases across both species.
     */
    public static List<MSigDBRelease> fetchReleaseCatalog(String catalogUrl) throws IOException {
        List<MSigDBRelease> cached = releaseCatalogCache.get(catalogUrl);
        if (cached != null) { return cached; }

        JSONObject root = fetchJson(catalogUrl);
        JSONArray releasesArr = (JSONArray) root.get("releases");
        List<MSigDBRelease> releases = new ArrayList<MSigDBRelease>();
        if (releasesArr != null) {
            for (Object obj : releasesArr) {
                JSONObject entry = (JSONObject) obj;
                MSigDBSpecies species = MSigDBSpecies.byName(getString(entry, "species"));
                releases.add(new MSigDBRelease(species,
                        getString(entry, "releaseName"),
                        getString(entry, "versionId"),
                        getString(entry, "description"),
                        getString(entry, "releaseDate"),
                        getString(entry, "geneSetsCatalogUrl"),
                        getString(entry, "chipCatalogUrl")));
            }
        }
        releases = Collections.unmodifiableList(releases);
        releaseCatalogCache.put(catalogUrl, releases);
        return releases;
    }

    /**
     * Fetches and parses one per-release file catalog -- either a GMT file catalog or a CHIP
     * catalog, which share the same schema, so one method serves both.
     */
    public static List<MSigDBCatalogFile> fetchFileCatalog(String catalogUrl) throws IOException {
        List<MSigDBCatalogFile> cached = fileCatalogCache.get(catalogUrl);
        if (cached != null) { return cached; }

        JSONObject root = fetchJson(catalogUrl);
        MSigDBSpecies species = MSigDBSpecies.byName(getString(root, "species"));
        MSigDBVersion version = new MSigDBVersion(species, getString(root, "versionId"));

        JSONArray filesArr = (JSONArray) root.get("files");
        List<MSigDBCatalogFile> files = new ArrayList<MSigDBCatalogFile>();
        if (filesArr != null) {
            for (Object obj : filesArr) {
                JSONObject entry = (JSONObject) obj;
                files.add(new MSigDBCatalogFile(getString(entry, "name"), getString(entry, "description"),
                        getString(entry, "url"), version));
            }
        }
        files = Collections.unmodifiableList(files);
        fileCatalogCache.put(catalogUrl, files);
        return files;
    }

    /**
     * Discards all cached catalog data. Not wired to any UI action today -- the cache is
     * otherwise only ever cleared implicitly by the process ending -- but exposed for testing
     * and in case a future "refresh catalog" affordance is wanted.
     */
    public static void clearCache() {
        releaseCatalogCache.clear();
        fileCatalogCache.clear();
    }

    private static JSONObject fetchJson(String catalogUrl) throws IOException {
        klog.debug("Fetching MSigDB catalog: {}", catalogUrl);
        URL url = URI.create(catalogUrl).toURL();
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        String json = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        try {
            Object parsed = new JSONParser().parse(json);
            if (!(parsed instanceof JSONObject)) {
                throw new IOException("Expected a JSON object at the root of MSigDB catalog: " + catalogUrl);
            }
            return (JSONObject) parsed;
        } catch (ParseException pe) {
            throw new IOException("Malformed MSigDB catalog JSON at " + catalogUrl, pe);
        }
    }

    private static String getString(JSONObject obj, String key) {
        Object value = obj.get(key);
        return (value == null) ? null : value.toString();
    }
}
