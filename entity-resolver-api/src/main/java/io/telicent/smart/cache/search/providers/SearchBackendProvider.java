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
package io.telicent.smart.cache.search.providers;

import io.telicent.smart.cache.search.SearchBackend;
import io.telicent.smart.cache.search.SearchException;

/**
 * A provider of search backends
 *
 * @param <T> Search backend type
 */
public interface SearchBackendProvider<T extends SearchBackend> {
    /**
     * Checks for sufficient configuration to be able to instantiate an instance of the desired search backend
     * interface
     *
     * @return {@code true} if sufficient configuration is available, otherwise {@code false}
     */
    Boolean supports();

    /**
     * Returns the minimum set of required configuration properties for this provider to configure a search backend
     *
     * @return Minimum set of required configuration properties
     */
    String[] minimumRequiredConfiguration();

    /**
     * Loads a search backend
     *
     * @return Search backend
     * @throws SearchException Thrown if unable to load search backend
     */
    T load();
}
