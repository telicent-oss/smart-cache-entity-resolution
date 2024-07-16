/**
 *   Copyright (c) Telicent Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.telicent.smart.cache.cli.options.search.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.RequiredUnlessEnvironment;
import com.github.rvesse.airline.annotations.restrictions.ranges.IntegerRange;
import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.SearchIndexer;
import io.telicent.smart.cache.search.SearchUtils;
import io.telicent.smart.cache.search.configuration.IndexConfigurations;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import io.telicent.smart.cache.search.elastic.AbstractElasticClient;
import io.telicent.smart.cache.search.elastic.ElasticIndexManager;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.elastic.compat.OpenSearchWithElasticIndexManager;
import io.telicent.smart.cache.search.elastic.utils.ElasticIndexMonitor;
import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Options for connecting to ElasticSearch
 */
public class ElasticSearchOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchOptions.class);
    /**
     * Environment variable used to specify ElasticSearch hostname
     */
    public static final String ELASTIC_HOST = "ELASTIC_HOST";
    /**
     * Environment variable used to specify ElasticSearch port
     */
    public static final String ELASTIC_PORT = "ELASTIC_PORT";
    /**
     * Environment variable used to specify ElasticSearch index
     */
    public static final String ELASTIC_INDEX = "ELASTIC_INDEX";
    /**
     * Environment variable used to specify ElasticSearch username
     */
    public static final String ELASTIC_USER = "ELASTIC_USER";
    /**
     * Environment variable used to specify ElasticSearch password
     */
    public static final String ELASTIC_PASSWORD = "ELASTIC_PASSWORD";
    /**
     * Environment variable used to specify whether OpenSearch compatibility mode should be enabled
     */
    public static final String OPENSEARCH_COMPATIBILITY = "OPENSEARCH_COMPATIBILITY";

    private ElasticIndexManager manager;

    @Option(name = "--elastic-host", title = "ElasticSearchHostname", description = "Specifies the ElasticSearch hostname for the ElasticSearch server/cluster to connect to.")
    @RequiredUnlessEnvironment(variables = ELASTIC_HOST)
    String host = Configurator.get(ELASTIC_HOST);

    @Option(name = "--elastic-user", title = "ElasticSearchUsername", description = "Specifies the ElasticSearch username for the ElasticSearch server/cluster to connect to (default 'elastic'). The value can also be set using the environment variable ELASTIC_USER")
    String user = Configurator.get(ELASTIC_USER, v -> v, AbstractElasticClient.DEFAULT_ELASTICSEARCH_USERNAME);

    @Option(name = "--elastic-password", title = "ElasticSearchPassword", description = "Specifies the ElasticSearch password for the ElasticSearch server/cluster to connect to.  The value can also be set using the environment variable ELASTIC_PASSWORD.")
    String password = Configurator.get(ELASTIC_PASSWORD);

    @Option(name = "--elastic-port", title = "ElasticSearchPort", description = "Specifies the ElasticSearch port for the ElasticSearch server/cluster to connect to.")
    int port = Configurator.get(ELASTIC_PORT, Integer::parseInt, 9200);

    @Option(name = {
            "--opensearch-compatibility", "--no-opensearch-compatibility"
    }, arity = 0, description = "Specifies that the ElasticSearch clients should be configured in such a way as to make them compatible with OpenSearch servers.  ElasticSearch Client APIs are still used but the HTTP client is modified to adjust some headers so that the APIs can communicate with OpenSearch servers successfully.  Defaults to disabled.")
    boolean makeOpenSearchCompatible = Configurator.get(OPENSEARCH_COMPATIBILITY, Boolean::parseBoolean, false);

    @Option(name = "--index", title = "ElasticSearchIndex", description = "Specifies the ElasticSearch index upon which operations operate.")
    @RequiredUnlessEnvironment(variables = ELASTIC_INDEX)
    String index = Configurator.get(ELASTIC_INDEX);

    @Option(name = "--recreate-index", arity = 0, description = "Specifies that the ElasticSearch index upon which operations operate should be dropped and recreated.")
    boolean recreateIndex = false;

    @Option(name = "--elastic-max-connect-attempts", title = "MaxConnectionAttempts", description = "Specifies the maximum number of times to attempt to connect to ElasticSearch.  If a successful connection cannot be established within this number of attempts then the process will abort.  Default is 10.")
    @IntegerRange(min = 1)
    int maxConnectAttempts = 10;

    @Option(name = "--elastic-min-connect-interval", title = "MinConnectInterval", description = "Specifies the minimum amount of time between ElasticSearch connection attempts in seconds.  Default is 5 seconds")
    @IntegerRange(min = 1)
    int minConnectInterval = 5;

    @Option(name = "--elastic-max-connect-interval", title = "MaxConnectInterval", description = "Specifies the maximum amount of time between ElasticSearch connection attempts in seconds.  Default is 30 seconds.")
    @IntegerRange(min = 5)
    int maxConnectInterval = 30;

    @Option(name = "--elastic-monitor-interval", title = "MonitorInterval", description = "Specifies how often we check with ElasticSearch whether the underlying index has been removed/modified.  Default is 60 seconds.")
    @IntegerRange(min = 5)
    int monitorInterval = 60;

    @Option(name = "--elastic-max-retries", title = "MaxRetries", description = "Specifies the maximum number of retries for non-connection related ElasticSearch operations.  If an operation is not successful within this number of retries then the process will abort.  Default is 3.")
    @IntegerRange(min = 1)
    int maxRetries = 3;

    @Option(name = {
            "--upsert", "--no-upsert"
    }, description = "Specifies whether documents are indexed into ElasticSearch via upserts i.e. modifying existing documents rather than each generated document completely overwriting any previous document.  Default mode is upsert.")
    boolean upsert = true;

    /**
     * Default options constructor, assumes that this options instance will be populated by Airline's CLI injection
     */
    public ElasticSearchOptions() {
        // Nothing to do
    }

    /**
     * Custom options constructor for allowing options to be manually constructed for testing or non-Airline usage
     *
     * @param host     Host
     * @param port     Port
     * @param index    Index
     * @param user     Username
     * @param password Password
     */
    public ElasticSearchOptions(String host, int port, String index, String user, String password,
                                boolean makeOpenSearchCompatible) {
        this.host = host;
        this.port = port;
        this.index = index;
        this.user = user;
        this.password = password;
        this.makeOpenSearchCompatible = makeOpenSearchCompatible;
    }

    /**
     * Prepares an ElasticSearch backed {@link SearchIndexer}
     *
     * @param indexConfigName     The name of the indexing configuration to use
     * @param updateScriptBuilder A function that can build ElasticSearch update scripts for update operations.  If not
     *                            specified content updates will still be supported <strong>BUT</strong> certain updates
     *                            may cause some data loss/overwriting.
     * @param deleteScriptBuilder A function that can build ElasticSearch delete scripts for delete operations. If not
     *                            specified content deletions will not be supported by the resulting indexer.
     * @param <T>                 Item type
     * @return ElasticSearch Indexer
     */
    public <T> SearchIndexer<T> prepareElasticIndexer(String indexConfigName, Function<T, Script> updateScriptBuilder,
                                                      Function<T, Script> deleteScriptBuilder) {
        if (!IndexConfigurations.supported(indexConfigName, SimpleMappingRule.class)) {
            throw new SearchException("Specified index configuration " + indexConfigName + " is not supported");
        }

        prepareElasticManager();

        // ElasticSearch might not be up yet so wait for it to come up
        // Do this by submitting a task on a background thread as this allows us to reliably respond to interrupts and
        // cancel the ongoing wait, otherwise if we wait directly on our main thread the application won't respond to
        // interrupts cleanly, and it's impossible to abort the application during this startup phase
        ExecutorService executor = null;
        try {
            executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                SearchUtils.waitForReady(this.manager, this.maxConnectAttempts, this.minConnectInterval,
                                         this.maxConnectInterval);
            });
            try {
                ExecutorService finalExecutor = executor;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    future.cancel(true);
                    finalExecutor.shutdownNow();
                }));
                future.get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof SearchException) {
                    throw (SearchException) e.getCause();
                } else {
                    throw new SearchException("Failed waiting for Search backend to become ready", e);
                }
            } catch (InterruptedException e) {
                future.cancel(true);
                SearchUtils.notWaitingDueToShutdown(this.manager);
            }
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }


        SearchUtils.waitForReady(this.manager, this.maxConnectAttempts, this.minConnectInterval,
                                 this.maxConnectInterval);

        // Try to create the index if needed
        Boolean exists = this.manager.hasIndex(this.index);
        if (Boolean.FALSE.equals(exists) || this.recreateIndex) {
            if (this.recreateIndex && Boolean.TRUE.equals(exists)) {
                LOGGER.warn("Dropping ElasticSearch index {} as requested", this.index);
                this.manager.deleteIndex(this.index);
            }

            LOGGER.info("Attempting to create required ElasticSearch index {} ", this.index);
            LOGGER.info("Using Index Configuration '{}'", indexConfigName);
            this.manager.createIndex(this.index, IndexConfigurations.load(indexConfigName, SimpleMappingRule.class));
        } else {
            LOGGER.debug("ElasticSearch index {} already exists", this.index);
        }

        return ElasticSearchIndexer.<T>create()
                                   .index(this.index)
                                   .onIndexBehaviour(this.upsert)
                                   .updatingContentsWith(updateScriptBuilder)
                                   .deletingContentsWith(deleteScriptBuilder)
                                   .withCredentials(this.user, this.password, null)
                                   .maxRetries(this.maxRetries)
                                   .withOpenSearchCompatibility(makeOpenSearchCompatible)
                                   .host(this.host)
                                   .port(this.port)
                                   .build();
    }

    /**
     * Prepares the Elastic Index Manager (or returns the existing instance thereof)
     *
     * @return Index Manager
     */
    public synchronized ElasticIndexManager prepareElasticManager() {
        if (this.manager == null) {
            this.manager = makeOpenSearchCompatible ?
                           new OpenSearchWithElasticIndexManager(this.host, this.port, this.user, this.password, null) :
                           new ElasticIndexManager(this.host, this.port, this.user, this.password);
        }
        return this.manager;
    }

    /**
     * Creates a monitor for the target index
     *
     * @return Index monitor
     */
    public ElasticIndexMonitor createMonitor() {
        return new ElasticIndexMonitor(this.manager, this.index, false, Duration.ofSeconds(this.monitorInterval));
    }

    /**
     * Creates a low level ElasticSearch Client, useful for debug tools that need to bypass our higher level APIs
     *
     * @return Low level client
     */
    public ElasticsearchClient createLowLevelClient() {
        return AbstractElasticClient.buildElasticClient(this.host, this.port, this.user, this.password,
                                                        this.makeOpenSearchCompatible);
    }

    /**
     * Gets the index that the user has supplied in their options
     *
     * @return Index
     */
    public String getIndex() {
        return this.index;
    }

    /**
     * Gets a list of libraries whose version information should be presented by the health probe servers liveness
     * endpoint
     *
     * @return Health Probe Libraries
     */
    public String[] getHealthProbeLibraries() {
        return new String[] { "cli-elastic-index", "search-index-elastic", "search-api" };
    }

    /**
     * Gets a health probe supplier function that uses the underlying {@link ElasticIndexManager} to determine the
     * application readiness status
     *
     * @return Health Probe supplier function
     */
    public Supplier<HealthStatus> getHealthProbeSupplier() {
        this.prepareElasticManager();
        return () -> {
            Boolean ready = manager.isReady();
            if (ready == null) {
                return HealthStatus.builder()
                                   .healthy(false)
                                   .reasons(
                                           List.of("Failed to obtain readiness status from underlying Index Manager " + manager.toString()))
                                   .build();
            } else if (ready) {
                return HealthStatus.builder().healthy(true).build();
            } else {
                return HealthStatus.builder()
                                   .healthy(false)
                                   .reasons(
                                           List.of("Underlying Index Manager (" + manager.toString() + ") indicated it is not ready"))
                                   .build();
            }
        };
    }
}
