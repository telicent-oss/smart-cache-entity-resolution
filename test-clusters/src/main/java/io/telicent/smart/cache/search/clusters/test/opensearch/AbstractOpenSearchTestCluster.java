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
package io.telicent.smart.cache.search.clusters.test.opensearch;

import io.telicent.smart.cache.search.clusters.test.AbstractSearchCluster;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.Assert;

import java.time.Duration;

/**
 * Provides a test OpenSearch cluster using Docker Test Containers
 */
public abstract class AbstractOpenSearchTestCluster extends AbstractSearchCluster<OpensearchContainer> {

    /**
     * Default image to use for OpenSearch clusters
     */
    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("opensearchproject/opensearch:1.3.10");

    /**
     * Creates a new OpenSearch cluster
     *
     * @param port Port, or {@code -1} to pick an arbitrary port
     */
    public AbstractOpenSearchTestCluster(int port) {
        super(port, null, null);
    }

    @Override
    public void setup() {
        this.container =
                (OpensearchContainer) new FixedPortOpenSearchContainer(DEFAULT_IMAGE, this.port).withStartupTimeout(Duration.ofSeconds(180));
        // TODO Can we support these with OpenSearch?  Note that OpensearchContainer disabled security by default
        if (password != null) {
            Assert.fail("Password protection not currently supported for OpenSearch Test cluster");
            //this.container.withPassword(password);
        }
        if (username != null) {
            Assert.fail("Username not currently supported for OpenSearch Test Cluster");
            //this.container.withEnv("ELASTIC_USER", username);
        }

        this.container.start();
        Assert.assertTrue(this.container.isRunning(), "ElasticSearch failed to start up in time");

        super.setup();
    }
}
