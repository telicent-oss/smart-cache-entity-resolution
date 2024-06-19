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
package io.telicent.smart.cache.entity.resolver.server;

import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.configuration.sources.ConfigurationSource;
import io.telicent.smart.cache.configuration.sources.PropertiesSource;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.providers.ElasticsearchClientProvider;
import io.telicent.smart.caches.configuration.auth.AuthConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.Properties;

import static io.telicent.smart.cache.entity.resolver.server.EntityResolutionApplication.ADDITIONAL_CONFIG_ENABLED;

public class AbstractEntityResolutionApiDockerTests extends AbstractEntityResolutionApiServerTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityResolutionApiDockerTests.class);

    @BeforeClass
    @Override
    public void setupServers() throws Exception {
        Properties properties = new Properties();
        prepareTestConfiguration(properties);

        Configurator.setSingleSource(new PropertiesSource(properties));
        LOGGER.info("Reconfigured Configurator for test class {} with properties:\n{}",
                    this.getClass().getCanonicalName(), properties);

        super.setupServers();
    }

    /**
     * Prepares the configuration necessary for this test class
     *
     * @param properties Properties
     */
    protected void prepareTestConfiguration(Properties properties) {
        properties.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_HOST),
                       "localhost");
        properties.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_PORT),
                       System.getProperty("elastic.port", "19200"));
        properties.put(
                ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_SIMILARITY_INDEX),
                SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       AuthConstants.AUTH_DISABLED);
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                       AuthConstants.AUTH_DISABLED);
        properties.put(ConfigurationSource.asSystemPropertyKey(ADDITIONAL_CONFIG_ENABLED), "true");
    }

    @AfterClass
    @Override
    public void teardownServers() throws Exception {
        super.teardownServers();

        Configurator.reset();
        LOGGER.info("Reset to default Configurator after test class {}", this.getClass().getCanonicalName());
    }
}
