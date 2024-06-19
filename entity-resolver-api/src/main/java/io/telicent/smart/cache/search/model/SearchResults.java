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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.*;

/**
 * Represents a slice of search results
 */
@JsonPropertyOrder({"maybeMore", "limit", "offset", "query", "type", "results"})
public class SearchResults {

    /**
     * Constant indicating that no limit was applied to results
     */
    public static final long UNLIMITED = -1;

    /**
     * Constant indicating the first valid offset
     */
    public static final long FIRST_OFFSET = 1;

    private final List<SearchResult> results = new ArrayList<>();
    private String query;
    private boolean maybeMore;
    private long limit;
    private long offset;
    private QueryType type;
    private final Map<String, Object> extensions = new HashMap<>();

    /**
     * Creates new empty search results
     */
    public SearchResults() {
    }

    /**
     * Creates new search results
     *
     * @param maybeMore Whether there may be more results available for the query than are returned in these
     * @param limit     Limit i.e. the maximum number of results returned.  This may be {@code -1} to indicate no
     *                  limit.
     * @param offset    Offset i.e. the point from where in the results this set of results represents.  This is a 1
     *                  based offset.
     * @param query     The query that produced these results
     * @param type      The type of query that produced these results
     * @param results   The search results themselves
     */
    public SearchResults(boolean maybeMore, long limit, long offset, String query, QueryType type,
                         Collection<SearchResult> results) {
        if (limit < UNLIMITED) {
            throw new IllegalArgumentException("Limit must be >= -1");
        }
        if (offset < FIRST_OFFSET) {
            throw new IllegalArgumentException("Offset must be >= 1");
        }
        Objects.requireNonNull(query, "Query cannot be null");
        Objects.requireNonNull(type, "Query Type cannot be null");
        Objects.requireNonNull(results, "Results cannot be null");

        if (maybeMore && limit == SearchResults.UNLIMITED) {
            throw new IllegalArgumentException("maybeMore MUST always be false if limit is UNLIMITED");
        }

        this.maybeMore = maybeMore;
        this.limit = limit;
        this.offset = offset;
        this.query = query;
        this.type = type;
        this.results.addAll(results);
    }

    /**
     * Gets the search results
     *
     * @return Search results
     */
    public List<SearchResult> getResults() {
        return results;
    }

    /**
     * Gets whether there may be more results for this search
     * <p>
     * A return of {@code true} does not guarantee that there are more results, similarly a return of {@code false}
     * merely means that there are no new results available at this time.  Even when {@code true} there may be no more
     * results available when asked (most likely due to security filtering).  Equally even when {@code false} there may
     * be more results available if the same query is run again in the future e.g. because the underlying search index
     * got updated.
     * </p>
     *
     * @return True if there may be more results available now, false otherwise
     */
    public boolean isMaybeMore() {
        return this.maybeMore;
    }

    /**
     * Sets whether there are more results available for this search
     *
     * @param maybeMore Whether more results may be available
     */
    public void setMaybeMore(boolean maybeMore) {
        this.maybeMore = maybeMore;
    }

    /**
     * Gets the total number of results
     * <p>
     * This has been removed as of {@code 0.11.0} because calculating an accurate total with respect to security
     * labelling is a performance issue and acts as an effective side channel timing attack.  This is because an
     * adversary can judge how many results there are based on how long it takes to respond to a query irrespective of
     * the number of results reported.  If a query takes longer to return then they could imply that there were more
     * possible results to filter.
     * </p>
     *
     * @return Total results
     * @deprecated No longer supported, see {@link #isMaybeMore()} instead
     */
    @JsonIgnore
    @Deprecated(since = "0.11.0", forRemoval = true)
    public long getTotal() {
        throw new IllegalStateException("Total is no longer supported");
    }

    /**
     * Sets the total number of results
     * <p>
     * No longer supported, see {@link #getTotal()} for more details.
     * </p>
     *
     * @param total Total results
     * @deprecated No longer supported, see {@link #setMaybeMore(boolean)} instead
     */
    @JsonIgnore
    @Deprecated(since = "0.11.0", forRemoval = true)
    @SuppressWarnings("unused")
    public void setTotal(long total) {
        throw new IllegalStateException("Total is no longer supported");
    }

    /**
     * Gets the limit of results
     *
     * @return Limit
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Sets the limit of results
     *
     * @param limit Limit
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }

    /**
     * Gets the offset of the results
     *
     * @return Offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Sets the offset of the results
     *
     * @param offset Offset
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Gets the query type of the results
     *
     * @return Query type
     */
    public QueryType getType() {
        return type;
    }

    /**
     * Sets the query type of the results
     *
     * @param type Query Type
     */
    public void setType(QueryType type) {
        this.type = type;
    }

    /**
     * Gets the query that produced these results
     *
     * @return Query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query that produced these results
     *
     * @param query Query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Gets any extensions
     *
     * @return Extensions
     */
    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return this.extensions;
    }

    /**
     * Sets an extension
     *
     * @param key   Extension key
     * @param value Extension value
     */
    @JsonAnySetter
    public void setExtension(String key, Object value) {
        this.extensions.put(key, value);
    }
}
