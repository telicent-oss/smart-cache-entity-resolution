# IES 4 Configuration

The `ies-entity-resolver-configuration` module provides IES 4 specific implementations of various APIs from other 
modules. In particular this focuses on providing IES 4 specific configuration that can be used to utilise those APIs with data
expressed in the IES 4 ontology.

# Constants

The `Ies4` class provides access to a variety of constants relating to the IES 4 ontology e.g. `Ies4.NAMESPACE`
provides the namespace URI for the ontology.

This also provides constants for various pre-defined collections of configuration instances, for example there is
`Ies4.DEFAULT_IGNORED_TYPES` that lists types to ignore for use with the
[`SimpleTypeSelector`](entity-collector/simple-selector.md).

The `IesOutputFields` class provides constants for the field names used in outputting documents based upon collected IES
data.

# Implementations

## `IesHasNameCollector` and `IesIsIdentifiedByCollector`

These are implementations of the [`EntityDataCollector`](entity-collector/data-collectors.md) API that collect data
expressed using the IES 4 indirect representation style. See that documentation for more discussion and examples of how
this functions.

An `AbstractIesInfoCollector` provides the base implementation of the collection algorithm for these representations,
should API users need to collect data expressed in this style via other predicates then they can derive from this base
class.

## `IesConfiguration`

`IesConfiguration` extends the [`EntityProjectionConfig`](entity-collector/index.md#the-entityprojectionconfig)
providing a no argument constructor that injects suitable default configuration for collecting entities from graphs
expressed using the IES 4 ontology.

The `Ies4Projection` class implements the
[`EntityProjectionProvider`](entity-collector/index.md#the-entityprojectionconfig) interface meaning that it can be
dynamically loaded via the `EntityProjectionConfigurations` registry using the name `ies4`.

## `IesTypeSelector`

The `IesTypeSelector` is an extension of the [`SimpleTypeSelector`](entity-collector/simple-selector.md) that applies
additional filtering conditions to ignore any entities that are themselves indirect representation objects.  
This ensures that when working with data expressed using the IES 4 Ontology we only select the top level entities, and
not the intermediate entities.

## `IesRepresentationsConverter`

The `IesRepresentationsConverter` is used to map indirect IES representations collected by one the earlier mentioned IES
specific `EntityDataCollector` into document output. This is an implementation of the
[`EntityToMapOutputConverter`](entity-collector/output-converters.md) interface specific to IES representations.  
See that documentation for more discussion and examples of the output from this.

## IES Document Formats

This module contains several classes that implement the
[`EntityDocumentFormatProvider`](entity-collector/index.md#entitydocumentformatprovider) interface and provide several
document formats for use in outputting IES 4 data as JSON documents:

- `Ies4DocumentProviderV1`
- `Ies4DocumentProviderV2`
- `Ies4DocumentProviderV3`

The `V1` class is the original document format that replicates the document structure from the earliest prototypes of
the Search Smart Cache.  The `V2` class is the revised format that is designed to be fully compatible with streaming
updates and deletions against the documents.  While the `V2` instance was intended to be used in preference to the `V1`
format testing has shown that some document stores do not cope well with that format due to how they map unique paths
through the document structure to fields in their underlying indices.  Therefore, the choice of format will depend on the
intended underlying document store.

The `V3` class is a variation on the `V1` class that includes additional output fields for conveying 4D state
information associated with entities, or states thereof.

When accessed via the `EntityDocumentFormats` helper methods these formats are named `ies4-v1`, `ies4-v2` and `ies4-v3`
respectively.
