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

public class TestCanonicalTypeConfiguration {

    private static final String INVALID_FILE_PATH = "src/test/resources/canonical/invalid_yaml.yml";
    private static final String INVALID_STRING = "{ \"rubbish\" : \"NotYAML\" }";
    private static final String UNKNOWN_FILE_PATH = "src/test/resources/canonical/unrecognised_item.yml";
    private static final String UNKNOWN_STRING = """
            type: CoreCanonicalTestType
            fields:
              - name: unknown-name
                type: unknown
                required: true
                boost: 1.2
            """;
    private static final String HAPPY_FILE_PATH = "src/test/resources/canonical/good_sample_item.yml";

    public static final String HAPPY_STRING = """
            type: CoreCanonicalTestType
            fields:
              - name: text-field-fuzzy
                type: text
                required: true
                boost: 1.2
                fuzziness:
                  enabled: true
                  min: 0
                  max: 3
              - name: text-field-nonfuzzy
                type: text
                required: true
                boost: 4.0
                fuzziness:
                  enabled: false
              - name: keyword-field-not-required
                type: keyword
                required: false
                boost: 0.1
              - name: keyword-field-no-boost
                type: keyword
                required: true
              - name: age
                type: number
                required: true
                boost: 5.0
                decay:
                  offset: "10"
                  scale: "10"
                  decay: 0.5
              - name: date-field
                type: date
                required: true
                boost: 1.5
                distance:
                  pivot: 3d
              - name: date-field-no-distance
                type: number
                required: false
                boost: 1.5
              - name: location-field
                type: geo-point
                required: false
                boost: 10.0
                distance:
                  pivot: 5m
              - name: location-field-no-distance
                type: geo-point
                required: false
                boost: 1.5
            """;

