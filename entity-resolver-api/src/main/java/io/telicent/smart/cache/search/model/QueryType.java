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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Represents supported query types
 */
public enum QueryType {
    /**
     * A Query that is evaluated using the underlying search indices own querystring syntax.  This exposes the full
     * power of the underlying implementations query language to the end user.
     * <p>
     * This is the most powerful kind of query but capabilities may vary across implementations, and it can be user
     * unfriendly because simple queries can require using special syntax to actually return useful results.  See the
     * Markdown documentation in the repository for more discussion on this.
     * </p>
     */
    QUERY("query", "Querystring"),
    /**
     * A Query that is evaluated as a term query, i.e. the query is tokenised into terms, some/all of which must occur
     * in a document for it to be considered a match.
     */
    TERM("term", "Term"),
    /**
     * A Query that is evaluated as a phrase query, i.e. the query is treated as a phrase which must occur within a
     * document for it to be considered a match.
     */
    PHRASE("phrase", "Phrase"),
    /**
     * A Query that is evaluated as a wildcard query, i.e. the query is treated as a set of wildcards that are used to
     * find matching terms, and from those matching documents.
     */
    WILDCARD("wildcard", "Wildcard");

    private final String apiValue;
    private final String displayName;

    /**
     * Creates a query type
     *
     * @param apiValue    Query type API value
     * @param displayName Display name, used primarily for logging purposes
     */
    QueryType(String apiValue, String displayName) {
        this.apiValue = apiValue;
        this.displayName = displayName;
    }

    /**
     * Gets the value of this query type when used in the Search REST API
     *
     * @return API Value
     */
    @JsonValue
    public String getApiValue() {
        return this.apiValue;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

    /**
     * Parses a query type from the REST API query type value
     *
     * @param rawQueryType Raw query type
     * @return Query type
     */
    @JsonCreator
    public static QueryType parse(String rawQueryType) {
        return QueryType.valueOf(rawQueryType.toUpperCase(Locale.ROOT));
    }
}
