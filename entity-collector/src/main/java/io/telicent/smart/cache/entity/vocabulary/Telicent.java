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
package io.telicent.smart.cache.entity.vocabulary;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * Provides constants relating to Telicent specific vocabulary
 */
public final class Telicent {
    /**
     * Private constructor prevents instantiation
     */
    private Telicent() {
    }

    /**
     * Telicent Ontology Namespace URI
     */
    public static final String NAMESPACE = "http://telicent.io/ontology/";

    /**
     * Telicent Security Namespace URI
     */
    public static final String SECURITY_NAMESPACE = "http://telicent.io/security#";

    /**
     * The Graph URI used to identify a graph as being the security labels graph
     */
    public static final Node SECURITY_LABELS_GRAPH_URI = NodeFactory.createURI(SECURITY_NAMESPACE + "labels");

    /**
     * A predicate used to define the match pattern for a labelling rule
     */
    public static final Node SECURITY_LABELS_PATTERN = NodeFactory.createURI(SECURITY_NAMESPACE + "pattern");

    /**
     * A predicate used to define the labels applied to triples that match a labelling rule
     */
    public static final Node SECURITY_LABELS_LABEL = NodeFactory.createURI(SECURITY_NAMESPACE + "label");

    /**
     * A predicate used to specify the primary name for an entity
     */
    public static final Node PRIMARY_NAME = NodeFactory.createURI(NAMESPACE + "primaryName");

    /**
     * A predicate used to specify the primary image for an entity
     */
    public static final Node PRIMARY_IMAGE = NodeFactory.createURI(NAMESPACE + "primaryImage");

}
