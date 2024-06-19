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

import io.telicent.smart.cache.canonical.exception.ValidationException;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DockerTestElasticSearchEntityResolverSimilarityConfig extends AbstractElasticSearchClientTests {

    @BeforeMethod
    private void init() {
        this.elastic.resetIndex("er_config_models");
    }

    public static final String EMPTY_MODEL_JSON = "{\"modelId\":\"test_id\",\"indexes\":[],\"relations\":[],\"scorers\":[]}";
    public static final String PARTIAL_MODEL_JSON = "{\"modelId\":\"test_id\",\"indexes\":[\"updated_index\"],\"relations\":[],\"scorers\":[]}";

    @Test
    public void test_configEndpoints_and_to_string() {
        // given
        ElasticSearchEntityResolver client = this.getEntityResolver();
        // when
        String actual = client.toString();
        // then
        Assert.assertTrue(actual.matches("localhost:[0-9]*/tests_similarity"));

        // and ...
        client.addConfig("models", EMPTY_MODEL_JSON, "model_id");
        String result = client.readConfig("models", "model_id");
        client.deleteConfig("models", "model_id");
        String actualResult = client.readConfig("models", "model_id");
        Assert.assertTrue(StringUtils.isBlank(actualResult));
        client.addConfig("models", EMPTY_MODEL_JSON, "model_id");
        client.updateConfig("models", PARTIAL_MODEL_JSON, "model_id");
        String laterResult = client.readConfig("models", "model_id");
        Assert.assertEquals(laterResult, PARTIAL_MODEL_JSON);

        String allResults = client.readAllConfig("models");
        Assert.assertFalse(allResults.contains(result));
        Assert.assertTrue(allResults.contains(laterResult));
    }


    @Test
    public void test_readConfig_fullModel() {
        // given
        ElasticSearchEntityResolver client = this.getEntityResolver();

        client.addConfig("fullmodel", EMPTY_MODEL_JSON, "model_id");
        String result = client.readConfig("models", "model_id");
        client.deleteConfig("fullmodel", "model_id");
        boolean caught = false;
        try {
            // when
            client.readConfig("fullmodel", "model_id");
        } catch (ValidationException exception) {
            caught = true;
        }
        // then
        Assert.assertTrue(caught);
        client.addConfig("fullmodel", EMPTY_MODEL_JSON, "model_id");
        client.updateConfig("fullmodel", PARTIAL_MODEL_JSON, "model_id");
        // and
        String laterResult = client.readConfig("fullmodel", "model_id");
        Assert.assertNotEquals(result, laterResult);
        Assert.assertNotEquals(laterResult, PARTIAL_MODEL_JSON);
    }

    @Test
    public void test_readAllConfig_fullModel() {
        // given
        ElasticSearchEntityResolver client = this.getEntityResolver();
        client.addConfig("fullmodel", EMPTY_MODEL_JSON, "model_id");
        client.deleteConfig("fullmodel", "model_id");
        boolean caught = false;
        String expected = "{}";
        try {
            // when
            String actual = client.readAllConfig("fullmodel");
            Assert.assertEquals(actual, expected);
        } catch (ValidationException exception) {
            caught = true;
        }
        // then
        Assert.assertFalse(caught);
        client.addConfig("fullmodel", EMPTY_MODEL_JSON, "model_id");
        client.updateConfig("fullmodel", EMPTY_MODEL_JSON, "model_id");
        // and
        String laterActual = client.readAllConfig("fullmodel");
        String laterExpected = "{\"model_id\":{\"modelId\":\"model_id\",\"indexes\":[],\"relations\":[],\"scorers\":[]}}";
        Assert.assertEquals(laterActual, laterExpected);
    }

    @Test
    public void test_validateConfig_model() {
        // given
        ElasticSearchEntityResolver client = this.getEntityResolver();
        client.addConfig("models", EMPTY_MODEL_JSON, "model_id");
        boolean caught = false;
        String expected ="";
        try {
            // when
            String actual = client.validateConfig("models", "model_id", SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX);
            Assert.assertEquals(actual, expected);
        } catch (ValidationException exception) {
            caught = true;
        }
        // then
        Assert.assertFalse(caught);
    }
}
