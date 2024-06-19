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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import io.telicent.smart.cache.search.SearchException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;

public class TestCanonicalTypeConfigurationValidator {
    final ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
    final ElasticsearchIndicesClient mockElasticsearchIndicesClient = mock(ElasticsearchIndicesClient.class);
    final GetMappingResponse mockGetMappingResponse = mock(GetMappingResponse.class);
    final IndexMappingRecord mockIndexMappingRecord = mock(IndexMappingRecord.class);
    final TypeMapping mockTypeMapping = mock(TypeMapping.class);

    @Test
    public void test_validateConfig() throws IOException {
        // given
        when(mockClient.indices()).thenReturn(mockElasticsearchIndicesClient);
        when(mockElasticsearchIndicesClient.getMapping(any(GetMappingRequest.class))).thenReturn(mockGetMappingResponse);
        when(mockGetMappingResponse.get("index")).thenReturn(mockIndexMappingRecord);
        when(mockIndexMappingRecord.mappings()).thenReturn(mockTypeMapping);


        String index = "index";
        CanonicalTypeConfiguration canonicalTypeConfiguration = new CanonicalTypeConfiguration();
        // when
        // then
        CanonicalTypeConfigurationValidator.validateConfig(mockClient, index, canonicalTypeConfiguration);
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "Invalid configuration.*")
    public void test_validateConfig_mappingException() throws IOException {
        // given
        when(mockClient.indices()).thenReturn(mockElasticsearchIndicesClient);
        when(mockElasticsearchIndicesClient.getMapping(any(GetMappingRequest.class))).thenThrow(ElasticsearchException.class);
        String index = "index";
        CanonicalTypeConfiguration canonicalTypeConfiguration = new CanonicalTypeConfiguration();
        // when
        // then
        CanonicalTypeConfigurationValidator.validateConfig(mockClient, index, canonicalTypeConfiguration);
    }

    @Test
    public void test_validateConfigVsMapping_emptyMap_noFields_Exceptions() {
        // given
        // when
        // then
        CanonicalTypeConfigurationValidator.validateConfigVsMapping(emptyMap(), new CanonicalTypeConfiguration());
    }

    @Test(expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "Unrecognised field: don't recognise me\\?")
    public void test_validateConfigVsMapping_noMatch() {
        // given
        CanonicalTypeConfiguration canonicalTypeConfiguration = new CanonicalTypeConfiguration();
        CanonicalTypeConfiguration.TextField textField = new CanonicalTypeConfiguration.TextField();
        textField.name = "don't recognise me?";
        canonicalTypeConfiguration.fields = List.of(textField);

        Map<String,Property> propertyMap = new HashMap<>();
        propertyMap.put("something else", PropertyBuilders.text().build()._toProperty());
        // when
        // then
        CanonicalTypeConfigurationValidator.validateConfigVsMapping(propertyMap, canonicalTypeConfiguration);
    }

    @Test
    public void test_validateConfigVsMapping_happyPath() {
        // given
        CanonicalTypeConfiguration.TextField textField = new CanonicalTypeConfiguration.TextField();
        textField.name = "text-field";
        CanonicalTypeConfiguration.KeywordField keywordField = new CanonicalTypeConfiguration.KeywordField();
        keywordField.name = "keyword-field";
        CanonicalTypeConfiguration.NumberField numberField = new CanonicalTypeConfiguration.NumberField();
        numberField.name = "number-field";
        CanonicalTypeConfiguration.NumberField integerField = new CanonicalTypeConfiguration.NumberField();
        integerField.name = "integer-field";
        CanonicalTypeConfiguration.NumberField longField = new CanonicalTypeConfiguration.NumberField();
        longField.name = "long-field";
        CanonicalTypeConfiguration.NumberField floatField = new CanonicalTypeConfiguration.NumberField();
        floatField.name = "float-field";
        CanonicalTypeConfiguration.NumberField doubleField = new CanonicalTypeConfiguration.NumberField();
        doubleField.name = "double-field";
        CanonicalTypeConfiguration.BooleanField booleanField = new CanonicalTypeConfiguration.BooleanField();
        booleanField.name = "boolean-field";
        CanonicalTypeConfiguration.DateField dateField = new CanonicalTypeConfiguration.DateField();
        dateField.name = "date-field";
        CanonicalTypeConfiguration.LocationField locationField = new CanonicalTypeConfiguration.LocationField();
        locationField.name = "location-field";

        CanonicalTypeConfiguration canonicalTypeConfiguration = new CanonicalTypeConfiguration();
        canonicalTypeConfiguration.fields = List.of(textField,
                                                    keywordField,
                                                    numberField,
                                                    integerField,
                                                    longField,
                                                    floatField,
                                                    doubleField,
                                                    booleanField,
                                                    dateField,
                                                    locationField);

        Map<String,Property> propertyMap = new HashMap<>();
        propertyMap.put("text-field", PropertyBuilders.text().build()._toProperty());
        propertyMap.put("keyword-field", PropertyBuilders.keyword().build()._toProperty());
        propertyMap.put("number-field", PropertyBuilders.integer().build()._toProperty());
        propertyMap.put("integer-field", PropertyBuilders.integer().build()._toProperty());
        propertyMap.put("long-field", PropertyBuilders.long_().build()._toProperty());
        propertyMap.put("float-field", PropertyBuilders.float_().build()._toProperty());
        propertyMap.put("double-field", PropertyBuilders.double_().build()._toProperty());
        propertyMap.put("boolean-field", PropertyBuilders.boolean_().build()._toProperty());
        propertyMap.put("date-field", PropertyBuilders.date().build()._toProperty());
        propertyMap.put("location-field", PropertyBuilders.geoPoint().build()._toProperty());
        // when
        // then
        CanonicalTypeConfigurationValidator.validateConfigVsMapping(propertyMap, canonicalTypeConfiguration);
    }

