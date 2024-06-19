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

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCanonicalTypeConfigurationMap {

    private static final String INVALID_YAML_FILE_PATH = "src/test/resources/canonical/invalid_yaml.yml";
    private static final String INVALID_JSON_FILE_PATH = "src/test/resources/canonical/invalid_json.json";
    private static final String HAPPY_YAML_FILE_PATH = "src/test/resources/canonical/good_sample_map.yml";
    private static final String HAPPY_JSON_FILE_PATH = "src/test/resources/canonical/good_sample_map.json";

    private static final String HAPPY_JSON_STRING = """
                    {
                                     "Hostels": {
                                       "index": "canonical_hostels",
                                       "fields": [
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
                                           "distance": {
                                             "pivot": "1in"
                                           }
                                         }
                                       ]
                                     }
                                   }
                """;

    @Test
    public void loadConfig_badInput() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromConfigFile(
                INVALID_YAML_FILE_PATH);
        // then
        Assert.assertNull(configurationMap);
    }

    @Test
    public void loadConfig_badInput_JSON() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromConfigFile(
                INVALID_JSON_FILE_PATH);
        // then
        Assert.assertNull(configurationMap);
    }

    @Test
    public void loadConfig_missingFile() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromConfigFile("missingFile.yml");
        // then
        Assert.assertNull(configurationMap);
    }

    @Test
    public void loadConfig_happyPath() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromConfigFile(
                HAPPY_YAML_FILE_PATH);
        // then
        Assert.assertNotNull(configurationMap);
    }

    @Test
    public void loadConfig_happyPath_JSON() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromConfigFile(
                HAPPY_YAML_FILE_PATH);
        // then
        Assert.assertNotNull(configurationMap);
    }

    @Test
    public void loadConfigFromString_happyPath_JSON() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromString(HAPPY_JSON_STRING);
        // then
        Assert.assertNotNull(configurationMap);
    }

    @Test
    public void loadConfigFromString_empty() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromString("");
        // then
        Assert.assertNull(configurationMap);
    }

    @Test
    public void loadConfigFromString_null() {
        // given
        // when
        CanonicalTypeConfigurationMap configurationMap = CanonicalTypeConfigurationMap.loadFromString(null);
        // then
        Assert.assertNull(configurationMap);
    }

    @Test
    public void loadConfig_identical_results() {
        // given
        CanonicalTypeConfigurationMap configurationJSONMap = CanonicalTypeConfigurationMap.loadFromConfigFile(
                HAPPY_JSON_FILE_PATH);
        CanonicalTypeConfigurationMap configurationYAMLMap = CanonicalTypeConfigurationMap.loadFromConfigFile(
                HAPPY_YAML_FILE_PATH);
        // when
        // then
        Assert.assertNotNull(configurationJSONMap);
        Assert.assertNotNull(configurationYAMLMap);
        Assert.assertEquals(configurationJSONMap, configurationYAMLMap);
        Assert.assertEquals(configurationJSONMap.hashCode(), configurationJSONMap.hashCode());
    }

}
