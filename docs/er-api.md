# The Entity Resolution API

The Entity Resolution API is intended to abstract the underlying search index implementation from our Smart Cache 
code so 
that we
can swap out the underlying index as needed.  Rather than a single interface the API provides interfaces for each
functional area so that different implementations can provide support only for those areas that make sense for them.

Implementations are free to implement each interface separately, or to provide a single implementation class that
implements multiple interfaces.

# Interfaces

A `SearchIndexer` is used to index data into a search index, it can be used with the corresponding `SearchIndexerSink`
and `BulkSearchIndexerSink` to automate indexing.  See [Indexing Documents](indexing-documents.md) for more details on
that API.

An `IndexManager` allows for management of indices combined with the `IndexConfiguration` and `IndexMappingRule`
interfaces.  See [Managing Indices](managing-indices.md) for more detail.

The `SearchClient` interface provides the ability to query a search index, it provides methods for several different
`QueryType` and an implementation can choose to support a subset if they cannot support all the defined query types.
See [Querying Indices](querying-indices.md) for further documentation.

There is also a `SearchException` which is an unchecked runtime exception that can be thrown by any of these interfaces
from most methods, unless that method contract already covers error handling in other ways.  For example there are some
methods that return a nullable `Boolean` which return `true` for success, `false` for failure and `null` for
undetermined/error.

# Dependency

The Entity Resolution API is provided by the `entity-resolution-api` module which can be depended on from Maven like so:

```xml

<dependency>
    <groupId>io.telicent.smart-caches.entity-resolution</groupId>
    <artifactId>entity-resolution-api</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../README.md) in this repository for that
information.

You should only need a direct dependency if you are implementing the API itself, if you are using a concrete
implementation of the API then this will come as a transitive dependency.

# Implementations

The `entity-resolution-api` module itself contains some basic implementations of some of the helper interfaces e.g.
`SimpleIndexConfiguration` and `SimpleMappingRule`.   However, there is only a single concrete implementation, the
[ElasticSearch Implementation](elastic-impl.md).
