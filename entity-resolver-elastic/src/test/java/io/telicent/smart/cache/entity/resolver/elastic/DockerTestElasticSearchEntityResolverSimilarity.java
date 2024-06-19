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

import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DockerTestElasticSearchEntityResolverSimilarity extends AbstractElasticSearchClientTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerTestElasticSearchEntityResolverSimilarity.class);

    private static final String FIRSTNAME = "first_name";
    private static final String LASTNAME = "last_name";

    @BeforeMethod
    private void init() {
        this.elastic.resetIndex(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        populateSmallTestData();
    }

    @Test
    public void test_searchWithoutID() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mike");
        doc.setProperty(LASTNAME, "Patton");

        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "4");

        double score = res.getHits()[0].getScore();

        Assert.assertEquals(score, 1d);
    }

    @Test
    public void elastic_similarity() {

        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mike");
        doc.setProperty(LASTNAME, "Patton");

        boolean caught = false;
        SimilarityResult res = null;

        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception");

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "4");

        double score = res.getHits()[0].getScore();

        Assert.assertEquals(score, 1d);

        // now look for one with a slight variation
        doc.setProperty(LASTNAME, "Patlon");

        res = client.findSimilar(doc, 1, 0);

        Assert.assertEquals(res.getHits()[0].getId(), "4");

        double scoreVar = res.getHits()[0].getScore();

        // the initial match was better than the second one
        Assert.assertTrue(scoreVar < score);

        // now try to put a minimal score
        doc.setProperty(FIRSTNAME, "Al");
        doc.setProperty(LASTNAME, "Capone");

        res = client.findSimilar(doc, 1, 0.8f);

        // should return nothing
        Assert.assertEquals(res.getHits().length, 0);
    }

    @Test
    public void elastic_similarity_2hits() {

        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Truphon");
        doc.setProperty(LASTNAME, "Tournesil");
        doc.setProperty("id", "inputDoc");

        boolean caught = false;

        SimilarityResult res = null;

        try {
            res = client.findSimilar(doc, 2, 0);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception");

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return 2 documents
        Assert.assertEquals(res.getHits().length, 2);

        // Tryphon Tournesol
        Assert.assertEquals(res.getHits()[0].getId(), "7");
        // Triphon Tournesal
        Assert.assertEquals(res.getHits()[1].getId(), "8");
    }

    @Test
    public void elastic_similarity_batch2() {

        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mohamed");
        doc.setProperty(LASTNAME, "Ali");
        doc.setProperty("id", "moali");

        Document doc2 = new Document();
        doc2.setProperty(FIRSTNAME, "Mohamed");
        doc2.setProperty(LASTNAME, "Ali");
        doc2.setProperty("id", "moali2");

        List<Document> batch = new ArrayList<>();
        batch.add(doc);
        batch.add(doc2);

        // no intra res
        SimilarityResults res = client.findSimilar(batch, 1, 0, false);

        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return 2 documents
        Assert.assertEquals(res.getResults().size(), 2);

        Assert.assertEquals(res.getResults().get(0).getHits().length, 0);
        Assert.assertEquals(res.getResults().get(1).getHits().length, 0);

        // now allow similarity between them
        res = client.findSimilar(batch, 1, 0, true);

        // should return 2 documents
        Assert.assertEquals(res.getResults().size(), 2);

        Assert.assertEquals(res.getResults().get(0).getHits().length, 1);
        Assert.assertEquals(res.getResults().get(1).getHits().length, 1);
    }

    /**
     * Test case illustrating issue #259. Search for a similarity then search again but with different ID. The previous
     * search should not be returned.
     */
    @Test
    public void test_temporaryIndexDeletedAfterSearch() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "No");
        doc.setProperty(LASTNAME, "Match");
        doc.setProperty("id", "inputDocID");
        boolean caught = false;
        SimilarityResult res = null;

        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");
        // Does not return any matches
        Assert.assertEquals(res.getHits().length, 0);

        // Search for same failing terms but include temporary items in search set (withinInput=true)
        doc.setProperty("id", "inputDocDifferentID");
        SimilarityResults results = null;
        try {
            results = client.findSimilar(Collections.singletonList(doc), 1, 0, true);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        // Return is never null but there are no actual hits in the results.
        Assert.assertEquals(results.getResults().size(), 1);
        Assert.assertEquals(results.getResults().get(0).getHits().length, 0, "Previous temporary Index not deleted");
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
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "7", FIRSTNAME, "Tryphon", LASTNAME, "Tournesol"));
        documents.add(Map.of(AbstractDockerElasticSearchTests.ID_FIELD, "8", FIRSTNAME, "Triphon", LASTNAME, "Tournesal"));

        indexer.bulkIndex(x -> x.get(AbstractDockerElasticSearchTests.ID_FIELD).toString(), documents);
        indexer.flush(true);

        for (int i = 1; i <= 8; i++) {
            Assert.assertTrue(indexer.isIndexed(Integer.toString(i)));
        }
    }

}
