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


# A simple script for populating an Elastic Search Index (provided) from a Kafka topic (provided).

# Default values
ELASTIC_HOST="localhost"
ELASTIC_INDEX="search"
ELASTIC_PORT="9200"
ELASTIC_SIMILARITY_INDEX="canonical"
JWKS_URL="disabled"
KAFKA_BROKER="docker.for.mac.localhost:9092"
KAFKA_TOPIC="canonical"
USER_ATTRIBUTES_URL="disabled"

# Check if parameters are provided and override defaults
if [ $# -ge 1 ]; then
  KAFKA_TOPIC="$1"
fi

if [ $# -ge 2 ]; then
  ELASTIC_SIMILARITY_INDEX="$2"
fi

# Export environment variables
export ELASTIC_HOST
export ELASTIC_INDEX
export ELASTIC_PORT
export ELASTIC_SIMILARITY_INDEX
export JWKS_URL
export USER_ATTRIBUTES_URL
export KAFKA_BROKER
export KAFKA_TOPIC


./cli-canonical-index/elastic-can-index.sh --bootstrap-server "$KAFKA_BROKER" \
  --topic "$KAFKA_TOPIC" --elastic-host="$ELASTIC_HOST" \
  --elastic-port="$ELASTIC_PORT" --index="$ELASTIC_SIMILARITY_INDEX"
