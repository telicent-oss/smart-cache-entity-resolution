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
package io.telicent.smart.cache.entity;

/**
 * Provides constants for naming entity collection related metrics
 */
public final class EntityMetricNames {

    /**
     * Metric for the total number of entities
     */
    public static final String PROJECTION_ENTITIES_TOTAL = "entities.total";
    /**
     * Metric for the number of entities that produced errors during projection
     */
    public static final String PROJECTION_ENTITIES_ERRORS = "entities.errors";
    /**
     * Metric for a histogram of the input graph sizes from which entities were collected
     */
    public static final String PROJECTION_ENTITIES_GRAPH_SIZES = "entities.graph_sizes";
    /**
     * Metric for a histogram of the number of entities collected per unique graph
     */
    public static final String PROJECTION_ENTITIES_PER_GRAPH = "entities.per_graph";
    /**
     * Description for the {@link #PROJECTION_ENTITIES_TOTAL} metric
     */
    public static final String PROJECTED_ENTITIES_TOTAL_DESCRIPTION = "Number of entities projected";
    /**
     * Description for the {@link #PROJECTION_ENTITIES_ERRORS} metric
     */
    public static final String PROJECTION_ENTITIES_ERROR_DESCRIPTION =
            "Number of entities that encountered errors during projection";
    /**
     * Description for the {@link #PROJECTION_ENTITIES_GRAPH_SIZES} metric
     */
    public static final String PROJECTION_ENTITIES_GRAPH_SIZES_DESCRIPTION =
            "Size of the graphs (in number of triples) used to project entities";
    /**
     * Description for the {@link #PROJECTION_ENTITIES_PER_GRAPH_DESCRIPTION} metric
     */
    public static final String PROJECTION_ENTITIES_PER_GRAPH_DESCRIPTION =
            "Tracks the number of entities produced by each graph";

    /**
     * Private constructor prevents instantiation
     */
    private EntityMetricNames() {
    }
}
