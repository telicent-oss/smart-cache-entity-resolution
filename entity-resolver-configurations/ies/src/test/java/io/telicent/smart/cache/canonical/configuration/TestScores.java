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

public class TestScores {
    private static final String JSON = "{\"fieldScores\":{\"field_1\":5.0},\"id\":null}";
    @Test
    public void test_toString_empty() {
        // given
        Scores scores = new Scores();
        String expected = "{\"fieldScores\":{},\"id\":null}";
        // when
        String result = scores.toString();
        // then
        Assert.assertEquals(result, expected);
    }

    @Test
    public void test_toString_happy() {
        // given
        Scores scores = new Scores();
        scores.fieldScores.put("field_1", 5.0);

        String expected = "{\"fieldScores\":{\"field_1\":5.0},\"id\":null}";
        // when
        String result = scores.toString();
        // then
        Assert.assertEquals(result, expected);
    }

    @Test
    public void test_loadFromString_happy() {
        // given
        Scores expectedScores = new Scores();
        expectedScores.fieldScores.put("field_1", 5.0);
        // when
        Scores scores = Scores.loadFromString(JSON);
        // then
        Assert.assertEquals(scores.toString(), expectedScores.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromString_invalidString() {
        // given
        // when
        // then
        Scores.loadFromString("{\"badJson\":\"test\"}");
    }

    @Test
    public void test_loadFromNode_happy() {
        // given
        Scores expectedScores = new Scores();
        expectedScores.fieldScores.put("field_1", 5.0);
        JsonNode node = new TextNode(JSON);
        // when
        Scores scores = Scores.loadFromNode(node);
        // then
        Assert.assertEquals(scores.toString(), expectedScores.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromNode_invalidString() {
        // given
        // when
        // then
        Scores.loadFromNode(new TextNode("{\"badJson\":\"test\"}"));
    }

    @Test
    public void test_getScore_happy() {
        // given
        Scores scores = new Scores();
        scores.fieldScores.put("field_1", 5.0);
        double expectedScore = 5.0;
        // when
        double actualScore = scores.getScore("field_1");
        // then
        Assert.assertEquals(actualScore, expectedScore);
    }

    @Test
    public void test_getScore_miss() {
        // given
        Scores scores = new Scores();
        scores.fieldScores.put("field_1", 5.0);
        double expectedScore = 0.0;
        // when
        double actualScore = scores.getScore("field_2");
        // then
        Assert.assertEquals(actualScore, expectedScore);
    }

    @Test
    public void test_hasField_happy() {
        // given
        Scores scores = new Scores();
        scores.fieldScores.put("field_1", 5.0);
        // when
        // then
        Assert.assertTrue(scores.hasField("field_1"));
    }

    @Test
    public void test_hasField_miss() {
        // given
        Scores scores = new Scores();
        // when
        // then
        Assert.assertFalse(scores.hasField("field_1"));
    }

}
