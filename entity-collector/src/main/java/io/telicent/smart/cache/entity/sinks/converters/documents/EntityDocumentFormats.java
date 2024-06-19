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
package io.telicent.smart.cache.entity.sinks.converters.documents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry for entity document format providers
 */
public final class EntityDocumentFormats {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDocumentFormats.class);

    private static final Map<String, EntityDocumentFormatProvider> PROVIDERS = new HashMap<>();

    static {
        try {
            ServiceLoader<EntityDocumentFormatProvider> loader = ServiceLoader.load(EntityDocumentFormatProvider.class);
            Iterator<EntityDocumentFormatProvider> iterator = loader.iterator();
            while (iterator.hasNext()) {
                EntityDocumentFormatProvider provider = iterator.next();
                LOGGER.debug("Discovered entity document format {} from provider class {}", provider.name(),
                             provider.getClass().getCanonicalName());
                PROVIDERS.put(provider.name(), provider);
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.warn("Failed to load Document Format Providers: {}", e.getMessage());
        }
    }

    /**
     * Private constructor prevents instantiation
     */
    private EntityDocumentFormats() {

    }

    /**
     * Gets the available document formats
     *
     * @return Available formats
     */
    public static Collection<EntityDocumentFormatProvider> availableFormats() {
        return Collections.unmodifiableCollection(PROVIDERS.values());
    }

    /**
     * Gets whether there is a registered format
     *
     * @param name Name
     * @return True if a recognised document format, false otherwise
     */
    public static boolean isFormat(String name) {
        return PROVIDERS.containsKey(name);
    }

    /**
     * Gets a document format provider
     *
     * @param name Name
     * @return Document Format provider
     * @throws IllegalArgumentException Thrown if the given format name is not recognised
     */
    public static EntityDocumentFormatProvider getFormat(String name) {
        EntityDocumentFormatProvider provider = PROVIDERS.get(name);
        if (provider == null) {
            throw new IllegalArgumentException(String.format("%s is not a recognised document format", name));
        }
        return provider;
    }
}
