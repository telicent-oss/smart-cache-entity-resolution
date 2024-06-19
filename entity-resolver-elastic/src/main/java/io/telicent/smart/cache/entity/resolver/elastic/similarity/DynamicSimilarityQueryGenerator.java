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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import io.telicent.smart.cache.entity.resolver.elastic.index.CachedIndexMapper;
import io.telicent.smart.cache.search.model.Document;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Dynamic Query generation by configuration
 **/
public final class DynamicSimilarityQueryGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimilarityQueryGenerator.class);

    private DynamicSimilarityQueryGenerator() {}

    /**
     * Generate a query for the given source document. Provided we have
     * configuration loaded and an entry for the given type.
     *
     * @param doc  input document
     * @param type canonical type
     * @return Elasticsearch Query
     **/
    public static Query generateQuery(Document doc, String type) {
        CanonicalTypeConfiguration config = CachedIndexMapper.getCanonicalTypeConfiguration(type);
        return generateQuery(doc, config);
    }

    /**
     * Generate a query for the given source document using the configuration
     * that has been provided.
     *
     * @param doc input document
     * @param config configuration to use when generating query
     * @return Elasticsearch Query
     **/
    public static Query generateQuery(Document doc, CanonicalTypeConfiguration config) {
        if (null == config) {
            LOGGER.error("No configuration available to generate query");
            return null;
        }
        // build a Boolean query with one sub-query per field
        List<Query> subQ = new ArrayList<>();
        doc.getProperties().forEach((k, v) -> {
            if ("id".equals(k)) {
                return;
            }
            if ("originalId".equals(k)) {
                return;
            }
            if (CanonicalTypeConfiguration.TYPE.equalsIgnoreCase(k)) {
                return;
            }
            // build a query analysing the content of the query
            Query subQuery = getSubQuery(config, k, v);
            if (null != subQuery) {
                subQ.add(subQuery);
            }
        });
        if (subQ.isEmpty()) {
            LOGGER.error("No sufficient data provided to generate query");
            return null;
        }
        return Query.of(q -> q.bool(qq -> qq.should(subQ)));
    }

    /**
     * Generate a (sub)query for the given configuration, field and value.
     * Provided the field exists, otherwise default to an exact match query.
     * TODO: Decide - should this be the default behaviour for un-configured fields?
     * @param config canonical type configuration
     * @param fieldName the name of the field to query
     * @param value the object to query with
     * @return Elasticsearch Query
     **/
    private static Query getSubQuery(CanonicalTypeConfiguration config, String fieldName, Object value) {
        CanonicalTypeConfiguration.SimilarityField fieldConfig = config.getField(fieldName);
        if (null != fieldConfig && fieldConfig.required) {
            if (fieldConfig.exactMatch) {
                return Query.of(q -> q.term(qq -> qq.boost(fieldConfig.boost).field(fieldConfig.name).queryName(fieldName).value(value.toString())));
            } else {
                SimilarityQueryVisitor similarityQueryVisitor = new SimilarityQueryVisitor();
                fieldConfig.accept(similarityQueryVisitor, value);
                return similarityQueryVisitor.getQuery();
            }
        }
        return null;
    }

    /**
     * Obtain the index to use with the given canonical type.
     * Otherwise, return null.
     * @param canonicalType the canonical type in use
     * @return index to query against
     **/
    public static String obtainIndex(String canonicalType) {
        CanonicalTypeConfiguration config = CachedIndexMapper.getCanonicalTypeConfiguration(canonicalType);
        if (null != config && StringUtils.isNotBlank(config.index)) {
            return config.index;
        }
        return null;
    }
}
