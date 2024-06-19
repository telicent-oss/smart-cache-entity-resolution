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
package io.telicent.smart.cache.search.clusters.test.elastic;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An extension of the standard {@link ElasticsearchContainer} that allows for configuring the cluster to be exposed on
 * a fixed port
 */
public class FixedPortElasticsearchContainer extends ElasticsearchContainer {

    /**
     * Creates a new ElasticSearch container
     *
     * @param imageName Image name
     * @param hostPort  Fixed host port to expose the cluster on, or {@code -1} to not use a fixed port
     */
    public FixedPortElasticsearchContainer(DockerImageName imageName, int hostPort) {
        super(imageName);
        if (hostPort != -1) {
            this.addFixedExposedPort(hostPort, 9200);
        }
    }
}
