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
import io.telicent.smart.cache.entity.EntityData;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.stream.Stream;

/**
 * An output converter that outputs a single field representing the data from a single group of collected data
 */
public abstract class AbstractEntityDataGroupConverter
        extends AbstractNodeCompactingOutputConverter {
    /**
     * The data group whose data will be output by this converter
     */
    protected final String group;

    /**
     * Creates a new converter
     *
     * @param outputField Output field
     * @param compact     Whether to compact
     * @param group       Group whose data will be output
     */
    public AbstractEntityDataGroupConverter(String outputField, boolean compact, String group) {
        super(outputField, compact);
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Entity Data group cannot be null/blank");
        }
        this.group = group;
    }

    @Override
    protected final Object getOutput(Entity entity) {
        return getOutput(entity, entity.getData(group));
    }

    @Override
    protected final void populateSecurityLabels(Entity entity, Map<String, Object> securityLabels) {
        populateSecurityLabels(entity, entity.getData(group), securityLabels);
    }

    /**
     * Populates the security labels object for the output
     *
     * @param entity Entity
     * @param data   Entity Data
     * @param output Security labels object into which additional security labels should be populated (if any)
     */
    protected abstract void populateSecurityLabels(Entity entity, Stream<EntityData> data, Map<String, Object> output);

    /**
     * Gets the output object produced by converting the given stream of entity data that represents the entity data
     * group that this converter operates upon
     *
     * @param entity Entity
     * @param data   Entity Data
     * @return Object to output, or {@code null} if there's nothing to output
     */
    protected abstract Object getOutput(Entity entity, Stream<EntityData> data);

    @Override
    protected boolean shouldOutputFineGrainedSecurityLabels(Entity entity) {
        return shouldOutputFineGrainedSecurityLabels(entity, entity.getData(group));
    }

    /**
     * Indicates whether there are any fine-grained security labels to output
     * <p>
     * If this returns {@code true} for the given entity then {@link #populateSecurityLabels(Entity, Stream, Map)} will
     * be called.
     * </p>
     *
     * @param entity Entity
     * @param data   Entity Data
     * @return True if fine-grained security labels should be output, false otherwise
     */
    protected abstract boolean shouldOutputFineGrainedSecurityLabels(Entity entity, Stream<EntityData> data);
}
