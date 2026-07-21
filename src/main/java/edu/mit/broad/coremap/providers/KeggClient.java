/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap.providers;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.mit.broad.coremap.CoreMapTypes.InteractomeEdge;

/** KEGG REST KGML — directed pathway relation edges among seed genes. */
public final class KeggClient {

    private static final Logger klog = LoggerFactory.getLogger(KeggClient.class);
    private static final String KEGG_GET = "https://rest.kegg.jp/get";
    private static final Pattern KEGG_ORG = Pattern.compile("((?:hsa|mmu|rno)\\d+)", Pattern.CASE_INSENSITIVE);

    private KeggClient() {
    }

    public static List<InteractomeEdge> fetchMechanismEdges(List<String> pathwayIds, Set<String> seedGenes) {
        Set<String> seeds = new HashSet<>();
        for (String s : seedGenes) {
            if (s != null) {
                seeds.add(s.toUpperCase(Locale.ROOT));
            }
        }
        List<String> ids = new ArrayList<>();
        for (String raw : pathwayIds) {
            String pid = normalizePathwayId(raw);
            if (pid != null) {
                ids.add(pid);
            }
        }
        ids = ids.stream().distinct().sorted().toList();
        if (ids.isEmpty() || seeds.size() < 2) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, ids.size()));
        try {
            List<Future<List<InteractomeEdge>>> futures = new ArrayList<>();
            for (String pid : ids) {
                futures.add(pool.submit(() -> edgesForPathway(pid, seeds)));
            }
            List<InteractomeEdge> out = new ArrayList<>();
            for (Future<List<InteractomeEdge>> f : futures) {
                try {
                    out.addAll(f.get());
                } catch (Exception ignored) {
                }
            }
            klog.info("KEGG: {} pathways → {} edges", ids.size(), out.size());
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String normalizePathwayId(String pathwayId) {
        if (pathwayId == null) {
            return null;
        }
        String pid = pathwayId.trim();
        if (pid.matches("(?i)^(hsa|mmu|rno)\\d+$")) {
            return pid.toLowerCase(Locale.ROOT);
        }
        Matcher m = KEGG_ORG.matcher(pid);
        return m.find() ? m.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private static List<InteractomeEdge> edgesForPathway(String pid, Set<String> seeds) {
        try {
            String kgml = ProviderHttp.get(KEGG_GET + "/" + pid + "/kgml", 60_000, 1);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(kgml)));
            Map<String, Set<String>> entryGenes = new HashMap<>();
            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                if (!"gene".equals(entry.getAttribute("type"))) {
                    continue;
                }
                String eid = entry.getAttribute("id");
                NodeList graphics = entry.getElementsByTagName("graphics");
                String gname = graphics.getLength() > 0
                        ? ((Element) graphics.item(0)).getAttribute("name") : "";
                Set<String> symbols = symbolsFromEntry(gname);
                symbols.retainAll(seeds);
                if (!symbols.isEmpty()) {
                    entryGenes.put(eid, symbols);
                }
            }
            List<InteractomeEdge> edges = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            NodeList relations = doc.getElementsByTagName("relation");
            for (int i = 0; i < relations.getLength(); i++) {
                Element relation = (Element) relations.item(i);
                Set<String> left = entryGenes.get(relation.getAttribute("entry1"));
                Set<String> right = entryGenes.get(relation.getAttribute("entry2"));
                if (left == null || right == null) {
                    continue;
                }
                List<String> subtypeNames = new ArrayList<>();
                NodeList subtypes = relation.getElementsByTagName("subtype");
                for (int j = 0; j < subtypes.getLength(); j++) {
                    String name = ((Element) subtypes.item(j)).getAttribute("name");
                    if (name != null && !name.isBlank()) {
                        subtypeNames.add(name);
                    }
                }
                String mechanism = subtypeNames.isEmpty()
                        ? (relation.getAttribute("type").isBlank() ? "PPrel" : relation.getAttribute("type"))
                        : String.join(", ", subtypeNames);
                String effect = "regulates";
                int sign = 0;
                String joined = String.join(" ", subtypeNames).toLowerCase(Locale.ROOT);
                if (joined.contains("inhibition") || joined.contains("repression")
                        || joined.contains("dephosphorylation")) {
                    effect = "down-regulates";
                    sign = -1;
                } else if (joined.contains("activation") || joined.contains("expression")
                        || joined.contains("phosphorylation")) {
                    effect = "up-regulates";
                    sign = 1;
                }
                for (String source : left) {
                    for (String target : right) {
                        if (source.equals(target)) {
                            continue;
                        }
                        String key = source + "\t" + target + "\t" + pid;
                        if (!seen.add(key)) {
                            continue;
                        }
                        InteractomeEdge e = new InteractomeEdge(source, target, 0.78);
                        e.directed = true;
                        e.effect = effect;
                        e.mechanism = mechanism;
                        e.evidence = "KEGG:" + pid;
                        e.provider = "kegg";
                        e.pathwayId = pid;
                        e.channels = new LinkedHashMap<>();
                        e.channels.put("sign", (double) sign);
                        edges.add(e);
                    }
                }
            }
            return edges;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static Set<String> symbolsFromEntry(String graphicsName) {
        Set<String> symbols = new HashSet<>();
        for (String chunk : (graphicsName == null ? "" : graphicsName).split(",")) {
            String token = chunk.trim().split("\\s+")[0].trim();
            if (token.endsWith("...")) {
                continue;
            }
            if (token.matches("^[A-Za-z][A-Za-z0-9\\-]*$")) {
                symbols.add(token.toUpperCase(Locale.ROOT));
            }
        }
        return symbols;
    }
}
