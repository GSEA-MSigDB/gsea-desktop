/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;

import org.apache.ecs.html.Div;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.coremap.CoreMapTypes.Bridge;
import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.api.ToolReport;
import xtools.gsea.CoreMapTool;

/**
 * Persist / reload CoreMap jobs as first-class GSEA report folders.
 */
public final class CoreMapJobStore {

    private static final Logger klog = LoggerFactory.getLogger(CoreMapJobStore.class);

    private CoreMapJobStore() {
    }

    /**
     * Write a CoreMap report under {@code outDir} with label {@code rptLabel}, register in history.
     *
     * @return the report directory
     */
    public static File save(CoreMapJob job, String rptLabel, File outDir, byte[] pngBytes) throws Exception {
        if (job == null || job.result == null) {
            throw new IllegalArgumentException("CoreMap job requires an integration result");
        }
        if (rptLabel == null || rptLabel.isBlank()) {
            rptLabel = "CoreMap";
        }
        rptLabel = rptLabel.trim().replace(' ', '_');
        if (outDir == null) {
            throw new IllegalArgumentException("Output directory is required");
        }
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("Could not create output dir: " + outDir);
        }

        job.formatVersion = CoreMapJob.FORMAT_VERSION;
        job.savedAt = Instant.now().toString();
        job.resultFile = CoreMapJob.RESULT_FILE;

        Properties props = new Properties();
        props.setProperty("rpt_label", rptLabel);
        props.setProperty("out", outDir.getAbsolutePath());
        props.setProperty("gui", "false");

        CoreMapTool tool = new CoreMapTool(props);
        tool.startJobReport(new ReportIndexState(true, "CoreMap job"));
        ToolReport report = (ToolReport) tool.getReport();
        File reportDir = report.getReportDir();

        String resultJson = CoreMapJson.integrationResultJson(job.result);
        report.savePage("coremap-result", "CoreMap integration result JSON", resultJson, "json", false, true);

        String jobJson = CoreMapJson.jobJson(job);
        report.savePage("coremap-job", "CoreMap reloadable job store", jobJson, "json", false, true);

        String bridgesTsv = formatBridgesTsv(job.result);
        if (!bridgesTsv.isBlank()) {
            report.savePage("coremap-bridges", "CoreMap bridges TSV", bridgesTsv, "tsv", false, true);
        }

        if (pngBytes != null && pngBytes.length > 0) {
            File png = report.createFile(CoreMapJob.PNG_FILE, "CoreMap graph PNG");
            Files.write(png.toPath(), pngBytes);
        }

        if (report.getIndexPage() != null) {
            Div div = new Div();
            div.addElement(summaryHtml(job));
            report.getIndexPage().addBlock(div, false);
        }

        tool.finishReport();
        klog.info("Saved CoreMap job to {}", reportDir.getAbsolutePath());
        return reportDir;
    }

    public static CoreMapJob load(File jobDirOrFile) throws Exception {
        File jobFile = resolveJobFile(jobDirOrFile);
        File jobDir = jobFile.getParentFile();
        String jobText = Files.readString(jobFile.toPath(), StandardCharsets.UTF_8);
        CoreMapJob job = CoreMapJson.parseJob(jobText);
        if (job.result == null) {
            File resultFile = new File(jobDir, job.resultFile != null ? job.resultFile : CoreMapJob.RESULT_FILE);
            if (!resultFile.isFile()) {
                throw new IllegalArgumentException("Missing CoreMap result file: " + resultFile.getAbsolutePath());
            }
            job.result = CoreMapJson.parseIntegrationResult(
                    Files.readString(resultFile.toPath(), StandardCharsets.UTF_8));
        }
        return job;
    }

    public static boolean looksLikeJobDir(File dir) {
        return dir != null && dir.isDirectory() && new File(dir, CoreMapJob.JOB_FILE).isFile();
    }

    public static File resolveJobFile(File jobDirOrFile) {
        if (jobDirOrFile == null) {
            throw new IllegalArgumentException("Job path is null");
        }
        if (jobDirOrFile.isFile()) {
            return jobDirOrFile;
        }
        File job = new File(jobDirOrFile, CoreMapJob.JOB_FILE);
        if (!job.isFile()) {
            throw new IllegalArgumentException("Not a CoreMap job folder (missing " + CoreMapJob.JOB_FILE + "): "
                    + jobDirOrFile.getAbsolutePath());
        }
        return job;
    }

    public static byte[] decodePngDataUri(String dataUri) {
        if (dataUri == null || !dataUri.contains(",")) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Format bridges as TSV (header + rows), or empty string if none. */
    public static String formatBridgesTsv(IntegrationResult result) {
        if (result == null || result.bridges == null || result.bridges.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("bridge_score\tmechanistic_set\tphenotypic_set\thops\tempirical_p\tpath\n");
        for (Bridge b : result.bridges) {
            sb.append(b.bridgeScore).append('\t')
                    .append(tsv(b.mechanisticSetName != null ? b.mechanisticSetName : b.mechanisticSet)).append('\t')
                    .append(tsv(b.phenotypicSetName != null ? b.phenotypicSetName : b.phenotypicSet)).append('\t')
                    .append(b.length).append('\t')
                    .append(b.empiricalP != null ? b.empiricalP : "").append('\t')
                    .append(tsv(b.nodes != null ? String.join(" → ", b.nodes) : ""))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String tsv(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ');
    }

    private static String summaryHtml(CoreMapJob job) {
        IntegrationResult r = job.result;
        int bridges = r.bridges != null ? r.bridges.size() : 0;
        int drivers = r.sharedDrivers != null ? r.sharedDrivers.size() : 0;
        int hubs = r.layerHubs != null ? r.layerHubs.size() : 0;
        Object nodes = r.stats != null ? r.stats.get("nodes") : null;
        Object edges = r.stats != null ? r.stats.get("edges") : null;
        String src = job.options != null && job.options.interactomeSource != null
                ? job.options.interactomeSource.wire() : "";
        return "<div><h4>CoreMap job</h4>"
                + "<p>Saved: " + esc(job.savedAt) + "</p>"
                + "<p>Interactome: " + esc(src) + "</p>"
                + "<p>Nodes: " + esc(String.valueOf(nodes))
                + ", edges: " + esc(String.valueOf(edges))
                + ", bridges: " + bridges
                + ", shared drivers: " + drivers
                + ", hubs: " + hubs + "</p>"
                + "<p>Open this report in Analysis History and choose <b>Open in CoreMap</b> to reload.</p>"
                + "<p><i>Rescore requires re-Integrate after reload (interactome is not stored).</i></p>"
                + "</div>";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
