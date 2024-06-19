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
package io.telicent.smart.cache.search.elastic.schema.factories;

import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * An abstract Elastic mapping factory that supports a single field type
 */
public abstract class AbstractMappingFactory implements ElasticMappingFactory {

    private final String supportedType;
    private final String dynamicMatchType;

    /**
     * Creates a new factory
     *
     * @param supportedType    Supported field type
     * @param dynamicMatchType The Elastic data type to match on for dynamic template mappings
     */
    public AbstractMappingFactory(String supportedType, String dynamicMatchType) {
        if (StringUtils.isBlank(supportedType)) {
            throw new IllegalArgumentException("supportedType cannot be null/empty");
        }
        if (StringUtils.isBlank(dynamicMatchType)) {
            throw new IllegalArgumentException("dynamicMatchType cannot be null/empty");
        }
        this.supportedType = supportedType;
        this.dynamicMatchType = dynamicMatchType;
    }

    @Override
    public final boolean supports(IndexMappingRule rule) {
        return StringUtils.equals(this.supportedType, rule.getFieldType());
    }

    @Override
    public final Map<String, DynamicTemplate> toDynamicTemplate(IndexMappingRule rule) {
        return Map.of(rule.getName(), DynamicTemplate.of(t -> t.matchMappingType(this.dynamicMatchType)
                                                               .pathMatch(rule.getMatchPattern())
                                                               .mapping(toProperty(rule))));
    }
}
