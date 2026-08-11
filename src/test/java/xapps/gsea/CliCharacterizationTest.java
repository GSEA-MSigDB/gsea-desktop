/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xtools.api.Tool;
import xtools.api.param.Param;
import xtools.api.param.ParamSet;
import xtools.chip2chip.Chip2Chip;
import xtools.gsea.Gsea;
import xtools.gsea.GseaPreranked;
import xtools.gsea.LeadingEdgeTool;
import xtools.gsea.SsGsea;
import xtools.munge.CollapseDataset;

/**
 * CLI / tool contract characterization. Must stay green across UI modernization.
 */
public class CliCharacterizationTest {

    @Test
    public void cliRejectsUnknownOperation() {
        assertThrows(Exception.class, () -> CLI.main(new String[] { "NotARealOp" }));
    }

    @Test
    public void cliUsageMessageDocumentsOperations() throws Exception {
        // Do not call CLI.main(empty) — it System.exit's. Assert usage text contract instead.
        java.lang.reflect.Field f = CLI.class.getDeclaredField("USAGE_MESSAGE");
        f.setAccessible(true);
        String usage = (String) f.get(null);
        assertTrue(usage.contains("GSEA"));
        assertTrue(usage.contains("GSEAPreranked"));
        assertTrue(usage.contains("ssGSEA") || usage.contains("SsGsea"));
        assertTrue(usage.contains("CollapseDataset"));
        assertTrue(usage.contains("Chip2Chip"));
        assertTrue(usage.contains("LeadingEdgeTool"));
    }

    @Test
    public void paramRoundTripPreservesKeys_Gsea() {
        assertParamRoundTrip(new Gsea(), Gsea::new);
    }

    @Test
    public void paramRoundTripPreservesKeys_GseaPreranked() {
        assertParamRoundTrip(new GseaPreranked(), GseaPreranked::new);
    }

    @Test
    public void paramRoundTripPreservesKeys_SsGsea() {
        assertParamRoundTrip(new SsGsea(), SsGsea::new);
    }

    @Test
    public void paramRoundTripPreservesKeys_CollapseDataset() {
        assertParamRoundTrip(new CollapseDataset(), CollapseDataset::new);
    }

    @Test
    public void paramRoundTripPreservesKeys_Chip2Chip() {
        assertParamRoundTrip(new Chip2Chip(), Chip2Chip::new);
    }

    @Test
    public void paramRoundTripPreservesKeys_LeadingEdgeTool() {
        assertParamRoundTrip(new LeadingEdgeTool(), LeadingEdgeTool::new);
    }

    @Test
    public void defaultToolParamSetsExposeStableNames() {
        Tool[] tools = {
                new Gsea(), new GseaPreranked(), new SsGsea(),
                new CollapseDataset(), new Chip2Chip(), new LeadingEdgeTool()
        };
        for (Tool tool : tools) {
            ParamSet pset = tool.getParamSet();
            assertNotNull(pset);
            assertTrue(pset.getNumParams() > 0, tool.getName());
            Set<String> names = paramNames(pset);
            assertFalse(names.isEmpty(), tool.getName());
        }
    }

    @FunctionalInterface
    private interface PropertiesToolFactory {
        Tool create(Properties properties);
    }

    private static void assertParamRoundTrip(Tool fresh, PropertiesToolFactory factory) {
        ParamSet original = fresh.getParamSet();
        Properties props = original.toProperties();
        assertNotNull(props);

        Tool rebuilt = factory.create(props);
        ParamSet roundTrip = rebuilt.getParamSet();

        Set<String> rebuiltNames = paramNames(roundTrip);
        for (String name : paramNames(original)) {
            assertTrue(rebuiltNames.contains(name),
                    () -> fresh.getName() + " lost param name: " + name);
        }
    }

    private static Set<String> paramNames(ParamSet pset) {
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < pset.getNumParams(); i++) {
            Param p = pset.getParam(i);
            if (p != null && p.getName() != null) {
                names.add(p.getName());
            }
        }
        return names;
    }
}
