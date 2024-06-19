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

import java.util.ArrayList;
import java.util.List;

/**
 * Initial similarity implementation
 **/

public final class BasicSimilarityQueryGenerator {


    private BasicSimilarityQueryGenerator() {}
    /**
     * Generate a query given a source document
     *
     * @param doc input document
     * @return Elasticsearch Query
     **/
    public static Query generateQuery(Document doc) {

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
            subQ.add(Query.of(q -> q.match(qq -> qq.fuzziness("AUTO").field(k).queryName(k).query(v.toString()))));
        });

        if(subQ.isEmpty()){
            return null;
        }

        return Query.of(q -> q.bool(qq -> qq.should(subQ)));
    }

}
