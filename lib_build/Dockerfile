FROM genepattern/docker-openjdk_11\:0.1
RUN set -o pipefail && mkdir -p /opt/gsea && \
    wget -O /tmp/GSEA_4.3.2.zip https://data.broadinstitute.org/gsea-msigdb/gsea/software/desktop/4.3/GSEA_4.3.2.zip && \
    unzip -d /opt/gsea /tmp/GSEA_4.3.2.zip && rm -f /tmp/GSEA_4.3.2.zip