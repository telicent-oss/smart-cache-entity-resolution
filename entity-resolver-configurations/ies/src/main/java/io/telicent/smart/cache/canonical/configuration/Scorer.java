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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a set of scores per fields for matching purposes.
 */
public class Scorer {
    /**
     * Type representing scorers
     */
    public static final String TYPE = "scores";
    /**
     * Mapping of fields and scores.
     */
    public final Map<String, Double> fieldScores = new HashMap<>();
    /**
     * Unique ID of scorer
     */
    public String scorerId;

    private static final Logger LOGGER = LoggerFactory.getLogger(Scorer.class);

    /**
     * Create new Scorer
     */
    public Scorer() {
    }

    /**
     * Create a new Scorer from the given JSON String
     * @param json representation of class
     * @return Scorer
     */
    public static Scorer loadFromString(String json) {
        return Mapper.loadFromString(Scorer.class, json);
    }

    /**
     * Create a new Scorer from the given JSON Node
     * @param node representation of class
     * @return Scorer
     */
    public static Scorer loadFromNode(JsonNode node) {
        return Mapper.loadFromString(Scorer.class, node.asText());
    }

    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }
}
