# ElasticSearch Indexing Pipeline

The high level design sketch of the pipeline is as follows:

![ElasticSearch Indexing Pipeline Diagram](images/elasticsearch-indexing-pipeline.jpeg)

In practise there are actually additional sinks in the pipeline used to report throughput metrics, filter out
uninteresting documents and suppress duplicate documents. The full pipeline consists of the following steps:

1. [Read from an [Event Source][6]](#read-from-event-source6)
2. [Projection with Entity Centric Projector](#projection-with-entity-centric-projector)
3. [Throughput Metric Reporting](#throughput-metric-reporting)
4. [Filter Irrelevant Entities](#filter-irrelevant-entities)
5. [Convert Entities into JSON Document Structure](#convert-entities-into-json-document-structure)
6. [Duplicate Document Suppression](#duplicate-document-suppression)
7. [Bulk Indexing to ElasticSearch](#bulk-indexing-to-elasticsearch)

## Read from [Event Source][6]
The ElasticSearch Indexing pipeline reads entity data from the Knowledge topic before converting them into JSON documents. These JSON documents are then indexed into an ElasticSearch index.

## Projection with Entity Centric Projector
For E.R. entity projection is straight forward - the entity information being loaded in is expected to already be 
in Canonical form. However, there is scope for expansion as illustrated in Smart Cache Search's processing of RDF data.

## Throughput Metric Reporting
As the name suggests, this involves tracking the progress of entities being loaded, errors encountered and other 
relevant information.

# Filter Irrelevant Entities
Not as applicable here, but when processing data like RDF that can have items with nothing for searching, we can 
recognise and filter that out of the data being processed. 

## Convert Entities into JSON Document Structure
This step involves the conversion of the entities into JSON document structure. Note that we don't actually convert
directly to JSON in this step, rather we build an in-memory data structure that can be serialized straight to JSON
later.

Internally we actually use a `Map<String, Object>` since this can be used to represent an arbitrary JSON document
structure. Similar to the Projection step the conversion from `Entity` representation into a document structure is fully
configurable. Thus, we can easily create variations on the pipeline that generate and index documents in different
formats if we need to.

## Duplicate Document Suppression

Due to the underlying data structures used in most search indices it isn't possible to directly replace a previously
indexed document. If they encounter a document with the same ID as a previous document then in practical terms they mark
the previous document as deleted and index the new document. Then at periodic intervals the index gets rewritten to only
contain currently valid documents.

In a platform like Telicent Core where we have a continuous stream of data, and the same entity may be encountered many
times, much of the time we are generating identical documents for this entity. Continually re-indexing documents creates
a performance problem because it wastes both CPU and IO cycles doing unnecessary work. This impacts both the performance
of indexing and querying, because in a live production system users may be querying the cache while it is also indexing
new data. If there are lots of deleted documents in the index there is additional overhead in filtering those out of the
search results.

Therefore, we use a large Least Recently Used (LRU) cache to track recently generated documents and avoid passing the
documents on to ElasticSearch if they are identical to what we've previously asked it to index. This is an in-memory
cache so its size needs to be chosen carefully based on knowledge of the data i.e. how frequently we expect to encounter
the same entities and thus duplicate documents.

Special care is taken to handle deleted `Entity`'s, when a document is generated for a deleted `Entity` it invalidates
the cache for that document and is passed onwards for further processing.

## Bulk Indexing to ElasticSearch

We use a [`BulkIndexingSink`](indexing-documents.md#using-an-indexing-sink) to actually index the generated documents
into ElasticSearch. It is in this step that we actually convert from the internal representation (`Map<String, Object>`)
into an actual JSON document, this is handled automatically for us by the ElasticSearch Client APIs. This utilises the
ElasticSearch [Bulk API][3] to request the indexing of a batch of documents in a single HTTP request. This reduces some
indexing overheads so increases the overall performance of the pipeline.

Like many other aspects of the pipeline the batch size is configurable, with a default of 1,000 documents. Choosing a
suitable batch size depends on how large the generated documents are, e.g. data with lots of long descriptive literals
will produce larger documents, and how frequently you want new documents to be visible to users querying the cache.

In order to ensure additions and deletions are processed in the correct order the bulk indexing code tracks the current
operation it is building a batch for.  Once it receives an operation that is different from the current batch it will
immediately process the current batch and start building a new batch.  This is important to ensure the correct ordering
of add and delete operations.

Note that when a delete removes all the content for an entity from its corresponding document in the search index the
document itself still remains in the search index.  The resulting document will be a stub containing only the `uri`
field for the entity and possibly some leftover security labels (since security labels are intentionally never deleted).
Thus such documents **may** still appear in search results if searches use terms that appear in the entity URI.  Future
releases may look to handle this particular situation more gracefully.

Where a pipeline is slow, or receives data infrequently, you can also configure a maximum idle time which specifies the
maximum time allowed between indexing operations. If this is exceeded then the current batch of documents is indexed
into ElasticSearch, regardless of whether that batch has reached the configured batch size. Setting a value for this
ensures that new data regularly reaches ElasticSearch irrespective of the performance of, and data flow in the pipeline.
Using some value, even a high one, will make the pipeline more resilient because it ensures new data is regularly
indexed, and not held waiting for the batch size to be reached indefinitely.  While shutting down the pipeline will also
cause the outstanding batched documents to be indexed, you cannot guarantee that the pipeline will be shutdown
gracefully, therefore setting an idle time reduces the risk of data loss.

Finally, you can configure how often the sink flushes the index to disk. For ElasticSearch indices are held in
memory and only periodically backed up to disk so flushing the index regularly ensures data is durably stored. This is
expressed in terms of number of batches, with a default of 10. In practise this means that every 10 batches we'll ask
ElasticSearch to flush the index to disk, so with a default batch size of 1,000 documents we'll flush the index after
every 10,000 documents are indexed. Again choosing an appropriate value here depends on how rapidly the pipeline is
generating new documents and how quickly you want those durably stored.

Even when keeping an index in-memory ElasticSearch does maintain a transaction log meaning that in the event of a
failure the state of an index can be recovered. However, explicitly flushing the index periodically avoids this log
growing too large.

# Running the Pipeline
Running the indexing pipeline requires the following configuration to be known:

- An Event Source, usually:
    - The Kafka Server(s) to connect to, and
    - The topic to read from Kafka
    - (see [Alternative Event Sources](#alternative-event-sources) for alternatives)
- ElasticSearch 
  - Hostname and Port to connect to
  - Index to populate

As discussed in this document there are also lots of other configuration choices that can be made to customise the
behaviour of the pipeline.

## Running Directly

You can run the pipeline via its CLI found in the `cli-canonical-index` module:

```bash
$ ./cli-canonical-index/elastic-can-index --bootstrap-server <kafka-server> \
  --topic <knowledge-topic> --elastic-host=<elastic-hostname> \
  --elastic-port=<elastic-port> --index=<elastic-index>
```

In this example we show running it with a minimal required configuration, all other configuration is left to the default
values.

You can run with the `--help` option to see the full help for the command i.e.

```bash
$ ./cli-canonical-index/elastic-can-index --help
```

Some of the following options may be of interest based on the aspects of pipeline behaviour discussed earlier in this
document.

### `--duplicate-cache-size`

The `--duplicate-cache-size <cache-size>` option takes a value indicating the desired size of
the [duplicate suppression cache](#duplicate-document-suppression). The default value of this is 1,000,000 i.e. 1
million documents may be cached in memory. If you know the documents will be large, or that duplicates will be
infrequent, for the data this should be reduced accordingly.

### `--index-batch-size` and `--flush-per-batches`

These two options can be used to control the behaviour of the [Bulk Indexing](#bulk-indexing-to-elasticsearch) step,
they default to 1000 and 10 respectively.

Both take desired sizes, for example `--index-batch-size 10000 --flush-per-batches 5` would use an indexing batch size
of 10,000 documents and flush the index every 5 batches i.e. every 50,000 documents.

### `--max-idle-time`

The `--max-idle-time <idle-seconds>` option takes a value indicating the maximum idle time allowed between indexing
operations.  If this time is exceeded then the pipeline will index the current batch of documents into ElasticSearch
regardless of whether the configured batch size has been reached.  Thus setting a suitable value for this ensures
new documents are regularly indexed regardless of the performance and data flow in the pipeline.

By default, this is not set and the system default value is used, this default is currently 1 hour.

### `--limit`

The `--limit <limit>` option specifies the maximum number of RDF graphs to process for search indexing. This defaults
to `-1` aka unlimited meaning that the indexing pipeline will run forever unless otherwise aborted. However, when
experimenting with new pipeline configurations it may be helpful to only process a small number of graphs to check that
the configuration has the desired effect.

### `--max-stalls`

The `--max-stalls <max>` option specifies the maximum number of times the pipeline is allowed to encounter no new RDF
Graphs before it. This defaults to `-1` aka unlimited.

This is primarily useful in development environments where the event source is not unbounded, and you want the pipeline
to naturally exit when it has finished indexing the whole knowledge topic.

### `--read-policy` and `--group` options

The `--read-policy <policy>` option specifies how the pipeline consumes the configured Kafka topic. In conjunction with
the `--group <group>` option this leverages Kafka's Consumer Groups capabilities to automatically assign the pipeline a
fair share of the topics partitions, and to ensure that the pipeline processes each event exactly-once. As long as the
same `<group>` value is consistently supplied the pipeline is guaranteed to read the entire topic exactly once
regardless of how many times it is started and restarted.

The `--group`, if not explicitly specified, will use a default value based on the command being invoked. So for
this pipeline it will default the Consumer Group to `elastic-index`.

The default value for `--read-policy` is `EARLIEST` meaning it indexes from the earliest available events that it has
not previously consumed. If it has previously starting consuming the topic it resumes from the last known position.

The converse of this is `LATEST` which has the pipeline only consume new events on the topic, unless it has previously
started consuming the topic in which case it resumes from the last known position. This can be though of as processing
the topic exactly once **only** from the point in time the pipeline is first run.

Finally, the `BEGINNING` value forces the pipeline to read the entire topic from the beginning regardless of any
previously known positions. In the process of re-reading the topic the previously known positions will be overwritten.
However, this is very useful if you want to re-index the topic with a different configuration or need to rebuild the
index for any reason.

### `--recreate-index`

When this is specified the target ElasticSearch index, as specified via the `--index` option, will be deleted prior to
indexing. This **SHOULD** only be used if the index you are targeting is not being used by search clients e.g. you're
experimenting with building an index with different configuration.

### `--upsert` and `--no-upsert`

This option controls whether documents are indexed into ElasticSearch via upserts or not.  When upserts are used,
which is the default as of 0.6.0, each generated document for an entity is merged with any existing document.  This
allows knowledge for each entity to accumulate into a single document from many events seen over time.  For example,
you might have one adapter that produces the main representation of entities, and additional adapters that feed in
extra information about entities.

However, if your dataset generates complete entity representations within a single event, and you only want search to
reflect the most recent representation of that entity, then you would want to disable this via the `--no-upsert` flag.

From 0.9.0 onwards the internals of update handling were substantially rewritten to use ElasticSearch update scripts
rather than upserts.  This avoids known data loss issues that can occur when updates to a list field within a document
occur.

### ElasticSearch connection related options

There are several options that are used to configure how the indexing pipeline establishes a connection to ElasticSearch
before starting work.  The simplest option is `--elastic-max-connect-attempts` which takes an integer greater than or
equal to 1 indicating how many times to attempt to connect to ElasticSearch before aborting.  If we cannot successfully
connect to ElasticSearch within this number of attempts, and ElasticSearch indicates that the cluster is ready to handle
requests, then the pipeline will abort.

The interval times between connection attempts can be configured via the `--elastic-min-connect-interval` and
`--elastic-max-connect-interval` options, each of these take a time in seconds.  So `--elastic-min-connect-interval 3
--elastic-max-connect-interval 60` would wait a minimum of 3 seconds and a maximum of 60 seconds between connection
attempts.  Note that the actual interval between attempts is calculated via an exponential calculation, and thus subject
to the current number of connection attempts that have been made (assuming the maximum attempts has not been succeeded).

Additionally, the `--elastic-max-retries` option controls how many backoff retry attempts are made when an indexing
operation against ElasticSearch fails.  The default value for this is `3` i.e. all operations will be retried up to a
maximum of 3 attempts.  If the retry limit is reached and the operation is still failing the pipeline will throw an
error that will lead to it aborting.

Finally, the `--opensearch-compatibility` option when specified will configure the Elastic client APIs to try and provide
"compatibility" with OpenSearch servers.  This should allow the pipeline to run directly against OpenSearch servers
running OpenSearch 1.x, compatibility with other versions is not guaranteed.

### Alternative Event Sources

Instead of using a Kafka topic as the event source the pipeline also supports [file-based event sources][7].  These are
intended primarily for developers who are developing or testing the pipeline as it allows running the pipeline with
fewer external dependencies.

Note that a single event source must be selected when running the pipeline, if no event source options (or environment
variables) are provided, or if multiple event sources are configured via options, the pipeline will issue an error and
exit e.g.

```bash
$ ./cli-canonical-index/elastic-index
3 errors encountered parsing your arguments:

#1 - Required option '--index' is missing and no default value was available from the environment variable ELASTIC_INDEX
#2 - At least one of the following options must be specified but none were found: --bootstrap-server, --bootstrap-servers, --source-file, --source-dir, --source-directory
#3 - Required option '--elastic-host' is missing and no default value was available from the environment variable ELASTIC_HOST

Try re-running your command with -h/--help to see help for this command
```

A [file-based event source][7] is either a single file, or a directory of files, in one of the supported formats
detailed in the linked documentation.  To use this instead of a Kafka cluster you specify either the `--source-file` or
`--source-directory` option supplying the path to the file/directory to use as the event source.  You may also need to
supply the `--source-format` option to specify the file event format that the events are encoded in.

For example if we wanted to load in a single RDF file we could do so as follows:

```bash
$ ./cli-canonical-index/elastic-index --source-file example.ttl --source-format rdf \
  --elastic-host=<elastic-hostname> --elastic-port=<elastic-port> --index=<elastic-index>
```

Would attempt to use `example.ttl` as a single file event source in the format `rdf` which allows for taking RDF files
in any Apache Jena supported RDF serialization.

Note that if you are using the `--source-directory` option to supply a directory then the format specific rules
determine which files within that directory are treated as events, and the order in which they are yielded by the event
source.  Generally speaking only files with the correct file extension(s) and a numeric portion within their name are
treated as events, with the numeric portion used to sort the events into order.  For example if we had a directory of
YAML formatted events we could load it like so:

```bash
$ ./cli-canonical-index/elastic-can-index --source-directory /path/to/directory \
  --elastic-host=<elastic-hostname> --elastic-port=<elastic-port> --index=<elastic-index>
```

Since `yaml` is the default file event format there was no need to specify the `--source-format` option in this case.

### Capturing Events for Replay

Developers may also wish to avail themselves of the ability to capture events from one event source to be replayed as a
file-based [event source][6] in the future.  This can be used to prepare test scenarios and may prove useful since some
of the file event formats, while human-readable, are not directly editable e.g. in the `yaml` format the key and value
are the Base 64 encoded strings of the byte sequences generated by the Kafka `Serializer` for those types.

Capturing events for replay is achieved via the `--capture-directory` option which takes in a directory to which events
will be captured.  The associated `--capture-format` option specifies which [file event format][7] is used in capturing
the events e.g.

```bash
$ ./cli-canonical-index/elastic-can-index --bootstrap-server <kafka-server> \
  --topic <knowledge-topic> --elastic-host=<elastic-hostname> \
  --elastic-port=<elastic-port> --index=<elastic-index> \
  --capture-directory /path/to/directory --limit 100
```

Would read up to 100 events from the Kafka event source and capture those as files into the supplied
`/path/to/directory` directory.  When capturing events from an unbounded event source like Kafka it is advisable to
specify the `--limit` option to limit how many events will be captured.

**NB:** Capturing events involves disk IO and **MAY** considerably impact pipeline performance, hence the recommendation
to only use it to capture small numbers of events.

We could then later replay this capture via the [aforementioned `--source-directory`](#alternative-event-sources) option
e.g.

```bash
$ ./cli-canonical-index/elastic-index --source-directory /path/to/directory \
  --elastic-host=<elastic-hostname> --elastic-port=<elastic-port> --index=<elastic-index>
```

Would re-run the pipeline with the same events as we got from Kafka before but this time read from the supplied
directory instead.

## Running via Docker

If you prefer you can run via Docker provided you have first done the Docker Build e.g.

```bash
$ docker run -e BOOTSTRAP_SERVERS=<kafka-server> -e TOPIC=<knowledge-topic> \
  -e ELASTIC_HOST=<elastic-hostname> -e ELASTIC_PORT=<elastic-port> \
  -e ELASTIC_INDEX=<elastic-index> smart-cache-entity-resolution-api:<tag> --index-batch-size 10000
```

Where the relevant environment variables are suitably set for your environment, see [Networking
Considerations](#networking-considerations) below for some discussion of this especially around hostnames for Kafka and
ElasticSearch. `<tag>` reflects the tag used when you built the Docker images. This will normally be your git branch
unless you've explicitly supplied a different one, for example if you've currently got `main` checked out then your
image tag would be `main`.

This runs a Docker container that runs the indexing pipeline. This will continue to index documents until you `Ctrl+C`
the spawned container, or it encounters a halting condition as defined by your options (e.g. [`--limit`](#--limit) or
[`--max-stalls`](#--max-stalls)).

### Environment Variables vs Options

When running with the Docker container the required configuration may be injected via Environment Variables, if the
environment variables shown are present then there is no need to specify those options directly. Any additional options
you want to specify can also be supplied after the image reference.

The Docker container essentially just wraps up the `elastic-can-index` script into a runnable image. Thus, you can 
use it exactly as if you were running directly if you so wish:

```bash
$ docker run smart-cache-elastic-index:<tag> --bootstrap-server <kafka-server> \
  --topic <knowledge-topic> --elastic-host=<elastic-hostname> \
  --elastic-port=<elastic-port> --index=<elastic-index> --index-batch-size 10000
```

Will run the pipeline exactly the same way as in the above environment variable driven example.

Note that if you specify both environment variables and options then the options take precedence. For example:

```bash
$ docker run -e BOOTSTRAP_SERVERS=localhost:9092 smart-cache-elastic-index:<tag> \
  --bootstrap-server remotehost:9092 \
  --topic <knowledge-topic> --elastic-host=<elastic-hostname> \
  --elastic-port=<elastic-port> --index=<elastic-index> --index-batch-size 10000
```

The Kafka bootstrap servers would be `remotehost:9092` i.e. the value from the option.

### Networking Considerations

One consideration when running as a Docker container is that the pipeline needs to talk to both Kafka and ElasticSearch.
Due to running in a container the hostnames and ports you use for these locally in your development environment may not
be usable from within the container.

For example a hostname of `localhost:9092` for Kafka isn't going to work inside the container. The actual hostname to
use here will depend on your container runtime and OS. For example using [Docker Desktop for Mac][4] you can use
`host.docker.internal:9092` instead.

In a fully containerised deployment Kafka and ElasticSearch would both themselves be running in other containers, in
some kind of shared network, such that there are hostnames that allow the containers to communicate between themselves.
Refer to the documentation for your deployment for more details on that.



[1]: https://github.com/Telicent-io/smart-cache-search/tree/main/docs/entity-collector/index.md
[2]: https://github.com/Telicent-io/smart-caches-core/blob/main/docs/sinks/duplicate-suppression.md
[3]: https://www.elastic.co/guide/en/elasticsearch/reference/8.1/docs-bulk.html
[4]: https://docs.docker.com/desktop/mac/networking/#use-cases-and-workarounds
[5]: https://github.com/Telicent-io/rdf-abac/blob/main/docs/abac-specification.md#transport-in-rdf
[6]: https://github.com/Telicent-io/smart-caches-core/blob/main/docs/event-sources/index.md
[7]: https://github.com/Telicent-io/smart-caches-core/blob/main/docs/event-sources/file.md#supported-formats
