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
package io.telicent.smart.cache.entity.resolver.elastic.similarity;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.telicent.smart.cache.search.model.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBasicSimilarityQueryGenerator {

    private static final String FIRSTNAME = "first_name";
    private static final String LASTNAME = "last_name";

    @Test
    public void test_generateQuery_happyPath() {
        // given
        Document doc = new Document();
        doc.setProperty(FIRSTNAME, "John");
        doc.setProperty(LASTNAME, "Doe");
        // when
        Query query = BasicSimilarityQueryGenerator.generateQuery(doc);
        // then
        Assert.assertNotNull(query);
    }

    @Test
    public void test_generateQuery_nullQuery() {
        // given
        Document doc = new Document();
        doc.setProperty("id", "WillBeIgnored");
        doc.setProperty("originalId", "SimilarlyIgnored");
        doc.setProperty("canonicaltype", "AgainIgnoredForGoodReason");
        // when
        Query query = BasicSimilarityQueryGenerator.generateQuery(doc);
        // then
        Assert.assertNull(query);
    }

}
