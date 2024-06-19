# Test Clusters

The `test-clusters` module provides some wrapper functionality around using [Test Containers][1] to spin up temporary
search clusters for use in unit and integration testing.

The core concept in this module is the `AbstractSearchCluster` class, this provides the generic functionality about
starting and stopping a search cluster based upon a single test container.  A derived implementation **MUST** override
the `setup()` method to configure the `this.container` field with the necessary test container and start it up.  The
other method that **MUST** be overridden is `createIndexManager()` which returns an
[`IndexManager`](managing-indices.md).

The `AbstractElasticTestCluster` and `AbstractOpenSearchTestCluster` classes derive from this base class and add the
basic cluster setup for ElasticSearch or OpenSearch as appropriate.  However, these are still abstract as no
`IndexManager` implementations are provided in the `test-clusters` module itself, thus individual implementations such
as the [ElasticSearch implementation](elastic-impl.md#testing), are expected to provide concrete subclasses of these
for the purposes of their own testing.

# Usage

Assuming you have a suitable concrete instance of `AbstractSearchCluster` the overall API is relatively simple:

1. Call `setup()` to start up a new cluster
2. Call `getIndexManager()` to get the `IndexManager` for the cluster if needed
3. Call `getHost()` and `getPort()` to get the connection details necessary to construct instances of the [Entity 
   Resolution Interface](er-api#interfaces) as needed.
4. Call `teardown()` to destroy the current cluster

Typically, the calls to `setup()` and `teardown()` would be done from test setup methods that are annotated 
appropriately
i.e. `@BeforeClass`/`@AfterClass`.

If you want to reuse a cluster across multiple tests within a class **BUT** reset the index between each test you can
call `resetIndex()` to do that, optionally supplying the name of the [Index
Configuration](managing-indices.md#predefined-configurations) that you want to reset the index with.  Note that if you
are going to do this you may want to place a small sleep after calling this as sometimes the cluster state does not
update immediately.

# Dependency

The core implementation is provided by the `test-clusters` module which can be depended on from Maven
like so:

```xml
<dependency>
    <groupId>io.telicent.smart-caches.entity-resolution</groupId>
    <artifactId>test-clusters</artifactId>
    <version>VERSION</version>
    <scope>test</scope>
</dependency>
```

You may additionally need the `entity-resolver-elastic` modules `tests` classifier in order to obtain concrete
implementations of the `AbstractSearchCluster` class for [testing against ElasticSearch](elastic-impl.md#testing):

```xml
<dependency>
    <groupId>io.telicent.smart-caches.entity-resolution</groupId>
    <artifactId>entity-resolver-elastic</artifactId>
    <version>VERSION</version>
    <classifer>tests</classifier>
    <scope>test</scope>
</dependency>
```


Where `VERSION` is the desired version, see the top level [README](../README.md) in this repository for that
information.

[1]: https://testcontainers.com
