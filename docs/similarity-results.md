# Similarity Results Data Model

The Similarity Results Data Model forms part of the [Entity Resolution API](er-api), it provides a minimalist model for
representing similar matching results.  In particular, it allows for the actual document portion of each result to be a 
completely arbitrary structure i.e. no mandatory document structure is imposed on the results.

In reality a given Smart Cache will have a defined document structure because that has to be defined as part of its
indexing pipeline, and is needed in order to filter results based on security labels.  Plus a well-defined document
structure is needed in order for applications to process and render results in a user-friendly
manner.

## Data Model

## Similarity Results
This is simply a listing of [Similarity Results](##Similarity-Result) which is returned when calling the endpoint 
with a list of documents.

## Similarity Result

A `SimilarityResult` consists of a Source Entity ID and a list of search Hits.

### Source Entity ID
The Source Entity ID is a identifier provided by the underlying caller. 
Given the similarity endpoint will be passed a document representing an entity of similar type to those stored, it 
is advisable to use the unique ID for the entity. 

### Hit
A Hit consists of an ID, Score and Document. 

The ID, accessed via getId() is a unique identifier for the document within the underlying search index, it is not 
intended to be exposed to users directly but may be used internally within applications. For example, you might use it to determine whether two different queries have a set of documents that match both.

The score is a floating point number providing a relevance score for the result, based on
relevance for the query that produced these results.  Thus, the same document, returned by different queries may have
very different scores each time depending on how well it matched a given query.  Within a given set of search results
the individual results are expected to be sorted in descending order from highest to lowest score. Based on the 
criteria pass in the call, matching documents with a score lower than a given threshold will be filtered out of the 
returned results. 

The document  is the actual JSON document as indexed and stored by the underlying search index.


## JSON Representation
Since search results may be exchanged between different components and applications within the Telicent Core 
platform we use JSON as the standard representation.

A sample result set is shown below. In this example, we searched with an entity that matched 100% with the first 
hit - hence the 100% score; and partially matched with the second. We can assume that the maxResults parameter, which 
defaults to 1, was at least 2 and the minScore was at least 0.8.

```json
{
  "IDSourceEntity": "sampleId",
  "hits": [
    {
      "id": "hitId1",
      "score": 1.0,
      "document": {
        "properties": {
          "name": "Frances",
          "surname": "Diamond"
        }
      }
    },
    {
      "id": "hitId2",
      "score": 0.8,
      "document": {
        "properties": {
          "name": "rFances",
          "surname": "Diamond"
        }
      }
    }
  ]
}
```

It is possible to return a result set that doesn't have any matches in it, partial or otherwise. For example, if the 
minScore parameter was sufficiently high or the data set being matched was suitably small with no close matches.
In that case the result set would appear like this:

```json
{
    "idSourceEntity": "sampleId",
    "hits": []
}
```

