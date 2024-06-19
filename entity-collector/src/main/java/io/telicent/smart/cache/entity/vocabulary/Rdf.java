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

import io.telicent.smart.cache.entity.Entity;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;

/**
 * Provides useful constants relating to RDF vocabulary
 */
public final class Rdf {
    /**
     * Private constructor prevents instantiation
     */
    private Rdf() {
    }

    /**
     * The RDF {@code type} predicate as a node
     */
    public static final Node TYPE = RDF.type.asNode();
    /**
     * The group used to collect RDF types on an {@link Entity}
     */
    public static final String TYPE_GROUP = RDF.type.getURI();
    /**
     * The RDF {@code subject} predicate as a node
     */
    public static final Node SUBJECT = RDF.subject.asNode();
    /**
     * The RDF {@code predicate} predicate as a node
     */
    public static final Node PREDICATE = RDF.predicate.asNode();
}
