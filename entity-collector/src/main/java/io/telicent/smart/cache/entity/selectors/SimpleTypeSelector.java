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
package io.telicent.smart.cache.entity.selectors;

import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.EntityCentricProjector;
import io.telicent.smart.cache.entity.EntityData;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import java.util.*;
import java.util.stream.Stream;

/**
 * An Entity Selector that simply collects things with a declared type, assuming that types are declared via
 * {@link Rdf#TYPE_GROUP} triples.
 * <p>
 * It can optionally limit selection to either a pre-defined list of types, and/or can optionally ignore certain types
 * as well.
 * </p>
 */
public class SimpleTypeSelector implements EntitySelector {

    /**
     * Whether all types should be included
     */
    protected final boolean includeAllTypes;
    /**
     * Set of types to include
     */
    protected final Set<Node> includedTypes;
    /**
     * Set of types to ignore
     */
    protected final Set<Node> ignoredTypes;
    /**
     * Default prefixes to add to entities
     */
    protected final Map<String, String> defaultPrefixes;

    /**
     * Creates a new type selector that selects all entities with a defined type
     */
    public SimpleTypeSelector() {
        this(true, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Creates a new type selector
     *
     * @param includeAllTypes Whether to select all entities with a defined type by default.  If {@code true} then
     *                        {@code includedTypes} are ignored, if {@code false} then {@code includedTypes}
     *                        <strong>MUST</strong> be provided.
     * @param includedTypes   When set, and {@code includeAllTypes} is {@code false}, then only select entities who have
     *                        the given types.
     * @param ignoredTypes    When set any entity that has any of these types will not be selected, even if they would
     *                        otherwise be selected.  Use this to exclude uninteresting entities from your projection.
     * @param defaultPrefixes Provides a set of namespace prefixes that will be added to all entities, potentially
     *                        overriding any namespace prefixes defined in the entities original graph.
     */
    public SimpleTypeSelector(boolean includeAllTypes, Collection<Node> includedTypes, Collection<Node> ignoredTypes,
                              Map<String, String> defaultPrefixes) {
        this.includeAllTypes = includeAllTypes;
        this.includedTypes = Set.copyOf(includedTypes);
        this.ignoredTypes = Set.copyOf(ignoredTypes);
        this.defaultPrefixes = Map.copyOf(defaultPrefixes);
    }

    @Override
    public Stream<Entity> select(Graph graph, String defaultSecurityLabels, Graph securityLabels) {
        // Find all the entities by type
        Stream<Triple> ts = null;
        if (this.includeAllTypes) {
            ts = graph.stream(Node.ANY, Rdf.TYPE, Node.ANY);
        } else {
            for (Node type : this.includedTypes) {
                Stream<Triple> typeStream = graph.stream(Node.ANY, Rdf.TYPE, type);
                ts = ts == null ? typeStream : Stream.concat(ts, typeStream);
            }
            if (ts == null) {
                ts = Stream.empty();
            }
        }

        LabelsStore labelsStore = EntityCentricProjector.prepareLabelsStore(defaultSecurityLabels, securityLabels);

        // Group by subject
        Map<Node, Entity> entities = new HashMap<>();
        ts.forEach(t -> entities.computeIfAbsent(t.getSubject(),
                                                 key -> new Entity(key, graph.getPrefixMapping(), defaultSecurityLabels,
                                                                   securityLabels))
                                .addData(Rdf.TYPE_GROUP, EntityData.of(t.getPredicate(), t.getObject(),
                                                                       labelsStore != null
                                                                       ? StringUtils.join(labelsStore.labelsForTriples(t),
                                                                                        ",") : null)));

        // Filter out any ignored types, have to do this after grouping by subject as if an entity has multiple types
        // declared we might include it based on one type but ignore it based on another.  If we apply this prior to
        // grouping we may still include an entity we were meant to ignore.
        // Note also that we have to do this by referring back to the graph itself, because depending on the included
        // types we may not have actually collected the types to be ignored.  Thus, we need to see whether the graph
        // itself declares an entity to have an ignored type.
        if (!this.ignoredTypes.isEmpty()) {
            entities.entrySet().removeIf(kvp -> this.ignoredTypes.stream().anyMatch(t ->
                                                                                            graph.contains(kvp.getKey(),
                                                                                                           Rdf.TYPE, t)
            ));
        }

        entities.values().forEach(e -> e.getPrefixes().setNsPrefixes(this.defaultPrefixes));
        return entities.values().stream();
    }
}
