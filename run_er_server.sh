#!/usr/bin/env bash

# Simple script for running the ER server with minimal effort.
# It assumes the corresponding dockerfile (docker-compse-support.yml) 
# and as such defaults the Elastic Search values

# Default values
CANONICAL_CONFIG="entity-resolver-elastic/src/test/resources/dynamic_config_sample.yml"
ELASTIC_HOST="localhost"
ELASTIC_INDEX="search"
ELASTIC_PORT="9200"
ELASTIC_SIMILARITY_INDEX="canonical"
JWKS_URL="disabled"
USER_ATTRIBUTES_URL="disabled"

# Check if parameters are provided and override defaults
if [ $# -ge 1 ]; then
  CANONICAL_CONFIG="$1"
fi

if [ $# -ge 2 ]; then
  ELASTIC_SIMILARITY_INDEX="$2"
fi

# Export environment variables
export CANONICAL_CONFIG
export ELASTIC_HOST
export ELASTIC_INDEX
export ELASTIC_PORT
export ELASTIC_SIMILARITY_INDEX
export JWKS_URL
export USER_ATTRIBUTES_URL

# Execute the script
./entity-resolver-api-server/entity-resolver-api-server.sh