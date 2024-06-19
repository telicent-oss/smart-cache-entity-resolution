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
import io.telicent.smart.cache.search.model.Document;
import org.apache.commons.lang3.StringUtils;

import static io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration.TYPE;

/**
 * Class determining whether to use dynamical or default rules for similarity queries
 */
public final class QueryGeneratorResolver {

    private QueryGeneratorResolver() {}

    /**
     * Generate a query based upon given document. If mapping override provided use it, otherwise load from stored
     * mappings.
     *
     * @param doc                   incoming query document
     * @param overrideConfiguration the override configuration to apply
     * @return Similarity query for searching with
     */
    public static Query generateQuery(Document doc, CanonicalTypeConfiguration overrideConfiguration) {
        if (null != overrideConfiguration) {
            return DynamicSimilarityQueryGenerator.generateQuery(doc, overrideConfiguration);
        } else {
            String canonicalType = (String) doc.getProperty(TYPE);
            if (!StringUtils.isBlank(canonicalType)) {
                return DynamicSimilarityQueryGenerator.generateQuery(doc, canonicalType);
            }
        }
        return BasicSimilarityQueryGenerator.generateQuery(doc);
    }

    /**
     * Resolve the index to query against based on the incoming document. Rely on default if unavailable.
     *
     * @param doc          incoming query document
     * @param defaultIndex Index to use if no match can be found
     * @return The index to use
     */
    public static String resolveIndex(Document doc, String defaultIndex) {
        if (null != doc) {
            String canonicalType = (String) doc.getProperty(TYPE);
            if (!StringUtils.isBlank(canonicalType)) {
                String index = DynamicSimilarityQueryGenerator.obtainIndex(canonicalType);
                if (null != index) {
                    return index;
                }
            }
        }
        return defaultIndex;
    }
}
