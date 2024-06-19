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

import io.telicent.smart.cache.search.SearchClient;

import java.util.ServiceLoader;

/**
 * Factory from which search client can be initialised
 * <p>
 * The underlying {@link SearchClientProvider} instances are dynamically loaded via {@link ServiceLoader}
 * </p>
 */
public final class SearchClients extends
        AbstractSearchBackendFactory<SearchClient, SearchClientProvider> {
    private static final SearchClients INSTANCE = new SearchClients();

    private SearchClients() {
        super(SearchClientProvider.class);
    }

    /**
     * Loads an available search client
     *
     * @return Search client
     * @throws RuntimeException Thrown if unable to load search client
     */
    public static SearchClient load() {
        return INSTANCE.loadInternal();
    }

    /**
     * Clears the registry
     */
    public static void clearRegistry() {
        INSTANCE.clear();
    }

    /**
     * Resets the registry
     */
    public static void resetRegistry() {
        INSTANCE.reset();
    }
}
