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
package io.telicent.smart.cache.canonical.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import io.telicent.smart.cache.canonical.utility.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of relationships between fields for better watching.
 */
public class Relation {
    /**
     * Type representing relations
     */
    public static final String TYPE = "relations";
    /**
     * Unique ID of relations
     */
    public String resolverId;

    /**
     * List of fields that are connected.
     */
    public final List<String> fields = new ArrayList<>();

    /**
     * Weight (0-10) that is to be applied to the resolver
     * for ordering in relation to other relations.
     */
    public int weight;

    private static final Logger LOGGER = LoggerFactory.getLogger(Model.class);

    /**
     * Create new resolver
     */
    public Relation() {}

    /**
     * Create a new Resolver from the given JSON String
     * @param json representation of class
     * @return Resolver
     */
    public static Relation loadFromString(String json) {
        return Mapper.loadFromString(Relation.class, json);
    }

    /**
     * Create a new Resolver from the given JSON Node
     * @param node representation of class
     * @return Resolver
     */
    public static Relation loadFromNode(JsonNode node) {
        return Mapper.loadFromString(Relation.class, node.asText());
    }

    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }
}
