#!/usr/bin/env bash

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