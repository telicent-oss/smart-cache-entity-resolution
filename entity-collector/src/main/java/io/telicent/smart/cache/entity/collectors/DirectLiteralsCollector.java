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
package io.telicent.smart.cache.entity.collectors;

import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.entity.EntityData;
import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import java.util.stream.Stream;

/**
 * Collects literals directly on a given subject i.e. for any given subject find all literals directly asserted via
 * {@code <subject> ?pred "object"} triples
 */
public class DirectLiteralsCollector extends AbstractEntityDataCollector implements EntityDataCollector {
    @Override
    public Stream<EntityData> collect(Graph graph, Node subject, LabelsStore labelsStore) {
        EntityDataCollector.verifyArguments(graph, subject);

        return graph.stream(subject, Node.ANY, Node.ANY)
                    .filter(t -> t.getObject().isLiteral())
                    .map(t -> EntityData.of(t.getPredicate(), t.getObject(), getSecurityLabels(labelsStore, t)));
    }

    @Override
    public String getGroup() {
        return DefaultOutputFields.LITERALS;
    }
}
