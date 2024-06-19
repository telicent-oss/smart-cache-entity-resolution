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
 * An Elastic mapping factory that handles rules for field type {@value CommonFieldTypes#TEXT}
 */
public class TextMappingFactory implements ElasticMappingFactory {

    @Override
    public boolean supports(IndexMappingRule rule) {
        // Support rules whose type is explicitly text OR those that have an unknown type i.e. everything that doesn't
        // have a recognised type will default to a text mapping
        return StringUtils.equals(rule.getFieldType(), CommonFieldTypes.TEXT) || !StringUtils.equalsAny(
                rule.getFieldType(), CommonFieldTypes.ALL);
    }

    @Override
    public Map<String, DynamicTemplate> toDynamicTemplate(IndexMappingRule rule) {
        // Only want to match on "string", this ensures that we only apply this field mapping to the leaf nodes of the
        // document and don't try to apply it to the intermediate nodes (which would fail)
        return Map.of(rule.getName(), DynamicTemplate.of(t -> t.matchMappingType("string")
                                                               .pathMatch(rule.getMatchPattern())
                                                               .mapping(toProperty(rule))));
    }

    @Override
    public Property toProperty(IndexMappingRule rule) {
        return Property.of(p -> p.text(t -> t));
    }
}
