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

/**
 * An output converter that provides utilities for compacting nodes into prefixed name form
 */
public abstract class AbstractNodeCompactingOutputConverter extends AbstractSingleFieldOutputConverter {
    private final boolean compact;

    /**
     * Creates a new output converter
     *
     * @param outputField Output field
     * @param compact     Whether compaction is enabled
     */
    public AbstractNodeCompactingOutputConverter(String outputField, boolean compact) {
        super(outputField);
        this.compact = compact;
    }

    /**
     * Converts a Node into a string potentially compacting the output, if and only if, it represents a URI
     *
     * @param entity Entity
     * @param node   Node
     * @return String representation of the node
     */
    protected String nodeToString(Entity entity, SecurityLabelledNode node) {
        if (node == null) {
            return null;
        }

        return nodeToString(entity, node.getNode());
    }

    /**
     * Converts a Node into a string potentially compacting the output, if and only if, it represents a URI
     *
     * @param entity Entity
     * @param node   Node
     * @return String representation of the node
     */
    protected String nodeToString(Entity entity, Node node) {
        if (node == null) {
            return null;
        }

        if (node.isURI()) {
            return this.compact && entity.hasPrefixes()
                   ? entity.getPrefixes().shortForm(node.getURI()) : node.getURI();
        } else if (node.isLiteral()) {
            return node.getLiteralLexicalForm();
        } else if (node.isBlank()) {
            return node.getBlankNodeLabel();
        } else {
            return null;
        }
    }

    /**
     * Converts a list of nodes into a list of strings using {@link #nodeToString(Entity, Node)} for each individual
     * node
     *
     * @param entity Entity
     * @param nodes  Nodes
     * @return List of string representations of the nodes
     */
    protected List<String> nodesToStrings(Entity entity, List<SecurityLabelledNode> nodes) {
        if (nodes == null) {
            return null;
        }
        return nodes.stream().map(n -> nodeToString(entity, n.getNode())).toList();
    }
}
