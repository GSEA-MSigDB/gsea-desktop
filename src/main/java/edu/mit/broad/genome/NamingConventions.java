/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.parsers.DataFormat;

/**
 * Class NamingConventions
 * <p/>
 * Rules for object / default etc naming conventions are all captured here.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class NamingConventions {

    private static int kObjectCounter = 1;

    private static final int MAX_FILE_LEN_ALLOWED = 100;

    public static String splitLongHashName(final String possibleLongNameWithHashes, final String prefixStr) {
        if (possibleLongNameWithHashes == null ||
                possibleLongNameWithHashes.indexOf("#") == -1 ||
                possibleLongNameWithHashes.length() < 80) {
            return possibleLongNameWithHashes;
        }

        StringBuffer buf = new StringBuffer();

        for (int c = 0; c < possibleLongNameWithHashes.length(); c++) {
            char cr = possibleLongNameWithHashes.charAt(c);
            if (cr == '#') {
                buf.append(prefixStr);
            }
            buf.append(cr);
        }

        return buf.toString();
    }

    public static boolean isURL(final String path) {
        if (path.toLowerCase().startsWith("http")
                || path.toLowerCase().startsWith("https")
                || path.toLowerCase().startsWith("www")
                || path.toLowerCase().startsWith("ftp")
                ) {
            return true;
        } else {
            return false;
        }
    }

    public static String titleize(String title) {

        if (isNull(title)) {
            return Constants.NULL;
        } else {

            if (title.startsWith("\"")) {
                title = title.substring(1, title.length());
            }

            if (title.endsWith("\"")) {
                title = title.substring(0, title.length() - 1);
            }

            return title;
        }

    }

    public static String createNiceEnglishDate_for_dirs() {
        // Get today's date
        Date date = new Date();
        
        // Forcibly use the ENGLISH Locale to avoid possible bugs with file system paths in system Locale.  For example, 
        // a Chinese user reported an error launching our HTML reports in a web browser, tracing back to this JDK bug:
        // https://bugs.openjdk.java.net/browse/JDK-8146326
        Format formatter = new SimpleDateFormat("MMM", Locale.ENGLISH);
        String s = (formatter.format(date));
        formatter = new SimpleDateFormat("dd", Locale.ENGLISH);
        s = (s + (formatter.format(date))).toLowerCase();
        //s = s.replace('_', '\s');        
        return s;
    }

    public static boolean isNull(String s) {
        return StringUtils.isBlank(s);
    }

    public static String removeExtension(final File file) {
        return removeExtension(file.getName());
    }

    // TODO: possibly use Commons IO FilenameUtils.  We're doing some extra sophisticated stuff here, though, so it may not be valid.
    public static String removeExtension(final String fileName) {

        if (fileName == null) {
            throw new IllegalArgumentException("Param fileName cannot be null");
        }

        if (fileName.indexOf(".") == -1) {
            return fileName;
        }

        if (fileName.indexOf('#') != -1) { // aux names are of the form: foo.cls#classA so removing the ext is deleterious
            return fileName;
        }

        if (fileName.endsWith(".")) { // simply strip off the period
            return fileName.substring(0, fileName.length() - 1);
        }

        // we also dont want to do the following:
        // foo_genes.hgu95av2.grp_mapped_symbols_from_HuGeneFL -> foo_genes.hgu95av2
        // (this would happen if we simply remove the extension)
        final int index = fileName.lastIndexOf('.');

        if (-1 == index) { // no periods
            return fileName;
        } else {
            String just_name = fileName.substring(0, index);
            String ext = fileName.substring(index + 1, fileName.length());
            if (ext.length() <= 4) {
                return just_name; // common heuristic
            }

            StringTokenizer tok = new StringTokenizer(ext, "_");
            tok.nextToken(); // get rid of the "ext"
            StringBuffer extra = new StringBuffer("_");
            while (tok.hasMoreElements()) {
                extra.append(tok.nextToken());
                if (tok.hasMoreElements()) {
                    extra.append("_");
                }
            }

            if (extra.length() != 1) {
                return just_name + extra;
            } else {
                return just_name;
            }
        }
    }

    public static String removeExtension(final PersistentObject pob) {
        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }
        return removeExtension(pob.getName());
    }

    // replaces chars that make file paths barf such as @ and #
    public static File createSafeFile(final File inDir, final String name) {
        return new File(inDir, createSafeFileName(name));
    }

    public static String createSafeFileName(String name) {
        name = name.trim();
        String safename = name.replace('@', '_'); // this does all occurrences
        safename = safename.replace('#', '_');
        safename = safename.replace(' ', '_'); // get rid of whitespace
        safename = safename.replace('%', '_');
        safename = safename.replace('$', '_');
        safename = safename.replace(':', '_'); // IE often doesnt like colons in linked to files
        safename = safename.replace('*', '_');
        safename = safename.replace('\\', '_');
        safename = safename.replace('/', '_');
        // TODO: review the need for this.
        // Not sure the reason for this one; keeping for now.  Clearly we don't want control-backslash in our file names,
        // but we don't want *any* control chars in file names, so why target just this one?
        safename = safename.replace(Constants.LEGACY_FIELD_DELIM, '_');

        if (safename.length() >= MAX_FILE_LEN_ALLOWED) {
            String ext = getExtension(safename);
            safename = safename.substring(0, MAX_FILE_LEN_ALLOWED) + "." + ext; // too many chars make excel (for instance) not recognize the file
        }

        return safename;
    }

    public static Object[] parseReportTimestampFromName(final String rptname) {

        StringTokenizer tok = new StringTokenizer(rptname, ".");
        if (tok.countTokens() != 4) {
            throw new IllegalArgumentException("Invalid rpt name format: " + rptname + " got tokens #: " + tok.countTokens());
        }
        StringBuilder buf = new StringBuilder();
        buf.append(tok.nextToken()).append('.');
        buf.append(tok.nextToken());
        return new Object[]{buf.toString(), Long.parseLong(tok.nextToken())};
    }


    /**
     * Convention:
     * <p/>
     * orig_name.short_desc_tag.ts.ext
     * <p/>
     * short_desc_tag can be null
     *
     * @param short_desc_tag
     * @param pob
     * @return
     */
    public static String generateName(final PersistentObject pob, final String short_desc_tag, final boolean ultraUniquenessSafe) {

        if (pob == null) {
            return _generateName(null, short_desc_tag, null);
        } else {
            String ext = (DataFormat.getExtension(pob));
            return _generateName(pob.getName(), short_desc_tag, ext, ultraUniquenessSafe);
        }
    }

    public static String generateName(final Dataset ds, final Template t, final boolean stripExtensions) {

        StringBuffer buf = new StringBuffer();

        if (stripExtensions) {
            buf.append(removeExtension(ds.getName())).append(".");
            buf.append(removeExtension(t.getName()));
        } else {
            buf.append(ds.getName()).append('.');
            buf.append(t.getName());
        }

        return buf.toString();
    }

    public static String generateName(final Dataset ds, final GeneSet gset, final boolean stripExtensions) {

        StringBuffer buf = new StringBuffer();

        if (stripExtensions) {
            buf.append(removeExtension(ds.getName())).append(".");
            buf.append(removeExtension(gset.getName()));
        } else {
            buf.append(ds.getName()).append('.');
            buf.append(gset.getName());
        }

        return buf.toString();
    }

    public static String generateName(final PersistentObject pob) {
        return generateName(pob, null, false);
    }

    private static String _generateName(final String origname, final String short_desc_tag, final String ext) {
        return _generateName(origname, short_desc_tag, ext, false);
    }

    // orig_name.short_desc_tag.ts_or_counter.ext
    private static String _generateName(final String origname, final String short_desc_tag, final String ext, final boolean ultraUniquenessSafe) {

        StringBuffer buf = new StringBuffer();

        if (origname != null) {
            buf.append(removeExtension(origname));
            buf.append('.');
        }

        if (short_desc_tag != null) {
            StringUtils.replace(short_desc_tag, " ", "");
            StringUtils.replace(short_desc_tag, "\t", "");
            buf.append(short_desc_tag);
            buf.append('.');
        }

        if (ultraUniquenessSafe) {
            buf.append(generateUniqueNameTag());
        } else {
            buf.append(generateCounterNameTag());
        }

        if (ext != null) {
            buf.append('.');
            buf.append(ext);
        }

        return buf.toString();
    }

    private static synchronized String generateUniqueNameTag() {
        return new StringBuffer().append(System.currentTimeMillis()).toString();
    }

    // use me if you decide decided against System.currentTimeMillis() as it
    // makes the names very long and uniintuitive
    // as to who came first
    public static synchronized String generateCounterNameTag() {
        return new StringBuffer().append(kObjectCounter++).toString();
    }

    /**
     * Gets the extension of a specified file name. The extension is
     * everything after the last dot.
     * <p/>
     * <pre>
     * foo.txt    --> "txt"
     * a\b\c.jpg  --> "jpg"
     * foo        --> ""
     * </pre>
     *
     * @param f String representing the file name
     * @return extension of the file (or <code>""</code> if it had none)
     */
    public static String getExtension(final String f) {

        String ext;
        int pos;

        pos = f.lastIndexOf(".");

        if (pos == -1) {
            //log.warn("No extension found - returning \"\" file=" + f);
            ext = "";
        } else {
            ext = f.substring(pos + 1);
        }

        //og.debug("got: " + f + " foind: " + ext);

        if (f.indexOf(".meta.") != -1) {
            return "meta." + ext;
        } else {
            return ext;
        }
    }

    /**
     * Gets the extension of a specified file. The extension is
     * everything after the last dot.
     * <p/>
     * <pre>
     * foo.txt    --> "txt"
     * a\b\c.jpg  --> "jpg"
     * foo        --> ""
     * </pre>
     *
     * @param f the File
     * @return extension of the file (or <code>""</code> if it had none)
     * @see String getExtension(String f)
     */
    public static String getExtension(File f) {
        return getExtension(f.getName());
    }


    /**
     * Gets the extension of a specified file name. The extension is
     * everything after the last dot.  Files created with excel may
     * have had a .txt extension appended to the end of the intended file
     * name.  This routine checks txt files to see whether there is a
     * recognized extension preceding it.  Recognized extensions are those
     * associated with file formats that likely to be created/edited using
     * excel: GCT, RES, PCL, GMX, GMT, RNK.  We have introduced this "liberal"
     * parsing of file extension in order to cut back on the number of RT tickets.
     * </p>
     * <pre>
     * foo.txt    --> "txt"
     * a\b\c.jpg  --> "jpg"
     * foo        --> ""
     * foo.gct.txt --> "gct"
     * foo.gmx.txt --> "gmx"
     * </pre>
     *
     * @param f String representing the file name
     * @return extension of the file (or <code>""</code> if it had none)
     */
    public static String getExtensionLiberal(final String f) {

        String ext, ext2;
        int pos, pos2;

        pos = f.lastIndexOf(".");

        if (pos == -1) {
            //log.warn("No extension found - returning \"\" file=" + f);
            ext = "";
        } else {

            if (pos > 0) {
                pos2 = f.lastIndexOf(".", pos - 1 );
                if (pos2 != -1) {
                    ext2 = f.substring(pos2+1, pos);
                    if (ext2.equalsIgnoreCase(Constants.GCT))
                        return Constants.GCT;
                    else if (ext2.equalsIgnoreCase(Constants.RES))
                        return Constants.RES;
                    else if (ext2.equalsIgnoreCase(Constants.PCL))
                        return Constants.PCL;
                    else if (ext2.equalsIgnoreCase(Constants.GMX))
                        return Constants.GMX;
                    else if (ext2.equalsIgnoreCase(Constants.GMT))
                        return Constants.GMT;
                    else if (ext2.equalsIgnoreCase(Constants.RNK))
                        return Constants.RNK;
                    else if (ext2.equalsIgnoreCase(Constants.CLS))
                        return Constants.CLS;
                    }
            }
            ext = f.substring(pos + 1);
        }

        return ext;
    }

}    // End NamingConventions
