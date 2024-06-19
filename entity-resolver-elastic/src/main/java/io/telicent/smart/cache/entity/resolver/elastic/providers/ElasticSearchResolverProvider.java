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
package io.telicent.smart.cache.entity.resolver.elastic.providers;

import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver;
import io.telicent.smart.cache.entity.resolver.elastic.index.CachedIndexMapper;
import io.telicent.smart.cache.entity.resolver.providers.EntityResolverProvider;
import io.telicent.smart.cache.search.SearchException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver.DEFAULT_NAME_SIMILARITY_INDEX;
import static io.telicent.smart.cache.search.elastic.providers.ElasticsearchClientProvider.*;

/**
 * A provider of ElasticSearch backed {@link EntityResolver}'s
 */
public class ElasticSearchResolverProvider implements EntityResolverProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchResolverProvider.class);

    @Override
    public Boolean supports() {
        return StringUtils.isNoneBlank(Configurator.get(ENV_ELASTIC_HOST),
                                       Configurator.get(ENV_ELASTIC_SIMILARITY_INDEX));
    }

    @Override
    public String[] minimumRequiredConfiguration() {
        return new String[]{ENV_ELASTIC_HOST, ENV_ELASTIC_SIMILARITY_INDEX};
    }

    @Override
    public EntityResolver load() {
        // Retrieve current configuration
        // MUST happen in load() method as otherwise this breaks tests where they set different configurations
        final String elasticHost = Configurator.get(ENV_ELASTIC_HOST);
        final String elasticPort = Configurator.get(ENV_ELASTIC_PORT);
        final String elasticSimilarityIndex = Configurator.get(ENV_ELASTIC_SIMILARITY_INDEX);
        final String elasticUser = Configurator.get(ENV_ELASTIC_USER);
        final String elasticPassword = Configurator.get(ENV_ELASTIC_PASSWORD);
        final boolean openSearchCompatibility =
                StringUtils.equalsIgnoreCase(Configurator.get(ENV_OPENSEARCH_COMPATIBILITY), "true");
        try {
            final int port = StringUtils.isNotBlank(elasticPort) ? Integer.parseInt(elasticPort) : 9200;
            final String similarityIndex = StringUtils.isNotBlank(elasticSimilarityIndex) ? elasticSimilarityIndex
                                           : DEFAULT_NAME_SIMILARITY_INDEX;

            CachedIndexMapper.load(Configurator.get(ENV_CANONICAL_CONFIG));
            LOGGER.info("Using ElasticSearch Entity Resolver with index {} on host {}:{}",
                        similarityIndex, elasticHost, port);
            return ElasticSearchEntityResolver.builder()
                                              .username(elasticUser)
                                              .password(elasticPassword)
                                              .host(elasticHost)
                                              .port(port)
                                              .similarityIndex(similarityIndex)
                                              .makeOpenSearchCompatible(openSearchCompatibility)
                                              .build();

        } catch (NumberFormatException e) {
            LOGGER.error("Bad ElasticSearch port configuration, expected a valid number but got {}", elasticPort);
            throw new SearchException(
                    "ELASTIC_PORT variable does not provide a valid port number to connect to ElasticSearch on");
        }
    }
}
