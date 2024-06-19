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

import io.telicent.smart.cache.search.elastic.ElasticIndexManager;

/**
 * A variant on the normal {@link ElasticIndexManager} that configures the ElasticSearch client to be "compatible", in
 * so far as possible, with OpenSearch servers
 */
public class OpenSearchWithElasticIndexManager extends ElasticIndexManager {

    /**
     * Creates a new OpenSearch backed index manager that uses Elasticsearch Client APIs
     *
     * @param host     Host
     * @param port     Port
     * @param username User
     * @param password Password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     */
    public OpenSearchWithElasticIndexManager(String host, int port, String username, String password, String elasticTlsCaCert) {
        super(host, port, username, password, elasticTlsCaCert, true);
    }
}
