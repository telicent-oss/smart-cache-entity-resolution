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
package io.telicent.smart.cache.entity.resolver.elastic.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.telicent.smart.cache.canonical.configuration.*;
import io.telicent.smart.cache.canonical.exception.IndexException;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import io.telicent.smart.cache.search.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.telicent.smart.cache.canonical.utility.Mapper.writeValueAsString;

/**
 * Handles the interactions (read/insert/delete) with the indices for the configuration
 */
public final class IndexMapper {

    private IndexMapper() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexMapper.class);
    private static final String CONFIG_INDEX_PREFIX = "er_config_";
    private static final String ID = "id";
    private static final String ENTRY = "entry";

    private static final String EMPTY_JSON = "{}";

    /**
     * Check if given index exists.
     *
     * @param client elastic search client
     * @param index  the name of the index
     * @return True/False depending on whether Index exists.
     */
    public static boolean indexExists(ElasticsearchClient client, String index) {
        try {
            BooleanResponse response = client.indices().exists(ExistsRequest.of(er -> er.index(index)));
            return response.value();
        } catch (IOException | ElasticsearchException e) {
            LOGGER.warn("Unable to determine if index {} exists", index);
            throw new IndexException(e);
        }
    }

    private static Map<String, Property> getProperties() {
        Map<String, Property> properties = new HashMap<>();
        properties.put(ID, Property.of(p -> p.text(t -> t)));
        properties.put(ENTRY, Property.of(p -> p.text(t -> t)));
        return properties;
    }

    /**
     * Create an index for the given type if it doesn't exist.
     *
     * @param client elastic search client
     * @param index  the name of the index to create
     */
    public static void createIndex(ElasticsearchClient client, String index) {
        try {
            if (!indexExists(client, index)) {
                IndexSettings settings =
                        new IndexSettings.Builder()
                                .hidden(false)
                                .numberOfReplicas("1")
                                .numberOfShards("1")
                                .build();
                Map<String, Property> propertyMap = getProperties();
                CreateIndexResponse response = client.indices().create(
                        CreateIndexRequest.of(cr -> cr.index(index)
                                                      .settings(settings)
                                                      .mappings(m -> m.dynamic(DynamicMapping.False)
                                                                      .properties(propertyMap))));
                LOGGER.info(response.toString());
            } else {
                LOGGER.info("Index already exists: {}", index);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to create index {}", index);
            throw new IndexException(e);
        }
    }

    /**
     * Validate the incoming string by attempting to create the given object.
     *
     * @param type  the type of config (i.e.  Resolver, Scorer, Model)
     * @param id    the name of the config
     * @param value the configuration value
     * @return Object representation of the configuration class created (with ID added). Note: better to have this side
     * effect than doing elsewhere
     */
    public static Object validateEntry(String type, String id, String value) {
        if (Scorer.TYPE.equalsIgnoreCase(type)) {
            Scorer scorer = Scorer.loadFromString(value);
            scorer.scorerId = id;
            return scorer;
        } else if (Relation.TYPE.equalsIgnoreCase(type)) {
            Relation relation = Relation.loadFromString(value);
            relation.resolverId = id;
            return relation;
        } else if (Model.TYPE.equalsIgnoreCase(type)) {
            Model model = Model.loadFromString(value);
            model.modelId = id;
            return model;
        } else if (FullModel.TYPE.equalsIgnoreCase(type)) {
            FullModel fullModel = FullModel.loadFromString(value);
            fullModel.modelId = id;
            return fullModel;
        } else if (CanonicalTypeConfiguration.TYPE.equalsIgnoreCase(type)) {
            return CanonicalTypeConfiguration.loadFromString(value);
        } else {
            LOGGER.error("Type is not recognised: {}", type);
        }
        throw new ValidationException("Type " + type + " not recognised for " + id);
    }

    /**
     * Add given config to the relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     * @param value  the configuration value
     */
    public static void addIndexEntry(ElasticsearchClient client, String type, String id, String value) {
        String index = CONFIG_INDEX_PREFIX + type;
        try {
            if (!indexExists(client, index)) {
                createIndex(client, index);
            }
            // validate
            Object validatedObject = validateEntry(type, id, value);
            addObjectToIndex(client, id, index, validatedObject);
        } catch (ValidationException e) {
            LOGGER.error(String.format("Unable to add entry %s to index %s", id, index), e);
            throw e;
        }
    }

    /**
     * Add the given object to the relevant index
     *
     * @param client elastic search client
     * @param id     the name of the config
     * @param index  the relevant index
     * @param object the actual object
     */
    public static void addObjectToIndex(ElasticsearchClient client, String id, String index, Object object) {
        try {
            Document doc = new Document();
            doc.setProperty(ID, id);
            doc.setProperty(ENTRY, object.toString());
            IndexResponse response =
                    client.index(IndexRequest.of(ir -> ir.document(doc).index(index).refresh(Refresh.True).id(id)));
            LOGGER.info(response.toString());
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    /**
     * Add given config for a Full Model to the relevant indices
     *
     * @param client elastic search client
     * @param id     the name of the config
     * @param value  the configuration value
     */
    public static void addIndexFullModelEntry(ElasticsearchClient client, String id, String value) {
        Object validatedObject = validateEntry(FullModel.TYPE, id, value);
        if (validatedObject instanceof FullModel fullModel) {
            Model model = Model.loadFromFullModel(fullModel);
            addObjectToIndex(client, model.modelId, CONFIG_INDEX_PREFIX + Model.TYPE, model);
            for (Relation relation : fullModel.relations) {
                addObjectToIndex(client, relation.resolverId, CONFIG_INDEX_PREFIX + Relation.TYPE, relation);
            }
            for (Scorer scorer : fullModel.scorers) {
                addObjectToIndex(client, scorer.scorerId, CONFIG_INDEX_PREFIX + Scorer.TYPE, scorer);
            }
        }
    }

    /**
     * For the given Full Model config - delete all relevant details
     *
     * @param client elastic search client
     * @param id     the name of the config
     */
    public static void deleteIndexFullModelEntry(ElasticsearchClient client, String id) {
        Object modelObject = getIndexEntryObject(client, Model.TYPE, id);
        if (modelObject instanceof Model model) {
            for (String resolverId : model.relations) {
                deleteIndexEntry(client, Relation.TYPE, resolverId);
            }
            for (String scorerId : model.scorers) {
                deleteIndexEntry(client, Scorer.TYPE, scorerId);
            }
            deleteIndexEntry(client, Model.TYPE, model.modelId);
        }
    }


//    public static void updateIndexFullModelEntry(ElasticsearchClient client, String id, String value) {
//        Object validatedObject = validateEntry(FullModel.TYPE, id, value);
//        if (validatedObject instanceof FullModel fullModel) {
//            Model model = Model.loadFromFullModel(fullModel);
//            addObjectToIndex(client, model.modelId, CONFIG_INDEX_PREFIX + Model.TYPE, model);
//            for (Relation relation : fullModel.relations) {
//                addObjectToIndex(client, relation.resolverId, CONFIG_INDEX_PREFIX + Relation.TYPE, relation);
//            }
//            for (Scorer scorer : fullModel.scorers) {
//                addObjectToIndex(client, scorer.scorerId, CONFIG_INDEX_PREFIX + Scorer.TYPE, scorer);
//            }
//        }
//    }


    /**
     * Get all entries from relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @return String representation of given configuration class.
     */
    public static String getAllIndexEntriesAsString(ElasticsearchClient client, String type) {
        Map<String, Object> hitMap = getAllIndexEntriesAsMap(client, type);
        return writeValueAsString(hitMap);
    }

    /**
     * Get all entries from relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @return Map of config classes by id.
     */
    public static Map<String, Object> getAllIndexEntriesAsMap(ElasticsearchClient client, String type) {
        String index = CONFIG_INDEX_PREFIX + type;
        try {
            Map<String, Object> hitMap = new HashMap<>();
            if (indexExists(client, index)) {
                // Create a search request to retrieve all documents
                SearchResponse<JsonNode> searchResponse =
                        client.search(SearchRequest.of(sr -> sr.index(index)), JsonNode.class);
                for (Hit<JsonNode> hit : searchResponse.hits().hits()) {
                    JsonNode sourceNode = hit.source();
                    JsonNode entryNode = sourceNode.get(ENTRY);
                    hitMap.put(hit.id(), parseResponseEntryToObject(type, entryNode));
                }
            } else {
                LOGGER.info("Nothing found for index {}", index);
            }
            return hitMap;
        } catch (IOException e) {
            LOGGER.warn("Unable to get entries from index {}", index);
            throw new IndexException(e);
        }
    }

    /**
     * Get all entries from relevant index
     *
     * @param client elastic search client
     * @return String representation of given configuration class.
     */
    public static String getAllIndexFullModelEntriesAsString(ElasticsearchClient client) {
        Map<String, Object> hitMap = getAllIndexEntriesAsMap(client, Model.TYPE);
        Map<String, FullModel> fullModelMap = hitMap.entrySet().stream().collect(
                HashMap::new,
                (map, entry) -> map.put(entry.getKey(), populateFullModelFromObject(client, entry.getValue())),
                HashMap::putAll);
        return writeValueAsString(fullModelMap);
    }

    /**
     * Get entry from relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     * @return String representation of given configuration class.
     */
    public static String getIndexEntry(ElasticsearchClient client, String type, String id) {
        String index = CONFIG_INDEX_PREFIX + type;
        try {
            if (indexExists(client, index)) {
                return parseResponseEntryToString(type, getJsonIndexEntry(client, type, id));
            } else {
                LOGGER.error("Cannot read {} as index {} does not exist", id, index);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to get entry {} from index {}", id, index);
            throw new IndexException(e);
        }
        return EMPTY_JSON;
    }

    /**
     * Get entry from relevant index (as object)
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     * @return Object representation of given configuration class.
     */
    public static Object getIndexEntryObject(ElasticsearchClient client, String type, String id) {
        String index = CONFIG_INDEX_PREFIX + type;
        try {
            if (indexExists(client, index)) {
                return parseResponseEntryToObject(type, getJsonIndexEntry(client, type, id));
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to get entry {} from index {}", id, index);
            throw new IndexException(e);
        }
        String errorMessage = String.format("Cannot read %s as index %s does not exist", id, index);
        LOGGER.error(errorMessage);
        throw new IndexException(errorMessage);
    }

    private static JsonNode getJsonIndexEntry(ElasticsearchClient client, String type, String id) throws IOException {
        String index = CONFIG_INDEX_PREFIX + type;
        GetResponse<JsonNode> response =
                client.get(GetRequest.of(gr -> gr.index(index).id(id)), JsonNode.class);
        if (response.found()) {
            JsonNode sourceNode = response.source();
            if (sourceNode != null && sourceNode.has(ENTRY)) {
                return sourceNode.get(ENTRY);
            }
        } else {
            LOGGER.info("Nothing found for {} in index {}", id, index);
        }
        return null;
    }


    /**
     * Parse the response details from ES and create the relevant class
     *
     * @param type      the type of config (i.e.  Resolver, Scorer, Model)
     * @param entryNode a Json Node containing the returned config
     * @return A string representation of the given configuration class
     */
    public static String parseResponseEntryToString(String type, JsonNode entryNode) {
        Object object = parseResponseEntryToObject(type, entryNode);
        if (null == object) {
            throw new ValidationException("Could not parse entry");
        }
        return object.toString();
    }

    /**
     * Parse the response details from ES and create the relevant class
     *
     * @param type      the type of config (i.e.  Resolver, Scorer, Model)
     * @param entryNode a Json Node containing the returned config
     * @return Object representation of the given configuration class
     */
    public static Object parseResponseEntryToObject(String type, JsonNode entryNode) {
        if (null == entryNode) {
            return null;
        }
        if (Scorer.TYPE.equalsIgnoreCase(type)) {
            return Scorer.loadFromNode(entryNode);
        } else if (Relation.TYPE.equalsIgnoreCase(type)) {
            return Relation.loadFromNode(entryNode);
        } else if (Model.TYPE.equalsIgnoreCase(type)) {
            return Model.loadFromNode(entryNode);
        } else if (CanonicalTypeConfiguration.TYPE.equalsIgnoreCase(type)) {
            return CanonicalTypeConfiguration.loadFromNode(entryNode);
        } else {
            String errorMessage = String.format("Type is not recognised: %s", type);
            LOGGER.error(errorMessage);
            throw new ValidationException(errorMessage);
        }
    }

    /**
     * Delete given entry from relevant index.
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     */
    public static void deleteIndexEntry(ElasticsearchClient client, String type, String id) {
        String index = CONFIG_INDEX_PREFIX + type;
        try {
            if (indexExists(client, index)) {
                client.delete(DeleteRequest.of(dr -> dr.index(index).id(id).refresh(Refresh.True)));
            } else {
                LOGGER.error("Cannot delete {} as index {} does not exist", id, index);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to add entry {} to index {}", id, index);
            throw new IndexException(e);
        }
    }

    /**
     * Populate detailed model from simple model
     *
     * @param client elastic search client
     * @param model  simple model (with string representations)
     * @return detailed model (with actual representations)
     */
    public static FullModel populateFullModelFromModel(ElasticsearchClient client, Model model) {
        try {
            FullModel fullModel = new FullModel();
            fullModel.modelId = model.modelId;
            fullModel.indexes = model.indexes;
            for (String resolverId : model.relations) {
                JsonNode node = getJsonIndexEntry(client, Relation.TYPE, resolverId);
                fullModel.relations.add(Relation.loadFromNode(node));
            }
            for (String scorerId : model.scorers) {
                JsonNode node = getJsonIndexEntry(client, Scorer.TYPE, scorerId);
                fullModel.scorers.add(Scorer.loadFromNode(node));
            }
            return fullModel;
        } catch (IOException e) {
            LOGGER.error("Problem building detailed model", e);
            throw new ValidationException(e);
        }
    }

    /**
     * Create a Full model from a model (object)
     *
     * @param client      elastic search client
     * @param modelObject an object which should be an object
     * @return a Full Model instance
     */
    public static FullModel populateFullModelFromObject(ElasticsearchClient client, Object modelObject) {
        if (modelObject instanceof Model model) {
            return populateFullModelFromModel(client, model);
        }
        throw new ValidationException(String.format("Expecting Model type not %s", modelObject));
    }

    /**
     * Get a detailed (full) model entry
     *
     * @param client elastic search client
     * @param id     the unique ID
     * @return a String representation of the full model instance
     */
    public static String getFullModelIndexEntry(ElasticsearchClient client, String id) {
        Object modelObject = getIndexEntryObject(client, Model.TYPE, id);
        FullModel fullModel = populateFullModelFromObject(client, modelObject);
        return fullModel.toString();
    }

    /**
     * Take the index mapping and convert it to a more "friendly" map
     *
     * @param indexMapping elasticsearch mapping details
     * @return a string, string mapping
     */
    public static Map<String, String> printFriendlyIndexMap(Map<String, Property> indexMapping) {
        return indexMapping.entrySet().stream().collect(
                HashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()._kind().jsonValue()),
                HashMap::putAll);
    }

    /**
     * @param client elastic search client
     * @param type   the configuration type
     * @param id     the unique id
     * @param index  the relevant index
     * @return a string representation of: - the config - the index details - the validation results
     */
    public static String validateIndexEntry(ElasticsearchClient client, String type, String id, String index) {
        Map<String, Property> indexMapping = getIndexMapping(client, index);
        if (indexMapping.isEmpty()) {
            LOGGER.error("Nothing to validate against in index {} for {} of type {}", index, id, type);
            return "";
        }
        Object object = getIndexEntryObject(client, type, id);

        Map<String, Object> results = new HashMap<>();
        results.put(type, object);
        results.put("indexMapping", printFriendlyIndexMap(indexMapping));

        Map<String, String> validationResults = new HashMap<>();
        if (object instanceof Model model) {
            if (model.indexes.contains(index)) {
                validationResults.put(index, "index matches");
            } else {
                validationResults.put(index, "index not included");
            }
        } else if (object instanceof Relation relation) {
            for (String fieldName : relation.fields) {
                if (indexMapping.containsKey(fieldName)) {
                    validationResults.put(fieldName, "matches Index entry");
                } else {
                    validationResults.put(fieldName, "no match in Index");
                }
            }
        } else if (object instanceof Scorer scorer) {
            // Check list of scorers against map
            for (String fieldName : scorer.fieldScores.keySet()) {
                if (indexMapping.containsKey(fieldName)) {
                    validationResults.put(fieldName, "Matches Index entry");
                } else {
                    validationResults.put(fieldName, "no match in Index");
                }
            }
        }
        results.put("validationResults", validationResults);
        return writeValueAsString(results);
    }

    /**
     * Get the mapping details for a given index
     *
     * @param client elastic search client
     * @param index  the relevant index
     * @return a mapping of fields and properties
     */
    public static Map<String, Property> getIndexMapping(ElasticsearchClient client, String index) {
        try {
            if (indexExists(client, index)) {
                GetMappingRequest request = GetMappingRequest.of(r -> r.index(index));
                GetMappingResponse response = client.indices().getMapping(request);
                IndexMappingRecord record = response.get(index);
                TypeMapping mappings = record.mappings();
                return mappings.properties();
            } else {
                LOGGER.error("No index available {}", index);
            }
        } catch (IOException e) {
            String errorMessage = String.format("Error obtaining index mapping: %s", index);
            LOGGER.error(errorMessage, e);
            throw new IndexException(errorMessage, e);
        }
        return Collections.emptyMap();
    }
}
