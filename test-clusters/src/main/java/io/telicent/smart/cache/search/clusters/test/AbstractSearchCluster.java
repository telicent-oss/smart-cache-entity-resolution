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
package io.telicent.smart.cache.search.clusters.test;

import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import io.telicent.smart.cache.search.IndexManager;
import io.telicent.smart.cache.search.configuration.CommonFieldTypes;
import io.telicent.smart.cache.search.configuration.IndexConfiguration;
import io.telicent.smart.cache.search.configuration.IndexConfigurations;
import io.telicent.smart.cache.search.configuration.SimpleIndexConfiguration;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;

import java.util.List;
import java.util.Properties;

/**
 * An abstract search test cluster
 *
 * @param <T> Search cluster container type
 */
public abstract class AbstractSearchCluster<T extends GenericContainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSearchCluster.class);

    /**
     * Fixed port to run the container on, or -1 to pick an arbitrary port
     */
    protected final int port;
    /**
     * Username to configure for the container
     */
    protected String username;
    /**
     * Password to configure for the container
     */
    protected String password;
    /**
     * If TLS/SSL connectivity is being used, the Base-64 encoded trusted CA Certificate of the search service
     */
    @Getter
    protected String elasticTlsCaCertString;
    /**
     * Container that runs the search cluster
     */
    protected T container = null;
    /**
     * Index manager for the test cluster
     */
    protected IndexManager manager = null;
    private int externalPort = -1;

    /**
     * Creates a new cluster
     *
     * @param port     Port to use, or {@code -1} for pick an arbitrary port
     * @param username Username, {@code null} for no/default username
     * @param password Password, {@code null} for no password
     */
    public AbstractSearchCluster(int port, String username, String password) {
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Setups the test cluster
     * <p>
     * Derived classes <strong>MUST</strong> override this method to create and start the necessary container storing it
     * into the {@code container} instance variable before calling {@code super.setup()} to finalise setup
     * </p>
     */
    public void setup() {
        if (this.container == null) {
            throw new RuntimeException(
                    "No container defined by derived implementation, please override setup() and define and start a container before calling this super method");
        }
        if (!this.container.isRunning()) {
            throw new RuntimeException(
                    "Container has already terminated, please ensure it is appropriately configured");
        }

        this.manager = createIndexManager(this.container.getHost(), this.container.getFirstMappedPort(), this.username,
                                          this.password, elasticTlsCaCertString);
    }

    /**
     * Creates a concrete instance of an Index Manager that can work with the cluster being tested using the given
     * connection details
     *
     * @param host     Host
     * @param port     Port
     * @param username Username
     * @param password Password
     * @param elasticTlsCaCert certificate as string
     * @return Index Manager
     */
    protected abstract IndexManager createIndexManager(String host, int port, String username, String password, String elasticTlsCaCert);

    /**
     * Updates the underlying index manager to use the given settings
     * @param host     Host
     * @param port     Port
     * @param username Username
     * @param password Password
     */
    public void resetIndexManager(String host, int port, String username, String password) {
        this.manager = createIndexManager(host, port, username, password, elasticTlsCaCertString);
    }

    /**
     * Resets the default test index
     */
    public void resetIndex() {
        resetIndex(SearchTestClusters.DEFAULT_TEST_INDEX);
    }

    /**
     * Resets a test index
     *
     * @param index Index to reset
     */
    public void resetIndex(String index) {
        if (this.manager == null) {
            Assert.fail("Failed to start up ElasticSearch test container");
        }
        if (Boolean.TRUE.equals(this.manager.hasIndex(index))) {
            this.manager.deleteIndex(index);
        }
        Assert.assertEquals(this.manager.createIndex(index,
                                                     getDefaultTestIndexConfiguration()),
                            true,
                            "Should have successfully created the index");
    }

    /**
     * Gets the default test index configuration to apply when (re)creating the test index
     *
     * @return Default index configuration
     */
    protected SimpleIndexConfiguration getDefaultTestIndexConfiguration() {
        return new SimpleIndexConfiguration(new Properties(),
                                            List.of(new SimpleMappingRule(
                                                            "topLevelSecurity",
                                                            DefaultOutputFields.SECURITY_LABELS,
                                                            CommonFieldTypes.NON_INDEXED),
                                                    new SimpleMappingRule(
                                                            "fineGrainedSecurity",
                                                            "*." + DefaultOutputFields.SECURITY_LABELS,
                                                            CommonFieldTypes.NON_INDEXED)));
    }

    /**
     * Resets a test index using a specific index configuration
     *
     * @param index           Index to reset
     * @param indexConfigName Index configuration name
     */
    public void resetIndex(String index, String indexConfigName) {
        if (this.manager == null) {
            Assert.fail("Failed to start up ElasticSearch test container");
        }
        if (this.manager.hasIndex(index)) {
            this.manager.deleteIndex(index);
        }
        IndexConfiguration<SimpleMappingRule> config =
                IndexConfigurations.load(indexConfigName, SimpleMappingRule.class);
        Assert.assertEquals(this.manager.createIndex(index, config), true,
                            "Should have successfully created the index");
    }

    /**
     * Tears down the test cluster
     */
    public void teardown() {
        if (this.container != null) {
            this.container.stop();
        }

    }

    /**
     * Provides an index manager for working with the cluster
     *
     * @return Index Manager
     */
    public IndexManager getIndexManager() {
        return this.manager;
    }

    /**
     * Gets the host that the cluster can be connected to on
     *
     * @return Hostname
     */
    public String getHost() {
        return this.externalPort > -1 ? "localhost" : this.container.getHost();
    }

    /**
     * Gets the port that the cluster can be connected to on
     *
     * @return Port
     */
    public int getPort() {
        return this.externalPort > -1 ? this.externalPort : this.container.getFirstMappedPort();
    }

    /**
     * Indicates to the test cluster that an existing external search cluster is being used
     *
     * @param port Port to use for the external search index
     */
    public void useExternal(int port) {
        this.externalPort = port;
        this.manager = this.createIndexManager("localhost", port, this.username, this.password, elasticTlsCaCertString);

        Boolean ready = this.manager.isReady();
        if (ready == null) {
            LOGGER.error("External search cluster (localhost:{}) did not return a valid status", port);
        } else if (ready) {
            LOGGER.info("External search cluster (localhost:{}) is ready", port);
        } else {
            LOGGER.warn("External search cluster (localhost:{}) is not ready", port);
        }
    }

    /**
     * Get the password used if any
     *
     * @return password or null if none was specified
     **/
    public String getPassword() {
        return password;
    }

    /**
     * Get the username used if any
     *
     * @return username or null if none was specified
     **/
    public String getUsername() {
        return username;
    }
}
