/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome;

import edu.mit.broad.xbench.core.api.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xapps.gsea.GseaWebResources;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * A collection of public static methods to aid in getting resources.
 * <p/>
 * JarResources manages pre-packaged resources that are bundled with the
 * downloaded xomics application and jar file.
 * xomics classes should use methods of this class to access resources
 * (files etc under the /resources dir) rather than access them directly.
 * <p/>
 * Resources refer to auxillary files used by the application (not user files).
 * Examples are icons, xml bootstrap files, application configuration files etc.
 * <p/>
 * Use methods of this class to avoid hardcoding paths, package structures etc in other places.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @maint If the package hierarchy changes, this class could need attention.
 */
public class JarResources {

    /**
     * @maint IMPORTANT: Be very careful defining public statics in this class.
     * Also do NOT be careful about using logging here as it might need initialization etc.
     * Generally provide accessor methods rather than the getProperty(const) kind of access
     * i.e getDataDir() rather than getProperty("FOO")
     * <p/>
     * Default icon returned when the specified icon is not found.
     * On such errors this default icon is returned rather than exceptioning out so that
     * the absence of an icon doesnt bring the application down.
     * (note: absence of this icon will probably cause grevious application startup damage)
     */
    public static Icon ICON_NOT_FOUND;

    public static Icon ICON_UNKNOWN_DATA_FORMAT;

    // ------------------------------------------------------------------------
    // class variables
    // ------------------------------------------------------------------------

    /**
     * The base genome package's hierarchy - dont use leading /
     */
    private static final String PKG_GENOME = "/edu/mit/broad/genome/";
    private static final String PKG_RESOURCE = PKG_GENOME + "resources/";

    private static Logger klog = LoggerFactory.getLogger(JarResources.class);

    /**
     * Ensure that "not founds" are present
     */
    static {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                //TraceUtils.showTrace();
                klog.debug("Loading basic icons ...");
                URL nf_url = JarResources.class.getResource(PKG_RESOURCE + "IconNotFound.gif");

                if (nf_url == null) {
                    System.err.println("FATAL resources error ICON_NOT_FOUND not found!");
                    System.err.println("Expected location: " + PKG_RESOURCE + "IconNotFound.gif");
                    ICON_NOT_FOUND = null;
                    ICON_UNKNOWN_DATA_FORMAT = null;
                } else {
                    Image image = Toolkit.getDefaultToolkit().getImage(nf_url);
                    if (image == null) {
                        System.err.println("FATAL resources error ICON_NOT_FOUND not found!");
                        System.err.println("Expected location: resources/IconNotFound.gif");
                    }

                    ICON_NOT_FOUND = new NotFoundIcon(image);

                    URL qu = JarResources.class.getResource(PKG_RESOURCE + "UnknownDataFormat16.gif");
                    if (qu != null) { // must check else app may not start
                        image = Toolkit.getDefaultToolkit().getImage(qu);
                        ICON_UNKNOWN_DATA_FORMAT = new ImageIcon(image);
                    }
                }
            } else {
                //klog.debug("Skipping icons as na headless");
            }

        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Fatal error initializing JarResources " + t.getMessage());
        }

    }

    public static class NotFoundIcon extends ImageIcon {
        NotFoundIcon(Image image) {
            super(image);
        }
    }

    public static Properties getBuildInfo() {
        URL url = toURL("build.properties");
        Properties buildProps = new Properties();
        try {
        InputStream urlStream = url.openStream();
        try {
            buildProps.load(urlStream);
            return buildProps;
        }
        finally {
            urlStream.close();
        }
        } catch (Throwable t) {
            klog.error(t.getMessage(), t);
        }
        return buildProps;
    }

    /**
     * Privatized constructor to prevent instantiation.
     * No instantiation needed. All public static methods.
     */
    private JarResources() {
    }

    /**
     * Gets a resource as a URL. Resources are all assumed to be in the
     * central "Resources" directory.
     *
     * @param filename The filename of the resource (not the path)
     *                 For example: "foo.gif"
     */
    public static URL toURL(final String filename) {
        // webstarting barfs if the system class loader is used
        //URL url = ClassLoader.getSystemClassLoader().getResource(PKG_RESOURCE + filename);
        return JarResources.class.getResource(PKG_RESOURCE + filename);
    }

    public static JButton createHelpButton(final String keyName) {
        Action a = createHelpAction(keyName);
        if (a != null) {
            return new JButton(a);
        } else {
            return new JButton("Help broken: " + keyName);
        }
    }

    public static Action createDataFormatAction(final String dfAnchor) {
        final String url = GseaWebResources.getGseaDataFormatsHelpURL() + dfAnchor;

        return new AbstractAction("Help", getIcon("Help16_v2.gif")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse((new URL(url)).toURI());
                } catch (Throwable t) {
                    Application.getWindowManager().showError(url + ": unable to launch web browser", t);
                }
            }
        };
    }

    public static Action createHelpAction(final String ugAnchor) {
        final String url = GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/" + ugAnchor;

        return new AbstractAction("Help", getIcon("Help16_v2.gif")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse((new URL(url)).toURI());
                } catch (Throwable t) {
                    Application.getWindowManager().showError("Unable to launch web browser for " + url, t);
                }
            }
        };
    }

    public static Action createHelpAction(final StandardException se) {
        final String url = GseaWebResources.getGseaHelpURL() + "GSEA/GSEA_User_Guide/#error-" + "" + se.getErrorCode();

        return new AbstractAction("Help for error " + se.getErrorCode(), getIcon("Help16_v2.gif")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(java.net.URI.create(url));
                } catch (Throwable t) {
                    Application.getWindowManager().showError("Unable to launch web browser for " + url, t);
                }
            }
        };

    }

    /**
     * if not found a ICON_NOT_FOUND is returned instead
     *
     * @param name of the filename that the resource is in. For example "foo.gif".
     * @return Icon
     */
    public static Icon getIcon(String name) {
        URL url = null;
        try {
            //log.warn("Looking up icon for: " + name);
            url = toURL(name);
            if (url != null) {
                return new ImageIcon(url);
            }
        } catch (Throwable t) {
            klog.error(t.getMessage(), t);
        }
        //throw new RuntimeException("cant find: " + name);
        klog.warn("could not find resource: {} url: {} ... using default icon instead.", name, url);

        return ICON_NOT_FOUND;
    }

    public static ImageIcon getImageIcon(String name) {
        return (ImageIcon) getIcon(name);
    }

    /**
     * Not a good way to use for large images that might take a while to load
     *
     * @param name
     * @return
     */
    public static Image getImage(String name) {
        return ((ImageIcon) getIcon(name)).getImage();
    }
}
