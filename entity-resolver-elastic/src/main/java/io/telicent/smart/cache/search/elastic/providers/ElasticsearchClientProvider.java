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
package io.telicent.smart.cache.search.elastic.providers;

import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.search.SearchClient;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.elastic.ElasticSearchClient;
import io.telicent.smart.cache.search.elastic.compat.OpenSearchWithElasticClient;
import io.telicent.smart.cache.search.providers.SearchClientProvider;
import io.telicent.smart.cache.search.security.RedactedDocumentsCache;
import io.telicent.smart.cache.search.security.RedactedDocumentsConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * A provider of search clients backed by ElasticSearch
 */
public class ElasticsearchClientProvider implements SearchClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchClientProvider.class);
    /**
     * Environment variable for specifying the ElasticSearch host to connect to
     */
    public static final String ENV_ELASTIC_HOST = "ELASTIC_HOST";
    /**
     * Environment variable for specifying the ElasticSearch port to connect to
     */
    public static final String ENV_ELASTIC_PORT = "ELASTIC_PORT";
    /**
     * Environment variable for specifying the ElasticSearch index to connect to
     */
    public static final String ENV_ELASTIC_INDEX = "ELASTIC_INDEX";
    /**
     * Environment variable for specifying the ElasticSearch index to connect to for similarity requests
     */
    public static final String ENV_ELASTIC_SIMILARITY_INDEX = "ELASTIC_SIMILARITY_INDEX";
    /**
     * Environment variable for specifying the ElasticSearch username to use
     */
    public static final String ENV_ELASTIC_USER = "ELASTIC_USER";
    /**
     * Environment variable for specifying the ElasticSearch password to use
     */
    public static final String ENV_ELASTIC_PASSWORD = "ELASTIC_PASSWORD";
    /**
     * Environment variable indicating whether to enable OpenSearch compatibility mode
     */
    public static final String ENV_OPENSEARCH_COMPATIBILITY = "OPENSEARCH_COMPATIBILITY";

    /**
     * Environment variable specifying path for canonical configuration
     */
    public static final String ENV_CANONICAL_CONFIG = "CANONICAL_CONFIG";

    @Override
    public Boolean supports() {
        return StringUtils.isNoneBlank(Configurator.get(ENV_ELASTIC_HOST), Configurator.get(ENV_ELASTIC_INDEX));
    }

    @Override
    public String[] minimumRequiredConfiguration() {
        return new String[]{ENV_ELASTIC_HOST, ENV_ELASTIC_INDEX};
    }

    @Override
    public SearchClient load() {
        // Retrieve current configuration
        // MUST happen in load() method as otherwise this breaks tests where they set different configurations
        final String elasticHost = Configurator.get(ENV_ELASTIC_HOST);
        final String elasticPort = Configurator.get(ENV_ELASTIC_PORT);
        final List<String> elasticIndices = getEnvCsv(ENV_ELASTIC_INDEX);
        final String elasticUser = Configurator.get(ENV_ELASTIC_USER);
        final String elasticPassword = Configurator.get(ENV_ELASTIC_PASSWORD);
        final boolean openSearchCompatibility =
                StringUtils.equalsIgnoreCase(Configurator.get(ENV_OPENSEARCH_COMPATIBILITY), "true");
        try {
            final int port = StringUtils.isNotBlank(elasticPort) ? Integer.parseInt(elasticPort) : 9200;
            LOGGER.info("Using ElasticSearch Client with indices {} on host {}:{}", elasticIndices, elasticHost, port);

            // Get the redacted documents cache, if any
            RedactedDocumentsCache cache = RedactedDocumentsConfiguration.tryCreateFromConfiguration();
            if (cache != null) {
                LOGGER.info("Using redacted documents cache {}", cache);
            }

            if (openSearchCompatibility) {
                LOGGER.info("Enabled OpenSearch compatibility mode");
                return build(OpenSearchWithElasticClient.builder(), elasticUser, elasticPassword, elasticHost, port,
                             elasticIndices, cache);
            } else {
                return build(ElasticSearchClient.builder(), elasticUser, elasticPassword, elasticHost, port,
                             elasticIndices, cache);
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Bad ElasticSearch port configuration, expected a valid number but got {}", elasticPort);
            throw new SearchException(
                    "ELASTIC_PORT variable does not provide a valid port number to connect to ElasticSearch on");
        }
    }

    private static ElasticSearchClient build(ElasticSearchClient.ElasticSearchClientBuilder<?, ?> builder,
                                             String elasticUser, String elasticPassword, String elasticHost, int port,
                                             List<String> elasticIndices, RedactedDocumentsCache cache) {
        return builder.username(elasticUser)
                      .password(elasticPassword)
                      .host(elasticHost)
                      .port(port)
                      .indices(elasticIndices)
                      .redactedDocumentsCache(cache)
                      .build();
    }

    private List<String> getEnvCsv(final String key) {
        final String val = Configurator.get(key);
        return val == null ? null : asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(val, ","));
    }
}
