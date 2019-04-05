/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature list reader.
 *
 * @author Joshua Gould
 */
public class FeatureListReader extends AbstractReader {

    public FeatureListReader() {
        super(new String[]{"grp"}, "grp");
    }

    /**
     * Gets the list of features contained in the given file
     *
     * @param fileName The file path
     * @return The feature list
     * @throws IOException If an error occurs while reading the file
     */
    public List<String> read(String fileName) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String s = null;
            List<String> probeList = new ArrayList<>();
            while ((s = br.readLine()) != null) {
                String feature = s.trim();
                if (!feature.equals("")) {
                    probeList.add(feature);
                }
            }
            return probeList;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}