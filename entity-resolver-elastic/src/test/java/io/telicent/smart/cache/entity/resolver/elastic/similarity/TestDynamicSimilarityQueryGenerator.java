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
import io.telicent.smart.cache.canonical.QueryVisitor;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfigurationMap;
import io.telicent.smart.cache.entity.resolver.elastic.index.CachedIndexMapper;
import io.telicent.smart.cache.search.model.Document;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class TestDynamicSimilarityQueryGenerator {

    private static final String HAPPY_PATH = "src/test/resources/dynamic_config_sample.yml";
    private static final String RANDOM_ID = RandomStringUtils.random(6);

    @Test
    public void test_generateQuery_invalidConfig() {
        // given
        Document doc = new Document();
        doc.setProperty("firstname", "Danielle");
        doc.setProperty("surname", "Mitchell");
        CachedIndexMapper.load("non-existent-path.yml");

        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "missing");

        // then
        Assert.assertNull(query);
    }

    @Test
    public void test_generateQuery_nullMap() {
        // given
        Document doc = new Document();
        doc.setProperty("firstname", "Danielle");
        doc.setProperty("surname", "Mitchell");
        CanonicalTypeConfigurationMap map = null;
        CachedIndexMapper.loadCTMapFromMap(map);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "missing");
        // then
        Assert.assertNull(query);
    }

    @Test
    public void test_generateQuery_emptyMap() {
        // given
        Document doc = new Document();
        doc.setProperty("firstname", "Danielle");
        doc.setProperty("surname", "Mitchell");
        CanonicalTypeConfigurationMap map = new CanonicalTypeConfigurationMap();
        CachedIndexMapper.loadCTMapFromMap(map);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "missing");
        // then
        Assert.assertNull(query);
    }

    @Test
    public void test_generateQuery_ignoreFields() {
        // given
        Document doc = new Document();
        doc.setProperty("id", RANDOM_ID);
        doc.setProperty("originalId", RANDOM_ID);
        doc.setProperty("date_of_birth-year", RANDOM_ID);
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "CoreCanonicalIesPerson");
        // then
        Assert.assertNull(query);
    }

    @Test
    public void test_generateQuery_noMatchField() {
        // given
        Document doc = new Document();
        doc.setProperty("nomatch", RANDOM_ID);
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "CoreCanonicalIesPerson");
        // then
        Assert.assertNull(query);
    }


    @Test
    public void test_generateQuery_happyPath() {
        // given
        List<String> fieldList = List.of(
                "text-field",
                "keyword-field",
                "integer-field",
                "long-field",
                "float-field",
                "double-field",
                "number-field",
                "location-field",
                "date-field",
                "boolean-field"
        );

        Document doc = new Document();
        fieldList.forEach(f -> doc.setProperty(f, RANDOM_ID));

        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "TestAllTypesMinimum");
        // then
        String queryString = query.toString();
        fieldList.forEach(f -> Assert.assertTrue(queryString.contains(f), "Query should contain: " + f));
        Assert.assertNotNull(query);
    }

    @Test
    public void test_generateQuery_happyPath_moreDetailedQuery() {
        // given
        List<String> fieldList = List.of(
                "text-field",
                "keyword-field",
                "integer-field",
                "long-field",
                "float-field",
                "double-field",
                "number-field",
                "location-field",
                "date-field",
                "boolean-field"
        );

        Document doc = new Document();
        fieldList.forEach(f -> doc.setProperty(f, RANDOM_ID));

        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "TestAllTypesMaximum");
        // then
        String queryString = query.toString();
        fieldList.forEach(f -> Assert.assertTrue(queryString.contains(f), "Query should contain: " + f));
        Assert.assertNotNull(query);
    }


    @Test
    public void test_generateQuery_missingKey() {
        // given
        Document doc = new Document();
        doc.setProperty("firstname", "Danielle");
        doc.setProperty("surname", "Mitchell");
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(doc, "missing");

        // then
        Assert.assertNull(query);
    }

    @Test
    public void test_generateQuery_nullConfig() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Query query = DynamicSimilarityQueryGenerator.generateQuery(new Document(), (CanonicalTypeConfiguration) null);
        // then
        Assert.assertNull(query);
    }

    @Test
    public void test_obtainIndex_invalidConfig() {
        // given
        CachedIndexMapper.load("non-existent-path.yml");
        // when
        String index = DynamicSimilarityQueryGenerator.obtainIndex("missing");
        // then
        Assert.assertNull(index);
    }

    @Test
    public void test_obtainIndex_emptyMap() {
        // given
        CanonicalTypeConfigurationMap map = null;
        CachedIndexMapper.loadCTMapFromMap(map);
        // when
        String index = DynamicSimilarityQueryGenerator.obtainIndex("missing");
        // then
        Assert.assertNull(index);
    }

    @Test
    public void test_obtainIndex_missingKey() {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        String index = DynamicSimilarityQueryGenerator.obtainIndex("missing");
        // then
        Assert.assertNull(index);
    }

    @Test
    public void test_obtainIndex_happyPath() {
        // given
        String expectedIndex = "canonical_boxers";
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        String actualIndex = DynamicSimilarityQueryGenerator.obtainIndex("Boxers");
        // then
        Assert.assertNotNull(actualIndex);
        Assert.assertEquals(actualIndex, expectedIndex);
    }

    @Test
    public void test_obtainIndex_configNoIndex() {
        // given
        CanonicalTypeConfigurationMap map = new CanonicalTypeConfigurationMap();
        CanonicalTypeConfiguration configuration = new CanonicalTypeConfiguration();
        map.putIfAbsent("entry", configuration);
        CachedIndexMapper.loadCTMapFromMap(map);
        // when
        String index = DynamicSimilarityQueryGenerator.obtainIndex("entry");
        // then
        Assert.assertNull(index);
    }

    @Test(dataProvider = "fieldTypeNames")
    public void test_useExactQuery_happyPath(String field) {
        // given
        CachedIndexMapper.load(HAPPY_PATH);
        // when
        Document doc = new Document();
        doc.setProperty(field, "Anything");
        Query actualQuery = DynamicSimilarityQueryGenerator.generateQuery(doc, "TestAllTypesExactMatch");
        // then
        Assert.assertNotNull(actualQuery);
        String expectedQuery = "Query: {\"bool\":{\"should\":[{\"term\":{\"" + field + "\":{\"boost\":1.0,\"_name\":\"" + field + "\",\"value\":\"Anything\"}}}]}}";
        Assert.assertEquals(actualQuery.toString(), expectedQuery);
    }


    @Test(dataProvider = "similarityFieldTypes")
    public void test_useExactQueryIfSpecified(CanonicalTypeConfiguration.SimilarityField similarityField) {
        // given
        similarityField.exactMatch = true;
        similarityField.name = "FieldName";
        similarityField.required = true;

        CanonicalTypeConfiguration configuration = new CanonicalTypeConfiguration();
        configuration.fields = List.of(similarityField);
        CanonicalTypeConfigurationMap map = new CanonicalTypeConfigurationMap();
        map.put("exactQueryConfiguration", configuration);

        CachedIndexMapper.loadCTMapFromMap(map);
        // when
        Document doc = new Document();
        doc.setProperty("FieldName", "Anything");

        Query actualQuery = DynamicSimilarityQueryGenerator.generateQuery(doc, "exactQueryConfiguration");

        // then
        Assert.assertNotNull(actualQuery);
        String expectedQuery = "Query: {\"bool\":{\"should\":[{\"term\":{\"FieldName\":{\"boost\":1.0,\"_name\":\"FieldName\",\"value\":\"Anything\"}}}]}}";
        Assert.assertEquals(actualQuery.toString(), expectedQuery);
    }

    @Test(dataProvider = "exactMatchFieldTypes")
    public void test_useExactQueryEvenIfNotSpecified(CanonicalTypeConfiguration.SimilarityField similarityField) {
        // given
        similarityField.name = "FieldName";
        similarityField.required = true;

        CanonicalTypeConfiguration configuration = new CanonicalTypeConfiguration();
        configuration.fields = List.of(similarityField);
        CanonicalTypeConfigurationMap map = new CanonicalTypeConfigurationMap();
        map.put("exactQueryConfiguration", configuration);

        CachedIndexMapper.loadCTMapFromMap(map);

        // when
        Document doc = new Document();
        doc.setProperty("FieldName", "Anything");

        Query actualQuery = DynamicSimilarityQueryGenerator.generateQuery(doc, "exactQueryConfiguration");

        // then
        Assert.assertNotNull(actualQuery);
        String expectedQuery = "Query: {\"bool\":{\"should\":[{\"term\":{\"FieldName\":{\"boost\":1.0,\"value\":\"Anything\"}}}]}}";
        Assert.assertEquals(actualQuery.toString(), expectedQuery);
    }

    @DataProvider(name = "fieldTypeNames")
    protected Object[][] useFieldTypeNames() {
        return new Object[][]{
                {"text-field"},
                {"number-field"},
                {"boolean-field"},
                {"keyword-field"},
                {"date-field"},
                {"location-field"},
                {"integer-field"},
                {"long-field"},
                {"float-field"},
                {"double-field"},
        };
    }

        @DataProvider(name = "similarityFieldTypes")
    protected Object[][] useSimilarityFieldTypes() {
        CanonicalTypeConfiguration.SimilarityField similarityField = new CanonicalTypeConfiguration.SimilarityField() {
            @Override
            public void accept(QueryVisitor visitor, Object object) {}
        };
        return new Object[][]{
                {similarityField},
                {new CanonicalTypeConfiguration.BooleanField()},
                {new CanonicalTypeConfiguration.TextField()},
                {new CanonicalTypeConfiguration.NumberField()},
                {new CanonicalTypeConfiguration.DateField()},
                {new CanonicalTypeConfiguration.KeywordField()},
                {new CanonicalTypeConfiguration.LocationField()},
                };
    }

    @DataProvider(name = "exactMatchFieldTypes")
    protected Object[][] useExactMatchFieldTypes() {
        return new Object[][]{
                {new CanonicalTypeConfiguration.BooleanField()},
                {new CanonicalTypeConfiguration.KeywordField()},
                };
    }
}
