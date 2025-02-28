#!/usr/bin/env bash
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

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

export CLASS_NAME="io.telicent.smart.cache.entity.resolver.server.EntityResolutionApiCommand"

PROJECT_VERSION=
if command -v mvn >/dev/null 2>&1; then
  PROJECT_VERSION=$(cd "${SCRIPT_DIR}" && mvn help:evaluate --batch-mode -Dexpression=project.version 2>/dev/null | grep -v "\[")
else
  PROJECT_VERSION=$(grep "<version>" "${SCRIPT_DIR}/pom.xml" 2>/dev/null | head -n 1 | awk -F "[><]" '{print $3}')
fi
if [ -z "${PROJECT_VERSION}" ]; then
  abort "Failed to detect Project Version"
fi

export JAR_NAME="entity-resolver-api-server-${PROJECT_VERSION}.jar"
export OTEL_SERVICE_NAME=entity-resolver-api-server.sh

if [ -f "${SCRIPT_DIR}/cli-common.sh" ]; then
  exec "${SCRIPT_DIR}/cli-common.sh" "${SCRIPT_DIR}" "$@"
elif [ -f "${SCRIPT_DIR}/../cli-common.sh" ]; then
  exec "${SCRIPT_DIR}/../cli-common.sh" "${SCRIPT_DIR}" "$@"
else
  echo "Failed to locate CLI Launcher script" 1>&2
  exit 255
fi
