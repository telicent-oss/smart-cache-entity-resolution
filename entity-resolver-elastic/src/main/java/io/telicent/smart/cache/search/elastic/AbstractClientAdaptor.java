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
package io.telicent.smart.cache.search.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import io.telicent.smart.cache.search.SearchException;

/**
 * Temporary workaround for the fact that we want to put
 * {@link io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver} in its own package, but it relies
 * on package protected methods and fields in the {@link AbstractElasticClient}.  This can be removed once the upstream
 * Smart Cache Search code is adjusted to make this unnecessary.
 */
public abstract class AbstractClientAdaptor extends AbstractElasticClient {
    /**
     * Creates a new client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param username                 ElasticSearch username
     * @param password                 ElasticSearch password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     * @param makeOpenSearchCompatible If {@code true} then the HTTP Client configuration will be customised to make the
     *                                 client "compatible", in so far as is possible, with OpenSearch servers
     */
    public AbstractClientAdaptor(String elasticHost, int elasticPort, String username, String password,
                                 String elasticTlsCaCert, boolean makeOpenSearchCompatible) {
        super(elasticHost, elasticPort, username, password, elasticTlsCaCert, makeOpenSearchCompatible);
    }

    /**
     * Gets the underlying ElasticSearch client
     *
     * @return ElasticSearch client
     */
    protected final ElasticsearchClient getClient() {
        return this.client;
    }

    /**
     * Translates an ElasticSearch exception into a Search exception
     *
     * @param e      ElasticSearch exception
     * @param action Action that was being attempted
     * @return Search exception
     */
    public static SearchException fromElasticException(ElasticsearchException e, String action) {
        return AbstractElasticClient.fromElasticException(e, action);
    }
}
