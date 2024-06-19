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

/**
 * Base interface for all search related interfaces
 *
 * @see SearchIndexer
 * @see SearchClient
 * @see IndexManager
 */
public interface SearchBackend {
    /**
     * Gets whether the underlying backend is ready to service requests
     *
     * @return True if ready, false if not, {@code null} if undetermined
     */
    Boolean isReady();

    /**
     * Gets the human-readable name of the backend
     * <p>
     * Implementations should also consider overriding {@link Object#toString()} to provide a more debug oriented
     * representation of the backend e.g. a connection string
     * </p>
     *
     * @return Backend name
     */
    String name();
}
