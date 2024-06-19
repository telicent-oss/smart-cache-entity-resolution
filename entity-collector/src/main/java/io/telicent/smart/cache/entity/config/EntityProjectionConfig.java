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

import io.telicent.smart.cache.entity.EntityCentricProjector;
import io.telicent.smart.cache.entity.collectors.EntityDataCollector;
import io.telicent.smart.cache.entity.selectors.EntitySelector;

import java.util.*;
import java.util.stream.Stream;

/**
 * Provides configuration used by a {@link EntityCentricProjector} to control what entities are selected and what data
 * about them is collected.
 * <p>
 * Typically a concrete sub-class of this will be used to provide a pre-defined projection configuration.
 * </p>
 * <p>
 * Predefined configurations may be dynamically loaded via the {@link ServiceLoader} mechanism using the
 * {@link EntityProjectionProvider} interface and its associated {@link EntityProjectionConfigurations} registry.
 * </p>
 */
public class EntityProjectionConfig {

    private final EntitySelector selector;
    private final List<EntityDataCollector> collectors = new ArrayList<>();

    /**
     * Creates a new projection configuration
     *
     * @param selector   Entity Selector
     * @param collectors Entity Data Collectors
     */
    public EntityProjectionConfig(EntitySelector selector, Collection<EntityDataCollector> collectors) {
        Objects.requireNonNull(selector, "Entity Selector cannot be null");
        Objects.requireNonNull(collectors, "Entity Data Collectors cannot be null");

        this.selector = selector;
        this.collectors.addAll(collectors);
    }

    /**
     * Gets the entity data collectors in-use
     *
     * @return Entity Data Collectors
     */
    public Stream<EntityDataCollector> getCollectors() {
        return this.collectors.stream();
    }

    /**
     * Gets the entity selector in-use
     *
     * @return Entity selector
     */
    public EntitySelector getSelector() {
        return this.selector;
    }
}
