/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

/** Concise Method Guide text for the desktop CoreMap pane. */
final class CoreMapMethodGuideText {

    private CoreMapMethodGuideText() {
    }

    static String text() {
        return """
                How it works

                CoreMap links two precomputed GSEA results — a mechanistic layer and a phenotypic layer —
                through a gene/set interactome. It ranks short paths (Bridges) from mechanistic sets to
                phenotypic sets and finds shared drivers (genes in both leading edges). It does not run GSEA.

                Scope
                • Does: parse Broad GSEA EDB outputs, fetch a neighborhood interactome, score edges, search
                  mech-set → pheno-set paths, and draw one Cytoscape.js map.
                • Does not: run GSEA, prove causality, or apply tissue-specific expression filters. Ranked
                  Bridges are network candidates given the chosen interactome and search settings.

                Desktop notes (vs. the browser app)
                • Calls SIGNOR / STRING / fused APIs directly (no CORS proxy).
                • Inputs are GSEA report folders or the report cache (not ZIP uploads).
                • No demo interactome — use none (co-membership), SIGNOR, STRING, or fused.
                • Set selection and Rescore stay on the Select-sets tab; Bridges / Drivers / Hubs and the
                  graph are on the map stage. Zoom (+/−/Fit/Reflow) is in the explore toolbar.
                • Graph layout animation is off in WebView (same fCoSE spacing; use Fit after layout).
                • Selection does not zoom the camera; green highlight matches the browser CoreMap.

                Workflow
                1. Load mechanistic and/or phenotypic GSEA result folders (or cache picks).
                   One layer builds hubs and a within-layer network; both layers add Bridges and
                   shared drivers.
                2. Preview gene sets; adjust |NES| / NP / FDR; select sets to include.
                3. Choose interactome (default SIGNOR), scoring options, and optional source enrichment.
                4. Integrate → explore Bridges, shared drivers, layer hubs, and the graph.
                5. Rescore reuses the cached interactome when the provider settings are unchanged.

                Source enrichment
                When enabled, CoreMap maps enrichment set IDs to Reactome / KEGG (and GO/HPO when the
                interactome is “none”) and adds those edges before bridge search. Accession patterns work
                without a catalog; optional MSigDB JSON improves ID resolution.

                Bridge search & nulls
                Bridges are short paths between leading-edge genes of mechanistic and phenotypic sets.
                Optional null permutations estimate empirical_p by shuffling labels within degree buckets
                (statistic: path_jaccard).

                Shared drivers & hubs
                Shared drivers are genes in both layers’ leading edges, scored with direction agreement.
                Layer hubs are high-degree genes within one layer’s search neighborhood.

                Parameters (defaults match the CoreMap engine)
                • Min interaction score, max path length, path-length penalty, direction weight
                • Top-K bridges, extra neighbors, organism (Human HSA/9606 or Mouse MMU/10090),
                  SIGNOR level / query type / filters
                • STRING mode (integrated / functional / regulatory / physical)
                • Source enrichment on/off, null permutations

                Limits
                STRING/fused queries are capped for dense association graphs. Search uses a beam width and
                node budget. Layout (fCoSE) affects display only — not ranking.
                """;
    }
}
