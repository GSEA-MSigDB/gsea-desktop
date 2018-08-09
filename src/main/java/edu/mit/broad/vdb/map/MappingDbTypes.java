/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.Constants;

/**
 * @author Aravind Subramanian
 */
public class MappingDbTypes {

    public static MappingDbType ORTHOLOG = new MappingDbTypeImpl("Ortholog");
    public static MappingDbType BEST_MATCH = new MappingDbTypeImpl("Best_Match");
    public static MappingDbType GOOD_MATCH = new MappingDbTypeImpl("Good_Match");
    public static MappingDbType COMBO = new MappingDbTypeImpl("Combo");
    public static MappingDbType GENE_SYMBOL = new MappingDbTypeImpl(Constants.GENE_SYMBOL);

    public static MappingDbType lookupMappingType(String sourcepath) {

        if (sourcepath == null) {
            throw new IllegalArgumentException("Param sourcepath cannot be null");
        }

        sourcepath = sourcepath.toUpperCase();

        if (sourcepath.indexOf("BEST") != -1) {
            return BEST_MATCH;
        }

        if (sourcepath.indexOf("GOOD") != -1) {
            return GOOD_MATCH;
        }

        if (sourcepath.indexOf("ORTHOLOG") != -1) {
            return ORTHOLOG;
        }

        if (sourcepath.indexOf("COMBO") != -1) {
            return COMBO;
        }

        if (sourcepath.indexOf("GENE_SYMBOL") != -1) {
            return GENE_SYMBOL;
        }

        throw new IllegalArgumentException("Unknown mapping db type: " + sourcepath);

    }

    static class MappingDbTypeImpl implements MappingDbType {

        String type;

        MappingDbTypeImpl(String type) {
            this.type = type;
        }

        public boolean equals(Object obj) {
            if (obj instanceof MappingDbType) {
                return ((MappingDbType) obj).getName().equals(this.type);
            }

            return false;
        }

        public String getName() {
            return type;
        }

    }

} // End class MappingDbTypes
