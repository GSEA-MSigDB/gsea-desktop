/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import edu.mit.broad.genome.parsers.ParseUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class CmdLineArgs {

    private static final Logger klog = Logger.getLogger(CmdLineArgs.class);

    public static String toString(String[] args) {
        // first recreate the arg line -- its easier to parse
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < args.length; i++) {
            //System.out.println(">" + args[i] + "<");
            buf.append(args[i]).append(" ");
        }

        return buf.toString();
    }

    /**
     * @param args
     * @todo '-' in file paths/names causes the parsing to barf
     * <p/>
     * two kinds of keyvals are allowed
     * 1) -cls foo    <-- parameter name "cls" has value foo
     * 2) -tag        <-- parameter name "tag: is TRUE
     * Trouble is that we need to know if tag is a boolean param or not
     * value less specification is only allowed for booleans
     * Other examples
     * -tag false  <-- param tag is Boolean.FALSE
     * -names foo,bar,zok -> param names -> foo, bar, zok (NOT parsed here)
     */
    public static Properties parse(String[] args) {

        Properties prp = new Properties();

        // first recreate the arg line -- its easier to parse
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < args.length; i++) {
            //System.out.println(">" + args[i] + "<");
            buf.append(args[i]).append(" ");
        }

        String argline = buf.toString().trim();
        //klog.debug("ARGLINE>" + argline + "<");

        StringTokenizer tok = new StringTokenizer(argline, "-"); // IMP spaces are NOT delimiters at this stage
        //klog.debug("# of tokens: " + tok.countTokens());
        Set keyval = new HashSet();
        while (tok.hasMoreElements()) {
            String kv = tok.nextToken();
            keyval.add(kv);
            //klog.debug("found kv: " + kv);
        }

        // now fill them up
        Iterator it = keyval.iterator();
        while (it.hasNext()) {
            String kv = it.next().toString();
            tok = new StringTokenizer(kv, " ="); // '=' as a favor to human errors
            _fillParam(tok, kv, prp);
        }

        return prp;
    }

    private static void _fillParam(StringTokenizer tok, String origString, Properties prp) {
        List tokens = ParseUtils.getUniqueTokens(tok);

        // now the trouble is that the second param, sometimes a file name, can have spaces
        // note that in general there is nothing to prevent the 'value' from having a space
        // as it doesnt affect the '-' based tokenizing (' ' is NOT a delim that seperates 'key-val' pairs
        // so as a mechanism to allow space in file names (the most common instance where space occurs in  a key-val)

        String param_name = null;
        String param_val = null;

        if (tokens.size() == 1) {
            param_name = tokens.get(0).toString();
        } else if (tokens.size() == 2) {
            param_name = tokens.get(0).toString();
            param_val = tokens.get(1).toString();
        } else {
            klog.warn("More than 2 tokens for key-value pair >" + origString + "<" + " " + tokens.size());
            int num = tokens.size();
            param_name = tokens.get(0).toString();

            // heres the space fix
            param_val = "";
            for (int i = 1; i < num; i++) {
                param_val = param_val + tokens.get(i) + " ";
            }
        }

        param_name = param_name.trim();
        if (param_val != null) {
            param_val = param_val.trim();
            prp.setProperty(param_name, param_val);
        }
    }

}
