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
package io.telicent.smart.cache.search;

import io.telicent.smart.cache.search.configuration.IndexConfiguration;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;

import java.util.List;

/**
 * Represents the ability to manage search indices
 *
 * @param <TRule> Index Mapping Rule type
 */
public interface IndexManager<TRule extends IndexMappingRule> extends SearchBackend {

    /**
     * Gets whether a given Index exists
     *
     * @param name Index name
     * @return True if exists, false if not, {@code null} if undetermined
     */
    Boolean hasIndex(String name);

    /**
     * Creates an Index
     *
     * @param name          Index Name
     * @param configuration Index Configuration
     * @return True if created, false if not, {@code null} if undetermined
     */
    Boolean createIndex(String name, IndexConfiguration<TRule> configuration);

    /**
     * Lists the available indices
     *
     * @return Indices
     */
    List<String> listIndices();

    /**
     * Deletes an Index
     *
     * @param name Index name
     * @return True if deleted, false if not, {@code null} if undetermined
     */
    Boolean deleteIndex(String name);

}
