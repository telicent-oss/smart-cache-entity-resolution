#!/bin/bash
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


# A simple script for populating a Kafka topic (provided) with a JSNO file (provided)

# Check if jq and kafka-console-producer are installed
if ! command -v jq &> /dev/null; then
  echo "jq could not be found. Please install jq."
  exit 1
fi

if ! command -v kafka-console-producer &> /dev/null; then
  echo "kafka-console-producer could not be found. Please install Kafka and ensure it's in your PATH."
  exit 1
fi

# Set default values
JSON_FILE=
KAFKA_TOPIC="canonical"
KAFKA_BROKER="docker.for.mac.localhost:9092"

# Check if parameters are provided and override defaults
if [ $# -ge 1 ]; then
  JSON_FILE="$1"
fi

if [ $# -ge 2 ]; then
  KAFKA_TOPIC="$2"
fi

# Check if JSON_FILE is provided
if [ -z "$JSON_FILE" ]; then
  echo "Error JSON file is required."
  exit 1
fi

# Execute the command
jq -rc . "$JSON_FILE" | kafka-console-producer --broker-list "$KAFKA_BROKER" --topic "$KAFKA_TOPIC"

# Check the exit status of the pipeline
if [ $? -ne 0 ]; then
  echo "Error executing the command."
  exit 1
fi

echo "Data from $JSON_FILE successfully sent to Kafka topic $KAFKA_TOPIC."
