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

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

public abstract class AbstractConfigurationResourceTests extends AbstractEntityResolutionApiDockerTests {

    public static final String UNIQUE_ID = UUID.randomUUID().toString();

    @BeforeClass
    @Override
    public void setupServers() throws Exception {
        super.setupServers();
        // give it a bit of time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract String getType();

    public String getPathType() {
        return getType();
    }

    public abstract String getEntry();

    public abstract String getExpectedResult();

    public abstract String getUpdatedEntry();

    public abstract String getExpectedUpdatedResult();

    public String getIdOnlyEntry() {
        return "{\"id\":\"" + UNIQUE_ID + "\"}";
    }

    public int getInvalidStatusCode() {
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Test
    public void test_putCall() throws IOException {
        // Add entry
        test_postCall();

        // Update entry
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request()
                                       .put(Entity.entity(getUpdatedEntry(), MediaType.APPLICATION_JSON_TYPE))) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "Updated " + UNIQUE_ID);
        }

        // Get Changed entry
        target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertNotNull(results);
            Assert.assertNotEquals(results, "");
            Assert.assertNotEquals(results, getExpectedResult());
            Assert.assertEquals(results, getExpectedUpdatedResult());
        }
    }


    @Test
    public void test_putInvalidCall() throws IOException {
        String MISSING_ID = UUID.randomUUID().toString();
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + MISSING_ID);
        try (Response response = target.request()
                                       .put(Entity.entity("{\"test\":\"break\"}",
                                                          MediaType.APPLICATION_JSON_TYPE))) {
            Assert.assertEquals(response.getStatus(), getInvalidStatusCode());
        }
    }

    @Test
    public void test_putEmptyUpdateCall() throws IOException {
        test_postCall();

        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request()
                                       .put(Entity.entity(getIdOnlyEntry(),
                                                          MediaType.APPLICATION_JSON_TYPE))) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "Updated " + UNIQUE_ID);
        }

        test_getCall_entry();
    }

    @Test
    public void test_postCall() throws IOException {
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request()
                                       .post(Entity.entity(getEntry(), MediaType.APPLICATION_JSON_TYPE))) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "Created " + UNIQUE_ID);
        }
    }

    @Test
    public void test_postInvalidCall() throws IOException {
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request()
                                       .post(Entity.entity("{\"rubbish\":\"test\"}",
                                                           MediaType.APPLICATION_JSON_TYPE))) {
            Assert.assertEquals(response.getStatus(), getInvalidStatusCode());
        }
    }

    @Test
    public void test_getCall_noEntry() {
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID + "missing");
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "");
        }
    }

    @Test
    public void test_getAllCall_noEntry() {
        WebTarget target = forApiServer("/config/" + getPathType() + "/");
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "{}");
        }
    }

    @Test
    public void test_deleteCall_noEntry() {
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request().delete()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "Deleted " + UNIQUE_ID);
        }
    }

    @Test
    public void test_getCall_validate_noEntry() {
        WebTarget target = forApiServer("/config/validate/" + getType() + "/" + UNIQUE_ID + "/index");
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "");
        }
    }

    @Test
    public void test_getCall_happyPath() throws IOException {
        test_postCall();
        test_getCall_entry();
    }

    @Test
    public void test_deleteCall_happyPath() throws IOException {
        test_postCall();
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request().delete()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertEquals(results, "Deleted " + UNIQUE_ID);
        }
        test_getCall_noEntry();
    }

    private void test_getCall_entry() {
        WebTarget target = forApiServer("/config/" + getPathType() + "/" + UNIQUE_ID);
        try (Response response = target.request().get()) {
            Assert.assertEquals(response.getStatus(), 200);
            String results = response.readEntity(String.class);
            Assert.assertNotNull(results);
            Assert.assertNotEquals(results, "");
            Assert.assertEquals(results, getExpectedResult());
        }
    }
}
