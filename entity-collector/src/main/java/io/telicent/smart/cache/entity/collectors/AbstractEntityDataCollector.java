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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * An abstract base class for use by {@link EntityDataCollector} implementations.
 * <p>
 * Mainly provides helper methods around associating security labels with the collected data.
 * </p>
 */
public class AbstractEntityDataCollector {
    /**
     * Finds the security labels for a given triple (if any)
     *
     * @param labelsStore Labels store
     * @param t           Triple
     * @return Security labels, or {@code null} if none
     */
    protected String getSecurityLabels(LabelsStore labelsStore, Triple t) {
        return labelsStore != null ? StringUtils.join(labelsStore.labelsForTriples(
                t), ",") : null;
    }

    /**
     * Finds the security labels for a given triple (if any)
     * <p>
     * Compared to {@link #getSecurityLabels(LabelsStore, Triple)} this overload avoids the creation of a {@link Triple}
     * object if there is not any label store to query for labels.  This may be useful when a data collector does not
     * directly operate on Triple(s) but instead on some other intermediate object e.g. SPARQL Results.
     * </p>
     *
     * @param labelsStore Labels store
     * @param subject     Triple subject
     * @param predicate   Triple predicate
     * @param object      Triple object
     * @return Security labels, or {@code null} if none
     */
    protected String getSecurityLabels(LabelsStore labelsStore, Node subject, Node predicate, Node object) {
        if (labelsStore == null) {
            return null;
        }
        return getSecurityLabels(labelsStore, Triple.create(subject, predicate, object));
    }
}
