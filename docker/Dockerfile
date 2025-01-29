# syntax=docker/dockerfile:1.7

# Application Base Image
FROM telicent/telicent-java21:1.1.2 AS app-base

USER root

# Set up necessary directories and user in a single step
RUN groupadd -r telicent-service && \
    useradd -r -g telicent-service -d /app/ telicent-service && \
    mkdir -p /app/lib /app/agents /opt/telicent/sbom && \
    chown -R telicent-service:telicent-service /app /opt/telicent/sbom

WORKDIR /app
USER telicent-service

# Define argument with a default value to prevent build failures
ARG PROJECT_VERSION
ENV PROJECT_VERSION=${PROJECT_VERSION:-latest}

# Copy common CLI scripts
COPY --chown=telicent-service:telicent-service cli-common.sh cli-entrypoint.sh /app/

# API Server Image
FROM app-base AS smart-cache-entity-resolution-api

# Copy application files
COPY --chown=telicent-service:telicent-service \
    entity-resolver-api-server/target/entity-resolver-api-server-${PROJECT_VERSION}-bom.json /opt/telicent/sbom/
COPY --chown=telicent-service:telicent-service \
    entity-resolver-api-server/target/entity-resolver-api-server-${PROJECT_VERSION}.jar \
    entity-resolver-api-server/lib/ \
    entity-resolver-api-server/agents/ \
    /app/lib/
COPY --chown=telicent-service:telicent-service entity-resolver-api-server/entity-resolver-api-server.sh /app/

# Set environment and entrypoint
ENV CLI_SCRIPT=/app/entity-resolver-api-server.sh
ENTRYPOINT [ "/usr/bin/dumb-init", "-v", "--single-child", "--", "/app/cli-entrypoint.sh" ]

# CLI Base Image
FROM app-base AS cli-base

# Copy CLI application files
COPY --chown=telicent-service:telicent-service \
    cli-canonical-index/target/cli-canonical-index-${PROJECT_VERSION}-bom.json /opt/telicent/sbom/
COPY --chown=telicent-service:telicent-service \
    cli-canonical-index/target/cli-canonical-index-${PROJECT_VERSION}.jar \
    cli-canonical-index/lib/ \
    cli-canonical-index/agents/ \
    /app/lib/

# Canonical Index Pipeline
FROM cli-base AS smart-cache-elastic-can-index

# Copy pipeline script
COPY --chown=telicent-service:telicent-service cli-canonical-index/elastic-can-index.sh /app/

# Set environment and entrypoint
ENV CLI_SCRIPT=/app/elastic-can-index.sh
ENTRYPOINT [ "/usr/bin/dumb-init", "-v", "--single-child", "--", "/app/cli-entrypoint.sh" ]
