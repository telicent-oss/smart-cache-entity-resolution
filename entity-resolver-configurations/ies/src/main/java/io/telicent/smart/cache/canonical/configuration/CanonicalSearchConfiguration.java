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

import io.telicent.smart.cache.search.configuration.CommonFieldTypes;
import io.telicent.smart.cache.search.configuration.IndexConfiguration;
import io.telicent.smart.cache.search.configuration.IndexConfigurationProvider;
import io.telicent.smart.cache.search.configuration.SimpleIndexConfiguration;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Provides configuration related to indexing canonical data for similarity
 */
public class CanonicalSearchConfiguration implements IndexConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalSearchConfiguration.class);
    private static final SimpleMappingRule EVERYTHING_IS_TEXT_RULE =
            new SimpleMappingRule("EverythingIsText", "*", CommonFieldTypes.ANY);

    /**
     * Default key for index settings file, often present in the default properties for index configurations.
     */
    public static final String DEFAULT_INDEX_SETTINGS_FILE_KEY = "index.settings.file";
    /**
     * Default value for index settings file, often present in the default properties for index configurations.
     */
    public static final String DEFAULT_INDEX_SETTINGS_FILE_VALUE = "index_settings.json";

    /**
     * Default set of index mapping rules for use with data canonical representation of data
     */
    //@formatter:off
    public static final List<SimpleMappingRule> V1_INDEXING_RULES =
                  List.of(EVERYTHING_IS_TEXT_RULE);
    //@formatter:on

    /**
     * Mapping of rules to canonical types
     */
    public static final Map<String, IndexConfiguration<SimpleMappingRule>> DYNAMIC_MAPPING_RULES = new HashMap<>();

    /**
     * Load in dynamic rules for each given canonical type
     * @param path Location of the configuration file
     */
    public static void loadDynamicMappingRules(String path) {
        if (null != path) {
            CanonicalTypeConfigurationMap
                    config = CanonicalTypeConfigurationMap.loadFromConfigFile(path);
            if (null != config) {
                config.forEach(
                        (key, value) -> DYNAMIC_MAPPING_RULES.put(key, new SimpleIndexConfiguration(DEFAULT_PROPERTIES,
                                                                                                    generateMappingRulesFromConfig(value))));
                LOGGER.debug("{} dynamic rules loaded for Canonical Indexer.", config.size());
            } else {
                LOGGER.error("Canonical configuration could not be loaded correctly from {}", path);
            }
        } else {
            LOGGER.debug("No dynamic rules needed for Canonical Indexer.");
        }
    }

    /**
     * Process the given config into a list of mapping rules per field.
     * @param config Canonical Type definitions
     * @return List of mapping rules
     */
    public static List<SimpleMappingRule> generateMappingRulesFromConfig(CanonicalTypeConfiguration config) {
        List<SimpleMappingRule> rules = new ArrayList<>();
        for (CanonicalTypeConfiguration.SimilarityField field : config.fields) {
            rules.add(new SimpleMappingRule(field.name, field.name, field.type));
        }
        return rules;
    }

    /**
     * Default properties to use within the configuration, contains a property specifying how to create an index with
     * Elasticsearch.
     **/
    public static final Properties DEFAULT_PROPERTIES = new Properties();

    static {
        // illustrates how to pass configs via the properties
        DEFAULT_PROPERTIES.put(DEFAULT_INDEX_SETTINGS_FILE_KEY, DEFAULT_INDEX_SETTINGS_FILE_VALUE);
    }

    /**
     * Default Index Configuration for indexing JSON generated from a canonical representation of data, i.e. flat
     * key/values without references to other entities.
     */
    public static final IndexConfiguration<SimpleMappingRule> V1_INDEX_CONFIGURATION =
            new SimpleIndexConfiguration(DEFAULT_PROPERTIES, V1_INDEXING_RULES);

    /**
     * Name of the IES documents V1 index configuration
     */
    public static final String CONFIG_NAME_V1 = "canonical-documents-v1";

    /**
     * IES 4 Document Format Version 1
     */
    public static final String DOCUMENT_FORMAT_IES4_V1 = "ies4-v1";
    /**
     * IES 4 Document Format Version 2
     */
    public static final String DOCUMENT_FORMAT_IES4_V2 = "ies4-v2";
    /**
     * IES 4 Document Format Version 3
     */
    public static final String DOCUMENT_FORMAT_IES4_V3 = "ies4-v3";

    /**
     * Name of the IES documents V2 index configuration
     */
    public static final String CONFIG_NAME_V2 = "ies-documents-v2";

    private static final String CONFIG_DESCRIPTION_V1 =
            generateDescription();

    private static String generateDescription() {
        return "A default indexing configuration for documents generated from a canonical representation";
    }

    @Override
    public List<String> configurations() {
        return List.of(CONFIG_NAME_V1);
    }

    @Override
    public boolean supports(String name) {
        if (StringUtils.equalsAny(name, CONFIG_NAME_V1)) {
            return true;
        }
        return DYNAMIC_MAPPING_RULES.containsKey(name);
    }

    @Override
    public String describe(String name) {
        if (supports(name)) {
            return switch (name) {
                case CONFIG_NAME_V1 -> CONFIG_DESCRIPTION_V1;
                default -> generateDescription();
            };
        } else {
            throw new IllegalArgumentException(name + " is not a supported index configuration for this provider");
        }
    }

    @Override
    public boolean supports(Class<? extends IndexMappingRule> ruleType) {
        return SimpleMappingRule.class.equals(ruleType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <TRule extends IndexMappingRule> IndexConfiguration<TRule> load(String name, Class<TRule> ruleType) {
        if (DYNAMIC_MAPPING_RULES.containsKey(name)) {
            return (IndexConfiguration<TRule>) DYNAMIC_MAPPING_RULES.get(name);
        }

        if (supports(name) && supports(ruleType)) {
            return (IndexConfiguration<TRule>) V1_INDEX_CONFIGURATION;
        } else {
            throw new IllegalArgumentException(name + " is not a supported index configuration for this provider");
        }
    }
}
