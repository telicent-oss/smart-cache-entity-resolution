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

import io.telicent.smart.cache.entity.Entity;
import org.apache.jena.graph.Graph;

import java.util.stream.Stream;

/**
 * Selects entities of interest from a Graph
 */
public interface EntitySelector {

    /**
     * Selects the entities of interest
     *
     * @param graph                 Graph
     * @param defaultSecurityLabels Default security labels for the entity, these apply to any data not covered by a
     *                              more specific rule in the security labels graph
     * @param securityLabels        Security Labels graph to associated with each selected entity
     * @return Stream of selected entities
     */
    Stream<Entity> select(Graph graph, String defaultSecurityLabels, Graph securityLabels);
}
