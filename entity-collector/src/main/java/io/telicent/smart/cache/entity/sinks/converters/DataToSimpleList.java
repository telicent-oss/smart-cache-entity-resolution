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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An entity data output converter that produces a simple list that collects all the values associated with a given
 * group of data.
 */
public class DataToSimpleList extends AbstractEntityDataGroupConverter {

    /**
     * Creates a new converter
     *
     * @param outputField   Output field name
     * @param group         Entity Data group to convert
     * @param compactValues Whether to compact URIs that occur in the entity data values into short forms where
     *                      possible
     */
    public DataToSimpleList(String outputField, String group, boolean compactValues) {
        super(outputField, compactValues, group);
    }

    @Override
    protected Object getOutput(Entity entity, Stream<EntityData> data) {
        List<String> values = new ArrayList<>();

        data.forEach(d -> {
            if (d.isSimple()) {
                values.add(nodeToString(entity, d.get()));
            } else if (d.keys().count() == 1) {
                values.addAll(nodesToStrings(entity, d.get(d.key())));
            } else {
                throw new IllegalArgumentException(
                        "EntityData is not in a format supported by this converter, expected EntityData containing a single key");
            }
        });
        return values.isEmpty() ? null : values;
    }

    @Override
    protected boolean shouldOutputFineGrainedSecurityLabels(Entity entity, Stream<EntityData> data) {
        return data.anyMatch(EntityData::hasSecurityLabels);
    }

    @Override
    protected void populateSecurityLabels(Entity entity, Stream<EntityData> data, Map<String, Object> securityLabels) {
        List<String> values = new ArrayList<>();

        data.forEach(d -> {
            if (d.isSimple()) {
                values.add(d.get().hasSecurityLabel() ? d.get().getSecurityLabel() : "");
            } else if (d.keys().count() == 1) {
                values.addAll(d.get(d.key())
                               .stream()
                               .map(v -> v.hasSecurityLabel() ? v.getSecurityLabel() : "")
                               .toList());
            } else {
                throw new IllegalArgumentException(
                        "EntityData is not in a format supported by this converter, expected EntityData containing a single key");
            }
        });
        if (!values.isEmpty()) {
            if (securityLabels.containsKey(this.outputField)) {
                ((List<String>) securityLabels.get(this.outputField)).addAll(values);
            } else {
                securityLabels.put(this.outputField, values);
            }
        }
    }

}
