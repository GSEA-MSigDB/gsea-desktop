FROM eclipse-temurin:17
ARG release_number=4.3.2
ARG release_series=4.3
RUN mkdir -p /opt/gsea && \
    curl -o /tmp/GSEA_${release_number}.zip https://data.broadinstitute.org/gsea-msigdb/gsea/software/${release_series}/GSEA_${release_number}.zip && \
    cd /opt && jar xf /tmp/GSEA_${release_number}.zip && mv GSEA_${release_number} gsea && rm -f /tmp/GSEA_${release_number}.zip && \
    chmod u+x /opt/gsea/gsea-cli.sh