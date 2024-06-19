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
package io.telicent.smart.cache.search.elastic;

import io.telicent.smart.cache.entity.resolver.elastic.AbstractDockerElasticSearchTests;
import io.telicent.smart.cache.entity.resolver.elastic.AbstractElasticSearchClientTests;
import io.telicent.smart.cache.entity.resolver.elastic.DockerTestElasticSearchEntityResolverSimilarity;
import io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver.TEMP_INDEXING_SIMILARITY_FIELD;
import static io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver.flagFutureDeleteForCleanUp;

public class DockerTestElasticSearchClient  extends AbstractElasticSearchClientTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerTestElasticSearchEntityResolverSimilarity.class);

    private static final String FIRSTNAME = "first_name";
    private static final String LASTNAME = "last_name";

    @BeforeMethod
    private void init() {
        this.elastic.resetIndex(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        populateSmallTestData();
    }

    @Test
    public void test_deleteClearsUpOnceFlagged() {
        // given
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mike");
        doc.setProperty(LASTNAME, "Patton");

        boolean caught = false;
        SimilarityResults results = null;
        try {
            results = client.findSimilar(List.of(doc), 1, 0, true);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");
        Assert.assertEquals(results.getResults().size(), 1);

        SimilarityResult res = results.getResults().get(0);

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "4");

        double score = res.getHits()[0].getScore();

        Assert.assertEquals(score, 1d);

        // when
        flagFutureDeleteForCleanUp();

        try {
            results = client.findSimilar(List.of(doc), 1, 0, true);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");
        Assert.assertEquals(results.getResults().size(), 1);

        res = results.getResults().get(0);

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "4");

        // then
        try {
            results = client.findSimilar(List.of(doc), 1, 0, true);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");
        Assert.assertEquals(results.getResults().size(), 1);

        res = results.getResults().get(0);

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return nothing as details deleted.
        Assert.assertEquals(res.getHits().length, 0);


    }

    /**
     * Populates some simple test data into the index
     */
    protected void populateSmallTestData() {
        LOGGER.info("Populating the small dataset into the test index");
        ElasticSearchIndexer<Map<String, Object>> indexer =
                getIndexer(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        List<Map<String, Object>> documents = new ArrayList<>();

        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "1",
                             FIRSTNAME, "Miles",
                             LASTNAME, "Davies",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "2",
                             FIRSTNAME, "John",
                             LASTNAME, "Coltrane",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "3",
                             FIRSTNAME, "Frank",
                             LASTNAME, "Zappa",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "4",
                             FIRSTNAME, "Mike",
                             LASTNAME, "Patton",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "5",
                             FIRSTNAME, "Al",
                             LASTNAME, "Di Meola",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "6",
                             FIRSTNAME, "Ivor",
                             LASTNAME, "Sorbum",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "7",
                             FIRSTNAME, "Tryphon",
                             LASTNAME, "Tournesol",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "8",
                             FIRSTNAME, "Triphon",
                             LASTNAME, "Tournesal",
                             TEMP_INDEXING_SIMILARITY_FIELD, "Something"));

        indexer.bulkIndex(x -> x.get(AbstractDockerElasticSearchTests.ID_FIELD).toString(), documents);
        indexer.flush(true);

        for (int i = 1; i <= 8; i++) {
            Assert.assertTrue(indexer.isIndexed(Integer.toString(i)));
        }
    }
}
