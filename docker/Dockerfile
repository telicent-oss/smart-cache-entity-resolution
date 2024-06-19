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
FROM eclipse-temurin:21-jre-jammy  AS app-base

RUN apt-get update && \
    apt-get install -y --no-install-recommends tini && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /app/ /app/lib/ /app/agents/ /opt/telicent/sbom/

ARG PROJECT_VERSION
COPY cli-common.sh cli-entrypoint.sh /app/

ENV PROJECT_VERSION=${PROJECT_VERSION} \
    TINI_SUBREAPER=true

# API Server Image
FROM app-base AS smart-cache-entity-resolution-api

ARG PROJECT_VERSION
COPY entity-resolver-api-server/target/entity-resolver-api-server-${PROJECT_VERSION}-bom.json /opt/telicent/sbom
COPY entity-resolver-api-server/target/entity-resolver-api-server-${PROJECT_VERSION}.jar \
     entity-resolver-api-server/lib/* \
     /app/lib/
COPY entity-resolver-api-server/agents/* /app/agents/
COPY entity-resolver-api-server/entity-resolver-api-server.sh /app/

ENV CLI_SCRIPT=/app/entity-resolver-api-server.sh
ENTRYPOINT [ "/usr/bin/tini", "--", "/app/cli-entrypoint.sh" ]

# CLI Base Image
FROM app-base AS cli-base

ARG PROJECT_VERSION
COPY cli-canonical-index/target/cli-canonical-index-${PROJECT_VERSION}-bom.json /opt/telicent/sbom
COPY cli-canonical-index/target/cli-canonical-index-${PROJECT_VERSION}.jar \
     cli-canonical-index/lib/* \
     /app/lib/
COPY cli-canonical-index/agents/* /app/agents/

# Canonical Index Pipeline
FROM cli-base AS smart-cache-elastic-can-index

COPY cli-canonical-index/elastic-can-index.sh /app/

ENV CLI_SCRIPT=/app/elastic-can-index.sh
ENTRYPOINT [ "/usr/bin/tini", "--", "/app/cli-entrypoint.sh" ]
