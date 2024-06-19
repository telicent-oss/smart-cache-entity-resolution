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
package io.telicent.smart.cache.entity.sinks.converters.metadata;

import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.sinks.converters.AbstractSingleFieldOutputConverter;
import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import io.telicent.smart.cache.observability.LibraryVersion;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * An output converter that includes a field indicating the version of the generator application being used
 */
public class GeneratorVersionConverter extends AbstractSingleFieldOutputConverter {

    private final String generatorLibrary;
    private final Map<String, String> version = new HashMap<>();

    /**
     * Creates a new converter using the default output field {@value DefaultOutputFields#GENERATOR_VERSION}
     *
     * @param generatorLibrary Generator library name that can be used to detect its version via {@link LibraryVersion}
     */
    public GeneratorVersionConverter(String generatorLibrary) {
        this(DefaultOutputFields.GENERATOR_VERSION, generatorLibrary);
    }

    /**
     * Creates a new converter using a custom output field
     *
     * @param outputField      Output field name
     * @param generatorLibrary Generator library name that can be used to detect its version via {@link LibraryVersion}
     */
    public GeneratorVersionConverter(String outputField, String generatorLibrary) {
        super(outputField);
        if (StringUtils.isBlank(generatorLibrary)) {
            throw new IllegalArgumentException("generatorLibrary cannot be blank");
        }
        this.generatorLibrary = generatorLibrary;
    }

    @Override
    protected Object getOutput(Entity entity) {
        return version.computeIfAbsent(this.generatorLibrary, LibraryVersion::get);
    }
}
