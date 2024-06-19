# Telicent Smart Cache - Entity Resolution

Entity resolution, also known as record linkage, deduplication, or similarity matching, is a crucial step in managing and analyzing data. 
It involves figuring out if different records in a dataset are talking about the same real-world things, like people, organizations, addresses, or devices. 
The ultimate goal is to find and combine duplicate or related information, making the data more accurate and reliable.

This repository provides the infrastructure for an Entity Resolution Smart Cache. 
There is currently a single concrete implementation backed by ElasticSearch.

It also includes an API Server that exposes the Entity Resolution Smart Cache to consumers via a REST API defined 
using the OpenAPI specification ([here](entity-resolution-api.yaml)).

There is also a command line tool for loading data individually or as part of a pipeline from a variety of sources 
that can be indexed. 

You can find more detail on the APIs and the Entity Resolution Smart Cache in the [documentation](docs/index.md) 
section within this repository.


## Build

For developer build instructions see [BUILD.md](BUILD.md).

## Usage

For running the Canonical Index Command line tool refer to [Running the Indexing Pipeline](docs/running-pipeline.md#running-the-pipeline).

For running the Entity Resolution API REST Server please refer to [Running the API Server](docs/rest-api.md#running-the-api-server).

## Further Documentation
You can find detailed documentation for this repository in the [docs/](docs) subdirectory.

If you are interested in learning more about Entity Resolution, the canonical forms used to 
represent real-world entities, how the data is configured, the underlying and external APIs, the pipelines, or the 
format of the results returned by the server, then specific links are below:

- [Entity Resolution Overview](docs/index.md)
- [Entity Resolution API](docs/er-api.md)
- [Similarity Results Data Model](docs/similarity-results.md)
- [Entity Resolution REST API](docs/rest-api.md) 
- [Canonical Types](docs/canonical-types.md) 
