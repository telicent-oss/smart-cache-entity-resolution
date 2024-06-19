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
package io.telicent.smart.cache.entity.sinks.converters;

import io.telicent.smart.cache.entity.Entity;

import java.util.Map;

/**
 * An output converter that produces some output for an entity
 * <p>
 * Each converter may only produce part of the entity output thus allowing different converters to be reused and
 * composed to produce different data structures depending on the use case.
 * </p>
 */
public interface EntityToMapOutputConverter {

    /**
     * Produces some output for an entity
     *
     * @param entity Entity
     * @param map    Output map being built
     */
    void output(Entity entity, Map<String, Object> map);
}
