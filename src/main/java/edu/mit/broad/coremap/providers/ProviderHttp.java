/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

/** Shared HTTP helpers for CoreMap live providers (retries on 5xx / timeouts). */
public final class ProviderHttp {

    public static final String USER_AGENT = "GSEA-Desktop-CoreMap/1.0 (research; live interactome)";
    private static final int DEFAULT_TIMEOUT_MS = 60_000;
    private static final int DEFAULT_RETRIES = 2;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER = createConnectionManager();
    private static final CloseableHttpClient SHARED_CLIENT = HttpClients.custom()
            .setConnectionManager(CONNECTION_MANAGER)
            .setConnectionManagerShared(true)
            .evictExpiredConnections()
            .evictIdleConnections(30L, TimeUnit.SECONDS)
            .build();

    private ProviderHttp() {
    }

    private static PoolingHttpClientConnectionManager createConnectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(64);
        cm.setDefaultMaxPerRoute(16);
        return cm;
    }

    public static String get(String url) throws IOException {
        return get(url, DEFAULT_TIMEOUT_MS, DEFAULT_RETRIES);
    }

    public static String get(String url, int timeoutMs, int retries) throws IOException {
        IOException last = null;
        int attempts = Math.max(1, retries + 1);
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                return executeGet(url, timeoutMs);
            } catch (IOException ex) {
                last = ex;
                if (!retryable(ex) || attempt >= attempts - 1) {
                    break;
                }
                sleep(1500L * (attempt + 1));
            }
        }
        throw new IOException("Request failed after " + attempts + " attempts: " + url
                + (last != null ? " (" + last.getMessage() + ")" : ""), last);
    }

    public static String postForm(String url, String formBody) throws IOException {
        return postForm(url, formBody, 90_000, DEFAULT_RETRIES);
    }

    public static String postForm(String url, String formBody, int timeoutMs, int retries) throws IOException {
        IOException last = null;
        int attempts = Math.max(1, retries + 1);
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                return executePost(url, formBody, timeoutMs);
            } catch (IOException ex) {
                last = ex;
                if (!retryable(ex) || attempt >= attempts - 1) {
                    break;
                }
                sleep(1500L * (attempt + 1));
            }
        }
        throw new IOException("POST failed after " + attempts + " attempts: " + url
                + (last != null ? " (" + last.getMessage() + ")" : ""), last);
    }

    private static boolean retryable(IOException ex) {
        String m = ex.getMessage() != null ? ex.getMessage().toLowerCase(Locale.ROOT) : "";
        return m.contains("timeout") || m.contains("502") || m.contains("503") || m.contains("504")
                || m.contains("upstream") || m.contains("connection");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String executeGet(String url, int timeoutMs) throws IOException {
        HttpGet get = new HttpGet(URI.create(url));
        get.setHeader("User-Agent", USER_AGENT);
        applyTimeout(get, timeoutMs);
        try (CloseableHttpResponse resp = SHARED_CLIENT.execute(get)) {
            return readBody(resp);
        }
    }

    private static String executePost(String url, String formBody, int timeoutMs) throws IOException {
        HttpPost post = new HttpPost(URI.create(url));
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(formBody, StandardCharsets.UTF_8));
        applyTimeout(post, timeoutMs);
        try (CloseableHttpResponse resp = SHARED_CLIENT.execute(post)) {
            return readBody(resp);
        }
    }

    private static void applyTimeout(HttpRequestBase request, int timeoutMs) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Math.min(30_000, timeoutMs))
                .setConnectionRequestTimeout(Math.min(30_000, timeoutMs))
                .setSocketTimeout(timeoutMs)
                .build();
        request.setConfig(config);
    }

    private static String readBody(CloseableHttpResponse resp) throws IOException {
        int code = resp.getStatusLine().getStatusCode();
        String body = resp.getEntity() != null
                ? EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)
                : "";
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + body.substring(0, Math.min(200, body.length())));
        }
        return body;
    }
}
