/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.cytoscape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

public class CytoscapeCyrest {
    private static final Logger klog = LoggerFactory.getLogger(CytoscapeCyrest.class);

    // path to the edb directory
    private EnrichmentMapParameters params;

    public CytoscapeCyrest(EnrichmentMapParameters params) {
        this.params = params;
    }

    private String getRestURL() throws IOException {
        // Build this every time in case the user changes the cyREST port
        return "http://localhost:" + XPreferencesFactory.kCytoscapeRESTPort.getInt() + "/v1/";
    }

    /*
     * Method to test if the cytoscape rest service is up and running and if one of the commands listed is enrichment map
     */
    public boolean CytoscapeRestActive() throws IOException, URISyntaxException {
        URL url = URI.create(getRestURL()).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn.getResponseCode() != 200) {
            klog.error("cannot connect to cytoscape rest server");
            conn.disconnect();
            return false;
        } else {
            klog.info("successfully connected to cytoscape rest");
            conn.disconnect();
            return true;
        }
    }

    /*
     * Method to test if the cytoscape rest service is up and running and if one of the commands listed is enrichment map
     */
    public boolean CytoscapeRestCommandEM() throws IOException, URISyntaxException {
        URL url = URI.create(getRestURL() + "commands/").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }

        // Buffer the result into a string
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            if (line.contains("enrichmentmap")) {
                conn.disconnect();
                klog.info("Found enrichment map command");
                return true;
            }

        }
        rd.close();

        conn.disconnect();
        return false;
    }

    public boolean createEM_get() throws IOException, URISyntaxException {
        URIBuilder builder = new URIBuilder();
        URI uri;

        builder.setScheme("http").setHost("localhost:1234/v1").setPath("/commands/enrichmentmap/gseabuild")
                .setParameter("edbdir", this.params.getEdbdir()).setParameter("pvalue", Double.toString(this.params.getPvalue()))
                .setParameter("qvalue", Double.toString(this.params.getQvalue()))
                .setParameter("overlap", Double.toString(this.params.getSimilarityCutOff()))
                .setParameter("similaritymetric", this.params.getSimilarityMetric())
                .setParameter("combinedconstant", Double.toString(this.params.getCombinedConstant()));

        if (!this.params.getExpressionFilePath().equals("")) builder.setParameter("expressionfile", this.params.getExpressionFilePath());

        if (this.params.getEdbdir2() != null && !this.params.getEdbdir2().equalsIgnoreCase("")) {
            builder.setParameter("edbdir2", this.params.getEdbdir2());

            if (!this.params.getExpression2FilePath().equals(""))
                builder.setParameter("expressionfile2", this.params.getExpression2FilePath());
        }
        uri = builder.build();

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(uri);
        klog.info("Get URL: {}", httpget.getURI());
        CloseableHttpResponse response = httpclient.execute(httpget);

        HttpEntity entity = response.getEntity();
        StatusLine statusLine = response.getStatusLine();
        klog.info("status: {}", statusLine.getReasonPhrase());
        if (!statusLine.getReasonPhrase().equalsIgnoreCase("ok")) {
            String message = EntityUtils.toString(entity);
            Application.getWindowManager().showMessage("Unable to create Enrichment Map: " + statusLine.getReasonPhrase() + "\n" + message);
            httpclient.close();            
            return false;
        }

        httpclient.close();
        return true;
    }
}
