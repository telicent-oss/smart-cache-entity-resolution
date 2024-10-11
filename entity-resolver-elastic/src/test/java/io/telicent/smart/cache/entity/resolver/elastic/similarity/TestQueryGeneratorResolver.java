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
import io.telicent.smart.cache.entity.resolver.elastic.index.CachedIndexMapper;
import io.telicent.smart.cache.search.model.Document;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class TestQueryGeneratorResolver {
    private static final String HAPPY_PATH = "src/test/resources/dynamic_config_sample.yml";
    private static final String RANDOM_ID = RandomStringUtils.secure().nextAlphanumeric(6);

    private static final String CANONICAL_TYPE = "canonicaltype";

    @AfterMethod
    public void test_cleanUp() {
        CachedIndexMapper.clearCache();
    }

    @Test
    public void test_load_null() {
        // given
        // when
        CachedIndexMapper.load(null);
        // then
        Assert.assertNull(CachedIndexMapper.getCanonicalTypeConfiguration(RANDOM_ID));
    }

    @Test
    public void test_load_badPath() {
        // given
        // when
        CachedIndexMapper.load("missing.yml");
        // then
        Assert.assertNull(CachedIndexMapper.getCanonicalTypeConfiguration(RANDOM_ID));
    }

    @Test
    public void test_load_twice() {
        // given
        // when
        CachedIndexMapper.load(HAPPY_PATH);
        CachedIndexMapper.load("missing.yml");
        // then
        Assert.assertNotNull(CachedIndexMapper.getCanonicalTypeConfiguration("FamousDates"));
    }

    @Test
    public void test_generateQuery_withoutDynamicLoad_useBasic() {
        // given
        Document doc = new Document();
        doc.setProperty("firstname", RANDOM_ID);
        doc.setProperty("surname", RANDOM_ID);
        Query expectedQuery = BasicSimilarityQueryGenerator.generateQuery(doc);
        // when
        Query actualQuery = QueryGeneratorResolver.generateQuery(doc, null);
        // then
        Assert.assertNotNull(actualQuery);
        Assert.assertEquals(actualQuery.toString(), expectedQuery.toString());
    }

    @Test
    public void test_generateQuery_withoutCanonicalType_useBasic() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        Document doc = new Document();
        doc.setProperty("firstname", RANDOM_ID);
        doc.setProperty("surname", RANDOM_ID);
        Query expectedQuery = BasicSimilarityQueryGenerator.generateQuery(doc);
        // when
        Query actualQuery = QueryGeneratorResolver.generateQuery(doc, null);
        // then
        Assert.assertNotNull(actualQuery);
        Assert.assertEquals(actualQuery.toString(), expectedQuery.toString());
    }

    @Test
    public void test_generateQuery_missingCanonicalType_returnNull() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        Document doc = new Document();
        doc.setProperty("firstname", RANDOM_ID);
        doc.setProperty("surname", RANDOM_ID);
        doc.setProperty(CANONICAL_TYPE, "missing");
        // when
        Query actualQuery = QueryGeneratorResolver.generateQuery(doc, null);
        // then
        Assert.assertNull(actualQuery);
    }

    @Test
    public void test_generateQuery_happyPath() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        Document doc = new Document();
        doc.setProperty("text-field", RANDOM_ID);
        doc.setProperty(CANONICAL_TYPE, "TestAllTypesMinimum");
        Query expectedQuery = BasicSimilarityQueryGenerator.generateQuery(doc);
        // when
        Query actualQuery = QueryGeneratorResolver.generateQuery(doc, null);
        // then
        Assert.assertNotNull(actualQuery);
        Assert.assertNotEquals(actualQuery.toString(), expectedQuery.toString());
    }

    @Test
    public void test_resolveIndex_withoutDynamicLoad_returnDefault() {
        // given
        Document doc = new Document();
        doc.setProperty(CANONICAL_TYPE, RANDOM_ID);
        String defaultIndex = RANDOM_ID;
        // when
        String actualIndex = QueryGeneratorResolver.resolveIndex(doc, defaultIndex);
        // then
        Assert.assertNotNull(actualIndex);
        Assert.assertEquals(actualIndex, defaultIndex);
    }

    @Test
    public void test_resolveIndex_withoutCanonicalType_returnDefault() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);

        Document doc = new Document();
        String defaultIndex = RANDOM_ID;
        // when
        String actualIndex = QueryGeneratorResolver.resolveIndex(doc, defaultIndex);
        // then
        Assert.assertNotNull(actualIndex);
        Assert.assertEquals(actualIndex, defaultIndex);
    }

    @Test
    public void test_resolveIndex_withoutMatch_returnDefault() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        Document doc = new Document();
        doc.setProperty(CANONICAL_TYPE, "no match");
        String defaultIndex = RANDOM_ID;
        // when
        String actualIndex = QueryGeneratorResolver.resolveIndex(doc, defaultIndex);
        // then
        Assert.assertNotNull(actualIndex);
        Assert.assertEquals(actualIndex, defaultIndex);
    }

    @Test
    public void test_resolveIndex_happyPath() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        Document doc = new Document();
        doc.setProperty(CANONICAL_TYPE, "Palaces");
        String expectedIndex = "canonical_palaces";
        // when
        String actualIndex = QueryGeneratorResolver.resolveIndex(doc, RANDOM_ID);
        // then
        Assert.assertNotNull(actualIndex);
        Assert.assertEquals(actualIndex, expectedIndex);
    }

}
