# Managing Search Indices

The management of search indices is provided by the `IndexManager` interface.  This is a strongly typed interface that
takes the type of the index mapping rules used, which must implement the `IndexMappingRule` interface.

```java
IndexManager<SimpleMappingRule> manager = createManager();
```

You check for the existence of a specific index via the `hasIndex()` method.  This returns a `Boolean` indicating
whether the index exists (`true`), does not exist (`false`) or is unknown (`null`).  Unknown means that the
implementation couldn't determine if the index exist, typically due to an error communicating with the underlying search
index.

```java
// Does index "example" exist?
if (Boolean.TRUE.equals(manager.hasIndex("example"))) {
    System.out.println("Index example exists");
} else {
    System.out.println("Index example does not exist");
}
```

You can also list the existing search indices via the `listIndices()` method which returns the available search indices.
This might be an empty list if that information is unknown or otherwise unobtainable.

```java
// List available indices
for (String index : manager.listIndices()) {
    System.out.println(index);
}
```

Indices may be created and deleted via the `createIndex()` and `deleteIndex()` methods, we'll start with the destructive
operation since that's simpler to explain.  As the name suggests `deleteIndex()` simply deletes an existing index by
name e.g.

```java
// Delete the example index
Boolean result = manager.deleteIndex("example");
```

The `Boolean` result here indicates whether the deletion succeeded (`true`), failed (`false`) or was unknown (`null`).
Unknown might mean that the underlying search index did not report success/failure, that some error occurred
communicating with it, or that you do not have permissions to delete that index.

# Creating an Index

Creating a new index is a more complex operation that requires more knowledge of how you want the index to be
constructed.  In order to call `createIndex()` you must provide both a name for the index, and an `IndexConfiguration`.
The `IndexConfiguration` provides a set of `Properties` that can be used to express implementation specific
configuration, and a set of `IndexMappingRules` that define how documents are going to be indexed within the index being
created.

`IndexConfiguration` is itself a generic interface taking a type parameter that expresses the type of mapping rules it
uses, which must implement the `IndexMappingRule` interface.  There is a `SimpleIndexConfiguration` implementation
available that uses `SimpleMappingRule` as the rule type e.g.

```java
// Define our mapping rules
List<SimpleMappingRule> rules = Arrays.asList(
    new SimpleMappingRule("uri", "uri", CommonFieldTypes.URI),
    new SimpleMappingRule("types", "types", CommonFieldTypes.URI),
    new SimpleMappingRule("literals", "literals.*", CommonFieldTypes.TEXT)
);

// Define implementation specific properties
Properties props = new Properties();
props.setProperty("some.example.config", "true");

// Wrap them into an index configuration
IndexConfiguration config = new SimpleIndexConfiguration(props, rules);
```

Note in the above example we've used some predefined constants from  `CommonFieldTypes` as part of our [rule
definitions](#mapping-rules) which are explored further later on this page.

Once we have our defined configuration we can create our desired index:

```java
manager.createIndex("example", config);
```

## Mapping Rules

Mapping rules are defined via the `IndexMappingRule` interface which simply defines 3 methods:

1. `getName()` which returns the name for the rule.
2. `getMatchPattern()` which returns a match pattern that identifies fields to which the rule applies.
3. `getFieldType()` which returns the field type that should be used to index fields that match this rule.

Specific Search API implementations may choose to use a rule type that exposes additional methods that are
implementation specific.  Each implementation will also translate rules expressed via this interface into the
appropriate mapping rule structures for their underlying implementation when creating a new index.

The match pattern is typically expressed as either an exact field name - `field` - or as a wildcard - `*.field`, whether
the underlying implementation supports both forms is an implementation detail.  Using an unsupported/invalid match
pattern will lead to errors in creating an index.  Refer to the documentation for an individual implementation for more
detail on what is and isn't supported.

As seen in earlier examples some useful constants are provided by `CommonFieldTypes`, this defines the common field
types that any implementation **SHOULD** support.  These include, but are not limited to, the following:

- `uri` - A field that will contain RDF URIs that should be both exact-matched and full-text searchable.
- `keyword` - A field that will contain values that should be exact-matched only.
- `text` - A field that will contain free-form text i.e. RDF Literals.
- `non-indexed` - A field that will be stored **BUT** not indexed for search.

A field of type `uri` should be indexed such that exact match lookups can be done on it based on the full URI.  Whereas
a field of type `text` should be indexed so full-text search can be done on it i.e. searching for a single word that
occurred in the text.

Please see the implementation document for more detail on supported field types.

## Predefined Configurations

For our common use cases it is useful to provide predefined index configurations that are suitable for those.  This is
done via the `IndexConfigurationProvider` interface.  This is a factory interface where each implementation provides
access to one/more predefined `IndexConfiguration` instances.  These are identified by names listed by the
`configurations()` method and descriptions of each may be obtained via the `describe(String)` method and the actual
instance loaded via the `load(String, Class<? extends IndexMappingRule>)` method.

Note that all the built-in providers only support `SimpleMappingRule` currently, so if you are working on an
implementation that needs to use an alternative rule type you would need to provide your own providers, potentially
decorating the existing ones if you so desired.

These providers are dynamically loaded via the `ServiceLoader` mechanism which means they need to be registered by a
suitable `META-INF/services/io.telicent.smart.cache.search.configuration.IndexConfigurationProvider` file.

The `IndexConfigurations` static class provides access to all the registered providers.  It's `available()` method lists
all available configurations, the `describe(String)` method describes an available configuration and the `load(String,
Class<? extends IndexMappingRule>)` method loads an available configuration.

# Implementations

Currently, there is a single concrete implementation - `ElasticIndexManager` - from the 
[ElasticSearch Implementation](elastic-impl.md).