    @DataProvider(name = "useIncorrectFieldTypes")
    protected Object[][] useIncorrectFieldTypes() {
        CanonicalTypeConfiguration.TextField wrongTextField = new CanonicalTypeConfiguration.TextField();
        wrongTextField.name = "float-field";
        CanonicalTypeConfiguration.KeywordField wrongKeywordField = new CanonicalTypeConfiguration.KeywordField();
        wrongKeywordField.name = "number-field";
        CanonicalTypeConfiguration.NumberField wrongNumberField = new CanonicalTypeConfiguration.NumberField();
        wrongNumberField.name = "text-field";
        CanonicalTypeConfiguration.NumberField wrongIntegerField = new CanonicalTypeConfiguration.NumberField();
        wrongIntegerField.name = "boolean-field";
        CanonicalTypeConfiguration.NumberField wrongLongField = new CanonicalTypeConfiguration.NumberField();
        wrongLongField.name = "keyword-field";
        CanonicalTypeConfiguration.NumberField wrongFloatField = new CanonicalTypeConfiguration.NumberField();
        wrongFloatField.name = "location-field";
        CanonicalTypeConfiguration.NumberField wrongDoubleField = new CanonicalTypeConfiguration.NumberField();
        wrongDoubleField.name = "date-field";
        CanonicalTypeConfiguration.BooleanField wrongBooleanField = new CanonicalTypeConfiguration.BooleanField();
        wrongBooleanField.name = "keyword-field";
        CanonicalTypeConfiguration.DateField wrongDateField = new CanonicalTypeConfiguration.DateField();
        wrongDateField.name = "location-field";
        CanonicalTypeConfiguration.LocationField wrongLocationField = new CanonicalTypeConfiguration.LocationField();
        wrongLocationField.name = "date-field";

        return new Object[][]{
                {wrongTextField},
                {wrongKeywordField},
                {wrongNumberField},
                {wrongIntegerField},
                {wrongLongField},
                {wrongFloatField},
                {wrongDoubleField},
                {wrongBooleanField},
                {wrongDateField},
                {wrongLocationField}
        };
    }

    @Test(dataProvider = "useIncorrectFieldTypes", expectedExceptions = SearchException.class, expectedExceptionsMessageRegExp = "Property.*needs to be .*")
    public void test_validateConfigVsMapping_incorrectTypes(CanonicalTypeConfiguration.SimilarityField wrongField) {
        // given
        CanonicalTypeConfiguration canonicalTypeConfiguration = new CanonicalTypeConfiguration();
        canonicalTypeConfiguration.fields = List.of(wrongField);

        Map<String,Property> propertyMap = new HashMap<>();
        propertyMap.put("text-field", PropertyBuilders.text().build()._toProperty());
        propertyMap.put("keyword-field", PropertyBuilders.keyword().build()._toProperty());
        propertyMap.put("number-field", PropertyBuilders.integer().build()._toProperty());
        propertyMap.put("integer-field", PropertyBuilders.integer().build()._toProperty());
        propertyMap.put("long-field", PropertyBuilders.long_().build()._toProperty());
        propertyMap.put("float-field", PropertyBuilders.float_().build()._toProperty());
        propertyMap.put("double-field", PropertyBuilders.double_().build()._toProperty());
        propertyMap.put("boolean-field", PropertyBuilders.boolean_().build()._toProperty());
        propertyMap.put("date-field", PropertyBuilders.date().build()._toProperty());
        propertyMap.put("location-field", PropertyBuilders.geoPoint().build()._toProperty());
        // then
        // when
        CanonicalTypeConfigurationValidator.validateConfigVsMapping(propertyMap, canonicalTypeConfiguration);
    }

}
