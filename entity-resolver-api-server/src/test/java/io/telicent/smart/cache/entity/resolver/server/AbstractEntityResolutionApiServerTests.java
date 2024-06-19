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

import io.telicent.smart.cache.search.IndexManager;
import io.telicent.smart.cache.search.clusters.test.AbstractSearchCluster;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.configuration.SimpleIndexConfiguration;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import io.telicent.smart.cache.search.elastic.ESTestCluster;
import io.telicent.smart.cache.server.jaxrs.model.Problem;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract base class for integration tests for the Entity Resolution API Server.  This uses test containers to stand
 * up a temporary ElasticSearch cluster in a Docker container and runs the Search API Server in a background thread.
 */
public class AbstractEntityResolutionApiServerTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityResolutionApiServerTests.class);

    public static final String API_HOST = "localhost";
    public static final int API_PORT = 18081;
    public static final int DEFAULT_ELASTIC_PORT = 19200;

    @SuppressWarnings("rawtypes")
    protected final AbstractSearchCluster elastic;
    protected EntityResolutionApiEntrypoint apiServer;

    private static final ClientConfig cc = new ClientConfig();

    static {
        cc.register(MultiPartFeature.class);
    }

    private final Client client = ClientBuilder.newClient(cc);

    public AbstractEntityResolutionApiServerTests() {
        int port = DEFAULT_ELASTIC_PORT;

        // NB - Intentionally using System.getenv() directly to detect when Maven Build is requesting use of an external
        //      ElasticSearch instance
        String rawPort = System.getenv("ELASTIC_PORT");
        if (StringUtils.isNotBlank(rawPort)) {
            try {
                port = Integer.parseInt(rawPort);
            } catch (NumberFormatException e) {
                throw new RuntimeException("ELASTIC_PORT had invalid non-numeric value: " + e.getMessage());
            }
        }
        this.elastic = createTestCluster(port);
    }

    /**
     * Creates the actual instance of the test cluster
     *
     * @param port Port number to use, or {@code -1} to pick an arbitrary port
     * @return Test cluster
     */
    @SuppressWarnings("rawtypes")
    protected AbstractSearchCluster createTestCluster(int port) {
        return new ESTestCluster(port);
    }

    @BeforeClass
    public void setupServers() throws Exception {
        startElasticSearch();
        startApiServer();
    }

    protected void startApiServer() {
        this.apiServer = new EntityResolutionApiEntrypoint(API_HOST, API_PORT);
        this.apiServer.runNonBlocking();
    }

    protected void startApiServerDefault() {
        this.apiServer = new EntityResolutionApiEntrypoint();
        this.apiServer.runNonBlocking();
    }

    protected void startElasticSearch() {
        if (usingExternalElasticSearch()) {
            // Intentionally using System.getenv() directly here
            int externalPort = Integer.parseInt(System.getenv("ELASTIC_PORT"));
            LOGGER.warn("Using External ElasticSearch instance on localhost:{}, no setup performed", externalPort);
            this.elastic.useExternal(externalPort);
        } else {
            this.elastic.setup();
        }
    }

    /**
     * Indicates whether an External ElasticSearch instance is being used
     *
     * @return True if an external instance is in-use, false otherwise
     */
    protected boolean usingExternalElasticSearch() {
        return StringUtils.isNotBlank(System.getProperty("elastic.external"));
    }

    @AfterClass
    public void teardownServers() throws Exception {
        stopElasticSearch();
        stopApiServer();
    }

    protected void stopApiServer() {
        this.apiServer.stop();
    }

    protected void stopElasticSearch() {
        if (usingExternalElasticSearch()) {
            LOGGER.warn("Using External ElasticSearch instance, no teardown performed");
        } else {
            this.elastic.teardown();
        }
    }

    /**
     * Creates a web target for invoking an HTTP request against some arbitrary path on the Search API Server
     *
     * @param path Path
     * @return Web target
     */
    protected WebTarget forApiServer(String path) {
        return this.client.target(String.format("http://%s:%d", API_HOST, this.apiServer.port)).path(path);
    }

    /**
     * Creates a target for invoking the {@code /healthz} endpoint of the Search API Server
     *
     * @return Web target
     */
    protected WebTarget getHealth() {
        return forApiServer("/healthz");
    }

    /**
     * Creates a target for invoking an HTTP Request against the ElasticSearch server
     *
     * @param path Request path
     * @return Web target
     */
    protected WebTarget forElastic(String path) {
        return this.client.target(String.format("http://%s:%d", this.elastic.getHost(), this.elastic.getPort()))
                          .path(path);
    }

    /**
     * Imports a sample data file into ElasticSearch
     *
     * @param file      Sample data file
     * @param indexName name of the index
     * @throws IOException Thrown if there's a problem reading in the sample data file
     */
    protected void importSampleData(File file, String indexName) throws IOException {
        if (!file.exists()) {
            throw new RuntimeException("Failed to locate sample data file " + file.getAbsolutePath());
        }

        WebTarget target = forElastic("/" + indexName + "/_bulk");
        Invocation.Builder invocation =
                target.request(MediaType.APPLICATION_JSON).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try (FileInputStream input = new FileInputStream(file)) {
            try (Response response = invocation.post(Entity.json(input))) {
                Assert.assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);
                Map<?,?> bulk = response.readEntity(Map.class);
                Assert.assertFalse((Boolean) bulk.get("errors"));
            }
        }

        // Explicitly flush the index at this point just to ensure all the data is persisted and visible for the
        // subsequent tests
        target = forElastic("/" + indexName + "/_flush");
        try (Response response = target.request(MediaType.APPLICATION_JSON).buildPost(Entity.json("")).invoke()) {
            Assert.assertTrue(response.getStatus() >= 200 && response.getStatus() < 300,
                              "Failed to flush the index");
        }
    }

    /**
     * Creates the test index and imports the given sample data file
     *
     * @param sampleDataFile Sample data file
     */
    protected void createIndexAndImportSampleData(File sampleDataFile) {
        createIndexAndImportSampleData(sampleDataFile, SearchTestClusters.DEFAULT_TEST_INDEX);
    }

    /**
     * Creates a test index and imports the given sample data file
     *
     * @param indexName      index
     * @param sampleDataFile Sample data file
     */
    protected void createIndexAndImportSampleData(File sampleDataFile, String indexName) {
        // Delete a pre-existing index
        IndexManager<SimpleMappingRule> indexManager = this.elastic.getIndexManager();
        Boolean exists = indexManager.hasIndex(indexName);
        if (exists) {
            indexManager.deleteIndex(indexName);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Boolean created = indexManager
                .createIndex(indexName,
                             new SimpleIndexConfiguration(new Properties(),
                                                          Collections.emptyList()));
        Assert.assertEquals(created, Boolean.TRUE,
                            "Failed to create required ElasticSearch index " + indexName);
        try {
            // Insert a short sleep before attempting to import the data, there's a chance that the index is not ready
            // if we try and upload immediately after creation
            Thread.sleep(200);
            this.importSampleData(sampleDataFile, indexName);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies that a {@code GET} request to the target returns an HTTP 404 Not Found
     * <p>
     * Returns the response so that callers can further verify the response if they so wish
     * </p>
     *
     * @param target Web target
     * @return Response
     */
    protected Response verifyNotFound(WebTarget target) {
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

        Response response = invocation.get();
        Assert.assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        return response;
    }

    /**
     * Verifies that a {@code GET} request to the target returns an HTTP 500 Internal Server Error
     * <p>
     * Returns the response so that callers can further verify the response if they so wish
     * </p>
     *
     * @param target Web target
     * @return Response
     */
    protected Response verifyServerError(WebTarget target) {
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

        Response response = invocation.get();
        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        return response;
    }

    /**
     * Converts the HTTP response into a Problem object for further verification
     *
     * @param response HTTP Response
     * @return Problem
     */
    protected Problem asProblem(Response response) {
        try {
            Assert.assertNotNull(response, "Expected a valid HTTP Response");
            Assert.assertTrue(response.hasEntity(), "Expected a Response body in this HTTP response");
            Problem problem = response.readEntity(Problem.class);
            Assert.assertEquals(problem.getStatus(), response.getStatus(),
                                "Expected Problem status code to reflect HTTP Status code of the HTTP Response");
            return problem;
        } catch (ProcessingException e) {
            Assert.fail("Failed to convert HTTP Response into a Problem object: " + e.getMessage());
        }
        return null;
    }
}
