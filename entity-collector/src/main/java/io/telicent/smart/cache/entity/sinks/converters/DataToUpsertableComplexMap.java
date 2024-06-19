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
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An entity data converter that produces an upsertable map.  This is a nested map in that it creates maps under the top
 * level map that group by each data key.  This is the upsertable equivalent of {@link DataToSimpleMap}.
 */
public class DataToUpsertableComplexMap extends DataToUpsertableMap {

    /**
     * Creates a new converter
     *
     * @param outputField   Output field name
     * @param group         Entity Data group to convert
     * @param keyFormat     Format to use for generating the map keys from the values
     * @param compactKeys   Whether to compact URIs that are used as keys.  This will be used for the top level map
     *                      keys, whether it is used for the nested map keys depends on the {@code keyFormat}
     *                      parameter.
     * @param compactValues Whether to compact URIs that occur in the entity data values into short forms where
     *                      possible
     */
    public DataToUpsertableComplexMap(String outputField, String group, UpsertableKeyFormat keyFormat,
                                      boolean compactKeys,
                                      boolean compactValues) {
        super(outputField, group, keyFormat, compactKeys, compactValues);
    }

    @Override
    protected Object getOutput(Entity entity, Stream<EntityData> data) {
        Map<String, Object> output = new HashMap<>();

        data.forEach(d -> {
            String mapKey = nodeToKey(entity, d.key());
            Map<String, Object> keyOutput =
                    (Map<String, Object>) output.computeIfAbsent(mapKey, k -> new HashMap<String, Object>());
            Map<String, Object> keySecurityLabels =
                    (Map<String, Object>) keyOutput.computeIfAbsent(DefaultOutputFields.SECURITY_LABELS,
                                                                    x -> new HashMap<>());

            outputValues(entity, keyOutput, keySecurityLabels, d);
        });

        trimMaps(output);

        return output.isEmpty() ? null : output;
    }

    /**
     * Trims the output of empty maps
     *
     * @param output Output
     */
    protected void trimMaps(Map<String, Object> output) {
        // Trim empty security labels maps
        output.values()
              .forEach(v -> ((Map<String, Object>) v).entrySet()
                                                     .removeIf(e -> StringUtils.equals(e.getKey(),
                                                                                       DefaultOutputFields.SECURITY_LABELS) && MapUtils.isEmpty(
                                                             (Map<?, ?>) e.getValue())));
        // Trim empty value maps
        output.entrySet().removeIf(e -> MapUtils.isEmpty((Map<?, ?>) e.getValue()));
    }
}
