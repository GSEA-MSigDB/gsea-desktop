/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

import edu.mit.broad.genome.utils.SystemUtils;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;


/**
 * NEW
 * <p/>
 * duties
 * <p/>
 * 1) ensure that there is always a configurator set
 * 2) manage configurators - diff ports on programmatic request
 * <p/>
 * Avoid using config file - safer + useful to have
 * programattic control for xomics
 */
public class XLogger {

    private static final Level DEFAULT_LEVEL = Level.ALL;

    private static Level kLevel = DEFAULT_LEVEL;

    public static Layout CLICK_AND_BROWSE_LAYOUT;

    private static Layout SIMPLE_LAYOUT;

    static {


        if (Conf.isDebugMode()) {
            kLevel = DEFAULT_LEVEL;
        } else {
            kLevel = Level.INFO;
        }


        CLICK_AND_BROWSE_LAYOUT = new MyPatternLayout("%-4r [%-5p] %m\tat %c.%M(%F:%L)\n");    // IDE click-and-browsable format
        BasicConfigurator.configure(new ConsoleAppender(CLICK_AND_BROWSE_LAYOUT, ConsoleAppender.SYSTEM_OUT));

    }

    /**
     * Logger configured to default appender
     *
     * @param cs
     * @return
     */
    public static Logger getLogger(final Class cs) {
        Logger logger = Logger.getLogger(cs);
        //System.out.println("#### getting logger: " + cs);
        logger.setLevel(kLevel);
        return logger;
        //return Logger.getLogger(cs);
    }


    // no tabs, html etc
    // for the status bar of applications
    // NO ts
    public static Layout getSimpleLayout() {
        if (SIMPLE_LAYOUT == null) {
            SIMPLE_LAYOUT = new XLogger.MyPatternLayout("[%-5p] %m at %c.%M (%F:%L)\n");
        }

        return SIMPLE_LAYOUT;
    }

    public static void addAppender(final Appender appender) {
        //System.out.println(">>>>> adding appender: " + appender.getClass());
        //TraceUtils.showTrace();
        Logger.getRootLogger().addAppender(appender);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("Finalizing XLogger -- removing all appenders");
        Logger.getRootLogger().removeAllAppenders();    // so that ports (if any) shut down ok
    }

    /**
     * Achieves a programmatic pattern
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static class MyPatternLayout extends PatternLayout {

        public MyPatternLayout() {
            this(DEFAULT_CONVERSION_PATTERN);
        }

        public MyPatternLayout(String pattern) {
            super(pattern);
        }

        public String format(LoggingEvent le) {

            if (SystemUtils.isPropertyDefined("XSERVLET")) {
                return le.getRenderedMessage();
            }

            if (Conf.isDebugMode()) {
                return super.format(le);
            } else {
                // dont like the stack trace thing for info loggings
                if (le.getLevel() == Level.INFO) {
                    String s = Long.toString(le.timeStamp);
                    return new StringBuffer(s.substring(s.length() - 4, s.length())).append(" [INFO ] ").append(le.getMessage().toString()).append('\n').toString();
                } else {
                    return super.format(le);
                }
            }
        }

    }    // End MyPatternLayout
}        // End XLogger
