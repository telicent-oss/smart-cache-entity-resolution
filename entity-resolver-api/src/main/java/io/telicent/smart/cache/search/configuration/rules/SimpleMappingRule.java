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
package io.telicent.smart.cache.search.configuration.rules;

import io.telicent.smart.cache.search.configuration.CommonFieldTypes;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple implementation of an index mapping rule
 */
public class SimpleMappingRule implements IndexMappingRule {
    private final String name;
    private final String pattern;
    private final String type;

    /**
     * Creates a new index mapping rule
     *
     * @param name         Rule name
     * @param matchPattern Rule match pattern
     * @param fieldType    Rule field type, generally a constant value taken from
     *                     {@link CommonFieldTypes}
     */
    public SimpleMappingRule(String name, String matchPattern, String fieldType) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Rule name cannot be null/empty");
        }
        if (StringUtils.isBlank(matchPattern)) {
            throw new IllegalArgumentException("Match Pattern cannot be null/empty");
        }
        if (StringUtils.isBlank(fieldType)) {
            throw new IllegalArgumentException("Field Type cannot be null/empty");
        }

        this.name = name;
        this.pattern = matchPattern;
        this.type = fieldType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getMatchPattern() {
        return this.pattern;
    }

    @Override
    public String getFieldType() {
        return this.type;
    }
}
