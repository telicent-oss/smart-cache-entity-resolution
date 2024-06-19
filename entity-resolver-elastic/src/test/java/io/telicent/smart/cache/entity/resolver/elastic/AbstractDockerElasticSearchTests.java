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

import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.AbstractElasticClient;
import io.telicent.smart.cache.search.elastic.ESTestCluster;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.SearchResults;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.*;

import static io.telicent.smart.cache.search.clusters.test.SearchTestClusters.DEFAULT_TEST_INDEX;

/**
 * A base class for writing tests against ElasticSearch that use Docker Test Containers to spin up a temporary
 * ElasticSearch instance to test against
 */
public class AbstractDockerElasticSearchTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDockerElasticSearchTests.class);

    public static final String ID_FIELD = "id";
    public static final String TEXT_FIELD = "text";
    public static final String NUMERICAL_FIELD = "priority";


    final Queue<AbstractElasticClient> clients = new LinkedList<>();
    protected final ESTestCluster elastic = createTestCluster();

    @SuppressWarnings("raw")
    protected ESTestCluster createTestCluster() {
        return new ESTestCluster();
    }

    @BeforeClass
    public void elasticSetup() {
        if (StringUtils.isNotBlank(System.getProperty("elastic.external"))) {
            LOGGER.warn("Using External ElasticSearch instance on localhost:{}, no setup needed",
                        System.getProperty("elastic.external"));
            this.elastic.useExternal(Integer.parseInt(System.getProperty("elastic.external")));
        } else {
            this.elastic.setup();
        }
    }

    @AfterClass
    public void elasticTeardown() {
        if (StringUtils.isNotBlank(System.getProperty("elastic.external"))) {
            LOGGER.warn("Using External ElasticSearch instance, no setup needed");
        } else {
            this.elastic.teardown();
        }
    }

    @BeforeMethod
    public void elasticCleanIndex() {
        this.elastic.resetIndex();
    }

    @AfterMethod
    public void elasticCleanupClients() throws Exception {
        while (!this.clients.isEmpty()) {
            AbstractElasticClient client = this.clients.poll();
            client.close();
        }
    }

    /**
     * Gets an indexer that connects to the test cluster
     *
     * @return Indexer
     */
    protected final ElasticSearchIndexer<Map<String, Object>> getIndexer() {
        return getIndexer(DEFAULT_TEST_INDEX);
    }

    /**
     * Gets an indexer that connects to the test cluster
     *
     * @param index index name
     * @return Indexer
     */
    protected ElasticSearchIndexer<Map<String, Object>> getIndexer(String index) {
        ElasticSearchIndexer<Map<String, Object>> indexer =
                ElasticSearchIndexer.<Map<String, Object>>create()
                                    .index(index)
                                    .usingOverwrites()
                                    .host(this.elastic.getHost())
                                    .port(this.elastic.getPort())
                                    .withCredentials(elastic.getUsername(), elastic.getPassword(), elastic.getElasticTlsCaCertString())
                                    .build();
        this.clients.add(indexer);
        return indexer;
    }


    /**
     * Gets an entity resolver that connects to the test cluster
     *
     * @return Entity Resolver
     */
    protected ElasticSearchEntityResolver getEntityResolver() {
        return getEntityResolver(DEFAULT_TEST_INDEX);
    }

    /**
     * Gets an entity resolver for a specific index that connects to the test cluster
     *
     * @param index The similarity index over which the resolver operates
     * @return Entity Resolver
     */
    protected ElasticSearchEntityResolver getEntityResolver(final String index) {
        ElasticSearchEntityResolver elasticSearchEntityResolver =
                new ElasticSearchEntityResolver(this.elastic.getHost(), this.elastic.getPort(),
                                                SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX,
                                                this.elastic.getUsername(), this.elastic.getPassword(), this.elastic.getElasticTlsCaCertString(), false);
        this.clients.add(elasticSearchEntityResolver);
        return elasticSearchEntityResolver;
    }



    /**
     * Populates some simple test data into the index
     */
    protected void populateSmallTestData() {
        LOGGER.info("Populating the small dataset into the test index");
        ElasticSearchIndexer<Map<String, Object>> indexer = getIndexer();
        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(Map.of(ID_FIELD, 1, TEXT_FIELD, "hello world", NUMERICAL_FIELD, 1));
        documents.add(Map.of(ID_FIELD, 2, TEXT_FIELD, "goodbye world", NUMERICAL_FIELD, 2));
        documents.add(Map.of(ID_FIELD, 3, TEXT_FIELD, "this is a test world", NUMERICAL_FIELD, 3));
        documents.add(Map.of(ID_FIELD, 4, TEXT_FIELD, "we want to query the world", NUMERICAL_FIELD, 4));
        documents.add(Map.of(ID_FIELD, 5, TEXT_FIELD, "world wide web", NUMERICAL_FIELD, 5));
        documents.add(Map.of(ID_FIELD, 6, TEXT_FIELD, "it's a small world after all", NUMERICAL_FIELD, 6));
        documents.add(
                Map.of(ID_FIELD, 7, TEXT_FIELD, "and now for something completely different", NUMERICAL_FIELD, 7));
        indexer.bulkIndex(x -> Integer.toString((Integer) x.get(ID_FIELD)), documents);
        indexer.flush(true);

        for (int i = 1; i <= 7; i++) {
            Assert.assertTrue(indexer.isIndexed(Integer.toString(i)));
        }
    }


    @DataProvider(name = "searchOptions")
    protected Object[][] getSearchOptions() {
        return new Object[][]{
                {SearchResults.UNLIMITED, SearchResults.FIRST_OFFSET},
                {SearchResults.UNLIMITED, 3},
                {SearchResults.UNLIMITED, 10},
                {SearchResults.UNLIMITED, 10_000},
                {0, SearchResults.FIRST_OFFSET},
                {0, 3},
                {0, 10},
                {0, 10_000},
                {3, SearchResults.FIRST_OFFSET},
                {3, 3},
                {3, 10},
                {3, 10_000},
                {10, SearchResults.FIRST_OFFSET},
                {10, 3},
                {10, 10},
                {10, 10_000},
                {100, SearchResults.FIRST_OFFSET},
                {100, 3},
                {100, 10},
                {100, 10_000},
                {1_000, SearchResults.FIRST_OFFSET},
                {1_000, 3},
                {1_000, 10},
                {1_000, 10_000}
        };
    }

    @DataProvider(name = "searchOptionsWithHighlighting")
    protected Object[][] getSearchOptionsWithHighlighting() {
        Object[] limits = new Object[]{SearchResults.UNLIMITED};
        Object[] offsets = new Object[]{SearchResults.FIRST_OFFSET};
        Object[] preTags = new Object[]{null, "", "<pre>", "<strong>", "<span class=\"highlight\">"};
        Object[] postTags = new Object[]{null, "", "</pre>", "</strong>", "</span>"};
        Object[][] params = new Object[limits.length * offsets.length * preTags.length * postTags.length][];
        int index = -1;
        for (Object limit : limits) {
            for (Object offset : offsets) {
                for (Object preTag : preTags) {
                    for (Object postTag : postTags) {
                        params[++index] = new Object[]{limit, offset, preTag, postTag};
                    }
                }
            }
        }
        return params;
    }
}
