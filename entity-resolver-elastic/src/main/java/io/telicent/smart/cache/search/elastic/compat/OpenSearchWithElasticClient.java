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
package io.telicent.smart.cache.search.elastic.compat;

import io.telicent.smart.cache.search.elastic.ElasticSearchClient;
import io.telicent.smart.cache.search.security.RedactedDocumentsCache;

import java.util.List;

/**
 * A variant on the normal {@link ElasticSearchClient} that configures the ElasticSearch client to be "compatible", in
 * so far as possible, with OpenSearch servers
 */
public class OpenSearchWithElasticClient extends ElasticSearchClient {

    /**
     * Creates a new OpenSearch backed search client that uses Elasticsearch Client APIs
     *
     * @param host     Host
     * @param port     Port
     * @param indices  The indexes for searching.
     * @param user     User
     * @param password Password
     * @param cache    Redacted documents cache
     */
    OpenSearchWithElasticClient(String host, int port, List<String> indices, String user, String password,
                                RedactedDocumentsCache cache) {
        super(host, port, indices, user, password, null, true, cache);
    }

    /**
     * Builds a new OpenSearch compatible client.
     *
     * @return the newly built OpenSearch compatible client.
     */
    public static ElasticSearchClientBuilder<?, ?> builder() {
        return new OpenSearchClientBuilderImpl();
    }

    private static final class OpenSearchClientBuilderImpl
            extends ElasticSearchClientBuilder<OpenSearchWithElasticClient, OpenSearchClientBuilderImpl> {
        private OpenSearchClientBuilderImpl() {
        }

        protected OpenSearchClientBuilderImpl self() {
            return this;
        }

        @Override
        public OpenSearchWithElasticClient build() {
            return new OpenSearchWithElasticClient(elasticHost, elasticPort, indices, username,
                                                   password, redactedDocumentsCache);
        }
    }
}
