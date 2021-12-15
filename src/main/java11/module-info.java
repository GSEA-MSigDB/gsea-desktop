/*
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
module org.gsea_msigdb.gsea {
    exports edu.mit.broad.cytoscape;
    exports edu.mit.broad.cytoscape.action;
    exports edu.mit.broad.cytoscape.view;
    exports edu.mit.broad.genome;
    exports edu.mit.broad.genome.alg;
    exports edu.mit.broad.genome.alg.distrib;
    exports edu.mit.broad.genome.alg.fdr;
    exports edu.mit.broad.genome.alg.gsea;
    exports edu.mit.broad.genome.alg.markers;
    exports edu.mit.broad.genome.charts;
    exports edu.mit.broad.genome.io;
    exports edu.mit.broad.genome.math;
    exports edu.mit.broad.genome.models;
    exports edu.mit.broad.genome.objects;
    exports edu.mit.broad.genome.objects.esmatrix.db;
    exports edu.mit.broad.genome.objects.strucs;
    exports edu.mit.broad.genome.parsers;
    exports edu.mit.broad.genome.reports;
    exports edu.mit.broad.genome.reports.api;
    exports edu.mit.broad.genome.reports.pages;
    exports edu.mit.broad.genome.reports.web;
    exports edu.mit.broad.genome.swing;
    exports edu.mit.broad.genome.swing.dnd;
    exports edu.mit.broad.genome.swing.fields;
    exports edu.mit.broad.genome.swing.image;
    exports edu.mit.broad.genome.swing.windows;
    exports edu.mit.broad.genome.utils;
    exports edu.mit.broad.genome.viewers;
    exports edu.mit.broad.vdb;
    exports edu.mit.broad.vdb.chip;
    exports edu.mit.broad.vdb.map;
    exports edu.mit.broad.vdb.meg;
    exports edu.mit.broad.xbench;
    exports edu.mit.broad.xbench.actions;
    exports edu.mit.broad.xbench.actions.ext;
    exports edu.mit.broad.xbench.actions.misc_actions;
    exports edu.mit.broad.xbench.core;
    exports edu.mit.broad.xbench.core.api;
    exports edu.mit.broad.xbench.explorer.filemgr;
    exports edu.mit.broad.xbench.explorer.objmgr;
    exports edu.mit.broad.xbench.heatmap;
    exports edu.mit.broad.xbench.prefs;
    exports edu.mit.broad.xbench.searchers;
    exports edu.mit.broad.xbench.tui;
    exports edu.mit.broad.xbench.xchoosers;
    exports org.broad.gsea.ui;
    exports org.genepattern.annotation;
    exports org.genepattern.data.expr;
    exports org.genepattern.data.matrix;
    exports org.genepattern.gsea;
    exports org.genepattern.heatmap.image;
    exports org.genepattern.io;
    exports org.genepattern.io.expr;
    exports org.genepattern.io.expr.cls;
    exports org.genepattern.io.expr.gct;
    exports org.genepattern.io.expr.res;
    exports org.genepattern.heatmap;
    exports org.genepattern.menu;
    exports org.genepattern.menu.jfree;
    exports org.genepattern.module;
    exports org.genepattern.modules;
    exports org.genepattern.plot;
    exports org.genepattern.table;
    exports org.genepattern.uiutil;
    exports xapps.api;
    exports xapps.api.frameworks;
    exports xapps.api.frameworks.fiji;
    exports xapps.api.vtools;
    exports xapps.gsea;
    exports xtools.api;
    exports xtools.api.param;
    exports xtools.api.ui;
    exports xtools.chip2chip;
    exports xtools.gsea;
    exports xtools.munge;

    requires algorithms;
    requires batik.awt.util;
    requires batik.dom;
    requires batik.svggen;
    requires commons.cli;
    requires commons.compress;
    requires commons.io;
    requires commons.lang3;
    requires dom4j.full;
    requires ecs;
    requires edtftpj;
    requires forms;
    requires httpclient;
    requires httpcore;
    requires java.datatransfer;
    requires java.desktop;
    requires java.prefs;
    requires java.xml;
    requires jcommon;
    requires jdk.xml.dom;
    requires jfreechart;
    requires jgoodies.uif.lite;
    requires jide.common;
    requires json.simple;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires looks;
    requires maven.artifact;
    requires SGLayout;
    requires trove;
    requires ujmp.complete;
}