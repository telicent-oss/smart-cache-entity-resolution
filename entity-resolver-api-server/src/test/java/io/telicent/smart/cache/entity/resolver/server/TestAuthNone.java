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

import io.telicent.jena.abac.core.AttributesStoreAuthServer;
import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.configuration.sources.NullSource;
import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import io.telicent.smart.caches.configuration.auth.AuthConstants;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestAuthNone extends AbstractEntityResolutionApiServerTests {

    @BeforeClass
    @Override
    public void setupServers() {
        Configurator.setSingleSource(NullSource.INSTANCE);
        super.startApiServer();
    }

    @AfterClass
    @Override
    public void teardownServers() {
        super.stopApiServer();
        Configurator.reset();
    }

    @Test
    public void env_correct() {
        Assert.assertNull(Configurator.get(AuthConstants.ENV_JWKS_URL));
        Assert.assertNull(Configurator.get(AuthConstants.ENV_USER_ATTRIBUTES_URL));
    }

    @Test
    public void api_server_unhealthy() {
        WebTarget target = getHealth();
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            HealthStatus health = response.readEntity(HealthStatus.class);
            Assert.assertNotNull(health);
            Assert.assertFalse(health.isHealthy());
            Assert.assertTrue(StringUtils.isBlank((String) health.getConfig().get("jwtVerifier")));
            Assert.assertEquals(health.getConfig().get("attributesStore"),
                                AttributesStoreAuthServer.class.getCanonicalName());
        }
    }
}
