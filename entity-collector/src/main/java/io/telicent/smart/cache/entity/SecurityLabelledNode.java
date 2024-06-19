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

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;

/**
 * An RDF Node which optionally has a Security Label associated with it
 */
public class SecurityLabelledNode {

    private final Node node;
    private final String securityLabel;

    /**
     * Creates a new node with no security labels associated with it
     *
     * @param n Node
     */
    public SecurityLabelledNode(Node n) {
        this(n, null);
    }

    /**
     * Creates a new node with security labels
     *
     * @param n     Node
     * @param label Security labels
     */
    public SecurityLabelledNode(Node n, String label) {
        this.node = n;
        this.securityLabel = label;
    }

    /**
     * Gets the node
     *
     * @return Node
     */
    public Node getNode() {
        return this.node;
    }

    /**
     * Gets the security label that applies
     *
     * @return Security label, or {@code null} if no specific label applies
     */
    public String getSecurityLabel() {
        return this.securityLabel;
    }

    /**
     * Gets whether there is a specific security label for this Node
     *
     * @return True if a security label, false otherwise
     */
    public boolean hasSecurityLabel() {
        return StringUtils.isNotBlank(this.securityLabel);
    }
}
