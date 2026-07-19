/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;

public class ChipOptParam extends AbstractParam {
    public ChipOptParam(boolean reqd) {
        this(CHIP, CHIP_ENGLISH, CHIP_DESC, reqd);
    }

    public ChipOptParam(final String name, final String nameEnglish, final String desc, final boolean reqd) {
        super(name, nameEnglish, Chip.class, desc, null, new Chip[] {}, reqd);
    }

    public Chip getChip() throws Exception {
        Object val = getValue();
        if (val == null) {
            return null;
        }
        return VdbRuntimeResources.getChip(val.toString());
    }

    private String format(final Object[] vals) {
        if (vals == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                continue;
            }

            log.debug("{}", vals[i].getClass());

            if (vals[i] instanceof PersistentObject) {
                String p = ParserFactory.getCache().getSourcePath(vals[i]);
                buf.append(p);
            } else {
                buf.append(vals[i].toString().trim());
            }

            if (i != vals.length - 1) {
                buf.append(',');
            }
        }

        return buf.toString();
    }

    public boolean isFileBased() {
        return true;
    }

    public String getValueStringRepresentation(final boolean full) {
        Object val = getValue();

        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            return (String) val;
        } else if (val instanceof Object[]) {
            Object[] objs = (Object[]) val;
            return format(objs);
        } else {
            return format(new Object[] { val });
        }
    }
}
