# Querying Search Indices

Search Indices can be queried via the `SearchClient` interface, this provides a number of methods for making different
[types of queries](#query-types) which offer different levels of expressiveness, and query performance.  Query results
are returned using the [Similarity Results Data Model](similarity-results.md) in which the actual document structure is
arbitrary.  As a result this interface does not need any type parameters.

```java
SearchClient client = createClient("my-index");
```

As seen above implementations are generally expected to be tied to a single search index, even if the underlying search
index supports multiple indices.  By requiring a client be created for each index you wish to query this helps to reduce
the possibility of mixing up results from different indices.

Depending on the implementation the underlying search index may not support all the `QueryType`'s that the API defines.
That means that attempting to call some of the query methods may result in a `SearchException` being thrown.  We can
check in advance whether an implementation supports a given query type via the `supports()` method e.g.

```java
SearchResults results;
SearchOptions options = new SearchOptions();
if (client.supports(QueryType.QUERY)) {
    results = client.searchByQuery("(some or another) and day", options);
} else {
    results = client.searchByPhrase("another day", options);
}
```

As seen above actual queries are made by calling one of the four variants of the search by methods:

- `searchByQuery()` performs a querystring query.
- `searchByTerm()` performs a term query.
- `searchByPhrase()` performs a phrase query.
- `searchByWildcard()` performs a wildcard query.

All four methods take the query as the first parameter and the `SearchOptions` as the second parameter.  The
`SearchOptions` express additional options that the underlying search index should apply to the query.  Currently, the
supported options are the customisation of the limit and offset, which can be used to page through results e.g.

```java
// Get up to 50 results starting from the first result
options = new SearchOptions(50, SearchResults.FIRST_OFFSET);
results = client.searchByTerm("another", options);
```

And the optional enablement of [result highlighting](#result-highlighting):

```java
// Get up to 10 results starting from the first result with highlighting enabled
options = new SearchOptions(10, SearchResults.FIRST_OFFSET, new HighlightingOptions(true));
results = client.searchByTerm("another", options);
```

Finally, if the caller knows the ID of a document they are interested in they can just retrieve that document via the
`getDocument()` method.  This either returns the `Document`, or `null` if no such document exists.

```java
// Get a specific document
Document doc = client.getDocument("some-id");
```

## Query Types

The API defines 4 query types that can be supported, an implementation must support at least one of these and generally
should support multiple types.  However, it may choose not to support some query types for performance reasons, or
because it does not handle the query type as defined in our API.

### Querystring Queries

Querystring queries are the most powerful kind of query, but equally the least user-friendly.  A querystring query is
interpreted on a per-implementation basis, so each implementation may support different syntax within the querystring,
and the same query used with different implementations could produce very different results.

A querystring query typically allows users to use various syntactic constructs to create complex queries which express
boolean conditions, term boosts for scoring calculation, wildcard patterns and so forth.  This can allow a knowledgeable
user to make very precise and targeted queries to find the documents they are interested in.

This power is also its downside, if a user is unaware that they can use these features are treat a querystring query as
a simple pattern match style query their queries can yield no results.  For example consider the various example search
results shown in the [Similarity Results Data Model](similarity-results.md#json-representation) documentation.  Those 
particular
examples are from a dataset containing ship movements, one such ship in the results is the `MSC Geneva`, however a query
issued for just `gen` would return zero results:

```json
{
  "total": 0,
  "limit": -1,
  "offset": 1,
  "query": "gen",
  "type": "query",
  "results": []
}
```

This is because the underlying implementation treats a simple term without any syntactic modifiers as being required to
match an exact term.  So since `gen` does not match `geneva` exactly the results are empty, but the user who knew that
ship was in there might be surprised at that result.  To actually return the boat in question they either need to query
for `geneva`:

```json
{
  "total": 1,
  "limit": -1,
  "offset": 1,
  "query": "geneva",
  "type": "query",
  "results": [
    {
      "id": "http://itu.int/MMSI/255805618",
      "score": 7.4829216,
      "document": {
        "types": [
          "http://ies.data.gov.uk/ontology/ies4#Vessel",
          "http://itu.int/VesselTaxonomy#FreightShip"
        ],
        "hasName": [
          {
            "instance": "http://itu.int/MMSI/255805618_NAME",
            "predicate": "http://ies.data.gov.uk/ontology/ies4#hasName",
            "types": [
              "http://ies.data.gov.uk/ontology/ies4#VehicleName"
            ],
            "representationValue": "MSC GENEVA"
          }
        ],
        "isIdentifiedBy": [
          {
            "instance": "http://itu.int/MMSI/255805618_CALLSIGN",
            "predicate": "http://ies.data.gov.uk/ontology/ies4#isIdentifiedBy",
            "types": [
              "http://ies.data.gov.uk/ontology/ies4#Callsign"
            ],
            "representationValue": "CQFQ"
          },
          {
            "instance": "http://itu.int/MMSI/255805618_IMO",
            "predicate": "http://ies.data.gov.uk/ontology/ies4#isIdentifiedBy",
            "types": [
              "http://ies.data.gov.uk/ontology/ies4#VehicleIdentificationNumber"
            ],
            "representationValue": "IMO9320427"
          },
          {
            "instance": "http://itu.int/MMSI/255805618_MMSI_ID",
            "predicate": "http://ies.data.gov.uk/ontology/ies4#isIdentifiedBy",
            "types": [
              "http://ies.data.gov.uk/ontology/ies4#CommunicationsIdentifier"
            ],
            "representationValue": "255805618"
          }
        ],
        "literals": {
          "telicent:PrimaryName": [
            "MSC GENEVA"
          ]
        },
        "uri": "http://itu.int/MMSI/255805618"
      }
    }
  ]
}
```

Or they need to use an appropriate syntactic modifier e.g. `gen*` so `gen` is treated as a wildcard prefix match.

Please refer to the individual implementations documentation for details of what querystring syntax they support.

### Term Queries

Term Queries break the provided query into terms and find documents that contain one/more of the terms.  This kind of
query is useful when a user has several terms of interest but no particular sense of which is more important.

With a term query the actual terms may occur across different fields in arbitrary orders.  For example in our earlier
API usage example we had the query `another day`, this would find documents that contained the terms `another` and
`day`, but an individual document may only contain one of those terms.

If you need all terms to occur in a specific order then use a [Phrase Query](#phrase-queries) instead.

### Phrase Queries

Phrase Queries treat the query as a phrase that must occur as a whole within a single field of a  document.  The phrase
is still broken into terms (because that's just how search indices work) but this kind of query requires that all the
terms occur in the document and that they occur as a phrase within a field.  This means that any document returned has
exactly the phrase given in one of its fields.

This differs from a [Term Query](#term-queries) where the terms can occur in any order, potentially across multiple
fields, and only some terms may occur in each document.

As a practical example for when a phrase query would be more useful consider searching for a person with a common
surname like `Smith` e.g. `Anita Smith`.  A [term query](#term-queries) would return anybody with either the name
`Anita` or the surname `Smith`, while `Anita` being a less common name would cause `Anita Smith` to score highly in the
results you'd also get a lot of irrelevant results. Whereas with a phrase query you would only find documents that
specifically contained `Anita Smith` in them, probably a much smaller and more accurate set of results.

### Wildcard Queries

Wildcard Queries break the query into terms treating each as a wildcard match.  Due to how search indexes are
constructed wildcard queries can be very expensive because they may require scanning the entire term index to identify
terms that match the wildcard, before any documents containing those terms can be identified.

The user can specify the wildcard(s) for each term via the `*` character, and if not specified the term is treated as
being a prefix.  For example `gen` would be treated as `gen*` for querying and match terms like `gene`, `geneva` etc.
On the other hand a term like `g*n` already contains a wildcard and match terms like `gene`, `gun`, `gone` etc.

## Result Highlighting

Result Highlighting allows augmenting the search results with highlighting information to show which portion(s) of a
document matched the search query.  Since this can be an expensive operation this is disabled by default and must be
explicitly enabled, this is done by passing a suitable `HighlightingOptions` instance into your `SearchOptions` that are
used for your queries.

At its simplest, result highlighting is enabled like so:

```java
SearchOptions withHighlighting 
  = new SearchOptions(10, SearchResults.FIRST_OFFSET, 
                      new HighlightingOptions(true), 
                      TypeFilterOptions.DISABLED);
```
When enabled like this the underlying search engines default highlighting tags are used.  This means that the underlying
search index chooses what tags it uses to surround the matched text within the document to denote highlighting.  For
example with ElasticSearch the defaults are `<em>` and `</em>`, so any field that matches the query will have these tags
inserted into it around the matching text.

The highlighted version of the document is returned in addition to the un-highlighted version.  This is done so that API
consumers can switch between using raw and highlighted fields as they wish.

### Highlighting Tag Customisation

Depending on the caller of the API you may wish to use different pre-tags and post-tags for highlighting, for example a Web
UI might want to control what HTML tags are used, so they can apply a specific style to highlighted text.  This can be
done by providing additional parameters to the options e.g.

```java
HighlightingOptions highlighting = new HighlightingOptions(true, "<span class=\"highlight\">", "</span>");
SearchOptions options = new SearchOptions(100, SearchResults.FIRST_OFFSET, highlighting);
```

In the above example we configure the pre-tag to be `<span class="highlight">` and the post-tag to be `</span>`.  These
values can be any string you wish that makes sense for your use case.

Note that for ElasticSearch if you choose to customise the tags you **MUST** customise both the pre-tag and post-tag.

### Type Filtering

You can also apply type filters to your search result by providing `TypeFilterOptions` to your `SearchOptions`
constructor.  For example:

```java
TypeFilterOptions typeFilter 
  = new TypeFilterOptions("http://some-type", TypeFilterMode.ENTITY);
SearchOptions options 
  = new SearchOptions(100, 
                      SearchResults.FIRST_OFFSET, 
                      HighlightingOptions.DISABLED,
                      typeFilter);
```

In this example we configure a type filter to restrict our search results to entities with a type of `http://some-type`.
Type filtering is exact-match based i.e. the specified type **MUST** exactly match a type declared within the document for the entity.

The `TypeFilterMode` can be one of the following:

- `Entity` - Filter by entity type
- `Identifier` - Filter by identifier type
- `Any` - Filter by either entity or identifier type

# Implementations

Currently, there is a single concrete implementation - `ElasticSearchClient` - from the 
[ElasticSearch Implementation](elastic-impl.md).
