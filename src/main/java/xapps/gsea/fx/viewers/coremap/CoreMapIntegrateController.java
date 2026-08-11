/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.mit.broad.coremap.CoreMapOrganism;
import edu.mit.broad.coremap.CoreMapPipeline;
import edu.mit.broad.coremap.CoreMapPipeline.IntegrateRequest;
import edu.mit.broad.coremap.CoreMapTypes.BuildProgress;
import edu.mit.broad.coremap.CoreMapTypes.EnrichmentParseResult;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;
import edu.mit.broad.coremap.IntegrationOptions;
import edu.mit.broad.xbench.core.api.WindowManager;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns CoreMap integrate / rescore background work so {@link FxCoreMapPane} can stay a layout shell.
 */
final class CoreMapIntegrateController {

    private static final Logger klog = LoggerFactory.getLogger(CoreMapIntegrateController.class);

    public interface Host {
        EnrichmentParseResult mechanistic();
        EnrichmentParseResult phenotypic();
        Set<String> mechanisticSetIds();
        Set<String> phenotypicSetIds();
        Double minNes();
        Double maxNp();
        Double maxFdr();
        boolean separateLayerThresholds();
        Double phenoMinNes();
        Double phenoMaxNp();
        Double phenoMaxFdr();
        String sessionId();
        AtomicBoolean cancelFlag();
        WindowManager dialogs();
        void setBusy(boolean busy, String message);
        void showExploreStage();
        void showSetupStage();
        void onProgress(BuildProgress progress);
        void applyResult(IntegrationResult result);
        boolean liveApiConfirmed();
        void setLiveApiConfirmed(boolean confirmed);
        boolean canRescoreWithCurrentOptions();
        IntegrationResult lastResult();
        void disableExploreIfEmpty();
        IntegrationOptionsSnapshot optionsSnapshot();
    }

    /** Snapshot of option widgets used to build {@link IntegrationOptions}. */
    public record IntegrationOptionsSnapshot(
            String interactomeSource,
            String stringMode,
            Double minInteractionScore,
            Integer maxPathLength,
            Double pathLengthPenalty,
            Double directionWeight,
            Double sharedDriverDirectionWeight,
            Integer topKBridges,
            boolean includeExtraNeighbors,
            Integer neighborLimit,
            Integer signorLevel,
            String signorOrganism,
            String signorQueryType,
            boolean signorProteinOnly,
            boolean signorDirectOnly,
            String signorPathways,
            boolean enableSourceEnrichment,
            String msigdbPath,
            Integer nullPermutations) {
    }

    private final Host host;

    public CoreMapIntegrateController(Host host) {
        this.host = host;
    }

    public void runIntegrate() {
        if (host.mechanistic() == null && host.phenotypic() == null) {
            host.dialogs().showMessage("Load a GSEA result first.");
            return;
        }
        InteractomeSource src = InteractomeSource.fromWire(host.optionsSnapshot().interactomeSource());
        if (src != InteractomeSource.NONE && !host.liveApiConfirmed()) {
            if (!host.dialogs().showConfirm(CoreMapWorkspace.LIVE_API_CONFIRM)) {
                return;
            }
            host.setLiveApiConfirmed(true);
        }
        host.cancelFlag().set(false);
        host.setBusy(true, "Integrating…");
        host.showExploreStage();
        IntegrateRequest req = buildRequest();
        req.cancelled = host.cancelFlag();
        Thread t = new Thread(() -> {
            try {
                IntegrationResult result = CoreMapPipeline.integrate(req, host::onProgress);
                Platform.runLater(() -> {
                    host.applyResult(result);
                    host.setBusy(false, summarize(result));
                });
            } catch (Throwable ex) {
                boolean cancelled = host.cancelFlag().get()
                        || (ex.getMessage() != null && ex.getMessage().toLowerCase(Locale.ROOT).contains("cancelled"));
                klog.error(cancelled ? "CoreMap integrate cancelled" : "CoreMap integrate failed", ex);
                Platform.runLater(() -> {
                    host.setBusy(false, cancelled ? "Integrate cancelled." : "Integrate failed: " + ex.getMessage());
                    if (host.lastResult() == null) {
                        host.disableExploreIfEmpty();
                        host.showSetupStage();
                    }
                    if (!cancelled) {
                        host.dialogs().showError("CoreMap integrate failed", ex);
                    }
                });
            }
        }, "coremap-integrate");
        t.setDaemon(true);
        t.start();
    }

    public void runRescore() {
        if (host.sessionId() == null) {
            host.dialogs().showMessage("Integrate first.");
            return;
        }
        if (!host.canRescoreWithCurrentOptions()) {
            host.dialogs().showMessage(
                    "Interactome source or fetch threshold changed — click Integrate to rebuild.");
            return;
        }
        host.cancelFlag().set(false);
        host.setBusy(true, "Rescoring…");
        IntegrationOptions opts = buildOptions(host.optionsSnapshot());
        String sid = host.sessionId();
        Thread t = new Thread(() -> {
            try {
                IntegrationResult result = CoreMapPipeline.rescore(sid, opts, host::onProgress, host.cancelFlag());
                Platform.runLater(() -> {
                    host.applyResult(result);
                    host.setBusy(false, "Rescored — " + summarize(result));
                });
            } catch (Throwable ex) {
                boolean cancelled = host.cancelFlag().get()
                        || (ex.getMessage() != null && ex.getMessage().toLowerCase(Locale.ROOT).contains("cancelled"));
                klog.error(cancelled ? "CoreMap rescore cancelled" : "CoreMap rescore failed", ex);
                Platform.runLater(() -> {
                    host.setBusy(false, cancelled ? "Rescore cancelled." : "Rescore failed: " + ex.getMessage());
                    if (!cancelled) {
                        host.dialogs().showError("CoreMap rescore failed", ex);
                    }
                });
            }
        }, "coremap-rescore");
        t.setDaemon(true);
        t.start();
    }

