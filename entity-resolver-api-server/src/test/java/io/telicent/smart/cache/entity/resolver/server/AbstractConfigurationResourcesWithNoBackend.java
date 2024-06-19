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
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;
import java.util.UUID;

abstract class AbstractConfigurationResourcesWithNoBackend extends AbstractEntityResolutionApiServerTests {
    public static final String UNIQUE_ID = UUID.randomUUID().toString();

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

    public abstract String getType();

    public abstract String getEntry();

    @Test
    public void test_putCall_noBackend() {
        FormDataBodyPart bodyPart = new FormDataBodyPart("entry",
                                                         getEntry(),
                                                         MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(
                FormDataContentDisposition.name("entry").fileName("data.ndjson").build());
        try (FormDataMultiPart part = new FormDataMultiPart()) {
            try (MultiPart multiPart = part.bodyPart(bodyPart)) {
                WebTarget target = forApiServer("/" + getType() + "/" + UNIQUE_ID);
                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {
                    Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                }
            }
        } catch (Throwable t) {
            Assert.assertNotNull(t, "Test failed, exception should not be thrown");
        }
    }

    @Test
    public void test_postCall_noBackend() {
        FormDataBodyPart bodyPart = new FormDataBodyPart("entry",
                                                         getEntry(),
                                                         MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(
                FormDataContentDisposition.name("entry").fileName("data.ndjson").build());
        try (FormDataMultiPart part = new FormDataMultiPart()) {
            try (MultiPart multiPart = part.bodyPart(bodyPart)) {
                WebTarget target = forApiServer("/" + getType() + "/" + UNIQUE_ID);
                try (Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()))) {
                    Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                }
            }
        } catch (Throwable t) {
            Assert.assertNotNull(t, "Test failed, exception should not be thrown");
        }
    }

    @Test
    public void test_getCall_noBackend() {
        WebTarget target = forApiServer("/" + getType() + "/" + UNIQUE_ID);
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        } catch (Throwable t) {
            Assert.assertNotNull(t, "Test failed, exception should not be thrown");
        }
    }

    @Test
    public void test_getAllCall_noBackend() {
        WebTarget target = forApiServer("/" + getType() + "/");
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        } catch (Throwable t) {
            Assert.assertNotNull(t, "Test failed, exception should not be thrown");
        }
    }

    @Test
    public void test_deleteCall_noBackend() {
        WebTarget target = forApiServer("/" + getType() + "/" + UNIQUE_ID);
        try (Response response = target.request().delete()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        } catch (Throwable t) {
            Assert.assertNotNull(t, "Test failed, exception should not be thrown");
        }
    }

    @Test
    public void test_validateCall_noEntry_noBackend() {
        WebTarget target = forApiServer("validate/" + getType() + "/" + UNIQUE_ID + "/test-index");
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        } catch (Throwable t) {
            Assert.assertNotNull(t, "Test failed, exception should not be thrown");
        }
    }

    @AfterClass
    @Override
    public void teardownServers() {
        super.stopApiServer();
        Configurator.reset();
    }

}
