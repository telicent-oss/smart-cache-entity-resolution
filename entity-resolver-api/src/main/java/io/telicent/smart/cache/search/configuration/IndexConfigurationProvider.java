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

import java.util.List;

/**
 * A provider of Index configurations
 */
public interface IndexConfigurationProvider {


    /**
     * Lists the names of the configurations that this implementation provides
     *
     * @return Configuration names
     */
    List<String> configurations();

    /**
     * Returns whether a configuration with the given name is supported by this provider
     *
     * @param name Configuration name
     * @return True if supported, false otherwise
     */
    boolean supports(String name);

    /**
     * Provides a description of a given configuration
     *
     * @param name Configuration name
     * @return Description, or {@code null} if none available
     * @throws IllegalArgumentException Thrown if there is no such configuration with that name provided by this
     *                                  provider
     */
    String describe(String name);

    /**
     * Gets whether the provider supports the given indexing rule type
     *
     * @param ruleType Indexing rule type
     * @return True if supported, false otherwise
     */
    boolean supports(Class<? extends IndexMappingRule> ruleType);

    /**
     * Loads a configuration by name
     *
     * @param name     Configuration name
     * @param ruleType Rule type
     * @param <TRule>  Rule type
     * @return Index configuration
     */
    <TRule extends IndexMappingRule> IndexConfiguration<TRule> load(String name, Class<TRule> ruleType);
}
