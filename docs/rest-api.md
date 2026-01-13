# Entity Resolution REST API

The Entity Resolution REST API is formally defined using Open API Specification in [`entity-resolution-api.yaml`](../entity-resolution-api.yaml), this
API allows for applications to query the search index for documents matching a given query.

## Data Model

The REST API utilises the [Similarity Results Data Model](similarity-results.md) for representing search results.  

## Error Reporting

Additionally, we use [RFC 7807 Problem Details for HTTP APIs][1] to communicate when an error is encountered servicing
an API request.  This should provide sufficient information for API callers to meaningfully address the error in many
cases.  A problem detail is a simple JSON structure with a few different fields, some example problem responses are
shown below.

### Bad Request

An invalid parameter, with no more specific detail:

```json
{
  "type": "BadRequestParameter",
  "status": 400,
  "detail": "Query Parameter 'type' received invalid value"
}
```

Where available more specific detail is given:

```json
{
  "type": "InvalidRequestParameters",
  "status": 400,
  "detail": "/documents received Query Parameter 'query' with invalid value: must not be null"
}
```

The level of detail may vary depending on the parameter and the constraints imposed upon it:

```json
{
  "type": "InvalidRequestParameters",
  "status": 400,
  "detail": "/documents received Query Parameter 'limit' with invalid value: must be greater than or equal to -1"
}
```

### Unsupported Query Type

