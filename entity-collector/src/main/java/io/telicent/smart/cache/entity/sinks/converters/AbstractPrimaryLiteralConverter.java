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
import io.telicent.smart.cache.entity.SecurityLabelledNode;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An abstract output converter that outputs a single top level field whose value is the first value found for a given
 * direct literal as identified by the configured predicate.
 */
public class AbstractPrimaryLiteralConverter extends AbstractSingleFieldOutputConverter {

    /**
     * The predicate used to select the primary literal of interest
     */
    protected final Node predicate;

    /**
     * Creates a new converter
     *
     * @param outputField Name of the output field to produce
     * @param predicate   Predicate identifying the direct literal whose value should be output (if present on the
     *                    entity)
     */
    public AbstractPrimaryLiteralConverter(String outputField, Node predicate) {
        super(outputField);
        this.predicate = predicate;
    }

    @Override
    protected boolean shouldOutputFineGrainedSecurityLabels(Entity entity) {
        return true;
    }

    @Override
    protected Object getOutput(Entity entity) {
        SecurityLabelledNode node = getPrimaryLiteral(entity);
        return node != null ? node.getNode().getLiteralLexicalForm() : null;
    }

    /**
     * Gets the primary literal for the entity
     *
     * @param entity Entity
     * @return Primary Literal, or {@code null} if no such literal for this entity
     */
    protected SecurityLabelledNode getPrimaryLiteral(Entity entity) {
        return entity.getData(DefaultOutputFields.LITERALS)
                                          .flatMap(d -> {
                                              List<SecurityLabelledNode> names = d.get(this.predicate);
                                              return names != null ? names.stream() : Stream.empty();
                                          })
                                          .filter(n -> n.getNode().isLiteral())
                                          .findFirst()
                                          .orElse(null);
    }

    @Override
    protected void populateSecurityLabels(Entity entity, Map<String, Object> securityLabels) {
        SecurityLabelledNode node = getPrimaryLiteral(entity);
        if (node != null && node.hasSecurityLabel()) {
            securityLabels.put(this.outputField, node.getSecurityLabel());
        }
    }
}
