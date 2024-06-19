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

import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public abstract class AbstractNoBackendTests extends AbstractEntityResolutionApiServerTests {

    @Test
    public void api_server_unhealthy() {
        WebTarget target = getHealth();
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            HealthStatus health = response.readEntity(HealthStatus.class);
            Assert.assertNotNull(health);
            Assert.assertFalse(health.isHealthy());
        }
    }

    @Test
    public void api_server_similarity_put() {
        FormDataBodyPart bodyPart = new FormDataBodyPart("file", "Hello, World", MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.txt").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            try (MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart)) {
                WebTarget target = forApiServer("/similarity");
                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {
                    Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                }
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Should never occur");
        }
    }
}
