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

#
# Utility script for uploading sample canonical data to a specified index.
# The expected data format can be seen in the boxers_small.json and palaces.jon sample files.

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

LOG_FILE=${SCRIPT_DIR}/elastic-log.txt

ELASTIC_ADDRESS=${1:-localhost:9200}
SAMPLE_DATA=${2:-test_data/boxers_small.json}
INDEX=${3:-canonical}

function abort() {
  echo "ERROR: $*"
  exit 1
}
command -v curl >/dev/null 2>&1 || abort "Required curl command not present"

echo "Uploading ${SAMPLE_DATA} to ElasticSearch index ${INDEX} on ${ELASTIC_ADDRESS}..."
if [ ! -f "${SAMPLE_DATA}" ]; then
  if [ -f "${SCRIPT_DIR}/${SAMPLE_DATA}" ]; then
    SAMPLE_DATA="${SCRIPT_DIR}/${SAMPLE_DATA}"
  else
    abort "Sample data ${SAMPLE_DATA} was not found"
  fi
fi

curl http://"${ELASTIC_ADDRESS}"/"${INDEX}"/_bulk -H "Content-Type: application/json" --data-binary @"${SAMPLE_DATA}" > "${LOG_FILE}" \
  || abort "Failed to upload example data to ElasticSearch index ${INDEX} on ${ELASTIC_ADDRESS} (see ${LOG_FILE} for details)"

rm -f "${LOG_FILE}" >/dev/null 2>&1

echo "Successfully uploaded example data ${SAMPLE_DATA}!"
exit 0


