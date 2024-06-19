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
import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.EntityData;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A data collector that collects entity data about an entity
 */
public interface EntityDataCollector {

    /**
     * Verifies that the provided arguments are valid for calling the {@link #collect(Graph, Node, LabelsStore)} method
     *
     * @param graph   Graph
     * @param subject Subject
     * @throws NullPointerException     No graph or subject is provided
     * @throws IllegalArgumentException The provided subject is not concrete
     */
    static void verifyArguments(Graph graph, Node subject) {
        Objects.requireNonNull(graph, "Graph cannot be null");
        Objects.requireNonNull(subject, "Subject cannot be null");
        if (!subject.isConcrete()) {
            throw new IllegalArgumentException("Subject must be a concrete node");
        }
    }

    /**
     * Collects the entity data
     *
     * @param graph          Graph
     * @param subject        Subject to collect entity data for, this <strong>MUST</strong> be a concrete node otherwise
     *                       an {@link IllegalArgumentException} is raised
     * @param securityLabels Security Labels store that the collector can use to determine the fine-grained labels that
     *                       apply to the collected data
     * @return Stream of collected entity data
     * @throws NullPointerException     No graph or subject is provided
     * @throws IllegalArgumentException The provided subject is not concrete
     */
    Stream<EntityData> collect(Graph graph, Node subject, LabelsStore securityLabels);

    /**
     * Gets the data group to which the entity data belongs.  This will be used elsewhere to add data to an
     * {@link Entity} via the {@link Entity#addData(String, EntityData)} method.
     *
     * @return Data group
     */
    String getGroup();
}
