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
import co.elastic.clients.elasticsearch._types.mapping.Property;
import io.telicent.smart.cache.search.configuration.CommonFieldTypes;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * An Elastic mapping factory that handles rules for field type {@value CommonFieldTypes#ANY}
 */
public class AnyToTextMappingFactory implements ElasticMappingFactory {

    @Override
    public boolean supports(IndexMappingRule rule) {
        // Support rules whose type is any, i.e. regardless of what Elasticsearch thinks it is
        return StringUtils.equals(rule.getFieldType(), CommonFieldTypes.ANY);
    }

    @Override
    public Map<String, DynamicTemplate> toDynamicTemplate(IndexMappingRule rule) {
        return Map.of(rule.getName(), DynamicTemplate.of(t -> t.pathMatch(rule.getMatchPattern())
                                                               .mapping(toProperty(rule))));
    }

    @Override
    public Property toProperty(IndexMappingRule rule) {
        return Property.of(p -> p.text(t -> t));
    }
}
