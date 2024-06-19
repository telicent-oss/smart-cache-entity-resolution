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

import com.github.tomakehurst.wiremock.client.WireMock;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class DockerTestElasticSearchEntityResolverSimilarityMock extends MockElasticSearchClientTests {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DockerTestElasticSearchEntityResolverSimilarityMock.class);

    private static final String FIRSTNAME = "first_name";
    private static final String LASTNAME = "last_name";
    private static final Document document = new Document(Map.of(FIRSTNAME, "Mike", LASTNAME, "Patton"));

    private ElasticSearchEntityResolver client;

    @BeforeMethod
    private void init() {
        wireMockServer.stubFor(WireMock.any(anyUrl())
                                       .willReturn(aResponse().proxiedFrom(
                                               "http://" + this.elastic.getHost() + ":" + this.elastic.getPort())));

        this.elastic.resetIndex(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        populateSmallTestData();
        client = this.getEntityResolver();
    }


    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "co.elastic.clients.transport.TransportException: status: 404.*")
    public void test_failureWhenIndexingTemporaryDocument_viaException() {
        // given
        wireMockServer.stubFor(post(urlEqualTo("/_bulk?refresh=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(
                                                                  404) // general exception path leading to SearchException
                                                          .withBody("ErrorResponse")
                                       ));

        // when
        // then
        client.findSimilar(document, 1, 0);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "ElasticSearch reported error while attempting to Similarity search temporarily indexing docs.*")
    public void test_failureWhenIndexingTemporaryDocument_viaElasticsearchException() {
        // given
        wireMockServer.stubFor(post(urlEqualTo("/_bulk?refresh=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(404)
                                                          .withBody(
                                                                  "{\"error\":{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\",\"root_cause\":[{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\"}]},\"status\":404}")
                                       ));

        // when
        // then
        client.findSimilar(document, 1, 0);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "co.elastic.clients.transport.TransportException: status: 404.*")
    public void test_failureWhenSearching_viaException() {
        // given
        wireMockServer.stubFor(post(urlEqualTo("/tests_similarity/_search?typed_keys=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(404)
                                                          .withBody("Rubbish")
                                       ));

        // when
        // then
        client.findSimilar(document, 1, 0);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "ElasticSearch reported error while attempting to Similarity search for doc.*")
    public void test_failureWhenSearching_viaElasticsearchException() {
        // given
        wireMockServer.stubFor(post(urlEqualTo("/tests_similarity/_search?typed_keys=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(404)
                                                          .withBody(
                                                                  "{\"error\":{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\",\"root_cause\":[{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\"}]},\"status\":404}")
                                       ));

        // when
        // then
        client.findSimilar(document, 1, 0);
    }

    @Test
    public void test_failureWhenDeletingTemporaryDocument_doesntImpactResults() {
        // given
        wireMockServer.stubFor(post(urlEqualTo("/tests_similarity/_delete_by_query?refresh=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(404)
                                                          .withBody(
                                                                  "{\"error\":{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\",\"root_cause\":[{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\"}]},\"status\":404}")
                                       ));

        // when
        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(document, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }

        // then
        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "4");

        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 1d);

        // ensure we are actually calling the stub
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/tests_similarity/_delete_by_query?refresh=true")));
    }

    @Test
    public void test_failureWhenDeletingTemporaryDocuments_doesntImpactResults() {
        // given
        wireMockServer.stubFor(post(urlEqualTo("/tests_similarity/_delete_by_query?refresh=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(404)
                                                          .withBody(
                                                                  "{\"error\":{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\",\"root_cause\":[{\"index_uuid\":\"_na_\",\"index\":\"missing\",\"resource.type\":\"index_or_alias\",\"resource.id\":\"missing\",\"type\":\"index_not_found_exception\",\"reason\":\"no such index [missing]\"}]},\"status\":404}")
                                       ));

        // when
        boolean caught = false;
        SimilarityResults results = null;
        try {
            results = client.findSimilar(List.of(document), 1, 0, false);
        } catch (RuntimeException re) {
            caught = true;
        }

        // then
        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");

        Assert.assertNotNull(results, "Similarity results can be empty but should not be null");
        Assert.assertEquals(results.getResults().size(), 1);

        SimilarityResult res = results.getResults().get(0);

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "4");

        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 1d);

        // ensure we are actually calling the stub
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/tests_similarity/_delete_by_query?refresh=true")));
    }

    @Test
    public void test_search_happyPath() {
        // given
        String body =
                "{\"took\":2,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":{\"value\":2,\"relation\":\"eq\"},\"max_score\":3.364334,\"hits\":[{\"_index\":\"tests_similarity\",\"_type\":\"_doc\",\"_id\":\"673f5395-9866-41b2-a428-410ab3ce4245\",\"_score\":3.364334,\"_source\":{\"last_name\":\"Capone\",\"originalId\":\"1bd3a485-e956-40c5-a667-46aab63bd0a8\",\"id\":\"673f5395-9866-41b2-a428-410ab3ce4245\",\"first_name\":\"Al\",\"tmp_similarity_input\":\"80605860-f359-4ec6-a32f-13f71eaba915\"}},{\"_index\":\"tests_similarity\",\"_type\":\"_doc\",\"_id\":\"5\",\"_score\":1.3862942,\"_source\":{\"first_name\":\"Al\",\"id\":\"5\",\"last_name\":\"Di Meola\"}}]}}";
        wireMockServer.stubFor(post(urlEqualTo("/tests_similarity/_search?typed_keys=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(200)
                                                          .withBody(body)
                                                          .withHeader("X-Elastic-Product", "Elasticsearch")
                                       ));

        // when
        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(document, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }

        // then
        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // ensure we are actually calling the stub
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/tests_similarity/_search?typed_keys=true")));
    }

    @Test
    public void test_search_malformedData() {
        // given
        // 2 documents, one without source, the other without score.
        String body = "{\"took\":2,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1,\"skipped\":0,\"failed\":0},\"hits\":{\"total\":{\"value\":2,\"relation\":\"eq\"},\"max_score\":3.364334,\"hits\":[{\"_index\":\"tests_similarity\",\"_type\":\"_doc\",\"_id\":\"673f5395-9866-41b2-a428-410ab3ce4245\",\"_source\":{\"last_name\":\"Capone\",\"originalId\":\"1bd3a485-e956-40c5-a667-46aab63bd0a8\",\"id\":\"673f5395-9866-41b2-a428-410ab3ce4245\",\"first_name\":\"Al\",\"tmp_similarity_input\":\"80605860-f359-4ec6-a32f-13f71eaba915\"}},{\"_index\":\"tests_similarity\",\"_type\":\"_doc\",\"_id\":\"5\",\"_score\":1.3862942}]}}";

       wireMockServer.stubFor(post(urlEqualTo("/tests_similarity/_search?typed_keys=true"))
                                       .atPriority(1)
                                       .willReturn(
                                               aResponse().withStatus(200)
                                                          .withBody(body)
                                                          .withHeader("X-Elastic-Product", "Elasticsearch")
                                       ));

        // when
        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(document, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }

        // then
        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should not return anything
        Assert.assertEquals(res.getHits().length, 0);

        // ensure we are actually calling the stub
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/tests_similarity/_search?typed_keys=true")));
    }


    /**
     * Populates some simple test data into the index
     */
    @Override
    protected void populateSmallTestData() {
        LOGGER.info("Populating the small dataset into the test index");
        ElasticSearchIndexer<Map<String, Object>> indexer =
                getIndexer(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        List<Map<String, Object>> documents = new ArrayList<>();

        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "1", FIRSTNAME, "Miles", LASTNAME, "Davies"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "2", FIRSTNAME, "John", LASTNAME, "Coltrane"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "3", FIRSTNAME, "Frank", LASTNAME, "Zappa"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "4", FIRSTNAME, "Mike", LASTNAME, "Patton"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "5", FIRSTNAME, "Al", LASTNAME, "Di Meola"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "6", FIRSTNAME, "Ivor", LASTNAME, "Sorbum"));
        documents.add(
                Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "7", FIRSTNAME, "Tryphon", LASTNAME, "Tournesol"));
        documents.add(
                Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "8", FIRSTNAME, "Triphon", LASTNAME, "Tournesal"));

        indexer.bulkIndex(x -> x.get(AbstractDockerElasticSearchTests.ID_FIELD).toString(), documents);
        indexer.flush(true);

        for (int i = 1; i <= 8; i++) {
            Assert.assertTrue(indexer.isIndexed(Integer.toString(i)));
        }
    }

}
