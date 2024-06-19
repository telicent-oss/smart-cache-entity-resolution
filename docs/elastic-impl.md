# ElasticSearch E.R. API Implementation

The `entity-resolver-elastic` module provides an [ElasticSearch][1] backed implementation of the [Entity Resolution API](er-api).
It provides an implementations of the interface defined by the API, in this part of the documentation we cover the details of those such as implementation specific quirks.

As a quick refresher, ElasticSearch is an application server that provides a wide range of REST APIs for managing,
indexing and searching over documents.  The server provides access to many indices, each of which has a unique name.
Indices may be distributed ([sharded][8] in ElasticSearch terminology) across a cluster of hosts for scalability.  Any
operation against an index will be automatically routed to the appropriate host(s) within the cluster in order to make
operations both scalable and fault-tolerant.  It also handles aggregating results from multiple index shards such that
each index appears as a single logical unit to the API consumer, regardless of whether it is actually sharded behind the
scenes.

Behind the scenes ElasticSearch is built atop of the [Apache Lucene][9] libraries with the core index data structures
and query capabilities provided by Lucene.  Lucene is however a very low level building block, so much of
ElasticSearch's value is in the API layers it provides since they expose Lucene capabilities in a much more user, and
developer, friendly way.

# Dependency

The ElasticSearch implementation is provided by the `entity-resolution-elastic` module which can be depended on 
from Maven
like so:

```xml

<dependency>
    <groupId>io.telicent.smart-caches.entity-resolution</groupId>
    <artifactId>entity-resolver-elastic</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../README.md) in this repository for that
information.

## Managing ElasticSearch Indices

ElasticSearch indices can be managed by using an `ElasticIndexManager` which implements the
[`IndexManager`](managing-indices.md) interface.  Creating an instance of this class requires knowing the hostname and
port:

```java
IndexManager<SimpleMappingRule> manager = new ElasticIndexManager("some-host", 1234);
```

Would create an instance that connects to an ElasticSearch server at `some-host:1234`.  The need to separate the
hostname and port is an implementation detail caused by how the underlying ElasticSearch client libraries define HTTP
connections.

### Creating Indices

When using the `createIndex()` method the `ElasticIndexManager` supports any configuration that is a
`IndexConfiguration<SimpleMappingRule>`.  There is an `ElasticMappings` utility class that contains static helper
methods that convert from `SimpleMappingRule` into ElasticSearch [mapping][2] rules.  Where a rule contains a `*` in its
match pattern it will be converted into a [Dynamic Template][3] using a `path_match` to match field names.  For any
other rule an [explicit mapping][4] is used instead.

Currently, mapping rules support the following field types:

| Field Type Value | Elastic Field Type         | Description                                                               |
|------------------|----------------------------|---------------------------------------------------------------------------|
| `uri`            | [text][6] and [keyword][5] | Indexed as both a keyword for exact matches and text for partial matching |
| `text`           | [text][6]                  | Indexed for full text search                                              |
| `keyword`        | [keyword][5]               | Indexed as a keyword for exact matches only                               |
| `date`           | [date][14]                 | Indexed as a date time for date range searches                            |
| `float`          | [float][15]                | Indexed as a floating point number for numeric range searches             |
| `geo-point`      | [geo_point][16]            | Indexed as a geospatial point for geospatial searches                     |
| `non-indexed`    | [object][12]               | Not indexed, stored only                                                  |
| Any other value  | [text][6]                  | Defaults to full text search for any unrecognised field type              |

While ElasticSearch supports lots of other [field types][7] the `ElasticIndexManager` only provides integrated support
for the above currently.  If you want an index that uses mapping rules not expressible via the API you should pre-create
the desired index directly with ElasticSearch instead.

However, it is possible to extend the set of supported field types by implementing the `ElasticMappingFactory` interface
and registering it within the
`META-INF/services/io.telicent.smart.cache.search.elastic.schema.factories.ElasticMappingFactory` file.  This factory
interface has three methods:

- `supports(IndexMappingRule)` to check whether your factory supports a given rule
- `toProperty(IndexMappingRule)` to convert a rule into an ElasticSearch property mapping
- `toDynamicTemplate(IndexMappingRule)` to convert a rule into an ElasticSearch dynamic template mapping

This allows implementing support for additional field types if so desired and is automatically discovered via
`ServiceLoader` provided the relevant `META-INF/services` files are present in your JAR files.

## Indexing Documents

To index documents into ElasticSearch an `ElasticSearchIndexer` is provided, this implements the
[`SearchIndexer`](indexing-documents.md) interface.  Each instance of an `ElasticSearchIndexer` operates over a single
index whose name must be known when the instance is created e.g.

```java
SearchIndexer<Document> indexer 
  = ElasticSearchIndexer.<Document>create()
      .index("my-index")
      .usingUpserts()
      .updatingContentsWith(ContentUpdate::forDocument)
      .deletingContentsWith(ContentDeletion::forDocument)
      .host("some-host")
      .port(1234)
      .build();
