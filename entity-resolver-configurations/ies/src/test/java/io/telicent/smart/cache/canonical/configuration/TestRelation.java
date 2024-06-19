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

public class TestRelation {

    private static final String JSON = "{\"weight\":5, \"fields\" : [\"field_1\", \"field_2\"]}";
    @Test
    public void test_toString_empty() {
        // given
        Relation relation = new Relation();
        String expected = "{\"resolverId\":null,\"fields\":[],\"weight\":0}";
        // when
        String result = relation.toString();
        // then
        Assert.assertEquals(result, expected);
    }

    @Test
    public void test_toString_happy() {
        // given
        Relation relation = new Relation();
        relation.fields.add("field_1");
        relation.fields.add("field_2");
        relation.weight = 5;
        relation.resolverId = "resolver_id";

        String expectedResult = "{\"resolverId\":\"resolver_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}";
        // when
        String actualResult = relation.toString();
        // then
        Assert.assertEquals(actualResult, expectedResult);
    }

    @Test
    public void test_loadFromString_happy() {
        // given
        Relation expectedRelation = new Relation();
        expectedRelation.fields.add("field_1");
        expectedRelation.fields.add("field_2");
        expectedRelation.weight = 5;
        // when
        Relation relation = Relation.loadFromString(JSON);
        // then
        Assert.assertEquals(relation.toString(), expectedRelation.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromString_invalidString() {
        // given
        // when
        // then
        Relation.loadFromString("{\"badJson\":\"test\"}");
    }

    @Test
    public void test_loadFromNode_happy() {
        // given
        Relation expectedRelation = new Relation();
        expectedRelation.fields.add("field_1");
        expectedRelation.fields.add("field_2");
        expectedRelation.weight = 5;

        JsonNode node = new TextNode(JSON);
        // when
        Relation relation = Relation.loadFromNode(node);
        // then
        Assert.assertEquals(relation.toString(), expectedRelation.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromNode_invalidString() {
        // given
        // when
        // then
        Relation.loadFromNode(new TextNode("{\"badJson\":\"test\"}"));
    }
}
