/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io.expr.cls;

import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.AbstractReader;

import edu.mit.broad.genome.utils.ParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class for reading cls documents.
 *
 * @author Joshua Gould
 */
public class ClsReader extends AbstractReader {

    public ClsReader() {
        super(new String[]{"cls"}, "cls");
    }

    public ClassVector read(String pathname) throws IOException, ParseException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(pathname);
            return read(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public ClassVector read(InputStream is) throws IOException, ParseException {
        ClsParser parser = new ClsParser();
        MyHandler handler = new MyHandler();
        parser.setHandler(handler);
        parser.parse(is);
        return new ClassVector(handler.x, handler.classes);
    }

    /**
     * @author Joshua Gould
     */
    private static class MyHandler implements IClsHandler {
        String[] x;

        String[] classes;

        public void assignments(String[] x) {
            this.x = x;
        }

        public void classes(String[] c) {
            this.classes = c;
        }
    }
}