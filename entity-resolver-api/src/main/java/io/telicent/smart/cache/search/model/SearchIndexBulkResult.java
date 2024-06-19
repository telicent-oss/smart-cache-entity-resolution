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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The result of a search index bulk operation.
 *
 * @param <T> the type items being indexed
 */
@Getter
@AllArgsConstructor
public class SearchIndexBulkResult<T> {
    /**
     * Whether the index operation was successful.
     */
    private final boolean successful;

    /**
     * The item being indexed
     */
    private final T item;

    /**
     * A reason for the success or failure outcome of the index operation, which may be if the operation was successful
     */
    private final String reason;

    /**
     * Creates a new search index bulk result.
     *
     * @param successful if the bulk operation was successful
     * @param item       the item whose bulk operation was applied to the index
     */
    public SearchIndexBulkResult(boolean successful, final T item) {
        this(successful, item, null);
    }

    /**
     * Determines whether the bulk operation failed
     *
     * @return true if the operation was a failure, false otherwise
     */
    public boolean isFailure() {
        return !isSuccessful();
    }
}
