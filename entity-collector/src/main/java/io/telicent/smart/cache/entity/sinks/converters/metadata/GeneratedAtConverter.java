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

import java.util.Date;

/**
 * A converter that outputs a single field with a date indicating when this document was generated
 */
public class GeneratedAtConverter extends AbstractSingleFieldOutputConverter {

    /**
     * Creates a new converter using the default field name {@value DefaultOutputFields#GENERATED_AT}
     */
    public GeneratedAtConverter() {
        this(DefaultOutputFields.GENERATED_AT);
    }

    /**
     * Creates a new converter using a custom field name
     *
     * @param field Field name
     */
    public GeneratedAtConverter(String field) {
        super(field);
    }

    @Override
    protected Object getOutput(Entity entity) {
        return new Date();
    }
}
