# Indexing Documents

Indexing documents into a search index is handled via the `SearchIndexer` interface that provides the basic operations
needed for this.  The type of documents to be indexed is given via a type parameter, so for example we could have a
`SearchIndexer<Map<String, Object>>` if we're representing the document structure as a `Map`.

```java
SearchIndexer<Map<String,Object>> indexer = createIndexer();
Map<String, Object> doc = createExampleDocument();
Function<Map<String, Object>, String> idFunction = d -> (String)d.get("uri");
```

To start with we can determine whether a given document is already indexed via the `isIndexed()` method.  This can
either take the ID of the document directly, or take a document and a function that calculates its ID. Knowing whether a
document is already indexed may affect indexing decisions, such as whether you want to merge the existing document with
a new version of the document, or whether the new document should just replace the existing document.

```java
if (indexer.isIndexed("some-id")) {
    System.out.println("Already indexed!");
}
if (indexer.isIndexed(idFunction, doc)) {
    System.out.println("Already indexed!");
}
```

Actually indexing documents is done via the `index()` or `bulkIndex()` methods, the former taking a single document and
the latter taking multiple documents.  In both cases a function to calculate the ID for a document must also be
provided.

```java
// Index a single document
indexer.index(idFunction, doc);

// Index a bunch of documents
List<Map<String,Object>> docs = generateExampleDocuments(100);
indexer.bulkIndex(idFunction, docs);
```

This will either succeed or will fail and throw an unchecked `SearchException`.  When using `bulkIndex()` an error
should only be thrown if none of the documents were successfully indexed.

If your `SearchIndexer` supports it, you can also make deletions against the index.  Deletions can either be done in
terms of whole documents via the `deleteDocument()` or `bulkDeleteDocuments()` methods which deletes documents
completely.  Or it may allow for partial deletion via the `deleteContents()` and `bulkDeleteContents()` methods:

```java
// Delete an entire document
indexer.deleteDocument(id);

// Delete partial document contents
indexer.deleteContents(idFunction, partialDoc);
```

Finally, the `flush()` method provides the ability to instruct the underlying index to flush any newly indexed documents.
The effect of this method is implementation dependent, and it may be an expensive operation so should be used sparingly.
The `boolean` parameter allows callers to tell the search index whether further documents are expected, allowing the
implementation to decide on what kind of flush operation (if any) to perform based on whether it expects further data.

```java
// Flush, more data to come
indexer.flush(false);

// Index more data...

// Flush, no further data
indexer.flush(true);
```

## Using an Indexing Sink

Often a `SearchIndexer` is used as part of a Smart Cache pipeline for ingesting data into a Search Smart Cache for later
querying.  The [Smart Caches Core][1] libraries provide the `Sink` abstraction for building linear data processing
pipelines like these and the Search API includes two `Sink` implementations specifically for this:

- `SearchIndexerSink` for indexing documents one at a time via `SearchIndexer.index()`
- `BulkSearchIndexerSink` for indexing documents in batches via `SearchIndexer.bulkIndex()`

These sinks operate over `Event` instances are strongly typed, so they require type parameters indicating the event key
and value types.  Note that the sink will only index the value portion of the event.  For example
`SearchIndexerSink<Integer, Map<String,Object>>` is a sink that indexes `Event<Integer, Map<String, Object>>` where the
documents being indexed are expressed as `Map<String, Object>`.

Creating an instance of one of these sinks requires a `SearchIndexer` that performs the actual indexing.  These sinks
simply handle the automatic calling of that API.

A `SearchIndexerSink` requires a `SearchIndexer` and an ID function.  Additionally, you can provide a reporting batch
size which controls how the sink tracks and reports indexing metrics.  It will log the current elapsed time and indexing
rate (in documents/seconds) each time the batch size is reached.  So if the reporting batch size is `10,000` it will
report metrics every 10,000 documents e.g.

