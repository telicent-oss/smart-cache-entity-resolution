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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CanonicalTypeConfigurationDeserializer extends JsonDeserializer<CanonicalTypeConfiguration> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public CanonicalTypeConfiguration deserialize(JsonParser jsonParser, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        // Deserialize fields manually if necessary
        String type = node.get("type").asText();
        String index = node.get("index").asText();
        JsonNode fieldsNode = node.get("fields");

        List<CanonicalTypeConfiguration.SimilarityField> fields = new ArrayList<>();
        for (JsonNode fieldNode : fieldsNode) {
            CanonicalTypeConfiguration.SimilarityField field = mapper.convertValue(fieldNode,
                                                                                   CanonicalTypeConfiguration.SimilarityField.class);
            fields.add(field);
        }

        CanonicalTypeConfiguration config = new CanonicalTypeConfiguration();
        config.type = type;
        config.index = index;
        config.fields = fields;

        return config;
    }
}
