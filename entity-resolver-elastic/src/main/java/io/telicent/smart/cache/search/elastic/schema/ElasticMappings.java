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
package io.telicent.smart.cache.search.elastic.schema;

import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import io.telicent.smart.cache.search.elastic.schema.factories.ElasticMappingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Provides helper methods for generating ElasticSearch index mappings based upon index mapping rules
 */
public final class ElasticMappings {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticMappings.class);

    private static final List<ElasticMappingFactory> FACTORIES = new ArrayList<>();

    static {
        ServiceLoader<ElasticMappingFactory> loader = ServiceLoader.load(ElasticMappingFactory.class);
        Iterator<ElasticMappingFactory> iter = loader.iterator();
        try {
            while (iter.hasNext()) {
                FACTORIES.add(iter.next());
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.error("Error loading Elastic Schema Mapping factories: " + e.getMessage());
            LOGGER.warn("Indexing of documents into Elastic may be incorrect as a result");
        }
    }

    /**
     * Private constructor to prevent instantiation
     */
    private ElasticMappings() {
    }

    /**
     * Converts mapping rules into actual Elastic Mappings, whether {@link Property} or {@link DynamicTemplate},
     * depending on the rule definition provided.
     *
     * @param rule       Rule
     * @param templates  Dynamic Templates to append to
     * @param properties Properties to append to
     */
    public static void ruleToElasticMapping(SimpleMappingRule rule, List<Map<String, DynamicTemplate>> templates,
                                            Map<String, Property> properties) {
        for (ElasticMappingFactory factory : FACTORIES) {
            if (!factory.supports(rule)) {
                continue;
            }

            if (rule.getMatchPattern().contains("*")) {
                templates.add(factory.toDynamicTemplate(rule));
            } else {
                properties.put(rule.getMatchPattern(), factory.toProperty(rule));
            }
            break;
        }
    }
}
