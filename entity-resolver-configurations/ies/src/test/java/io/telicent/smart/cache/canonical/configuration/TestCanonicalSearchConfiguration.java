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

import io.telicent.smart.cache.search.configuration.IndexConfiguration;
import io.telicent.smart.cache.search.configuration.IndexConfigurations;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration.loadDynamicMappingRules;
import static org.testng.AssertJUnit.assertNotNull;

public class TestCanonicalSearchConfiguration {

    @Test
    public void test_ies4_search_configuration_01() {
        List<String> available = IndexConfigurations.available();
        Assert.assertTrue(available.contains(CanonicalSearchConfiguration.CONFIG_NAME_V1));
        Assert.assertNotNull(IndexConfigurations.describe(CanonicalSearchConfiguration.CONFIG_NAME_V1));
        Assert.assertNotNull(IndexConfigurations.load(CanonicalSearchConfiguration.CONFIG_NAME_V1,
                                                      SimpleMappingRule.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_02() {
        IndexConfigurations.describe("foo");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_03() {
        IndexConfigurations.load("foo", SimpleMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_04() {
        IndexConfigurations.load(CanonicalSearchConfiguration.CONFIG_NAME_V1, FakeMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_05() {
        IndexConfigurations.load("foo", FakeMappingRule.class);
    }

    @Test
    public void test_ies4_search_configuration_direct_01() {
        CanonicalSearchConfiguration provider = new CanonicalSearchConfiguration();
        List<String> available = provider.configurations();
        Assert.assertEquals(available.size(), 1);
        Assert.assertTrue(available.contains(CanonicalSearchConfiguration.CONFIG_NAME_V1));
        Assert.assertNotNull(provider.describe(CanonicalSearchConfiguration.CONFIG_NAME_V1));
        Assert.assertNotNull(provider.load(CanonicalSearchConfiguration.CONFIG_NAME_V1, SimpleMappingRule.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_direct_02() {
        new CanonicalSearchConfiguration().describe("foo");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_direct_03() {
        new CanonicalSearchConfiguration().load("foo", SimpleMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_direct_04() {
        new CanonicalSearchConfiguration().load(CanonicalSearchConfiguration.CONFIG_NAME_V1, FakeMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_direct_05() {
        new CanonicalSearchConfiguration().load("foo", FakeMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_ies4_search_configuration_direct_06() {
        new CanonicalSearchConfiguration().load("Canonical-Documents-V1", SimpleMappingRule.class);
    }

    @Test
    public void test_loadDynamicRules_happyPath() {
        CanonicalSearchConfiguration canonicalSearchConfiguration = new CanonicalSearchConfiguration();
        loadDynamicMappingRules("src/test/resources/canonical/good_sample_map.yml");
        IndexConfiguration<SimpleMappingRule>
                indexConfiguration = canonicalSearchConfiguration.load("CoreCanonicalIesPerson", SimpleMappingRule.class);
        Assert.assertNotNull(canonicalSearchConfiguration.describe("CoreCanonicalIesPerson"));
        assertNotNull(indexConfiguration);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_load_noDynamicRules() {
        CanonicalSearchConfiguration.DYNAMIC_MAPPING_RULES.clear();
        new CanonicalSearchConfiguration().load("CoreCanonicalIesPerson", SimpleMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_loadDynamicRules_null() {
        CanonicalSearchConfiguration.DYNAMIC_MAPPING_RULES.clear();
        loadDynamicMappingRules(null);
        new CanonicalSearchConfiguration().load("CoreCanonicalIesPerson", SimpleMappingRule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_load_missingDynamicRules() {
        CanonicalSearchConfiguration.DYNAMIC_MAPPING_RULES.clear();
        CanonicalSearchConfiguration canonicalSearchConfiguration = new CanonicalSearchConfiguration();
        loadDynamicMappingRules("src/test/resources/canonical/invalid_yaml.yml");
        IndexConfiguration<SimpleMappingRule> indexConfiguration = canonicalSearchConfiguration.load("CoreCanonicalIesPerson", SimpleMappingRule.class);
        assertNotNull(indexConfiguration);
    }

    private static class FakeMappingRule implements IndexMappingRule {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getMatchPattern() {
            return null;
        }

        @Override
        public String getFieldType() {
            return null;
        }
    }
}
