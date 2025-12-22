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

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import lombok.Generated;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Triple;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A decorator for {@link LabelsStore} that returns fallback default labels
 */
@Generated
public class DefaultingLabelsStore implements LabelsStore {
    private final @Delegate LabelsStore store;
    private final List<Label> defaultLabels;

    /**
     * Creates a new labels store with fallback default labels
     *
     * @param labels        Actual labels store
     * @param defaultLabels Fallback default labels
     */
    public DefaultingLabelsStore(LabelsStore labels, String defaultLabels) {
        Objects.requireNonNull(labels);
        if (StringUtils.isBlank(defaultLabels)) {
            throw new IllegalArgumentException("Default Labels cannot be blank");
        }
        this.store = labels;

        this.defaultLabels = Arrays.stream(defaultLabels.split(","))
                                   .map(String::trim)
                                   .filter(s -> !s.isEmpty())
                                   .map(Label::fromText)
                                           .collect(Collectors.toList());
    }

    @Override
    public List<Label> labelsForTriples(Triple triple) {
        List<Label> ls = this.store.labelsForTriples(triple);
        return ls.isEmpty() ? this.defaultLabels : ls;
    }
}