```

Would create an indexer that connects to ElasticSearch at `some-host:1234` and operates upon the `my-index` index as
well as configuring updates and deletes.  You can then index documents directly with the instance, or use it with an
appropriate [`Sink`](indexing-documents.md#using-an-indexing-sink) as part of a data ingest pipeline.

Note that an `ElasticSearchIndexer` assumes that the index `my-index` already exists, or that the user has permissions
to automatically create new indices.  If you want to explicitly manage the lifecycle of `my-index` see the earlier
section on [managing indices](#managing-elasticsearch-indices) or use the relevant ElasticSearch REST APIs directly.

## Searching Documents

To search documents stored in ElasticSearch an `ElasticSearchClient` is provided, this implements the
[`SearchClient`](querying-indices.md) interface.  As with indexing documents each instance of the `ElasticSearchClient`
operates over a one or more indices whose name(s) must be known e.g.

```java
SearchClient client = ElasticSearchClient.builder()
                                         .host("some-host")
                                         .port(1234)
                                         .index("my-index")
                                         .build();
```

As in our prior example this search client connects to ElasticSearch at `some-host:1234` and searches the `my-index`
index.

An `ElasticSearchClient` supports [Querystring](querying-indices.md#querystring-queries),
[Term](querying-indices.md#term-queries) and [Phrase](querying-indices.md#phrase-queries) queries.  Wildcard queries are
not currently supported, mainly because they are very expensive performance wise.

For querystring queries the implementation uses the [query string syntax][10] defined in the ElasticSearch
documentation, this provides a variety of syntactic constructs.  Some notable ones are listed here:

- `dog OR cat` - Boolean `or` and `and` operators for combining terms and other clauses.
- `"some phrase"` - Double quotes to indicate a phrase, rather than a term, to match.
- `+dog -cat` - Must match `+` and must not match `-` operators.
- `gen*` - Wildcard `*` for prefix and other wildcard searches.
- `field:term` - Search a specific `field` for a given `term`, or other clause.

For term and phrase queries the underlying query sent to ElasticSearch will search for the term/phrase across all fields
of the document.

The implementation fully supports using the limit and offset provided by the `SearchOptions` to push the paging of
results directly onto ElasticSearch wherever possible.  When asking for unlimited results, or a larger limit (currently
greater than 100 results), the implementation uses the [Scroll API][11] to request the results in batches.  Since
scrolling and an offset cannot be applied simultaneously by ElasticSearch in these cases we apply the limit and offset
locally instead.

## Other Considerations

When using these APIs in any context where logging is configured care should be taken to set the desired log levels
correctly.  When the log level is set to `DEBUG` detailed HTTP traces will be printed in the logs, while very useful for
debugging these can be very noisy and should not be logged by default.

Additionally, when used in a development context where no ElasticSearch security configuration is in place the client
will log a warning on every single HTTP request.  As some operations may require multiple HTTP requests to be made this
can flood the logs with a lot of unnecessary warnings.  Consider adding something like the following to your log
configuration file:

```xml
<!-- ElasticSearch issues a Warning on every single HTTP Request if Auth is disabled -->
    <logger name="org.elasticsearch.client.RestClient" level="ERROR" />
```

The above snippet assumes a Logback configuration file, similar settings can also be applied with other logging
frameworks.

## Testing

In order to test our ElasticSearch integration we make use of our [`test-clusters`](test-clusters.md) module providing
an `ESTestCluster` and `OpenSearchWithElasticTestCluster` concrete subclasses of the `AbstractSearchCluster` that can
be used.  These allow search clusters to be spun up and down for individual tests or a set of tests as necessary.

[1]: https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html
[2]: https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html
[3]: https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html
[4]: https://www.elastic.co/guide/en/elasticsearch/reference/current/explicit-mapping.html
[5]: https://www.elastic.co/guide/en/elasticsearch/reference/8.1/keyword.html#keyword-field-type
[6]: https://www.elastic.co/guide/en/elasticsearch/reference/8.1/text.html#text-field-type
[7]: https://www.elastic.co/guide/en/elasticsearch/reference/8.1/mapping-types.html
[8]: https://www.elastic.co/guide/en/elasticsearch/reference/current/scalability.html
[9]: https://lucene.apache.org
[10]: https://www.elastic.co/guide/en/elasticsearch/reference/8.1/query-dsl-query-string-query.html#query-string-syntax
[11]: https://www.elastic.co/guide/en/elasticsearch/reference/8.1/paginate-search-results.html#scroll-search-results
[12]: https://www.elastic.co/guide/en/elasticsearch/reference/current/object.html
[13]: https://www.elastic.co/guide/en/elasticsearch/reference/8.4/mapping-source-field.html
[14]: https://www.elastic.co/guide/en/elasticsearch/reference/8.4/date.html
[15]: https://www.elastic.co/guide/en/elasticsearch/reference/8.4/number.html
[16]: https://www.elastic.co/guide/en/elasticsearch/reference/8.4/geo-point.html
