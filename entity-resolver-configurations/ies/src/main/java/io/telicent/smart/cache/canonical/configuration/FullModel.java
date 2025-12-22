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
import io.telicent.smart.cache.canonical.utility.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a (Full) Model but with the actual classes (not ids) for the related items
 */
public class FullModel {
    /**
     * Type representing (full) model
     */
    public static final String TYPE = "fullmodel";

    private static final Logger LOGGER = LoggerFactory.getLogger(FullModel.class);

    /**
     * The ID
     */
    @JsonProperty("id")
    @JsonAlias("modelId")
    public String modelId;
    /**
     * A list of the indices to which this model can be applied.
     */
    @JsonProperty("index")
    @JsonAlias("indexes")
    public String index = "";
    /**
     * A list of weighted relations that this model will use.
     */
    public final List<Relation> relations = new ArrayList<>();
    /**
     * A set of scores to be applied for this model.
     */
    public Scores scores = null;

    /**
     * Create new Full Model
     */
    public FullModel() {}

    /**
     * Create a new (Full) Model from the given JSON String
     * @param json representation of class
     * @return Full Model
     */
    public static FullModel loadFromString(String json) {
        return Mapper.loadFromStringLenient(FullModel.class, json);
    }

    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }

    /**
     * Calculate the combined score for a single relationship using the
     * product-based formula:
     *
     *   P = ∏ score_k
     *   Q = ∏ (1 - score_k)
     *   combined = P / (P + Q)
     *
     * Only scored if *all* fields in the relationship are present in matchedFields.
     */
    public static double calculateCombinedScore(Relation relationship,
                                                Scores scores,
                                                List<String> matchedFields) {
        double combinedProduct = 1.0;
        double complementProduct = 1.0;

        // Only score if all relationship fields are actually matched
        if (matchedFields.containsAll(relationship.fields)) {
            for (String matchedRelationshipField : relationship.fields) {
                double score = scores.getScore(matchedRelationshipField);
                combinedProduct *= score;
                complementProduct *= (1 - score);
            }
        } else {
            // If not all fields for this relationship matched, treat as no contribution
            return 0.0;
        }

        return combinedProduct / (combinedProduct + complementProduct);
    }

    /**
     * Calculate the weighted score for a single hit, given the list of fields
     * that matched for that hit.
     *
     * Each relationship contributes its combined score multiplied by its weight.
     * We then normalise by the total weight so the result stays in [0, 1].
     */
    public double calculateScore(List<String> matchedFields) {
        double totalScore = 0.0;

        // Accumulate weighted scores from each relationship
        for (Relation relationship : relations) {
            double relationshipScore = calculateCombinedScore(relationship, scores, matchedFields);
            totalScore += relationship.getWeight() * relationshipScore;
        }

        // Normalise by sum of weights
        double totalWeight = relations.stream()
                                      .mapToDouble(Relation::getWeight)
                                      .sum();
        return totalWeight == 0 ? 0.0 : totalScore / totalWeight;
    }

    /**
     * Calculate scores for all hits for this model.
     *
     * The input is a list of:
     *   - key:    the hit identifier (e.g. document ID)
     *   - value:  the list of field names that matched for that hit
     *
     * Returns a list of (id, score) entries sorted descending by score.
     */
    public List<Map.Entry<String, Double>> calculateScores(
            List<Map.Entry<String, List<String>>> matches) {

        Map<String, Double> scoresById = new HashMap<>();

        for (Map.Entry<String, List<String>> match : matches) {
            String id = match.getKey();
            List<String> matchedFields = match.getValue();
            scoresById.put(id, calculateScore(matchedFields));
        }

        // Sort by score descending
        return scoresById.entrySet()
                         .stream()
                         .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                         .toList();
    }

}