    private static final String HAPPY_STRING_OUTPUT = "{\"type\":\"CoreCanonicalTestType\",\"index\":\"\",\"fields\":[{\"name\":\"text-field-fuzzy\",\"type\":\"text\",\"required\":true,\"boost\":1.2,\"exactMatch\":false,\"fuzziness\":{\"enabled\":true,\"min\":0,\"max\":3}},{\"name\":\"text-field-nonfuzzy\",\"type\":\"text\",\"required\":true,\"boost\":4.0,\"exactMatch\":false,\"fuzziness\":{\"enabled\":false,\"min\":null,\"max\":null}},{\"name\":\"keyword-field-not-required\",\"type\":\"keyword\",\"required\":false,\"boost\":0.1,\"exactMatch\":false},{\"name\":\"keyword-field-no-boost\",\"type\":\"keyword\",\"required\":true,\"boost\":1.0,\"exactMatch\":false},{\"name\":\"age\",\"type\":\"number\",\"required\":true,\"boost\":5.0,\"exactMatch\":false,\"decay\":{\"decay\":0.5,\"offset\":\"10\",\"scale\":\"10\"}},{\"name\":\"date-field\",\"type\":\"date\",\"required\":true,\"boost\":1.5,\"exactMatch\":false,\"distance\":{\"pivot\":\"3d\"}},{\"name\":\"date-field-no-distance\",\"type\":\"number\",\"required\":false,\"boost\":1.5,\"exactMatch\":false,\"decay\":null},{\"name\":\"location-field\",\"type\":\"geo-point\",\"required\":false,\"boost\":10.0,\"exactMatch\":false,\"distance\":{\"pivot\":\"5m\"}},{\"name\":\"location-field-no-distance\",\"type\":\"geo-point\",\"required\":false,\"boost\":1.5,\"exactMatch\":false,\"distance\":null}]}";

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigFromFile_badInput() {
        // given
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile(INVALID_FILE_PATH);
        // then
        Assert.assertNull(configuration);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigFromFile_badPath() {
        // given
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile("missingFile");
        // then
        Assert.assertNull(configuration);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigFromFile_unrecognisedInput() {
        // given
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile(UNKNOWN_FILE_PATH);
        // then
        Assert.assertNull(configuration);
    }

    @Test
    public void loadConfigFromFile_goodInput() {
        // given
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile(HAPPY_FILE_PATH);
        // then
        Assert.assertNotNull(configuration);
        configuration.getField("nationality");
    }

    @Test
    public void getField_happyPath() {
        // given
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile(HAPPY_FILE_PATH);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        // then
        Assert.assertNotNull(field);
    }

    @Test
    public void getField_missing() {
        // given
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile(HAPPY_FILE_PATH);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("no field");
        // then
        Assert.assertNull(field);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigString_badInput() {
        // given
        // when
        // then
        CanonicalTypeConfiguration.loadFromString(INVALID_STRING);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigString_emptyString() {
        // given
        // when
        // then
        CanonicalTypeConfiguration.loadFromString("");
    }

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigString_nullString() {
        // given
        // when
        // then
        CanonicalTypeConfiguration.loadFromString(null);
    }


    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigString_unrecognisedInput() {
        // given
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromConfigFile(UNKNOWN_STRING);
        // then
        Assert.assertNull(configuration);
    }

    @Test
    public void loadConfigString_goodInput() {
        // given
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromString(HAPPY_STRING);
        // then
        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.toString(), HAPPY_STRING_OUTPUT);
        Assert.assertNull(configuration.getField("nationality"));
        Assert.assertNotNull(configuration.getField("text-field-fuzzy"));
    }

    @Test
    public void getFieldString_happyPath() {
        // given
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromString(HAPPY_STRING);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        // then
        Assert.assertNotNull(field);
    }

    @Test
    public void getFieldString_missing() {
        // given
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromString(HAPPY_STRING);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("no field");
        // then
        Assert.assertNull(field);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void loadConfigString_missingTypeField() {
        // given
        String missingTypeFieldString = """
            type: CoreCanonicalTestType
            fields:
              - name: unknown-name
                required: true
                boost: 1.2
            """;
        // when
        // then
        CanonicalTypeConfiguration.loadFromString(missingTypeFieldString);
    }

    @Test
    public void loadConfigString_noFields() {
        // given
        String noFieldsData = "type: CoreCanonicalTestType";
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromString(noFieldsData);
        // then
        Assert.assertNotNull(configuration);
    }

    @Test
    public void loadConfigString_reducedDataNoIndexOrCanonicalType() {
        // given
        String reducedData = """
            fields:
              - name: unknown-name
                type: text
                boost: 1.2
            """;
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromString(reducedData);
        // then
        Assert.assertNotNull(configuration);
    }


    @Test
    public void loadConfigString_JSON_reducedDataNoIndexOrCanonicalType() {
        // given
        String jsonString = """
{
    "index": "canonical_hostels",
    "fields":
    [
        {
            "name": "name",
            "type": "text",
            "required": true
        },
        {
            "name": "village",
            "type": "text",
            "boost": 1.5,
            "required": true
        },
        {
            "name": "city",
            "type": "text",
            "boost": 1.5,
            "required": true
        },
        {
            "name": "country",
            "type": "text",
            "boost": 1.5,
            "required": true
        },
        {
            "name": "location",
            "type": "geo-point",
            "boost": 10,
            "required": true,
            "distance":
            {
                "pivot": "1in"
            }
        }
    ]
}
                """;
        // when
        CanonicalTypeConfiguration configuration = CanonicalTypeConfiguration.loadFromString(jsonString);
        // then
        Assert.assertNotNull(configuration);
    }

    @Test
    public void test_loadFromNode_empty() {
        // given
        CanonicalTypeConfiguration expected = new CanonicalTypeConfiguration();
        JsonNode node = new TextNode("{}");
        // when
        CanonicalTypeConfiguration actual = CanonicalTypeConfiguration.loadFromNode(node);
        // then
        Assert.assertEquals(actual.toString(), expected.toString());
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_loadFromNode_invalid() {
        // given
        JsonNode node = new TextNode("rubbish");
        // when
        // then
        CanonicalTypeConfiguration.loadFromNode(node);
    }
}
