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
import org.apache.commons.lang3.StringUtils;

/**
 * An output converter that includes a field with a static value i.e. the value is independent of the {@link Entity} for
 * which output is being generated.
 * <p>
 * This is typically intended for incorporating metadata into the output documents e.g. about how/when the document was
 * generated.
 * </p>
 */
public class StaticFieldConverter extends AbstractSingleFieldOutputConverter {

    private final String staticValue;

    /**
     * Creates a new converter
     *
     * @param outputField Output field name
     * @param staticValue Static value to output
     */
    public StaticFieldConverter(String outputField, String staticValue) {
        super(outputField);
        if (StringUtils.isBlank(staticValue)) {
            throw new IllegalArgumentException("Static value to output cannot be blank");
        }
        this.staticValue = staticValue;
    }

    @Override
    protected Object getOutput(Entity entity) {
        return this.staticValue;
    }
}