A [Query Type](querying-indices.md#query-types) was used that the underlying search index doesn't support:

```json
{
  "type": "UnsupportedQueryType",
  "status": 400,
  "detail": "Underlying search index does not support queries of type Wildcard"
}
```

### Bad URL used

When requesting a URL that is not part of the API

```json
{
  "type": "NotFound",
  "status": 404,
  "detail": "/foo is not a valid URL in this API"
}
```

### Document Not Found

When requesting a specific document that is not found:

```json
{
  "type": "DocumentNotFound",
  "status": 404,
  "detail": "A document with ID 'foo' was not found"
}
```

## APIs

The REST API Server enables authentication by default.  This means that any request to the server
must include a `Authorization: Bearer <token>` header where `<token>` is a JSON Web Token (JWT) that can be
cryptographically verified by the server.  Alternatively within AWS there must be a `X-Amzn-Oidc-Data: <token>` header
present.  In either case this requires that the server be appropriately configured with the location of the public keys
used to verify the JWTs, or sufficient information to derive that itself.  See [Authentication
Considerations](#authentication-considerations) for more details on configuring this.

When authentication is enabled you may also need the User Attributes service if the data indexed for search has security
labels associated with it.  If this is not configured explicitly then it defaults to a local attributes store where all
users have no attributes, this means requests will return no results if the data is security labelled.  See [User
Attributes Service](#user-attributes-service) later for more details on configuring this.


### `PUT /similarity`

Returns the most similar entities to the ones passed as input in the JSON file. The file is in ndjson format with one line per entity, the content of the JSON must be the same as the structure of the documents in the index. 
The similarity uses a different index from the one used by search; it consists of flat key / values fields without references to other entities (as is the case with the search index). The documents in the similarity index are generated from a different source as the main search one where the entities have a 'canonical' representation. The only requirement for the documents in the similarity index is to have an _id_ field which is displayed in the search results.

The parameters are: 

- `file` - entities for which we want to find similar ones in the index (mandatory).
- `maxResults ` - max number of results returned per source entity (optional, default 1).
- `minScore ` - minimum score for results returned per source entity (optional, default 0).
- `withinInput ` - whether to match documents within the input (optional, default false)
- `override ` - mapping config to use in query (optional, default null)

The endpoint is called like this 
```shell
curl -XPUT localhost:8081/similarity -F file=@./flatEntities.ndjson
```
Or

```shell
curl -X PUT "localhost:8081/similarity" -F file='{"first_name": "Mike", "last_name": "Patton", "id": "4b"}'
```
The output is a JSON document with one entry per entity in the file sent as parameter and for each entry, a list of IDs for the documents considered to be similar as well as the associated score. The scores are expected to range from 0 (totally dissimilar) to 1 (identical).

For instance

```json
{"results":[{"hits":[{"id":"4","score":1.0}],"idsourceEntity":"4b"}]}
```

You can specify the minimum score for a hit with

```bash
curl -X PUT "localhost:8081/similarity?minScore=0.5" -F file='{"first_name": "Dolly", "last_name": "Parton", "id": "dp"}' 
```

You can also specify to override the fields/mapping/weights. Note: You need to supply the complete config, not just the fields you want changed.
```bash
curl --location --request PUT 'http://localhost:8081/similarity' \
--form 'file="{\"name\":\"JFK Airport\",\"location\":\"40.6446, -73.7797\",\"canonicalType\":\"Hostels\"}"' \
--form 'overrides="fields:
- name: city
type: text
required: true
boost: 1.0
- name: country
type: keyword
required: true
boost: 1.0
"'
```

No action is done based on whether a hit has been found for an entity, this is left to the client code.

### `PUT /similarity/v2`

This variant uses a configured model (relations + scores) to re-rank candidate matches. It requires the `modelId`
query parameter, which must refer to a model created via `/config/models/{model_id}`.

```bash
curl -X PUT "http://localhost:8081/similarity/v2?modelId=people-v2-demo" \
  -F file='{"first_name": "Miles", "last_name": "Davis", "id": "input-1"}'
```

Note: v1 similarity uses fuzzy matches on all input fields, which will fail if the underlying index field is not text
or keyword (e.g. `date`). Use v2 with a model that only references compatible fields in that case.

### Configuration APIs

Configuration endpoints are available under `/config` and accept JSON request bodies. Each create/update takes a
complete configuration document.

Example walkthrough script (creates config, loads a small demo dataset into ElasticSearch, and runs similarity v1/v2 to
show v1 preferring a near-match driven by extra fields while v2 selects the model-weighted name match):

```bash
ELASTIC_ADDRESS=localhost:9200 MODEL_INDEX=canonical \
  ./docs/demo-config-similarity.sh
```

For `/config/relations`, `/config/scores`, and `/config/models`, the JSON body must include an `id` (or legacy alias)
that matches the `{..._id}` path parameter. `full-models` accepts arbitrary JSON; extra fields are ignored.

If the body `id` is missing or does not match the path parameter, the API returns `400` with an
`InvalidRequestParameters` problem.

Example: create a relation

```bash
curl -X POST "http://localhost:8081/config/relations/name-only" \
  -H "Content-Type: application/json" \
  -d '{"id":"name-only","fields":["first-name","surname"],"weight":1.0}'
```

Example: create scores

```bash
curl -X POST "http://localhost:8081/config/scores/people-scores-v2-demo" \
  -H "Content-Type: application/json" \
  -d '{"id":"people-scores-v2-demo","fieldScores":{"first-name":0.3,"surname":0.3,"dob":0.8,"passport-number":0.9}}'
```

Example: create a model (used by `/similarity/v2`)

```bash
curl -X POST "http://localhost:8081/config/models/people-v2-demo" \
  -H "Content-Type: application/json" \
  -d '{"id":"people-v2-demo","index":"people","relations":["name-only","name-dob-passport"],"scores":"people-scores-v2-demo"}'
```

Example: create a full model (store full relations/scores inline)

```bash
curl -X POST "http://localhost:8081/config/full-models/people-v2-demo" \
  -H "Content-Type: application/json" \
  -d '{"id":"people-v2-demo","index":"people","relations":[{"id":"name-only","fields":["first-name","surname"],"weight":1.0},{"id":"name-dob-passport","fields":["first-name","surname","dob","passport-number"],"weight":10.0}],"scores":{"id":"people-scores-v2-demo","fieldScores":{"first-name":0.3,"surname":0.3,"dob":0.8,"passport-number":0.9}}}'
```

# Running the API Server

In order to run the API server you need to first export some environment variables, so it knows how to connect to
ElasticSearch, the following variables are required:

- `ELASTIC_HOST` - Sets the hostname of the ElasticSearch server to connect to.
- `ELASTIC_INDEX` - Sets the index of the ElasticSearch server that will be used for searches.
- `JWKS_URL` - Sets the URL for a JSON Web Key Set (JWKS) that can be used to verify the presented authentication
  tokens, see [Authentication Considerations](#authentication-considerations) for more detail.
- `USER_ATTRIBUTES_URL` - Sets the URL for the User Attributes service that provides the ability to lookup user
  attributes for authenticated users, see [User Attributes Service](#user-attributes-service) for more detail.

And optionally the following:

- `ELASTIC_PORT` - Sets the port of the ElasticSearch server to connect to. Defaults to 9200 if not set.
- `OPENSEARCH_COMPATIBILITY` - When set to `true` the server runs in OpenSearch compatibility which means it
  reconfigures the Elastic client code to achieve "compatibility" with OpenSearch servers.  This may not work with all
  versions of OpenSearch but should allow at 1.x to be used.

In all the following methods of running the server it will be available on `http://localhost:8081`

## Uploading Sample Data

If you don't have a pre-built index with some data in it to be queried then you can upload some sample data using the
[`upload-sample-data.sh`](../upload-sample-data.sh) script.  This defaults to uploading to ElasticSearch running on
`localhost:9200` the dataset [`boxers_small.json`](../test_data/boxers_small.json) into an index named `canonical`.  You can 
supply custom
values for this as arguments e.g.

```bash
$ ./upload-sample-data.sh localhost:4567 test_data/palaces.json my-index
```

Would upload the [`palaces.json`](../test_data/palaces.json) data to an ElasticSearch index `my-index` on an ElasticSearch
instance running on `localhost:4567`

If you are running on a non-Linux system you can also manually upload the relevant sample dataset using a suitable tool
e.g. Postman.  The file **MUST** be uploaded via an HTTP `POST` request to
`http://<elastic-host>:<elastic-port>/<index>/_bulk` (substituting suitable values for your environment) with
`Content-Type: application/json`.

The following sample datasets are currently available in this repository:

- [`boxers_small.json`](../test_data/boxers_small.json) - A sample dataset describing boxers physical characteristics 
  and fighting record. 
  Contains 14 documents.
- [`palaces.json`](../test_data/palaces.json) - A sample dataset describing the location of palaces around the United Kingdom. 
  Contains 14 documents.

## Running via the CLI

You can run via the `entity-resolver-api-server` script found in the `entity-resolver-api-server` module i.e.

```bash
$ ./entity-resolver-api-server/entity-resolver-api-server 
```

You can see the available options to customise the API servers behaviour by using the `--help` option i.e.

```bash
$ ./entity-resolver-api-server/entity-resolver-api-server --help
NAME
        entity-resolver-api-server - Runs the Entity Resolver API Rest Server

SYNOPSIS
        entity-resolver-api-server [ --base-path <BasePath> ] [ {-h | --help} ]
                [ {--host | --hostname} <hostname> ]
                [ {--live-bootstrap-server | --live-bootstrap-servers} <LiveBootstrapServers> ]
                [ --live-error-topic <LiveErrorTopic> ]
                [ {--live-report-interval | --live-reporter-interval} <LiveReportInterval> ]
                [ {--live-reporter | --no-live-reporter} ]
                [ --live-reporter-topic <LiveTopic> ] [ --localhost ]
                [ {-p | --port} <Port> ] [ --quiet ]
                [ {--runtime-info | --no-runtime-info} ] [ --trace ]
                [ --verbose ]
...
```

So for example to run the server on an alternative port and servicing requests under `/api/similarity` you could do the
following:

```bash
$ ./entity-resolver-api-server/entity-resolver-api-server --base-path "/api/similarity" --port 12345
```

## Running via Maven Exec plugin

You can alternatively run the API server via the Maven `exec` plugin like so:

```bash
$ mvn exec:java -pl :entity-resolver-api-server
```

This runs using the lightweight Jersey Grizzly2 HTTP server that services requires until you Ctrl+C it.

## Running via Docker

If you prefer you can run via Docker provided you have first done the [Docker
Build](../README.md#build) e.g.

```bash
$ docker run -it -e ELASTIC_HOST=<elastic-hostname> -e ELASTIC_PORT=<elastic-port> \
   -e ELASTIC_INDEX=<elastic-index> -e JWKS_URL=development -p 8081:8080 entity-resolver-api-server:<tag>
```

Where the relevant environment variables are suitably set for your environment, see [Networking
Considerations](#networking-considerations) and [Authentication Considerations](#authentication-considerations) below
for some discussion on this.  The `<tag>` reflects the tag used when you built the Docker images.  This will normally be
your git branch unless you've explicitly supplied a different one, for example if you've currently got `main` checked
out then your image tag would be `main`.

This runs a Docker container that uses the lightweight Jersey Grizzly2 web server to run the WAR file for the Search 
API. This will continue to service requests until you `Ctrl+C`, or otherwise stop, the spawned container.

The Docker image using the [entity-resolver-api-server](#running-via-the-cli) script internally, so you can also pass additional
arguments to your `docker run` invocation to pass those through e.g.

```bash
$ docker run -it -e ELASTIC_HOST=<elastic-hostname> -e ELASTIC_PORT=<elastic-port> \
   -e ELASTIC_INDEX=<elastic-index> -e JWKS_URL=development -p 8081:8080 entity-resolver-api-server:<tag> \
   --base-path "/api/similarity"
```
Runs the server under Docker and accepting requests under `/api/similarity`.

### Networking Considerations

One consideration when running as a Docker container is that the pipeline needs to talk to ElasticSearch.   Due to
running in a container the hostnames and ports you use for these locally in your development environment may not be
usable from within the container.

For example a hostname of `localhost` for ElasticSearch isn't going to work inside the container. The actual hostname to
use here will depend on your container runtime and OS. For example using [Docker Desktop for Mac][2] you can use
`host.docker.internal` instead.

In a fully containerised deployment ElasticSearch would itself be running in other containers, in some kind of shared
network, such that there are hostnames that allow the containers to communicate between themselves. Refer to the
documentation for your deployment for more details on that.

### Authentication Considerations

From 0.5.0 the server defaults to running with authentication enabled, this requires that all requests to the server
have either an `Authorization: Bearer <token>` or an `X-Amzn-OIDC-Data: <token>` header present where `<token>` is a
cryptographically verifiable JSON Web Token (JWT). Verifying the JWTs requires a JSON Web Key Set (JWKS) URL from which
the public keys of the token issuer can be obtained.

The `JWKS_URL` environment variable is used to configure the location of the public keys at server startup.  This may
either be a remote URL or a local file, for a local file the URL must be of the form `file:///path/to/jwks.json`.
Alternatively it may be one of several special values:

- `disabled` - Disables authentication entirely.
- `development` - Enables development mode authentication.  In this mode the presented `<token>` does not need to be a
  JWT and instead is just a Base 64 (URL safe) encoded username.  This allows developers to use the server and present
  arbitrary usernames to the server without needing an external authentication service to issue those tokens.
- `aws:<region>` - Enables AWS authentication where `<region>` is the AWS region in which the server is being deployed.
  This will configure authentication to use the custom AWS headers and resolve public keys from AWS ELB.  When this mode
  is specified then the token **MUST** be present in the `X-Amzn-OIDC-Data` header.

**NB:** `disabled` and `development` **MUST** only be used for local development and testing.

### User Attributes Service

The User Attributes Service is part of Telicent CORE and provides the ability to look up the attributes for an
authenticated user, these are then used to evaluate whether the user can see different documents (or parts thereof)
based on the security labels present on the documents.  [Telicent
Access](https://github.com/Telicent-io/telicent-access) is the default implementation of this service for CORE.

To use a User Attributes service you must set the `USER_ATTRIBUTES_URL` environment variable to the base URL of the
attributes service.  For example if you had deployed Access entirely locally it would be 
`https://localhost:8091/users/lookup/{user}`.  Note that your URL must include `{user}` as the placeholder where the
username should be substituted in order to derive a URL that looks up the users attributes.

Alternatively it may be one of several special values:

- `disabled` - Disables user attributes.
- `development` - Creates a fake local attributes store with a limited set of hardcoded user to attribute mappings, see
  UserAttributesInitializer in Smart Cache Core for the details.  

**NB:** `disabled` and `development` **MUST** only be used for local development and testing.

[1]: https://datatracker.ietf.org/doc/html/rfc7807
[2]: https://docs.docker.com/desktop/mac/networking/#use-cases-and-workarounds
