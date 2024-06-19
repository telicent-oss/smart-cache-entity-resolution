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

import io.telicent.smart.cache.entity.collectors.DirectLiteralsCollector;
import io.telicent.smart.cache.entity.selectors.SimpleTypeSelector;

import java.util.List;

/**
 * The simplest possible entity projection
 */
public class SimpleProjection implements EntityProjectionProvider {

    /**
     * The name of the simple projection
     */
    public static final String NAME = "simple";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "A simple projection that selects all entities with a rdf:type defined for them and collects only their direct literals.";
    }

    @Override
    public EntityProjectionConfig create() {
        return new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector()));
    }
}