```java
SearchIndexerSink<Integer, Map<String, Object>> sink 
  = SearchIndexerSink.create<Integer, Map<String,Object>>()
                     .indexer(indexer)
                     .idFunction(idFunction)
                     .reportBatchSize(10_000)
                     .build();
// Start sending documents to the sink
```

If your indexing pipeline needs to support deletions as well as additions then you also need to specify a function to
specify which events are considered to be deletions via the `.isDeletionWhen()` method and the desired deletion action
via the `.onDeletion()` method.  These sinks **will not** treat any events as representing deletions unless this
function has been explicitly configured.  If you want to be explicit that you are disabling deletes on this sink then
you can add a call to `.noDeletes()` when building the sink.

A `BulkSearchIndexerSink` can be built with several additional parameters:

- The indexing batch size
- The flush frequency
- Maximum idle time

Indexing batch size is expressed in terms of number of documents, so a value of `1,000` would mean
`SearchIndexer.bulkIndex()` is only called every 1,000 documents.  Note that the indexing batch size must be less than,
or equal to the reporting batch size and the reporting batch size must be a multiple of the indexing batch size.  The
flush frequency is expressed in terms of number of batches, so a frequency of `5` would mean that
`SearchIndexer.flush(false)` gets called every 5th time i.e. every 5,000 documents in our example.  If the indexing
batch size is set to `1` then this sink behaves exactly the same as the normal `SearchIndexerSink` in that documents are
immediately indexed upon arriving at the sink.

The maximum idle time is expressed as a `Duration`, if more than this duration passes without an indexing batch being
written then `bulkIndex()` is called regardless of whether the indexing batch size has been reached.  In our example the
idle time is set to 180 seconds, the minimum idle time that may be configured is 1 second.  Maximum idle time is
primarily useful for test and demo scenarios where minimal data will be pushed through the pipeline, or when the
pipeline receives data slowly/infrequently, and you want to ensure the search index is regularly updated.  If `null` is
passed in then a large default value is used, currently this default is 1 hour.

Users should take care in choosing a batch size and an idle time to reflect the characteristics of their pipeline.
Generally you **SHOULD NOT** set the idle time too low as this reduces the benefits of batching the documents for
indexing that this sink is designed to provide.  If the idle time is too low then the batch size will never be reached,
and indexing may happen too frequently reducing overall performance of the pipeline.  On the other hand if the batch
size is too large it may be reached very infrequently and documents wait a long time before being written to the index.

```java
BulkSearchIndexerSink<Integer, Map<String, Object>> bulk 
    = BulkSearchIndexerSink.<Integer, Map<String, Object>>createBulk()
                            .indexer(indexer)
                            .idFunction(idFunction)
                            .indexBatchSize(1_000)
                            .flushPerBatches(5)
                            .reportBatchSize(10_000)
                            .maxIdleTime(Duration.ofSeconds(180))
                            .build();
// Start sending documents to the sink
```

Note that whichever variant you choose to use you **MUST** call `close()` on the sink when you are done with it.  This
ensures that any remaining documents are indexed (if using `BulkSearchIndexerSink`) and that a final
`SearchIndexer.flush(true)` is called.  It will also report the final calculated indexing metrics for the lifetime of
the sink.

```java
// When done always call close() on our sinks
sink.close();
bulk.close();
```

As these sinks, like all `Sink`'s are `AutoCloseable` it is usually preferable to use them in a try with resources block
to ensure `close()` is always called e.g.

```java
try (BulkSearchIndexerSink<Map<String, Object>> bulk 
        = new BulkSearchIndexerSink<>(indexer, idFunction, 1_000, 5, 10_000, Duration.ofSeconds(180))) {
    // Send documents to the sink for indexing...
    
} catch (SearchException e) {
    // Handle the search error properly...
    System.err.println(e.getMessage());
}

```

# Implementations

Currently, there is a single concrete implementation - `ElasticSearchIndexer` - from the 
[ElasticSearch Implementation](elastic-impl.md).

[1]: https://github.com/Telicent-io/smart-caches-core
