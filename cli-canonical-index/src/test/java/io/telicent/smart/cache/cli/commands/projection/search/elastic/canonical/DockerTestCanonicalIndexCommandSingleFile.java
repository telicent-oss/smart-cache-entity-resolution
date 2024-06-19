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
package io.telicent.smart.cache.cli.commands.projection.search.elastic.canonical;

import com.github.rvesse.airline.parser.ParseResult;
import io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration;
import io.telicent.smart.cache.cli.commands.SmartCacheCommand;
import io.telicent.smart.cache.cli.commands.SmartCacheCommandTester;
import io.telicent.smart.cache.cli.commands.projection.search.elastic.AbstractElasticIndexCommandTests;
import io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.ElasticSearchClient;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.SearchResults;
import io.telicent.smart.cache.search.options.SearchOptions;
import io.telicent.smart.cache.search.options.SecurityOptions;
import io.telicent.smart.cache.sources.file.rdf.RdfFormat;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

public class DockerTestCanonicalIndexCommandSingleFile extends AbstractElasticIndexCommandTests {

    public static final String EXPECTED_ID_1 = "EXPECTED_ID_1";
    public static final String EXPECTED_ID_2 = "EXPECTED_ID_2";

    @BeforeClass
    @Override
    public void setup() {
        // Setup ElasticSearch
        this.elastic.setup();

        SmartCacheCommandTester.TEE_TO_ORIGINAL_STREAMS = true;

        SmartCacheCommandTester.setup();
    }

    @AfterMethod
    @Override
    public void testCleanup() {
        SmartCacheCommandTester.resetTestState();

        // Reset ElasticSearch
        this.elastic.resetIndex(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX,
                                CanonicalSearchConfiguration.CONFIG_NAME_V1);
    }

    @AfterClass
    @Override
    public void teardown() {
        SmartCacheCommandTester.teardown();
        try {
            this.elastic.teardown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void canonical_index_entity_01() throws Exception {
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(null, this.elastic);
        //@formatter:off
        builder.arguments("--index-document-format",
                          CanonicalSearchConfiguration.DOCUMENT_FORMAT_IES4_V3,
                          "--source-file",
                          new File("test-data/canonical", "test_entity_1.txt").getAbsolutePath(),
                          "--source-format",
                          RdfFormat.NAME);
        //@formatter:on
        builder.invoke();

        String[] indexes = {EXPECTED_ID_1};
        verifyIndexExists(indexes);

        try (ElasticSearchClient client = ElasticSearchClient.builder()
                                                             .host(this.elastic.getHost())
                                                             .port(this.elastic.getPort())
                                                             .index(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX)
                                                             .build()) {
            // Searching for Frank should provide results
            verifySearchResults(client, "Frank", EXPECTED_ID_1);

            // Are there any states? Not on these type of entities
            SearchResults results = client.getStates(EXPECTED_ID_1, SearchOptions.defaults());
            Assert.assertEquals(results.getResults().size(), 0);

            // Does Frank have any nationality?
            Document doc = client.getDocument(EXPECTED_ID_1, SecurityOptions.DISABLED);
            Assert.assertNotNull(doc.getProperty("nationality"));

            // There is no entry for Brian and no failures
            verifyNoSearchResults(client, "Brian");
        }
    }

    @Test
    public void canonical_index_entity_02() throws Exception {
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(null, this.elastic);
        //@formatter:off
        builder.arguments("--index-document-format",
                          CanonicalSearchConfiguration.DOCUMENT_FORMAT_IES4_V1,
                          "--source-file",
                          new File("test-data/canonical", "test_entity_2.txt").getAbsolutePath(),
                          "--source-format",
                          RdfFormat.NAME);
        //@formatter:on
        builder.invoke();

        String[] indexes = {EXPECTED_ID_2};
        verifyIndexExists(indexes);


        try (ElasticSearchEntityResolver client = ElasticSearchEntityResolver.builder()
                                                             .host(this.elastic.getHost())
                                                             .port(this.elastic.getPort())
                                                             .similarityIndex(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX)
                                                             .build()) {

            Document doc = new Document();
            doc.setProperty("id", "DIFFERING_ID");
            doc.setProperty("name_elements-forename", "Rfances");

            // Search for exact match - score 1.0
            SimilarityResult result = client.findSimilar(doc, 1, 0);

            Assert.assertNotEquals(result.getHits().length, 0);
            Assert.assertEquals(1.0, result.getHits()[0].getScore(), 0.1);

            // Search for similar match - score ~0.8
            doc.setProperty("name_elements-forename", "Frances");
            result = client.findSimilar(doc, 1, 0);

            Assert.assertNotEquals(result.getHits().length, 0);
            Assert.assertEquals(0.8, result.getHits()[0].getScore(), 0.5);
        }
    }

    private void verifyIndexExists(String[] ids) throws Exception {
        ParseResult<SmartCacheCommand>result = SmartCacheCommandTester.getLastParseResult();
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        verifyExpectedDocuments(ids);
    }

    @Override
    protected ElasticSearchIndexer<Document> createIndexer() {
        return ElasticSearchIndexer.<Document>create()
                                   .withCredentials(null, elastic.getPassword(), elastic.getElasticTlsCaCertString())
                                   .index(SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX)
                                   .host(this.elastic.getHost())
                                   .port(this.elastic.getPort())
                                   .build();
    }

}
