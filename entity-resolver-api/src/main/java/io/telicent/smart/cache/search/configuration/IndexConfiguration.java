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
package io.telicent.smart.cache.search.configuration;

import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;

import java.util.Properties;
import java.util.stream.Stream;

/**
 * Abstract representation of search index configuration
 */
public interface IndexConfiguration<TRule extends IndexMappingRule> {

    /**
     * Gets the properties
     *
     * @return Properties
     */
    Properties getProperties();

    /**
     * Sets the given properties, replacing any existing properties
     *
     * @param props Properties
     */
    void setProperties(Properties props);

    /**
     * Puts the given properties, appending to the existing properties
     *
     * @param props Properties
     */
    void putProperties(Properties props);

    /**
     * Gets the configured index mapping rules
     *
     * @return Rules
     */
    Stream<TRule> getRules();

    /**
     * Adds a rule
     *
     * @param rule Rule to add
     */
    void addRule(TRule rule);

    /**
     * Removes a rule
     *
     * @param rule Rule to remove
     */
    void removeRule(TRule rule);

    /**
     * Removes a rule with the given name
     *
     * @param name Rule name
     */
    void removeRule(String name);
}
