#
#   Copyright (c) Telicent Ltd.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

# syntax=docker/dockerfile:1.7

# Application Base Image
FROM telicent/telicent-java21:1.1.5 AS app-base

USER root

# Set up necessary directories and user in a single step
RUN mkdir -p /app/lib /app/agents /opt/telicent/sbom && \
    chown -R user:user /app /opt/telicent/sbom

WORKDIR /app
USER user

# Define argument with a default value to prevent build failures
ARG PROJECT_VERSION
ENV PROJECT_VERSION=${PROJECT_VERSION:-latest}

# Copy common CLI scripts
COPY --chown=user:user cli-common.sh cli-entrypoint.sh /app/

# API Server Image
FROM app-base AS smart-cache-entity-resolution-api

# Copy application files
COPY --chown=user:user \
    entity-resolver-api-server/target/entity-resolver-api-server-${PROJECT_VERSION}-bom.json /opt/telicent/sbom/
COPY --chown=user:user \
    entity-resolver-api-server/target/entity-resolver-api-server-${PROJECT_VERSION}.jar \
    entity-resolver-api-server/lib/ \
    entity-resolver-api-server/agents/ \
    /app/lib/
COPY --chown=user:user entity-resolver-api-server/entity-resolver-api-server.sh /app/

# Set environment and entrypoint
ENV CLI_SCRIPT=/app/entity-resolver-api-server.sh
ENTRYPOINT [ "/usr/bin/dumb-init", "-v", "--single-child", "--", "/app/cli-entrypoint.sh" ]

# CLI Base Image
FROM app-base AS cli-base

# Copy CLI application files
COPY --chown=user:user \
    cli-canonical-index/target/cli-canonical-index-${PROJECT_VERSION}-bom.json /opt/telicent/sbom/
COPY --chown=user:user \
    cli-canonical-index/target/cli-canonical-index-${PROJECT_VERSION}.jar \
    cli-canonical-index/lib/ \
    cli-canonical-index/agents/ \
    /app/lib/

# Canonical Index Pipeline
FROM cli-base AS smart-cache-elastic-can-index

# Copy pipeline script
COPY --chown=user:user cli-canonical-index/elastic-can-index.sh /app/

# Set environment and entrypoint
ENV CLI_SCRIPT=/app/elastic-can-index.sh
ENTRYPOINT [ "/usr/bin/dumb-init", "-v", "--single-child", "--", "/app/cli-entrypoint.sh" ]
