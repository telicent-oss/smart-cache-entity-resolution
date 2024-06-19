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

/**
 * Represents an index mapping rule that defines how fields with a given name (or name pattern) are indexed within a
 * search index
 */
public interface IndexMappingRule {

    /**
     * Gets the name of the rule
     *
     * @return Name
     */
    String getName();

    /**
     * Gets the match pattern for the rule
     *
     * @return Match pattern
     */
    String getMatchPattern();

    /**
     * Gets the field type that should be used for fields that match this rule
     *
     * @return Field type
     */
    String getFieldType();
}
