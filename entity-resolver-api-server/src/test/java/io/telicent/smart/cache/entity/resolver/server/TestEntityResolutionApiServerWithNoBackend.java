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
import io.telicent.smart.caches.configuration.auth.AuthConstants;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;

public class TestEntityResolutionApiServerWithNoBackend extends AbstractNoBackendTests {

    @BeforeClass
    @Override
    public void setupServers() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       AuthConstants.AUTH_DISABLED);
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                       AuthConstants.AUTH_DISABLED);
        Configurator.setSingleSource(new PropertiesSource(properties));

        super.startApiServer();

        // Make sure ElasticSearch really isn't running
        super.stopElasticSearch();
    }

    @AfterClass
    @Override
    public void teardownServers() {
        super.stopApiServer();
        Configurator.reset();
    }

    @Test
    public void env_correct() {
        Assert.assertNull(Configurator.get("ELASTIC_HOST"),
                          "ELASTIC_HOST environment variable MUST NOT be set for these tests, if it is set in your environment please unset it OR run the tests from a fresh console window");
        Assert.assertNull(Configurator.get("ELASTIC_INDEX"),
                          "ELASTIC_INDEX environment variable MUST NOT be set for these tests, if it is set in your environment please unset it OR run the tests from a fresh console window");
        Assert.assertTrue(
                StringUtils.equals(Configurator.get(AuthConstants.ENV_JWKS_URL), AuthConstants.AUTH_DISABLED));
        Assert.assertTrue(StringUtils.equals(Configurator.get(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                                             AuthConstants.AUTH_DISABLED));
    }


}
