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
package io.telicent.smart.cache.entity.resolver.elastic.similarity;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import io.telicent.smart.cache.search.SearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Class for validation configuration against a given index
 */
public final class CanonicalTypeConfigurationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalTypeConfigurationValidator.class);

    private CanonicalTypeConfigurationValidator() {}

    /**
     * Validate the presented configuration against the existing index's mappings.
     * Throws an exception if there is a problem.
     * @param client the elastic search client
     * @param index the index to check against
     * @param canonicalTypeConfiguration the override configuration
     */
    public static void validateConfig(ElasticsearchClient client, String index, CanonicalTypeConfiguration canonicalTypeConfiguration) {
        GetMappingRequest request = GetMappingRequest.of(r -> r.index(index));
        try {
            GetMappingResponse response = client.indices().getMapping(request);
            IndexMappingRecord record = response.get(index);
            TypeMapping mappings = record.mappings();
            Map<String, Property> propertyMap = mappings.properties();
            validateConfigVsMapping(propertyMap, canonicalTypeConfiguration);
        } catch (IOException | ElasticsearchException exception) {
            LOGGER.error("Invalid configuration", exception);
            throw new SearchException("Invalid configuration", exception);
        }
    }

    /**
     * Validate the presented configuration against the existing index's mapping property.
     * Throws an exception if there is a problem.
     * @param propertyMap the index's property mapping
     * @param canonicalTypeConfiguration the override configuration
     */
    public static void validateConfigVsMapping(Map<String, Property> propertyMap, CanonicalTypeConfiguration canonicalTypeConfiguration) {
        for (CanonicalTypeConfiguration.SimilarityField similarityField : canonicalTypeConfiguration.fields) {
            Property property = propertyMap.get(similarityField.name);
            if (null == property) {
                LOGGER.error("Unrecognised field: " + similarityField.name);
                throw new SearchException("Unrecognised field: " + similarityField.name);
            }
            if (similarityField instanceof CanonicalTypeConfiguration.TextField) {
                if (!property.isText()) {
                    LOGGER.error("Property " + similarityField.name + " needs to be Text");
                    throw new SearchException("Property " + similarityField.name + " needs to be Text");
                }
            } else if (similarityField instanceof CanonicalTypeConfiguration.NumberField) {
                if (!(
                        property.isInteger() || property.isDouble() || property.isLong() || property.isFloat()
                )) {
                    LOGGER.error("Property " + similarityField.name + " needs to be a Number");
                    throw new SearchException("Property " + similarityField.name + " needs to be a Number");
                }
            } else if (similarityField instanceof CanonicalTypeConfiguration.KeywordField) {
                if (!property.isKeyword()) {
                    LOGGER.error("Property " + similarityField.name + " needs to be a Keyword");
                    throw new SearchException("Property " + similarityField.name + " needs to be a Keyword");
                }
            } else if (similarityField instanceof CanonicalTypeConfiguration.BooleanField) {
                if (!property.isBoolean()) {
                    LOGGER.error("Property " + similarityField.name + " needs to be a Boolean");
                    throw new SearchException("Property " + similarityField.name + " needs to be a Boolean");
                }
            } else if (similarityField instanceof CanonicalTypeConfiguration.DateField) {
                if (!property.isDate()) {
                    LOGGER.error("Property " + similarityField.name + " needs to be a Date");
                    throw new SearchException("Property " + similarityField.name + " needs to be a Date");
                }
            } else if (similarityField instanceof CanonicalTypeConfiguration.LocationField) {
                if (!property.isGeoPoint()) {
                    LOGGER.error("Property " + similarityField.name + " needs to be a Location");
                    throw new SearchException("Property " + similarityField.name + " needs to be a Location");
                }
            }
        }
    }
}