    public IntegrateRequest buildRequest() {
        IntegrateRequest req = new IntegrateRequest();
        req.mechanistic = host.mechanistic();
        req.phenotypic = host.phenotypic();
        req.minNes = host.minNes();
        req.maxNp = host.maxNp();
        req.maxFdr = host.maxFdr();
        if (host.separateLayerThresholds()) {
            req.phenoMinNes = host.phenoMinNes();
            req.phenoMaxNp = host.phenoMaxNp();
            req.phenoMaxFdr = host.phenoMaxFdr();
        }
        req.mechanisticSetIds = host.mechanisticSetIds();
        req.phenotypicSetIds = host.phenotypicSetIds();
        req.options = buildOptions(host.optionsSnapshot());
        req.replaceSessionId = host.sessionId();
        return req;
    }

    public static IntegrationOptions buildOptions(IntegrationOptionsSnapshot s) {
        IntegrationOptions o = IntegrationOptions.defaults();
        o.interactomeSource = InteractomeSource.fromWire(s.interactomeSource());
        o.stringMode = StringMode.fromWire(s.stringMode());
        o.minInteractionScore = s.minInteractionScore();
        o.maxPathLength = s.maxPathLength();
        o.pathLengthPenalty = s.pathLengthPenalty();
        o.directionWeight = s.directionWeight();
        o.sharedDriverDirectionWeight = s.sharedDriverDirectionWeight();
        o.topKBridges = s.topKBridges();
        o.includeExtraNeighbors = s.includeExtraNeighbors();
        o.neighborLimit = s.neighborLimit();
        o.signorLevel = s.signorLevel();
        o.signorOrganism = CoreMapOrganism.fromDisplayLabel(s.signorOrganism());
        o.signorQueryType = s.signorQueryType() != null ? s.signorQueryType() : "connect";
        o.signorProteinOnly = s.signorProteinOnly();
        o.signorDirectOnly = s.signorDirectOnly();
        o.signorPathways = parseCsv(s.signorPathways());
        o.enableSourceEnrichment = s.enableSourceEnrichment();
        String msig = s.msigdbPath();
        o.msigdbPath = msig != null && !msig.isBlank() ? msig.trim() : null;
        o.nullPermutations = s.nullPermutations();
        return o;
    }

    public static List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    public static String summarize(IntegrationResult r) {
        if (r == null) {
            return "";
        }
        java.util.Map<String, Object> stats = r.stats != null ? r.stats : java.util.Map.of();
        String base = String.format(Locale.ROOT, "Nodes %s, edges %s, bridges %d, hubs %d, shared drivers %d",
                stats.getOrDefault("node_count", stats.getOrDefault("nodes", "?")),
                stats.getOrDefault("edge_count", stats.getOrDefault("edges", "?")),
                r.bridges != null ? r.bridges.size() : 0,
                r.layerHubs != null ? r.layerHubs.size() : 0,
                r.sharedDrivers != null ? r.sharedDrivers.size() : 0);
        StringBuilder extra = new StringBuilder();
        Object message = stats.get("message");
        if (message != null && !String.valueOf(message).isBlank()) {
            extra.append(" | ").append(message);
        }
        Object enrichStatus = stats.get("source_enrichment_status");
        Object enrichErr = stats.get("source_enrichment_error");
        if ("error".equals(String.valueOf(enrichStatus)) && enrichErr != null) {
            extra.append(" | Source enrichment warning");
        }
        Object warnings = stats.get("warnings");
        if (warnings instanceof List<?> list && !list.isEmpty()) {
            extra.append(" | ").append(list.size()).append(" warning(s)");
        }
        return base + extra;
    }

    public static IntegrationOptionsSnapshot snapshotFrom(
            ComboBox<String> interactomeSource,
            ComboBox<String> stringMode,
            Spinner<Double> minInteraction,
            Spinner<Integer> maxPathLength,
            Spinner<Double> pathLengthPenalty,
            Spinner<Double> directionWeight,
            Spinner<Double> sharedDriverDirectionWeight,
            Spinner<Integer> topKBridges,
            CheckBox includeNeighbors,
            Spinner<Integer> neighborLimit,
            Spinner<Integer> signorLevel,
            ComboBox<String> signorOrganism,
            ComboBox<String> signorQueryType,
            CheckBox signorProteinOnly,
            CheckBox signorDirectOnly,
            TextField signorPathways,
            CheckBox enableSourceEnrichment,
            TextField msigdbPath,
            Spinner<Integer> nullPermutations) {
        return new IntegrationOptionsSnapshot(
                interactomeSource.getValue(),
                stringMode.getValue(),
                minInteraction.getValue(),
                maxPathLength.getValue(),
                pathLengthPenalty.getValue(),
                directionWeight.getValue(),
                sharedDriverDirectionWeight.getValue(),
                topKBridges.getValue(),
                includeNeighbors.isSelected(),
                neighborLimit.getValue(),
                signorLevel.getValue(),
                signorOrganism.getValue(),
                signorQueryType.getValue(),
                signorProteinOnly.isSelected(),
                signorDirectOnly.isSelected(),
                signorPathways.getText(),
                enableSourceEnrichment.isSelected(),
                msigdbPath.getText(),
                nullPermutations.getValue());
    }
}
