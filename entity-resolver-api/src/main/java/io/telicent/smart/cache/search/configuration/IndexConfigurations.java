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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory from which index configurations can be obtained
 * <p>
 * The underlying {@link IndexConfigurationProvider} instances are dynamically loaded via {@link ServiceLoader}
 * </p>
 */
public final class IndexConfigurations {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexConfigurations.class);

    private static final List<IndexConfigurationProvider> PROVIDERS = new ArrayList<>();

    static {
        ServiceLoader<IndexConfigurationProvider> loader = ServiceLoader.load(IndexConfigurationProvider.class);
        Iterator<IndexConfigurationProvider> iter = loader.iterator();

        try {
            while (iter.hasNext()) {
                PROVIDERS.add(iter.next());
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.error("Failed to load Index Configuration Providers: {}", e.getMessage());
        }
    }

    private IndexConfigurations() {
    }

    /**
     * Lists all the available index configurations
     *
     * @return Available configurations
     */
    public static List<String> available() {
        Set<String> configurations = new LinkedHashSet<>();
        for (IndexConfigurationProvider provider : PROVIDERS) {
            configurations.addAll(provider.configurations());
        }

        return configurations.stream().toList();
    }

    /**
     * Gets whether a given index configuration is supported by an available provider
     *
     * @param name     Configuration name
     * @param ruleType Rule type
     * @return True if supported, false otherwise
     */
    public static boolean supported(String name, Class<? extends IndexMappingRule> ruleType) {
        for (IndexConfigurationProvider provider : PROVIDERS) {
            if (provider.supports(name) && provider.supports(ruleType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Describes an available configuration
     *
     * @param name Configuration name
     * @return Configuration description
     */
    public static String describe(String name) {
        for (IndexConfigurationProvider provider : PROVIDERS) {
            if (!provider.supports(name)) {
                continue;
            }

            return provider.describe(name);
        }

        throw new IllegalArgumentException(name + " is not a known index configuration");
    }

    /**
     * Loads an available configuration
     *
     * @param name     Configuration name
     * @param ruleType Rule type
     * @param <TRule>  Rule type
     * @return Index configuration
     */
    public static <TRule extends IndexMappingRule> IndexConfiguration<TRule> load(String name, Class<TRule> ruleType) {
        for (IndexConfigurationProvider provider : PROVIDERS) {
            if (!provider.supports(name) || !provider.supports(ruleType)) {
                continue;
            }

            return provider.load(name, ruleType);
        }

        throw new IllegalArgumentException(name + " is not a known index configuration");
    }


}
