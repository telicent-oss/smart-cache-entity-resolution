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

import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class DockerEntityResolutionApiSimilarityResourceTest extends AbstractEntityResolutionApiDockerTests {

    public static final File PEOPLE_SAMPLE_DATA = new File("../test_data/people-data.json");

    @BeforeClass
    @Override
    public void setupServers() throws Exception {
        super.setupServers();
        createIndexAndImportSampleData(PEOPLE_SAMPLE_DATA, SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        // give it a bit of time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void givenApiServer_whenQueryingHealthStatus_thenHealthyIsReturned() {
        WebTarget health = getHealth();
        HealthStatus status = health.request(MediaType.APPLICATION_JSON).get(HealthStatus.class);
        Assert.assertTrue(status.isHealthy());
    }

    @Test
    public void similarity() {

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);

        FormDataBodyPart bodyPart = new FormDataBodyPart("file",
                                                         "{ \"first_name\": \"Triphon\", \"last_name\": \"Tournesol\", \"id\": \"Tournesol\"}",
                                                         MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.ndjson").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            try (MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart)) {

                WebTarget target = forApiServer("/similarity");

                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {

                    Assert.assertEquals(response.getStatus(), 200);

                    SimilarityResults results = response.readEntity(SimilarityResults.class);

                    // one doc sent as input - one output for it
                    Assert.assertEquals(results.getResults().size(), 1);

                    SimilarityResult result = results.getResults().get(0);

                    // expecting results for Tournesol
                    // despite the slight variation in first name
                    Assert.assertEquals(result.getIDSourceEntity(), "Tournesol");
                    Assert.assertEquals(result.getHits().length, 1);
                    Assert.assertEquals(result.getHits()[0].getId(), "6");
                }
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Test failed, exception should not be thrown");
        }
    }

    @Test
    public void noSimilarityInBatch() {

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);

        String json = """
                        { "first_name": "Mohamed", "last_name": "Ali", "id": "firstEntry" }
                        { "first_name": "Mohamed", "last_name": "Ali", "id": "secondEntry" }
                """;

        FormDataBodyPart bodyPart = new FormDataBodyPart("file", json, MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.ndjson").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart);

            WebTarget target = forApiServer("/similarity");

            try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {

                Assert.assertEquals(response.getStatus(), 200);

                SimilarityResults results = response.readEntity(SimilarityResults.class);

                // 2 docs sent as input - 2 outputs for it
                Assert.assertEquals(results.getResults().size(), 2);

                SimilarityResult result1 = results.getResults().get(0);

                // should not match each other
                Assert.assertEquals(result1.getIDSourceEntity(), "firstEntry");
                Assert.assertEquals(result1.getHits().length, 0);

                SimilarityResult result2 = results.getResults().get(1);

                // should not match each other
                Assert.assertEquals(result2.getIDSourceEntity(), "secondEntry");
                Assert.assertEquals(result2.getHits().length, 0);
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Should never occur");
        }
    }

    @Test
    public void similarityInBatch() {
        // Re-populate index to clear out previous entries
        createIndexAndImportSampleData(PEOPLE_SAMPLE_DATA, SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);

        String json = """
                        { "first_name": "Mohamed", "last_name": "Ali", "id": "firstEntry"}
                        { "first_name": "Mohamed", "last_name": "Ali", "id": "secondEntry"}
                """;

        FormDataBodyPart bodyPart = new FormDataBodyPart("file", json, MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.ndjson").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            try (MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart)) {

                WebTarget target = forApiServer("/similarity").queryParam("withinInput", "true");

                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {

                    Assert.assertEquals(response.getStatus(), 200);

                    SimilarityResults results = response.readEntity(SimilarityResults.class);

                    // 2 docs sent as input - 2 outputs for it
                    Assert.assertEquals(results.getResults().size(), 2);

                    SimilarityResult result1 = results.getResults().get(0);

                    // should match each other when withinInput is true
                    Assert.assertEquals(result1.getIDSourceEntity(), "firstEntry");
                    Assert.assertEquals(result1.getHits().length, 1);
                    Assert.assertEquals(result1.getHits()[0].getDocument().getProperty("originalId"), "secondEntry");

                    SimilarityResult result2 = results.getResults().get(1);

                    // should match each other when withinInput is true
                    Assert.assertEquals(result2.getIDSourceEntity(), "secondEntry");
                    Assert.assertEquals(result2.getHits().length, 1);
                    Assert.assertEquals(result2.getHits()[0].getDocument().getProperty("originalId"), "firstEntry");
                }
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Should never occur");
        }
    }

    @Test
    public void similarity2Hits() {

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);

        FormDataBodyPart bodyPart = new FormDataBodyPart("file",
                                                         "{ \"first_name\": \"Truphon\", \"last_name\": \"Tournesil\", \"id\": \"Tournesol\"}",
                                                         MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.ndjson").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            try (MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart)) {

                WebTarget target = forApiServer("/similarity").queryParam("maxResults", "2");

                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {

                    Assert.assertEquals(response.getStatus(), 200);

                    SimilarityResults results = response.readEntity(SimilarityResults.class);

                    // one doc sent as input so expecting one output
                    Assert.assertEquals(results.getResults().size(), 1);
                    SimilarityResult result = results.getResults().get(0);

                    // but expecting 2 results for it
                    Assert.assertEquals(result.getIDSourceEntity(), "Tournesol");
                    Assert.assertEquals(result.getHits().length, 2);
                }
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Should never occur");
        }
    }


    @Test
    public void similarity_error_invalidDocument() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);

        FormDataBodyPart bodyPart = new FormDataBodyPart("file",
                                                         "{",
                                                         MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.ndjson").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            try (MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart)) {
                WebTarget target = forApiServer("/similarity");
                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {
                    Assert.assertEquals(response.getStatus(), 500);
                }
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Should never occur");
        }
    }

    @Test
    public void similarity_noID() {

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);

        FormDataBodyPart bodyPart = new FormDataBodyPart("file",
                                                         "{ \"first_name\": \"Triphon\", \"last_name\": \"Tournesol\"}",
                                                         MediaType.TEXT_PLAIN_TYPE);
        bodyPart.setFormDataContentDisposition(FormDataContentDisposition.name("file").fileName("data.ndjson").build());
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            try (MultiPart multiPart = formDataMultiPart.bodyPart(bodyPart)) {

                WebTarget target = forApiServer("/similarity");

                try (Response response = target.request().put(Entity.entity(multiPart, multiPart.getMediaType()))) {

                    Assert.assertEquals(response.getStatus(), 200);

                    SimilarityResults results = response.readEntity(SimilarityResults.class);

                    // one doc sent as input - one output for it
                    Assert.assertEquals(results.getResults().size(), 1);

                    SimilarityResult result = results.getResults().get(0);

                    // expecting results for Tournesol
                    // despite the slight variation in first name
                    Assert.assertNotNull(result.getIDSourceEntity());
                    Assert.assertEquals(result.getHits().length, 1);
                    Assert.assertEquals(result.getHits()[0].getId(), "6");
                }
            }
        } catch (IOException e) {
            Assert.assertNotNull(e, "Should never occur");
        }
    }
}
