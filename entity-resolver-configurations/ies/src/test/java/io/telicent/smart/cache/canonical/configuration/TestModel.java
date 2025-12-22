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
package io.telicent.smart.cache.canonical.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static io.telicent.smart.cache.canonical.configuration.TestFullModel.FULL_MODEL_HAPPY;

public class TestModel {

    public static final String JSON = "{\"id\":\"test_id\",\"index\":\"canonical_index\",\"relations\":[\"resolver-1\",\"resolver-2\",\"resolver-3\"],\"scores\":\"score-1\"}";

    private Model getExpectedModel() {
        Model expectedModel = new Model();
        expectedModel.modelId = "test_id";
        expectedModel.index = "canonical_index";
        expectedModel.relations = List.of("resolver-1", "resolver-2", "resolver-3");
        expectedModel.scores = "score-1";
        return expectedModel;
    }

    @Test
    public void test_loadFromString_happy() {
        // given
        Model expectedModel = getExpectedModel();
        // when
        Model model = Model.loadFromString(JSON);
        // then
        Assert.assertEquals(model.toString(), expectedModel.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromString_invalidString() {
        // given
        // when
        // then
        Model.loadFromString("{\"badJson\":\"test\"}");
    }

    @Test
    public void test_loadFromNode_empty() {
        // given
        Model expectedModel = new Model();
        JsonNode node = new TextNode("{}");
        // when
        Model model = Model.loadFromNode(node);
        // then
        Assert.assertEquals(model.toString(), expectedModel.toString());
    }

    @Test
    public void test_loadFromNode_happy() {
        // given
        Model expectedModel = getExpectedModel();
        JsonNode node = new TextNode(JSON);
        // when
        Model model = Model.loadFromNode(node);
        // then
        Assert.assertEquals(model.toString(), expectedModel.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromNode_invalid() {
        // given
        JsonNode node = new TextNode("rubbish");
        // when
        // then
        Model.loadFromNode(node);
    }

    @Test
    public void test_loadFromFullModel_happyPath() {
        // given
        FullModel fullModel = FullModel.loadFromString(FULL_MODEL_HAPPY);
        // when
        Model model = Model.loadFromFullModel(fullModel);
        // then
        Assert.assertNotNull(model);
        Assert.assertEquals(model.modelId, "testcase");
        Assert.assertEquals(model.index, "canonical_index");
        Assert.assertEquals(model.relations, List.of("testcase"));
        Assert.assertEquals(model.scores, "testcase");
    }
}
