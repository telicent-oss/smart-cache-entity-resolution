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
package io.telicent.smart.cache.search.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents facets results
 */
@JsonPropertyOrder({
        "query",
        "type",
        "sampleSize",
        "results"
})
@Getter
@Setter
@NoArgsConstructor
@ToString
public class FacetResults {

    private final List<FacetResult> results = new ArrayList<>();
    private long sampleSize;
    private String query;
    private QueryType type;

    /**
     * Creates new facet results
     *
     * @param query      The query that produced these facets
     * @param type       The type of query that produced these facets
     * @param sampleSize The sample size used to produce these facets
     * @param results    The facet values themselves
     */
    public FacetResults(String query, QueryType type, long sampleSize,
                        Collection<FacetResult> results) {

        Objects.requireNonNull(query, "Query cannot be null");
        Objects.requireNonNull(type, "Query Type cannot be null");
        Objects.requireNonNull(results, "Results cannot be null");

        this.query = query;
        this.type = type;
        this.sampleSize = sampleSize;
        this.results.addAll(results);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FacetResults that = (FacetResults) o;
        //@formatter:off
        return sampleSize == that.sampleSize
               && Objects.equals(results, that.results)
               && Objects.equals(query, that.query)
               && type == that.type;
        //@formatter:on
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, sampleSize, query, type);
    }
}
