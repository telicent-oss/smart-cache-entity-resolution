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
package io.telicent.smart.cache.entity.resolver.providers;

import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.search.providers.AbstractSearchBackendFactory;

import java.util.ServiceLoader;

/**
 * Factory from which entity resolvers can be initialised
 * <p>
 * The underlying {@link EntityResolverProvider} instances are dynamically loaded via {@link ServiceLoader}
 * </p>
 */
public final class EntityResolvers extends AbstractSearchBackendFactory<EntityResolver, EntityResolverProvider> {

    private static final EntityResolvers INSTANCE = new EntityResolvers();

    private EntityResolvers() {
        super(EntityResolverProvider.class);
    }

    /**
     * Loads an available entity resolver
     *
     * @return Entity resolver
     * @throws RuntimeException Thrown if unable to load entity resolver
     */
    public static EntityResolver load() {
        return INSTANCE.loadInternal();
    }
}
