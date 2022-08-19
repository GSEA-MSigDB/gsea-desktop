/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

public enum MSigDBSpecies {
    Human, Mouse, Unknown;
    
    public static MSigDBSpecies byName(String name) {
      if ("Human".equalsIgnoreCase(name)) { return Human; }
      if ("Mouse".equalsIgnoreCase(name)) { return Mouse; }
      return Unknown;
    }
}
