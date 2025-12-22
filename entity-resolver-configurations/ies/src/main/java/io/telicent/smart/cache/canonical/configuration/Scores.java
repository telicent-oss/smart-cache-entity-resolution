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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.telicent.smart.cache.canonical.utility.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a set of scores per fields for matching purposes.
 */
public class Scores {
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
    @JsonProperty("id")
    @JsonAlias("scorerId")
    public String scorerId;

    private static final Logger LOGGER = LoggerFactory.getLogger(Scores.class);

    /**
     * Create new Scorer
     */
    public Scores() {
    }

    /**
     * Create a new Scorer from the given JSON String
     * @param json representation of class
     * @return Scorer
     */
    public static Scores loadFromString(String json) {
        return Mapper.loadFromString(Scores.class, json);
    }

    /**
     * Create a new Scorer from the given JSON Node
     * @param node representation of class
     * @return Scorer
     */
    public static Scores loadFromNode(JsonNode node) {
        return Mapper.loadFromString(Scores.class, node.asText());
    }

    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }

    /**
     * Return the score for the given field
     * @param field the field to check
     * @return the score if we have one, 0.0 if not
     */

    public double getScore(String field) {
        return fieldScores.getOrDefault(field, 0.0);
    }

    /**
     * Return whether we have a score for the given field
     * @param field the field to check
     * @return true if we have a score, false if not
     */
    public boolean hasField(String field) {
        return fieldScores.containsKey(field);
    }
}
