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

public class TestScorer {
    private static final String JSON = "{\"fieldScores\":{\"field_1\":5.0},\"scorerId\":null}";
    @Test
    public void test_toString_empty() {
        // given
        Scorer scorer = new Scorer();
        String expected = "{\"fieldScores\":{},\"scorerId\":null}";
        // when
        String result = scorer.toString();
        // then
        Assert.assertEquals(result, expected);
    }

    @Test
    public void test_toString_happy() {
        // given
        Scorer scorer = new Scorer();
        scorer.fieldScores.put("field_1", 5.0);

        String expected = "{\"fieldScores\":{\"field_1\":5.0},\"scorerId\":null}";
        // when
        String result = scorer.toString();
        // then
        Assert.assertEquals(result, expected);
    }

    @Test
    public void test_loadFromString_happy() {
        // given
        Scorer expectedScorer = new Scorer();
        expectedScorer.fieldScores.put("field_1", 5.0);
        // when
        Scorer scorer = Scorer.loadFromString(JSON);
        // then
        Assert.assertEquals(scorer.toString(), expectedScorer.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromString_invalidString() {
        // given
        // when
        // then
        Scorer.loadFromString("{\"badJson\":\"test\"}");
    }

    @Test
    public void test_loadFromNode_happy() {
        // given
        Scorer expectedScorer = new Scorer();
        expectedScorer.fieldScores.put("field_1", 5.0);
        JsonNode node = new TextNode(JSON);
        // when
        Scorer scorer = Scorer.loadFromNode(node);
        // then
        Assert.assertEquals(scorer.toString(), expectedScorer.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromNode_invalidString() {
        // given
        // when
        // then
        Scorer.loadFromNode(new TextNode("{\"badJson\":\"test\"}"));
    }
}
