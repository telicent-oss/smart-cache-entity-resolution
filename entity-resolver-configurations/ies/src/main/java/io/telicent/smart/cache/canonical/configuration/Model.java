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
 * Represents an E.R. Model which constitutes a grouping of indices, relations and scores.
 */
public class Model {
    /**
     * Type representing models
     */
    public static final String TYPE = "models";
    private static final Logger LOGGER = LoggerFactory.getLogger(Relation.class);

    /**
     * Create new model
     */
    public Model() {
    }

    /**
     * Unique ID of models
     */
    public String modelId;
    /**
     * List of indices that the model can be applied to
     */
    public List<String> indexes = new ArrayList<>();
    /**
     * List of relations that the model can use
     */
    public List<String> relations = new ArrayList<>();
    /**
     * List of scorers that the model can use
     */
    public List<String> scorers = new ArrayList<>();

    /**
     * Create a new Model from the given JSON String
     * @param json representation of class
     * @return Model
     */
    public static Model loadFromString(String json) {
        return Mapper.loadFromString(Model.class, json);
    }

    /**
     * Create a new Model from the given JSON Node
     * @param node representation of class
     * @return Model
     */
    public static Model loadFromNode(JsonNode node) {
        return Mapper.loadFromString(Model.class, node.asText());
    }

    /**
     * Transform a full model to an ordinary model
     * @param fullModel a model with classes
     * @return a model with simply IDs
     */
    public static Model loadFromFullModel(FullModel fullModel) {
        Model model = new Model();
        model.modelId = fullModel.modelId;
        model.indexes = fullModel.indexes;
        for(Relation relation : fullModel.relations) {
            model.relations.add(relation.resolverId);
        }
        for(Scorer scorer : fullModel.scorers) {
            model.scorers.add(scorer.scorerId);
        }
        return model;
    }
    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }
}
