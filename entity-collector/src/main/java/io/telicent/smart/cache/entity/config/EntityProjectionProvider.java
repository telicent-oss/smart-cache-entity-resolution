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

/**
 * Configuration provider for {@link EntityProjectionConfig} instances i.e. defines and provides a specific entity
 * projection
 */
public interface EntityProjectionProvider {

    /**
     * Gets the name of the entity projection
     *
     * @return Name
     */
    String name();

    /**
     * Gets the description of the entity projection
     *
     * @return Description
     */
    String description();

    /**
     * Creates a new entity projection configuration
     *
     * @return Entity Projection configuration
     */
    EntityProjectionConfig create();
}
