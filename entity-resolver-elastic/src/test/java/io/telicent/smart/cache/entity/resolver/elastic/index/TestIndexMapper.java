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
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.telicent.smart.cache.canonical.configuration.FullModel;
import io.telicent.smart.cache.canonical.configuration.Model;
import io.telicent.smart.cache.canonical.exception.IndexException;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import io.telicent.smart.cache.canonical.utility.Mapper;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.telicent.smart.cache.canonical.utility.Mapper.writeValueAsString;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TestIndexMapper {
    final ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
    final ElasticsearchIndicesClient mockIndices = mock(ElasticsearchIndicesClient.class);
    final CreateIndexResponse mockCreateIndexResponse = mock(CreateIndexResponse.class);
    final DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);
    final IndexResponse mockIndexResponse = mock(IndexResponse.class);
    final GetResponse mockGetResponse = mock(GetResponse.class);

    final SearchResponse mockSearchResponse = mock(SearchResponse.class);
    final HitsMetadata mockHitsMetadata = mock(HitsMetadata.class);

    final JsonNode mockJsonNode = mock(JsonNode.class);

    final GetMappingResponse mockGetMappingResponse = mock(GetMappingResponse.class);
    final IndexMappingRecord mockIndexMappingRecord = mock(IndexMappingRecord.class);

    final TypeMapping mockTypeMapping = mock(TypeMapping.class);

    private static final String MODEL_JSON =
            "{\"index\":\"canonical_index\",\"relations\":[\"resolver-1\",\"resolver-2\",\"resolver-3\"],\"scores\":\"score-1\"}";
    private static final String MODEL_JSON_WITH_ID =
            "{\"id\":\"test_id\",\"index\":\"canonical_index\",\"relations\":[\"resolver-1\",\"resolver-2\",\"resolver-3\"],\"scores\":\"score-1\"}";

    private static final String RESOLVER_JSON = "{\"weight\":5, \"fields\" : [\"field_1\", \"field_2\"]}";
    private static final String RESOLVER_JSON_WITH_ID =
            "{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}";

    private static final String SCORER_JSON = "{\"fieldScores\":{\"field_1\":5.0},\"id\":null}";
    private static final String SCORER_JSON_WITH_ID = "{\"fieldScores\":{\"field_1\":5.0},\"id\":\"other_id\"}";

    private static final String CANONICAL_JSON_WITH_ID =
            "{\"type\":\"CoreCanonicalTestType\",\"index\":\"\",\"fields\":[{\"name\":\"text-field-fuzzy\",\"type\":\"text\",\"required\":true,\"boost\":1.2,\"exactMatch\":false,\"fuzziness\":{\"enabled\":true,\"min\":0,\"max\":3}}]}";

    @DataProvider(name = "correctMappings")
    private Object[][] correctMappings() {
        return new Object[][]{
                {"models", MODEL_JSON, "test_id", MODEL_JSON_WITH_ID},
                {"relations", RESOLVER_JSON, "unique_id", RESOLVER_JSON_WITH_ID},
                {"scores", SCORER_JSON, "other_id", SCORER_JSON_WITH_ID},
                {"canonicaltype", CANONICAL_JSON_WITH_ID, "differing_id", CANONICAL_JSON_WITH_ID}
        };
    }


    @DataProvider(name = "incorrectMappings")
    private Object[][] incorrectMappings() {
        return new Object[][]{
                {"models"},
                {"relations"},
                {"scores"},
                {"UNRECOGNISED-TYPE"},
                {"canonicaltype"}
        };
    }

    @BeforeClass
    public void setUpMocks() {
        when(mockClient.indices()).thenReturn(mockIndices);
    }

    @Test
    public void test_indexExists_exists() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        // when
        boolean actual = IndexMapper.indexExists(mockClient, "anything");
        // then
        Assert.assertTrue(actual);
    }

    @Test
    public void test_indexExists_missing() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        // when
        boolean actual = IndexMapper.indexExists(mockClient, "anything");
        // then
        Assert.assertFalse(actual);
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_indexExists_elasticsearchException() throws IOException {
        // given
        when(mockIndices.exists(any(ExistsRequest.class))).thenThrow(ElasticsearchException.class);
        // when
        // then
        IndexMapper.indexExists(mockClient, "anything");
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_indexExists_ioException() throws IOException {
        // given
        when(mockIndices.exists(any(ExistsRequest.class))).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.indexExists(mockClient, "anything");
    }

    @Test
    public void test_createIndex_happyPath() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockIndices.create(any(CreateIndexRequest.class))).thenReturn(mockCreateIndexResponse);
        // when
        Throwable t = null;
        try {
            IndexMapper.createIndex(mockClient, "anyType");
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test
    public void test_createIndex_alreadyExists() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        // when
        Throwable t = null;
        try {
            IndexMapper.createIndex(mockClient, "anyType");
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_createIndex_fail() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockIndices.create(any(CreateIndexRequest.class))).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.createIndex(mockClient, "anyType");
    }

    @Test
    public void test_deleteIndex_happyPath() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.delete(any(DeleteRequest.class))).thenReturn(mockDeleteResponse);
        // when
        Throwable t = null;
        try {
            IndexMapper.deleteIndexEntry(mockClient, "anyType", "id");
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test
    public void test_deleteIndex_noIndex() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.delete(any(DeleteRequest.class))).thenReturn(mockDeleteResponse);
        // when
        Throwable t = null;
        try {
            IndexMapper.deleteIndexEntry(mockClient, "anyType", "id");
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_deleteIndex_fail() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.delete(any(DeleteRequest.class))).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.deleteIndexEntry(mockClient, "anyType", "id");
    }

    @Test(dataProvider = "correctMappings")
    public void test_validateEntry(String type, String jsonValue, String id, String expectedJson) {
        // given
        // when
        Object actualObject = IndexMapper.validateEntry(type, id, jsonValue);
        // then
        assertJsonEquals(actualObject.toString(), expectedJson);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_validateEntry_invalidType() {
        // given
        // when
        // then
        IndexMapper.validateEntry("UNRECOGNISED-TYPE", "ID", "VALUE");
    }

    @Test(dataProvider = "incorrectMappings", expectedExceptions = ValidationException.class)
    public void test_validateEntry_invalidValues(String type) {
        // given
        // when
        // then
        IndexMapper.validateEntry(type, "ID", "{\"rubbish\"}");
    }


    @Test(dataProvider = "correctMappings")
    public void test_addIndexEntry_happyPath(String type, String jsonValue, String id, String ignore) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.index(any(IndexRequest.class))).thenReturn(mockIndexResponse);
        // when
        Throwable t = null;
        try {
            IndexMapper.addIndexEntry(mockClient, type, id, jsonValue);
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test(dataProvider = "correctMappings")
    public void test_addIndexEntry_noIndex(String type, String jsonValue, String id, String ignore) throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockIndices.create(any(CreateIndexRequest.class))).thenReturn(mockCreateIndexResponse);
        when(mockClient.index(any(IndexRequest.class))).thenReturn(mockIndexResponse);
        // when
        Throwable t = null;
        try {
            IndexMapper.addIndexEntry(mockClient, type, id, jsonValue);
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test(dataProvider = "incorrectMappings", expectedExceptions = ValidationException.class)
    public void test_addIndexEntry_invalidMappings(String type) throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        // when
        // then
        IndexMapper.addIndexEntry(mockClient, type, "ID", "{\"rubbish\"}");
    }

    @Test(dataProvider = "correctMappings", expectedExceptions = IndexException.class)
    public void test_addIndexEntry_failedIndex(String type, String jsonValue, String id, String ignore) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.index(any(IndexRequest.class))).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.addIndexEntry(mockClient, type, id, jsonValue);
    }

    @Test(dataProvider = "correctMappings")
    public void test_getAllIndexEntries_happyPath(String type, String ignore, String id,
                                                  String expectedJsonWithoutID) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.search(any(SearchRequest.class), any())).thenReturn(mockSearchResponse);
        when(mockSearchResponse.hits()).thenReturn(mockHitsMetadata);
        Hit<JsonNode> hit = Hit.of(h -> h.id(id).index("index").source(mockJsonNode));
        when(mockHitsMetadata.hits()).thenReturn(List.of(hit));
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJsonWithoutID);
        when(mockJsonNode.get("entry")).thenReturn(node);

        String expectedJson = "{\"" + id + "\":" + expectedJsonWithoutID + "}";
        // when
        String actualJson = IndexMapper.getAllIndexEntriesAsString(mockClient, type);
        // then
        assertJsonEquals(actualJson, expectedJson);
    }

    @Test(dataProvider = "incorrectMappings")
    public void test_getAllIndexEntries_noIndex(String type) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        String expectedJson = "{}";
        // when
        String actualJson = IndexMapper.getAllIndexEntriesAsString(mockClient, type);
        // then
        Assert.assertEquals(expectedJson, actualJson);
    }

    @Test(dataProvider = "incorrectMappings", expectedExceptions = IndexException.class)
    public void test_getAllIndexEntries_invalidJson(String type) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.search(any(SearchRequest.class), any())).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.getAllIndexEntriesAsString(mockClient, type);
    }


    @Test(dataProvider = "correctMappings")
    public void test_getIndexEntry_happyPath(String type, String ignore, String id, String expectedJson) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJson);
        when(mockJsonNode.get("entry")).thenReturn(node);
        // when
        String actualJson = IndexMapper.getIndexEntry(mockClient, type, id);
        // then
        assertJsonEquals(actualJson, expectedJson);
    }

    @Test(dataProvider = "correctMappings", expectedExceptions = ValidationException.class)
    public void test_getIndexEntry_noEntry_throwsException(String type, String ignore, String id,
                                                           String expectedJson) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(false);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJson);
        when(mockJsonNode.get("entry")).thenReturn(node);
        // when
        // then
        IndexMapper.getIndexEntry(mockClient, type, id);
    }

    @Test(dataProvider = "correctMappings")
    public void test_getIndexEntry_noIndex(String type, String ignore, String id, String expectedJson) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(false);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJson);
        when(mockJsonNode.get("entry")).thenReturn(node);
        // when
        String actualJson = IndexMapper.getIndexEntry(mockClient, type, id);
        // then
        Assert.assertEquals("{}", actualJson);
    }

    @Test(dataProvider = "correctMappings", expectedExceptions = IndexException.class)
    public void test_getIndexEntry_exception(String type, String ignore, String id, String expectedJson) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenThrow(IOException.class);
        when(mockGetResponse.found()).thenReturn(false);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJson);
        when(mockJsonNode.get("entry")).thenReturn(node);
        // when
        String actualJson = IndexMapper.getIndexEntry(mockClient, type, id);
        // then
        Assert.assertEquals("{}", actualJson);
    }

    @Test(dataProvider = "correctMappings", expectedExceptions = ValidationException.class)
    public void test_getIndexEntry_nullNode_throwsException(String type, String ignore, String id,
                                                            String ignoreJson) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(null);
        when(mockJsonNode.has("entry")).thenReturn(true);
        when(mockJsonNode.get("entry")).thenReturn(null);
        // when
        String actualJson = IndexMapper.getIndexEntry(mockClient, type, id);
        // then
        Assert.assertEquals("{}", actualJson);
    }


    @Test(dataProvider = "correctMappings", expectedExceptions = ValidationException.class)
    public void test_getIndexEntry_noEntryNode_throwsException(String type, String ignore, String id,
                                                               String ignoreJson) throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(false);
        when(mockJsonNode.get("entry")).thenReturn(null);
        // when
        String actualJson = IndexMapper.getIndexEntry(mockClient, type, id);
        // then
        Assert.assertEquals("{}", actualJson);
    }

    @Test(dataProvider = "incorrectMappings", expectedExceptions = ValidationException.class)
    public void test_parseResponseEntryToString_incorrectMappings_throwsException(String type) {
        // given
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode entryNode = new TextNode("{\"rubbish\":\"test\"}");

        // when
        // then
        IndexMapper.parseResponseEntryToString(type, entryNode);
    }

    @Test(dataProvider = "incorrectMappings", expectedExceptions = ValidationException.class)
    public void test_parseResponseEntryToString_incorrectMappings_throwsExceptionX(String type) {
        // given
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode entryNode = new TextNode("{\"rubbish\":\"test\"}");

        // when
        // then
        IndexMapper.parseResponseEntryToString(type, entryNode);
    }


    @Test
    public void test_populateFullModel_empty() {
        // given
        String expectedString = "{\"id\":null,\"index\":\"\",\"relations\":[],\"scores\":null}";
        Model model = Model.loadFromString(expectedString);
        // when
        FullModel result = IndexMapper.populateFullModelFromModel(mockClient, model);
        // then
        assertJsonEquals(result.toString(), expectedString);
    }

    @Test
    public void test_populateFullModel_happyPath() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        when(mockJsonNode.get("entry"))
                .thenReturn(new TextNode(SCORER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID));

        String modelString =
                "{\"index\":\"canonical_index\",\"relations\":[\"resolver-1\",\"resolver-2\",\"resolver-3\"],\"scores\":\"score-1\"}";
        Model model = Model.loadFromString(modelString);
        String expectedString =
                "{\"id\":null,\"index\":\"canonical_index\",\"relations\":[{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5},{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5},{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}],\"scores\":{\"fieldScores\":{\"field_1\":5.0},\"id\":\"other_id\"}}";

        // when
        FullModel result = IndexMapper.populateFullModelFromModel(mockClient, model);
        // then
        assertJsonEquals(result.toString(), expectedString);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_populateFullModel_invalid() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        when(mockJsonNode.get("entry")).thenReturn(new TextNode("{\"rubbish\":\"test\"}"));

        String modelString =
                "{\"index\":\"canonical_index\",\"relations\":[\"resolver-1\",\"resolver-2\",\"resolver-3\"],\"scores\":\"score-1\"}";
        Model model = Model.loadFromString(modelString);
        // when
        // then
        IndexMapper.populateFullModelFromModel(mockClient, model);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_populateFullModel_exception() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenThrow(IOException.class);
        String modelString =
                "{\"index\":\"canonical_index\",\"relations\":[\"resolver-1\",\"resolver-2\",\"resolver-3\"],\"scores\":\"score-1\"}";
        Model model = Model.loadFromString(modelString);
        // when
        // then
        IndexMapper.populateFullModelFromModel(mockClient, model);
    }

    @Test
    public void test_getAllIndexFullModelEntriesAsString_happyPath() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.search(any(SearchRequest.class), any())).thenReturn(mockSearchResponse);
        Hit<JsonNode> hit = Hit.of(h -> h.id("id").index("index").source(mockJsonNode));
        when(mockHitsMetadata.hits()).thenReturn(List.of(hit));
        when(mockSearchResponse.hits()).thenReturn(mockHitsMetadata);
        when(mockJsonNode.has("entry")).thenReturn(true);
        when(mockJsonNode.get("entry")).thenReturn(new TextNode(MODEL_JSON_WITH_ID));


        JsonNode mockJsonNode2 = mock(JsonNode.class);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode2);

        when(mockJsonNode2.has("entry")).thenReturn(true);
        when(mockJsonNode2.get("entry"))
                .thenReturn(new TextNode(SCORER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID));


        String expectedJson =
                "{\"id\":{\"id\":\"test_id\",\"index\":\"canonical_index\",\"relations\":[{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5},{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5},{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}],\"scores\":{\"fieldScores\":{\"field_1\":5.0},\"id\":\"other_id\"}}}";
        // when
        String actualJson = IndexMapper.getAllIndexFullModelEntriesAsString(mockClient);
        // then
        assertJsonEquals(actualJson, expectedJson);
    }

    @Test
    public void test_getAllIndexFullModelEntriesAsString_noIndex() throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        String expectedJson = "{}";
        // when
        String actualJson = IndexMapper.getAllIndexFullModelEntriesAsString(mockClient);
        // then
        Assert.assertEquals(expectedJson, actualJson);
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_getAllIndexFullModelEntriesAsString_invalidJson() throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.search(any(SearchRequest.class), any())).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.getAllIndexFullModelEntriesAsString(mockClient);
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_getAllIndexFullModelEntriesAsString_exception() throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.search(any(SearchRequest.class), any())).thenThrow(IOException.class);
        // when
        // then
        IndexMapper.getAllIndexFullModelEntriesAsString(mockClient);
    }

    @Test
    public void test_addIndexFullModelEntry() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.index(any(IndexRequest.class))).thenReturn(mockIndexResponse);
        String fullModelJSON =
                "{\"id\":\"test_id\",\"index\":\"canonical_index\",\"relations\":[{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}],\"scores\":{\"fieldScores\":{\"field_1\":5.0},\"id\":\"other_id\"}}";
        // when
        Throwable t = null;
        try {
            IndexMapper.addIndexFullModelEntry(mockClient, "id", fullModelJSON);
        } catch (Exception e) {
            t = e;
        }
        // then
        Assert.assertNull(t);
    }

    @Test
    public void test_getFullModelIndexEntry_happyPath() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.search(any(SearchRequest.class), any())).thenReturn(mockSearchResponse);
        Hit<JsonNode> hit = Hit.of(h -> h.id("id").index("index").source(mockJsonNode));
        when(mockHitsMetadata.hits()).thenReturn(List.of(hit));
        when(mockSearchResponse.hits()).thenReturn(mockHitsMetadata);
        when(mockJsonNode.has("entry")).thenReturn(true);
        when(mockJsonNode.get("entry")).thenReturn(new TextNode(MODEL_JSON_WITH_ID));


        JsonNode mockJsonNode2 = mock(JsonNode.class);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode2);

        when(mockJsonNode2.has("entry")).thenReturn(true);
        when(mockJsonNode2.get("entry"))
                .thenReturn(new TextNode(MODEL_JSON_WITH_ID))
                .thenReturn(new TextNode(SCORER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID))
                .thenReturn(new TextNode(RESOLVER_JSON_WITH_ID));


        String expectedJson =
                "{\"id\":\"test_id\",\"index\":\"canonical_index\",\"relations\":[{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5},{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5},{\"id\":\"unique_id\",\"fields\":[\"field_1\",\"field_2\"],\"weight\":5}],\"scores\":{\"fieldScores\":{\"field_1\":5.0},\"id\":\"other_id\"}}";
        // when
        String actualJson = IndexMapper.getFullModelIndexEntry(mockClient, "id");
        // then
        assertJsonEquals(actualJson, expectedJson);
    }

    private static void assertJsonEquals(String actualJson, String expectedJson) {
        try {
            JsonNode actualNode = Mapper.getJsonMapper().readTree(actualJson);
            JsonNode expectedNode = Mapper.getJsonMapper().readTree(expectedJson);
            Assert.assertTrue(actualNode.equals(expectedNode),
                              "Expected JSON " + expectedNode + " but found " + actualNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON for comparison", e);
        }
    }


    @Test(expectedExceptions = IndexException.class)
    public void test_getFullModelIndexEntry_indexNotExists() throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        // when
        // then
        IndexMapper.getFullModelIndexEntry(mockClient, "id");
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_getFullModelIndexEntry_nothingFound() throws
            IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(false);
        // when
        // then
        IndexMapper.getFullModelIndexEntry(mockClient, "id");
    }

    @Test
    public void test_deleteIndexFullModelEntry_happyPath() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);
        when(mockGetResponse.found()).thenReturn(true);

        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        when(mockJsonNode.get("entry")).thenReturn(new TextNode(MODEL_JSON_WITH_ID));

        // when
        IndexMapper.deleteIndexFullModelEntry(mockClient, "id");
        // then
        verify(mockClient, times(5)).delete(any(DeleteRequest.class));
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_getIndexEntryObject_exception() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenThrow(IOException.class);

        // when
        // then
        IndexMapper.getIndexEntryObject(mockClient, "type", "id");
    }

    @Test(expectedExceptions = ValidationException.class)
    public void test_populateFullModelFromObject_otherObject() {
        // given
        // when
        // then
        IndexMapper.populateFullModelFromObject(mockClient, null);
    }


    @Test(dataProvider = "correctMappings")
    public void test_validateIndexEntry_happyPath(String type, String ignore, String id, String expectedJson) throws
            IOException {
        // given
        String indexName = "canonical_index";

        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);

        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJson);
        when(mockJsonNode.get("entry")).thenReturn(node);

        when(mockIndices.getMapping(any(GetMappingRequest.class))).thenReturn(mockGetMappingResponse);
        when(mockGetMappingResponse.get(indexName)).thenReturn(mockIndexMappingRecord);
        when(mockIndexMappingRecord.mappings()).thenReturn(mockTypeMapping);
        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put("something", Property.of(p -> p.text(TextProperty.of(t -> t.index(true)))));
        propertyMap.put("field_1", Property.of(p -> p.text(TextProperty.of(t -> t.index(true)))));
        propertyMap.put("text-field-nonfuzzy", Property.of(p -> p.text(TextProperty.of(t -> t.index(true)))));
        when(mockTypeMapping.properties()).thenReturn(propertyMap);

        // when
        String results = IndexMapper.validateIndexEntry(mockClient, type, id, indexName);
        // then
        Assert.assertNotEquals("{}", results);
    }

    @Test(dataProvider = "correctMappings")
    public void test_validateIndexEntry_happyPath_noMapping(String type, String ignore, String id,
                                                            String expectedJson) throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);

        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode(expectedJson);
        when(mockJsonNode.get("entry")).thenReturn(node);

        when(mockIndices.getMapping(any(GetMappingRequest.class))).thenReturn(mockGetMappingResponse);
        when(mockGetMappingResponse.get("index")).thenReturn(mockIndexMappingRecord);
        when(mockIndexMappingRecord.mappings()).thenReturn(mockTypeMapping);
        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put("something", Property.of(p -> p.text(TextProperty.of(t -> t.index(true)))));
        when(mockTypeMapping.properties()).thenReturn(propertyMap);

        // when
        String results = IndexMapper.validateIndexEntry(mockClient, type, id, "index");
        // then
        Assert.assertNotEquals("{}", results);
    }

    @Test
    public void test_validateIndexEntry_emptyMap() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockClient.get(any(GetRequest.class), any())).thenReturn(mockGetResponse);

        when(mockGetResponse.found()).thenReturn(true);
        when(mockGetResponse.source()).thenReturn(mockJsonNode);
        when(mockJsonNode.has("entry")).thenReturn(true);
        JsonNode node = new TextNode("{}");
        when(mockJsonNode.get("entry")).thenReturn(node);

        when(mockIndices.getMapping(any(GetMappingRequest.class))).thenReturn(mockGetMappingResponse);
        when(mockGetMappingResponse.get("index")).thenReturn(mockIndexMappingRecord);
        when(mockIndexMappingRecord.mappings()).thenReturn(mockTypeMapping);
        when(mockTypeMapping.properties()).thenReturn(emptyMap());

        // when
        String results = IndexMapper.validateIndexEntry(mockClient, "anyType", "anyId", "index");
        // then
        Assert.assertEquals("", results);
    }

    @Test
    public void test_getIndexMapping_noEntry() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(false);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);

        // when
        Map<String, Property> actual = IndexMapper.getIndexMapping(mockClient, "index");
        // then
        Assert.assertTrue(actual.isEmpty());
    }

    @Test(expectedExceptions = IndexException.class)
    public void test_getIndexMapping_exception() throws IOException {
        // given
        BooleanResponse response = new BooleanResponse(true);
        when(mockIndices.exists(any(ExistsRequest.class))).thenReturn(response);
        when(mockIndices.getMapping(any(GetMappingRequest.class))).thenThrow(IOException.class);

        // when
        // then
        IndexMapper.getIndexMapping(mockClient, "index");
    }
}
