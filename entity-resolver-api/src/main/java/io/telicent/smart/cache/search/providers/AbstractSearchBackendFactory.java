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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory from which search backends can be initialised
 * <p>
 * The underlying providers, which must extend {@link SearchBackendProvider} instances are dynamically loaded via
 * {@link ServiceLoader}
 * </p>
 */
public class AbstractSearchBackendFactory<T extends SearchBackend, TProvider extends SearchBackendProvider<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSearchBackendFactory.class);

    private final List<TProvider> providers = new ArrayList<>();
    private final Class<TProvider> cls;

    /**
     * Creates a new factory for search backends that can be created by the given provider class
     *
     * @param providerClass Provider class
     */
    public AbstractSearchBackendFactory(Class<TProvider> providerClass) {
        this.cls = Objects.requireNonNull(providerClass);
        reset(this.cls);
    }

    /**
     * Resets the registry
     */
    public void reset() {
        this.reset(this.cls);
    }

    /**
     * Resets the registry
     * @param providerClass Provider class
     */
    private void reset(Class<TProvider> providerClass) {
        synchronized (providers) {
            providers.clear();

            ServiceLoader<TProvider> loader = ServiceLoader.load(providerClass);
            Iterator<TProvider> iter = loader.iterator();

            try {
                while (iter.hasNext()) {
                    providers.add(iter.next());
                    LOGGER.info("Discovered {} instance {}", providerClass.getSimpleName(),
                                providers.get(providers.size() - 1).getClass().getCanonicalName());
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.error("Failed to load {}: {}", providerClass.getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Clears the registry
     */
    public void clear() {
        synchronized (providers) {
            providers.clear();
        }
    }

    /**
     * Loads an available search backend
     *
     * @return Search backend
     * @throws RuntimeException Thrown if unable to load search backend
     */
    protected T loadInternal() {
        synchronized (providers) {
            for (TProvider provider : providers) {
                if (provider.supports()) {
                    return provider.load();
                }
            }
        }
        return null;
    }
}
