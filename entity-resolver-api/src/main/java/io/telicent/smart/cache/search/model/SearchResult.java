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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a search result
 */
public class SearchResult extends Hit {
    private Document highlighted;

    /**
     * Creates a new empty search result
     */
    public SearchResult() {
    }

    /**
     * Creates a new search result
     *
     * @param id       Document ID
     * @param score    Document Score
     * @param document Document
     */
    public SearchResult(String id, double score, Document document) {
        this(id, score, document, null);
    }

    /**
     * Creates a new search result
     *
     * @param id          Document ID
     * @param score       Document Score
     * @param document    Document
     * @param highlighted Highlighted document
     */
    public SearchResult(String id, double score, Document document, Document highlighted) {
        super(id, score, document);
        this.highlighted = highlighted;
    }

    /**
     * Gets the highlighted document
     *
     * @return Highlighted document, may be {@code null} if result highlighting was not requested
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Document getHighlighted() {
        return this.highlighted;
    }

    /**
     * Sets the highlighted document
     *
     * @param document Highlighted document
     */
    public void setHighlighted(Document document) {
        this.highlighted = document;
    }
}
