/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Access bundled resources under /edu/mit/broad/genome/resources/.
 */
public class JarResources {
    private static final String PKG_GENOME = "/edu/mit/broad/genome/";
    private static final String PKG_RESOURCE = PKG_GENOME + "resources/";
    private static final Logger klog = LoggerFactory.getLogger(JarResources.class);

    private JarResources() {
    }

    public static Properties getBuildInfo() {
        URL url = toURL("build.properties");
        Properties buildProps = new Properties();
        try {
            InputStream urlStream = url.openStream();
            try {
                buildProps.load(urlStream);
                return buildProps;
            } finally {
                urlStream.close();
            }
        } catch (Throwable t) {
            klog.error(t.getMessage(), t);
        }
        return buildProps;
    }

    public static URL toURL(final String filename) {
        return JarResources.class.getResource(PKG_RESOURCE + filename);
    }

    public static Image getImage(String name) {
        URL url = toURL(name);
        if (url == null) {
            klog.warn("could not find image resource: {}", name);
            return null;
        }
        return Toolkit.getDefaultToolkit().getImage(url);
    }
}
