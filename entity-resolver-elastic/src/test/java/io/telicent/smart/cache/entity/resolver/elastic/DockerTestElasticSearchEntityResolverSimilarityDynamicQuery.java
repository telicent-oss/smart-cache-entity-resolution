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

import io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration;
import io.telicent.smart.cache.entity.resolver.elastic.index.CachedIndexMapper;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.telicent.smart.cache.search.clusters.test.SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX;

public class DockerTestElasticSearchEntityResolverSimilarityDynamicQuery extends AbstractElasticSearchClientTests {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DockerTestElasticSearchEntityResolverSimilarityDynamicQuery.class);

    private static final String ID = "id";
    private static final String CANONICAL_TYPE = "canonicaltype";
    private static final String TEXT_FIELD = "text-field";
    private static final String KEYWORD_FIELD = "keyword-field";
    private static final String INTEGER_FIELD = "integer-field";
    private static final String LONG_FIELD = "long-field";
    private static final String FLOAT_FIELD = "float-field";
    private static final String DOUBLE_FIELD = "double-field";
    private static final String NUMBER_FIELD = "number-field";
    private static final String LOCATION_FIELD = "location-field";
    private static final String DATE_FIELD = "date-field";


    @BeforeClass
    private void setUp() {
        CachedIndexMapper.load("src/test/resources/dynamic_config_sample.yml");
        CanonicalSearchConfiguration.loadDynamicMappingRules("src/test/resources/dynamic_config_sample.yml");
    }

    @BeforeMethod
    private void init() {
        this.elastic.resetIndex(DEFAULT_TEST_SIMILARITY_INDEX, "TestAllTypesMaximum");
        populateSmallTestData();
    }

    @Test
    public void test_dynamicSearchWithMissingFieldsThrowsError() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");

        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertTrue(caught, "Should catch an exception as we can't build a query from those fields.");
        Assert.assertNull(res, "Similarity result should not be null");
    }

    @Test
    public void test_similarity_dynamic_Keyword() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        doc.setProperty(KEYWORD_FIELD, "keyword");
        doc.setProperty(TEXT_FIELD, "fiftieth");

        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(doc, 5, 0.27f);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception because the document was missing an id field");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 2);

        // should be the same doc and have a perfect score
        Assert.assertEquals(res.getHits()[0].getId(), "1");
        Assert.assertEquals(res.getHits()[1].getId(), "4");

        double score1 = res.getHits()[0].getScore();
        double score2 = res.getHits()[1].getScore();
        Assert.assertEquals(score1, score2);
    }

    @Test
    public void test_similarity_dynamic_Text() {
        ElasticSearchEntityResolver client = this.getEntityResolver();
        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches "five" at a distance of  1
        doc.setProperty(TEXT_FIELD, "fire");

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "5");
        double firstScore = res.getHits()[0].getScore();
        // Matches "five" at a distance of  1
        doc.setProperty(TEXT_FIELD, "fiver");

        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "5");
        double secondScore = res.getHits()[0].getScore();

        Assert.assertEquals(firstScore, secondScore, 0.001);
    }

    @Test
    public void test_similarity_dynamic_Integer() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches 30 (at 1.0) as it's within the offset (10)
        doc.setProperty(INTEGER_FIELD, 33);

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "3");
        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 1d);
    }


    @Test
    public void test_similarity_dynamic_Long() {
        ElasticSearchEntityResolver client = this.getEntityResolver();
        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches 200 (at 0.75) as it's outside the offset (10), inside the scale (10) with a decay of 0.5
        doc.setProperty(LONG_FIELD, 215L);

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "2");
        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 0.75d);
    }

    @Test
    public void test_similarity_dynamic_Float() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches 1.9f with a score of 1.0 as it's within the offset (0.1)
        doc.setProperty(FLOAT_FIELD, 2f);

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "4");
        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 1d);
    }

    @Test
    public void test_similarity_dynamic_Double() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches 25.45f with a score of 0.75 as it's outside the offset (0.05),
        // and halfway inside the scope (0.1) with decay (0.5)
        doc.setProperty(DOUBLE_FIELD, 25.35d);

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "1");
        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 0.75d);
    }


    @Test
    public void test_similarity_dynamic_Number() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches 2200 with a score of 0.95 as there's no offset (0),
        // and just inside the scale (500) with decay (0.5)
        doc.setProperty(NUMBER_FIELD, 2200);

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "1");
        double score = res.getHits()[0].getScore();
        Assert.assertEquals(score, 0.95d);
    }

    @Test
    public void test_similarity_dynamic_Location() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        // Matches MI6 building since it's within a 150m pivot
        doc.setProperty(LOCATION_FIELD, "51.49889,-0.12348"); // Telicent office

        boolean caught = false;
        SimilarityResult res = null;
        try {
            res = client.findSimilar(doc, 10, 0.04f);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");
        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "1");
    }

    @Test
    public void test_similarity_dynamic_Date() {
        ElasticSearchEntityResolver client = this.getEntityResolver();
        Document doc = new Document();
        doc.setProperty(ID, "id");
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMaximum");
        doc.setProperty(DATE_FIELD, "2023-09-30");

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

        // should be the correct doc
        Assert.assertEquals(res.getHits()[0].getId(), "1");

    }

    /**
     * Populates some simple test data into the index
     */
    @Override
    protected void populateSmallTestData() {
        LOGGER.info("Populating the small dataset into the test index");
        ElasticSearchIndexer<Map<String, Object>> indexer =
                getIndexer(DEFAULT_TEST_SIMILARITY_INDEX);
        List<Map<String, Object>> documents = new ArrayList<>();

        documents.add(Map.of(
                AbstractDockerElasticSearchTests.ID_FIELD, "1",
                TEXT_FIELD, "first",
                KEYWORD_FIELD, "keyword",
                INTEGER_FIELD, 10,
                LONG_FIELD, 100L,
                FLOAT_FIELD, 0.9f,
                DOUBLE_FIELD, 25.45d,
                NUMBER_FIELD, 2150,
                LOCATION_FIELD, "51.48741,-0.12385", // MI6 building
                DATE_FIELD, "2023-09-23"
        ));

        documents.add(Map.of(
                AbstractDockerElasticSearchTests.ID_FIELD, "2",
                TEXT_FIELD, "second",
                KEYWORD_FIELD, "alternative",
                INTEGER_FIELD, 20,
                LONG_FIELD, 200L,
                FLOAT_FIELD, 1.9f,
                DOUBLE_FIELD, 23.49d,
                NUMBER_FIELD, 2300,
                LOCATION_FIELD, "51.50480,-0.07866", // London Mayors office
                DATE_FIELD, "2022-09-23"
        ));

        documents.add(Map.of(
                AbstractDockerElasticSearchTests.ID_FIELD, "3",
                TEXT_FIELD, "third",
                KEYWORD_FIELD, "alternative",
                INTEGER_FIELD, 30,
                LONG_FIELD, 50L,
                FLOAT_FIELD, 3.9f,
                DOUBLE_FIELD, 23.49d,
                NUMBER_FIELD, 2300,
                LOCATION_FIELD, "54.60487,-5.83184", // Northern Ireland Assembly
                DATE_FIELD, "2024-09-30"
        ));

        documents.add(Map.of(
                AbstractDockerElasticSearchTests.ID_FIELD, "4",
                TEXT_FIELD, "fourth",
                KEYWORD_FIELD, "keyword",
                INTEGER_FIELD, 40,
                LONG_FIELD, 50L,
                FLOAT_FIELD, 1.99f,
                DOUBLE_FIELD, 23.49d,
                NUMBER_FIELD, 2300,
                LOCATION_FIELD, "53.34497,-6.26", // Irish Parliament
                DATE_FIELD, "2022-09-30"
        ));

        documents.add(Map.of(
                AbstractDockerElasticSearchTests.ID_FIELD, "5",
                TEXT_FIELD, "five",
                KEYWORD_FIELD, "other",
                INTEGER_FIELD, 40,
                LONG_FIELD, 50L,
                FLOAT_FIELD, 1.9f,
                DOUBLE_FIELD, 23.49d,
                NUMBER_FIELD, 2300,
                LOCATION_FIELD, "51.46454,-3.16145", // Welsh Parliament
                DATE_FIELD, "2023-10-09"
        ));

        documents.add(Map.of(
                AbstractDockerElasticSearchTests.ID_FIELD, "6",
                TEXT_FIELD, "six",
                KEYWORD_FIELD, "other",
                INTEGER_FIELD, 40,
                LONG_FIELD, 50L,
                FLOAT_FIELD, 1.9f,
                DOUBLE_FIELD, 23.49d,
                NUMBER_FIELD, 2300,
                LOCATION_FIELD, "55.95225,-3.17566", // Scottish Parliament
                DATE_FIELD, "2023-09-01"
        ));

        indexer.bulkIndex(x -> x.get(AbstractDockerElasticSearchTests.ID_FIELD).toString(), documents);
        indexer.flush(true);

        for (int i = 1; i <= 6; i++) {
            Assert.assertTrue(indexer.isIndexed(Integer.toString(i)));
        }
    }


}
