/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.vdb.chip.Chip;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Aravind Subramanian
 */
public class MapUtils {

    private static final Logger klog = Logger.getLogger(MapUtils.class);

    private static final char DELIM = '.';
    private static final String DELIMS = ".";

    /**
     * @param name
     * @param mgms
     * @return
     */
    public static GeneSetMatrix createCombinedGeneSetMatrix(final String name, final MGeneSetMatrix[] mgms) {

        if (mgms == null) {
            throw new IllegalArgumentException("Param mgms cannot be null");
        }
        if (mgms.length == 0) {
            throw new IllegalArgumentException("No mapped sets provided: " + mgms.length);
        }


        final Set[] sets = new Set[mgms[0].getNumMappedSets()];
        final String[] names = new String[sets.length];
        for (int i = 0; i < mgms.length; i++) {
            MGeneSetMatrix mgm = mgms[i];
            for (int j = 0; j < sets.length; j++) {
                GeneSet gset = mgm.getMappedGeneSet(j).getMappedGeneSet(true);
                if (sets[j] == null) {
                    sets[j] = new HashSet();
                }
                sets[j].addAll(gset.getMembersS());
                if (i == 0) {
                    names[j] = gset.getName();
                }
            }
        }

        GeneSet[] gsets = new GeneSet[sets.length];
        for (int i = 0; i < sets.length; i++) {
            gsets[i] = new FSet(AuxUtils.getAuxNameOnlyNoHash(names[i]), sets[i]);
        }

        return new DefaultGeneSetMatrix(name, gsets);
    }

    // --------------------------------------------------------------------------------------- //
    // ------------------------ ID RELATED METHODS ------------------------------------------- //
    // --------------------------------------------------------------------------------------- //
    public static String createId(final String sourceChipName, final String targetChipName) {
        return sourceChipName + DELIM + targetChipName;
    }

    public static String createId(final Chip sourceChip, final Chip targetChip) {
        return createId(sourceChip.getName(), targetChip.getName());
    }

    public static String createId(final String sourceChipName, final String targetChipName, final MappingDbType db) {
        return createId(sourceChipName, targetChipName, db.getName());
    }

    public static String createId(final String sourceChipName, final String targetChipName, final String db) {
        return sourceChipName + DELIM + targetChipName + DELIM + db;
    }

    public static String createId(final Chip sourceChip, final Chip targetChip, final MappingDbType db) {
        return createId(sourceChip.getName(), targetChip.getName(), db);
    }

    public static boolean equals(final Chip sourceChip, final Chip targetChip, final MappingDbType db,
                                 String fSourceChipName, String fTargetChipName, String fMappingDbType) {
        if ((sourceChip.getName().equals(fSourceChipName))
                && (targetChip.getName().equals(fTargetChipName))
                && (db.equals(fMappingDbType))) {
            return true;
        }

        return false;
    }

    public static boolean equals(final Chip sourceChip, final Chip targetChip, final MappingDbType db,
                                 Chip sourceChip2, Chip targetChip2, String db2) {
        return equals(sourceChip, targetChip, db, sourceChip2.getName(), targetChip2.getName(), db2);
    }

    public static boolean equals(final Chip sourceChip, final Chip targetChip,
                                 final Chip sourceChip2, final Chip targetChip2) {
        if ((sourceChip.getName().equals(sourceChip2.getName()))
                && (targetChip.getName().equals(targetChip2.getName()))) {
            return true;
        }

        return false;
    }

    public static boolean equals(final Chip sourceChip, final Chip targetChip,
                                 final String sourceChip2, final String targetChip2) {
        if ((sourceChip.getName().equals(sourceChip2))
                && (targetChip.getName().equals(targetChip2))) {
            return true;
        }

        return false;
    }

    public static void saveMappedChip(final Chip2ChipMapper mc, final File outFile) throws Exception {

        final File txtFile = new File(outFile.getPath() + ".map");
        final PrintWriter pw = new PrintWriter(new FileOutputStream(txtFile));
        final String[] probes = mc.getSourceProbes();

        for (int i = 0; i < probes.length; i++) {
            StringBuffer buf = new StringBuffer(probes[i]).append('\t');
            Set set = mc.map(probes[i]);
            //buf.append(set.size()).append('\t');
            for (Iterator iterator = set.iterator(); iterator.hasNext();) {
                Object o = iterator.next();
                buf.append(o.toString());
                if (iterator.hasNext()) {
                    buf.append(',');
                }
            }

            buf.append('\n');
            pw.print(buf.toString());
        }
        pw.close();
    }

    public static void printf(Chip2ChipMapper mc) {
        StringBuffer buf = new StringBuffer();
        buf.append("Info for mc: ").append(mc.getName()).append('\n');
        buf.append("# of source probes: ").append(mc.getNumSourceProbes()).append('\n');

        /*
        final String[] probes = mc.getSourceProbes();
        int tot = 0;
        for (int i = 0; i < probes.length; i++) {
            Set set = mc.map(probes[i]);
            tot += set.size();
        }

        buf.append("# of target probes: " + tot).append('\n');
        */

        klog.info(buf.toString());
    }


    public static class Struc {
        public String sourceChipName;
        public String targetChipName;
        public String mappingDbType;

        public Struc(File file) {

            //String s = FileUtils.removeExtension(file);
            String[] ss = ParseUtils.string2strings(file.getName(), DELIMS, false);
            if (ss.length < 3) {
                throw new IllegalArgumentException("Not a map file: " + file + " # tokens: " + ss.length);
            }

            this.sourceChipName = ss[0];
            this.targetChipName = ss[1];
            this.mappingDbType = ss[2];
        }

        public MappingDbType getMappingDbType() {
            return MappingDbTypes.lookupMappingType(mappingDbType);
        }

        public boolean equals(final Chip sourceChip, final Chip targetChip, final MappingDbType db) {
            return (sourceChip.getName().equals(sourceChipName))
                    && (targetChip.getName().equals(targetChipName)) && (db.getName().equals(mappingDbType));
        }

        public boolean equals(final Chip sourceChip, final Chip targetChip) {
            return (sourceChip.getName().equals(sourceChipName))
                    && (targetChip.getName().equals(targetChipName));
        }

        public String createChipId() {
            return MapUtils.createId(sourceChipName, targetChipName);
        }

        public String createId() {
            return MapUtils.createId(sourceChipName, targetChipName, mappingDbType);
        }
    }


} // End class MapUtils

