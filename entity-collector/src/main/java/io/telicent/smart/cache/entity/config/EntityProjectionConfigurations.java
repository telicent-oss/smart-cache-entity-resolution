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
package io.telicent.smart.cache.entity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry for entity projection providers
 */
public final class EntityProjectionConfigurations {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityProjectionConfigurations.class);

    private static final Map<String, EntityProjectionProvider> PROVIDERS = new HashMap<>();

    private EntityProjectionConfigurations() {
    }

    static {
        try {
            ServiceLoader<EntityProjectionProvider> loader = ServiceLoader.load(EntityProjectionProvider.class);
            Iterator<EntityProjectionProvider> iterator = loader.iterator();
            while (iterator.hasNext()) {
                EntityProjectionProvider provider = iterator.next();
                LOGGER.debug("Discovered entity projection {} from provider class {}", provider.name(),
                             provider.getClass().getCanonicalName());
                PROVIDERS.put(provider.name(), provider);
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.warn("Failed to load Entity Projection Providers: {}", e.getMessage());
        }
    }

    /**
     * Gets the available entity projection providers
     *
     * @return Available providers
     */
    public static Collection<EntityProjectionProvider> available() {
        return Collections.unmodifiableCollection(PROVIDERS.values());
    }

    /**
     * Gets whether the given name is associated with a register entity projection provider
     *
     * @param name Name
     * @return True if a registered projection provider, false otherwise
     */
    public static boolean isProjection(String name) {
        return PROVIDERS.containsKey(name);
    }

    /**
     * Gets the entity projection provider with the given name
     *
     * @param name Name
     * @return Entity projection provider
     * @throws IllegalArgumentException If the given name is not a registered entity projection
     */
    public static EntityProjectionProvider get(String name) {
        EntityProjectionProvider provider = PROVIDERS.get(name);
        if (provider == null) {
            throw new IllegalArgumentException(String.format("%s is not a recognised entity projection", name));
        }
        return provider;
    }
}
