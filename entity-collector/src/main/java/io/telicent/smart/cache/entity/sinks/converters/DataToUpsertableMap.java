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
import io.telicent.smart.cache.entity.SecurityLabelledNode;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.graph.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An entity data output converter that produces an upsertable map that collects all the values associated with a given
 * group of data.
 * <p>
 * As compared to {@link DataToSimpleList} rather than creating a list of the values it creates a map that replicates
 * the values as both the keys and values.  The resulting map can safely be upserted by subsequent updates whereas an
 * upsert on a list overwrites the entire list.
 * </p>
 * <p>
 * Where the values may themselves be long this converter may optionally compress the keys by cryptographically hashing
 * them giving them deterministic fixed length keys without overly bloating the document content.
 * </p>
 */
public class DataToUpsertableMap extends AbstractEntityDataGroupConverter {

    /**
     * Whether keys should be compacted (where possible)
     */
    protected final boolean compactKeys;
    private final UpsertableKeyFormat keyFormat;

    /**
     * Creates a new converter
     *
     * @param outputField   Output field name
     * @param group         Entity Data group to convert
     * @param keyFormat     Format to use for generating the map keys from the values
     * @param compactKeys   Whether to compact URIs that are used as keys, whether this has any effect will also depend
     *                      on the value of the {@code keyFormat} parameters
     * @param compactValues Whether to compact URIs that occur in the entity data values into short forms where
     *                      possible
     */
    public DataToUpsertableMap(String outputField, String group, UpsertableKeyFormat keyFormat, boolean compactKeys,
                               boolean compactValues) {
        super(outputField, compactValues, group);
        this.keyFormat = keyFormat;
        this.compactKeys = compactKeys;
    }

    /**
     * Converts a node into a key for use as part of an upsertable map, this will take into account the configured
     * {@link UpsertableKeyFormat} in deciding how to perform the conversion
     *
     * @param entity Entity
     * @param n      Node
     * @return Node as an upsertable string key
     */
    protected String nodeToKey(Entity entity, SecurityLabelledNode n) {
        if (n == null || n.getNode() == null) {
            return null;
        }
        switch (this.keyFormat) {
            case HASHED:
                if (n.getNode().isURI()) {
                    return UpsertableKeyFormat.hashKey(n.getNode().getURI());
                } else if (n.getNode().isBlank()) {
                    return UpsertableKeyFormat.hashKey(n.getNode().getBlankNodeLabel());
                } else if (n.getNode().isLiteral()) {
                    return UpsertableKeyFormat.hashKey(n.getNode().getLiteralLexicalForm());
                } else {
                    return null;
                }
            case COMPACTED:
            case AS_IS:
            default:
                return nodeToKey(entity, n.getNode());
        }
    }

    @Override
    protected void populateSecurityLabels(Entity entity, Stream<EntityData> data, Map<String, Object> output) {
        // Security labels are already output by getOutput() so nothing to do here
    }

    /**
     * Converts a Node into a potentially compacted form depending on how the converter was configured
     *
     * @param entity Entity
     * @param node   Node
     * @return Node as a string key
     */
    protected String nodeToKey(Entity entity, Node node) {
        if (node.isURI()) {
            return this.compactKeys && entity.hasPrefixes() ? entity.getPrefixes().shortForm(node.getURI())
                   : node.getURI();
        } else {
            return nodeToString(entity, node);
        }
    }

    @Override
    protected Object getOutput(Entity entity, Stream<EntityData> data) {
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> securityLabels = new HashMap<>();

        data.forEach(d -> {
            if (d.isSimple()) {
                SecurityLabelledNode n = d.get();
                String key = nodeToKey(entity, n);
                if (key != null) {
                    output.put(key, nodeToString(entity, n));
                    if (n.hasSecurityLabel()) {
                        securityLabels.put(key, n.getSecurityLabel());
                    }
                }
            } else if (d.keys().count() == 1) {
                outputValues(entity, output, securityLabels, d);
            } else {
                throw new IllegalArgumentException(
                        "EntityData is not in a format supported by this converter, expected EntityData containing a single key");
            }
        });
        if (MapUtils.isNotEmpty(securityLabels)) {
            output.put(DefaultOutputFields.SECURITY_LABELS, securityLabels);
        }
        return output.isEmpty() ? null : output;
    }

    /**
     * Outputs a stream of entity data into an upsertable map format
     *
     * @param entity         Entity
     * @param output         Output map
     * @param securityLabels Security labels map
     * @param d              Entity data
     */
    protected void outputValues(Entity entity, Map<String, Object> output, Map<String, Object> securityLabels,
                                EntityData d) {
        d.get(d.key()).forEach(n -> {
            String key = nodeToKey(entity, n);
            if (key != null) {
                output.put(key, nodeToString(entity, n));
                if (n.hasSecurityLabel()) {
                    securityLabels.put(key, n.getSecurityLabel());
                }
            }
        });
    }

    @Override
    protected boolean shouldOutputFineGrainedSecurityLabels(Entity entity, Stream<EntityData> data) {
        return false;
    }
}
