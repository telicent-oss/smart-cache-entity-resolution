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

import io.telicent.smart.cache.canonical.utility.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    public String modelId;
    /**
     * A list of the indices to which this model can be applied.
     */
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
        return Mapper.loadFromString(FullModel.class, json);
    }

    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }
}
