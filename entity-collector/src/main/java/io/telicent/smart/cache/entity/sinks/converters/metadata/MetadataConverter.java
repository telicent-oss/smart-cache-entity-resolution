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
import io.telicent.smart.cache.entity.sinks.converters.EntityToMapOutputConverter;
import io.telicent.smart.cache.observability.LibraryVersion;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An output converter that outputs a map containing a selection of metadata about the document generation process
 */
public class MetadataConverter extends AbstractSingleFieldOutputConverter {

    private final List<EntityToMapOutputConverter> converters = new ArrayList<>();

    /**
     * Creates a new converter using the default field name {@value DefaultOutputFields#METADATA}
     *
     * @param generatorLibrary    Generator library name that can be used to detect its version via
     *                            {@link LibraryVersion}
     * @param documentFormat      Document Format used in generating the documents
     * @param includedGeneratedAt Whether to include the generated at converter in the metadata output, it may be
     *                            advisable to exclude this as it will make every generated document unique and lead to
     *                            unnecessary work for a document generation pipeline
     */
    public MetadataConverter(String generatorLibrary, String documentFormat, boolean includedGeneratedAt) {
        this(DefaultOutputFields.METADATA, generatorLibrary, documentFormat, includedGeneratedAt);
    }

    /**
     * Creates a new converter using a custom field name
     *
     * @param outputField         Output field name
     * @param generatorLibrary    Generator library name that can be used to detect its version via
     *                            {@link LibraryVersion}
     * @param documentFormat      Document Format used in generating the documents
     * @param includedGeneratedAt Whether to include the generated at converter in the metadata output, it may be
     *                            advisable to exclude this as it will make every generated document unique and lead to
     *                            unnecessary work for a document generation pipeline
     */
    public MetadataConverter(String outputField, String generatorLibrary, String documentFormat,
                             boolean includedGeneratedAt) {
        super(outputField);
        CollectionUtils.addAll(this.converters,
                               new StaticFieldConverter(DefaultOutputFields.GENERATED_BY, generatorLibrary),
                               new GeneratorVersionConverter(generatorLibrary),
                               new StaticFieldConverter(DefaultOutputFields.DOCUMENT_FORMAT, documentFormat));
        if (includedGeneratedAt) {
            this.converters.add(new GeneratedAtConverter());
        }
    }

    @Override
    protected Object getOutput(Entity entity) {
        Map<String, Object> metadata = new HashMap<>();
        this.converters.forEach(c -> c.output(entity, metadata));
        return metadata;
    }
}
