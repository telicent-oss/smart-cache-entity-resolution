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
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
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

public class DockerTestElasticSearchEntityResolverSimilarityOverrides extends AbstractElasticSearchClientTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DockerTestElasticSearchEntityResolverSimilarityOverrides.class);

    private static final String OVERRIDE_TEST_SIMILARITY_INDEX = "tests_similarity_override";

    private static final String FIRSTNAME = "first_name";
    private static final String LASTNAME = "last_name";
    private static final String MIDDLENAME = "middle_name";

    @BeforeClass
    private void setUp() {
        CachedIndexMapper.load("src/test/resources/dynamic_config_sample.yml");
        CanonicalSearchConfiguration.loadDynamicMappingRules("src/test/resources/dynamic_config_sample.yml");
    }

    @BeforeMethod
    private void init() {
        this.elastic.resetIndex(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
        populateSmallTestData(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "Unrecognised field.*")
    public void test_invalidMapping_unrecognisedField() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mike");
        doc.setProperty(LASTNAME, "Patton");

        String invalidMapping = """
                 fields:
                  - name: unrecognised
                    type: text
                    required: true
                    fuzziness:
                      enabled: true
                    boost: 10.0
                """;

        client.findSimilar(doc, 1, 0, null, invalidMapping);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "Property first_name needs to be a Number")
    public void test_invalidMapping_wrongPropertyType() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mike");
        doc.setProperty(LASTNAME, "Patton");

        String invalidMapping = """
                 fields:
                  - name: first_name
                    type: number
                    required: true
                    boost: 10.0
                """;

        client.findSimilar(doc, 1, 0, null, invalidMapping);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "Invalid override configuration provided")
    public void test_invalidMapping_wrongConfiguration() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Mike");
        doc.setProperty(LASTNAME, "Patton");

        String invalidMapping = "Rubbish";

        client.findSimilar(doc, 1, 0, null, invalidMapping);
    }


    @Test
    public void elastic_similarity_applyOverride_weighting() {

        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Duke");
        doc.setProperty(MIDDLENAME, "Rick");
        doc.setProperty(LASTNAME, "Parson");

        boolean caught = false;
        SimilarityResult res = null;

        // First search with existing set-up
        try {
            res = client.findSimilar(doc, 3, 0.7f);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);

        // Matches "3" Duke, Mick, Patton
        Assert.assertEquals(res.getHits()[0].getId(), "3");

        // Add an override - emphasis on firstname
        String mappingOverride = """
                fields:
                  - name: first_name
                    type: text
                    required: false
                    fuzziness:
                      enabled: true
                    boost: 10.0
                  - name: middle_name
                    type: text
                    required: true
                    boost: 1.0
                    fuzziness:
                      enabled: true
                  - name: last_name
                    type: text
                    required: true
                    boost: 1.0
                    fuzziness:
                      enabled: true
                """;
        try {
            res = client.findSimilar(doc, 3, 0.7f, null, mappingOverride);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return three documents
        Assert.assertEquals(res.getHits().length, 3);

        // Matches "2" Mike, Rick, Parson
        Assert.assertEquals(res.getHits()[0].getId(), "2");
        Assert.assertEquals(res.getHits()[1].getId(), "1");
        Assert.assertEquals(res.getHits()[2].getId(), "3");

        // update mapping - emphasis on middle name
        mappingOverride = """
                fields:
                  - name: first_name
                    type: text
                    required: true
                    fuzziness:
                      enabled: true
                    boost: 1.0
                  - name: middle_name
                    type: text
                    required: true
                    boost: 10.0
                    fuzziness:
                      enabled: true
                  - name: last_name
                    type: text
                    required: true
                    boost: 1.0
                    fuzziness:
                      enabled: true
                """;
        try {
            res = client.findSimilar(doc, 3, 0.7f, null, mappingOverride);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return three documents
        Assert.assertEquals(res.getHits().length, 3);

        // Matches "1" Mike, Rick, Patton
        Assert.assertEquals(res.getHits()[0].getId(), "1");
        Assert.assertEquals(res.getHits()[1].getId(), "3");
        Assert.assertEquals(res.getHits()[2].getId(), "2");

        // update mapping - emphasis on last name
        mappingOverride = """
                fields:
                  - name: first_name
                    type: text
                    required: true
                    fuzziness:
                      enabled: true
                    boost: 1.0
                  - name: middle_name
                    type: text
                    required: true
                    boost: 1.0
                    fuzziness:
                      enabled: true
                  - name: last_name
                    type: text
                    required: true
                    boost: 10.0
                    fuzziness:
                      enabled: true
                """;
        try {
            res = client.findSimilar(doc, 3, 0.7f, null, mappingOverride);
        } catch (RuntimeException re) {
            caught = true;
        }

        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return two documents
        Assert.assertEquals(res.getHits().length, 2);

        // Matches "2" Mike, Rick, Parson
        Assert.assertEquals(res.getHits()[0].getId(), "2");
        Assert.assertEquals(res.getHits()[1].getId(), "3");

    }

    @Test
    public void elastic_similarity_applyOverrides_disableFields() {
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Alan");
        doc.setProperty(MIDDLENAME, "Ben");
        doc.setProperty(LASTNAME, "Colin");
        doc.setProperty("id", "inputDoc");

        boolean caught = false;
        SimilarityResult res = null;
        // basic search
        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return 1 documents
        Assert.assertEquals(res.getHits().length, 1);
        double score = res.getHits()[0].getScore();

        String mappingOverride;
        // disable one field to improve scoring
        try {
            mappingOverride = """
                    fields:
                      - name: first_name
                        type: text
                        required: true
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                      - name: middle_name
                        type: text
                        required: false
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                      - name: last_name
                        type: text
                        required: true
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                    """;
            res = client.findSimilar(doc, 1, 0, null, mappingOverride);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");
        Assert.assertEquals(res.getHits().length, 1);
        Assert.assertTrue(res.getHits()[0].getScore() > score);

        score = res.getHits()[0].getScore();
        // disable two fields to improve scoring further
        try {
            mappingOverride = """
                    fields:
                      - name: first_name
                        type: text
                        required: true
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                      - name: middle_name
                        type: text
                        required: false
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                      - name: last_name
                        type: text
                        required: false
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                    """;
            res = client.findSimilar(doc, 1, 0, null, mappingOverride);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");
        Assert.assertEquals(res.getHits().length, 1);
        Assert.assertTrue(res.getHits()[0].getScore() > score);
    }

    @Test
    public void test_indexOverride() {
        // set-up
        this.elastic.resetIndex(OVERRIDE_TEST_SIMILARITY_INDEX);
        populateSmallTestData(OVERRIDE_TEST_SIMILARITY_INDEX);

        // given
        ElasticSearchEntityResolver client = this.getEntityResolver();

        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "Alan");
        doc.setProperty(MIDDLENAME, "Ben");
        doc.setProperty(LASTNAME, "Colin");
        doc.setProperty("id", "inputDoc");

        boolean caught = false;
        SimilarityResult res = null;
        // basic search
        try {
            res = client.findSimilar(doc, 1, 0);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return 1 documents
        Assert.assertEquals(res.getHits().length, 1);

        // Apply override to index
        String mappingOverride;
        // disable one field to improve scoring
        try {
            mappingOverride = """
                    index: tests_similarity_override
                    fields:
                      - name: first_name
                        type: text
                        required: true
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                      - name: middle_name
                        type: text
                        required: false
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                      - name: last_name
                        type: text
                        required: true
                        boost: 1.0
                        fuzziness:
                          enabled: true
                          max: 10
                    """;
            res = client.findSimilar(doc, 1, 0, null, mappingOverride);
        } catch (RuntimeException re) {
            caught = true;
        }
        Assert.assertFalse(caught, "Should not have caught an exception");
        Assert.assertNotNull(res, "Similarity result can be empty but should not be null");

        // should return a single document
        Assert.assertEquals(res.getHits().length, 1);
    }

    /**
     * Populates some simple test data into the index
     */
    protected void populateSmallTestData(String index) {
        LOGGER.info("Populating the small dataset into the test index");
        ElasticSearchIndexer<Map<String, Object>> indexer =
                getIndexer(index);
        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(Map.of(ID_FIELD, "1", FIRSTNAME, "Mike", LASTNAME, "Patton", MIDDLENAME, "Rick"));
        documents.add(Map.of(ID_FIELD, "2", FIRSTNAME, "Mike", LASTNAME, "Parson", MIDDLENAME, "Nick"));
        documents.add(Map.of(ID_FIELD, "3", FIRSTNAME, "Duke", LASTNAME, "Patton", MIDDLENAME, "Mick"));
        documents.add(Map.of(ID_FIELD, "4", FIRSTNAME, "Adam", MIDDLENAME, "Bill", LASTNAME, "Chris"));
        documents.add(Map.of(ID_FIELD, "5", FIRSTNAME, "Alec", MIDDLENAME, "Bob", LASTNAME, "Caleb"));
        documents.add(Map.of(ID_FIELD, "6", FIRSTNAME, "Alana", MIDDLENAME, "Brian", LASTNAME, "Cooper"));
        documents.add(Map.of(ID_FIELD, "7", FIRSTNAME, "Nick", LASTNAME, "Rick", MIDDLENAME, "Jack"));
        documents.add(Map.of(ID_FIELD, "8", FIRSTNAME, "Lick", LASTNAME, "Nick", MIDDLENAME, "Pick"));
        documents.add(Map.of(ID_FIELD, "9", FIRSTNAME, "Fick", LASTNAME, "Pick", MIDDLENAME, "Sick"));

        indexer.bulkIndex(x -> x.get(ID_FIELD).toString(), documents);
        indexer.flush(true);

        for (int i = 1; i <= documents.size(); i++) {
            Assert.assertTrue(indexer.isIndexed(Integer.toString(i)));
        }
    }
}
