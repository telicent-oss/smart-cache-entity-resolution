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
package io.telicent.smart.cache.entity.resolver.elastic;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;

public class MockElasticSearchClientTests extends AbstractElasticSearchClientTests {
    protected WireMockServer wireMockServer = new WireMockServer(8089);

    private static final String WIREMOCK_DEBUG = "WIREMOCK_DEBUG";

    @BeforeClass
    public void wireMockSetup() {
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.any(anyUrl())
                                                   .willReturn(aResponse().proxiedFrom("http://" + this.elastic.getHost() + ":" + this.elastic.getPort())));

        this.elastic.resetIndexManager(this.elastic.getHost(), wireMockServer.port(), this.elastic.getUsername(), this.elastic.getPassword());
    }

    @AfterClass
    public void wireMockTeardown() {
        wireMockServer.stop();
    }

    @AfterMethod
    public void clear() {
        dumpWireMockRequests();
        wireMockServer.resetRequests();
        wireMockServer.resetMappings();
    }

    @Override
    protected ElasticSearchEntityResolver getEntityResolver(final String index) {
        ElasticSearchEntityResolver elasticSearchEntityResolver =
                new ElasticSearchEntityResolver(this.elastic.getHost(), wireMockServer.port(),
                                                SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX,
                                                this.elastic.getUsername(), this.elastic.getPassword(), this.elastic.getElasticTlsCaCertString(), false);
        this.clients.add(elasticSearchEntityResolver);
        return elasticSearchEntityResolver;
    }

    @Override
    protected ElasticSearchIndexer<Map<String, Object>> getIndexer(String index) {
        ElasticSearchIndexer<Map<String, Object>> indexer =
                ElasticSearchIndexer.<Map<String, Object>>create()
                                    .index(index)
                                    .usingOverwrites()
                                    .host(this.elastic.getHost())
                                    .port(wireMockServer.port())
                                    .withCredentials(elastic.getUsername(), elastic.getPassword(), elastic.getElasticTlsCaCertString())
                                    .build();
        this.clients.add(indexer);
        return indexer;
    }


    private boolean debugWireMockRequests() {
        return null != System.getProperty(WIREMOCK_DEBUG)
            || null != System.getenv(WIREMOCK_DEBUG);
    }

    private void dumpWireMockRequests() {
        if (!debugWireMockRequests()) {
            return;
        }

        List<ServeEvent> requests = getAllRequests();
        System.out.println("**************");
        System.out.println("Requests");
        System.out.println("**************");
        int i=0;
        for (ServeEvent request : requests) {
            System.out.println("Request - " + i);
            System.out.println("URL - " + request.getRequest().getUrl());
            System.out.println("Method - " + request.getRequest().getMethod());
            LoggedResponse response = request.getResponse();
            System.out.println("Response body - " + response.getBodyAsString());
            System.out.println("Status status - " + response.getStatus());
            ++i;
        }
        System.out.println("**************");
    }

    private List<LoggedResponse> getAllResponses() {
        return wireMockServer.getAllServeEvents().stream()
                             .map(ServeEvent::getResponse)
                             .toList();
    }

    private List<ServeEvent> getAllRequests() {
        return wireMockServer.getAllServeEvents();
    }
}
