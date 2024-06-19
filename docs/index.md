# Further Documentation

## Entity Resolution
### The Problem
When dealing with data that comes from a variety of differing sources who each have differing levels of fidelity, 
how can you be sure that two pieces of information are relating to the same thing?

### The solution
By establishing a baseline canonical form, we can weight fields based on their importance and _uniqueness_ and then 
attempt to identify similar entities based upon comparable scoring. 

#### Canonical forms
As the name suggests, this is a simple, clear representation of a real world entity. 

The simplest example would be a person. They'll have a first name, a surname, perhaps a middle name or names. None 
of which is particularly unique. A passport or National Insurance number is perfect as it's unique but unlikely to 
crop up. Superfluous details like a mobile number or a post-code can help narrow things down but is no guarantee.

#### Scoring
With each of the fields on the Canonical form, thought should be given to how useful it is at identifying unique 
entities, both in isolation and then in combination with other fields.

So in the person above we could say:
* firstname - low
* surname - low
* postcode - low
* NI - high
* Passport No - high
* mobile no - medium
* bank a/c - high
* bank sort-code - low

Ultimately the scoring will be in terms of percentage, i.e. 0.0 - 1.0 (=100%).

So two John Smith's could be the same person but equally likely different so might score lowly. Two John Smith's 
sharing a postcode is possible but has a greater likelihood and so on. Two John Smith's with the same postcode and 
same phone number is near impossible and so would be considered high.

## Telicent Smart Caches

All Telicent Smart Caches read data from Kafka that is then processed and indexed for later querying. 

For Entity Resolution, that data is in the form of canonical entities that is stored in either a generic canonical 
index or a customised index, depending on configuration. 

This repository contains supporting APIs for the operations, as well as a concrete
[ElasticSearch][1] backed implementation.  It provides a REST API that allows applications to query the 
similarity indices that have been created.

This cache is built on top of the [Smart Caches Core Libraries][2] which provides common building blocks for 
creating an indexing pipeline.

# What does it do?

An Entity Resolution Smart Cache is intended to provide similarity searching capabilities for given canonical forms. 
By searching for given entity, a data analyst can search an existing data set to find a potentially matching entity. 

The Entity Resolution Smart Cache is exposed to end-users via the similarity endpoint. A user, or more likely 
application, enters a document to the REST API, via the [Entity Resolution API](er-api) to an underlying search index e.g.
[ElasticSearch][1], or [Open Search][3], with its results converted into our own [similarity results data model](similarity-results.md).


## The E.R. API

The `entity-resolution-api` module defines a common API for interacting with search indexes. The intent of this API 
is to abstract away from the detail of the underlying search index so that we can drop in alternative search indices in future
e.g. [Apache Solr](https://solr.apache.org). See the [Entity Resolution API](er-api) documentation for details on this.

It also defines the similarity results data model that is used by this API, and the REST API that this Smart Cache 
exposes. Please see the [Similarity Results](similarity-results.md) documentation for details on the data model.  The REST 
API is defined using Open API Specification and can be found in the [`entity-resolution-api.yaml`](../entity-resolution-api.yaml) file, the [REST 
API](rest-api.md) documentation describes this more informally.

### ElasticSearch Implementation

The `search-index-elastic` module contains an ElasticSearch implementation of the Search API, see
the [ElasticSearch API Implementation](elastic-impl.md) for more details.


## Future work

### Improved configuration model and scoring
The work on Entity Resolution is certainly not finished. In order to provide a more tailored matching process we have 
outlined improvements to make going forward [here](further-work.md).

[1]: https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html
[2]: https://github.com/Telicent-io/smart-caches-core
[3]: https://opensearch.org/



