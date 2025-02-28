# Local Testing
## Brief
In order to make things easier for users to test, we have supplied a few scripts to assist.

This includes a docker script for bringing up the necessary supporting applications and scripts for populating kafka,
migrating from kafka to an ElasticSearch index and finally the ER application itself.

## Prerequisite
We need to ensure that the relevant jars are built. 

From the top level directory.
```bash
mvn clean install -DskipTests=true
```

## Supporting Applications
This will pull down the relevant images and launch them
- Kafka
- Zookeeper (for Kafka)
- ElasticSearch
- Kibana (for viewing ElasticSearch)
- Kafka Drop (for viewing Kafka)

```bash
docker compose -f ../docker-compose-support.yml up -d
```

## Populate Kafka
This script populates the given kafka topic (defaulting to "canonical" if not provided) with the given json file.

Needs run from the top level directory
```bash
./populate_kafka_topic.sh <JSON_FILE> <KAFKA_TOPIC>
```

## Migrate data from Kafka to ElasticSearch
This script migrates the contents of the given KAFKA_TOPIC (defaulting to "canonical", if not provided) to the given 
ElasticSearch 
Index (default again to "canonical", if not provided)

Needs to be run from top level directory
```bash
./populate_index.sh <KAFKA_TOPIC> <INDEX>
```

Ctrl-C to cancel the script once the data has been loaded.

## Run the ER App

This script will run the ER server. It accepts two (optional) parameters.
- config file
- index to use

If not provided, it will use a sample test config file (see [here](../entity-resolver-elastic/src/test/resources/dynamic_config_sample.yml)) 
and use the default index of "canonical"


Needs to be run from top level directory
```bash
./run_er_server.sh <CONFIG_FILE> <INDEX>
```

## Query the running server
To query the server, provide the details in the file and call it in a similar manner.

```bash
curl -s --location --request PUT 'http://localhost:8081/similarity?maxResults=3' \
--form 'file=@input.json' | jq
```

