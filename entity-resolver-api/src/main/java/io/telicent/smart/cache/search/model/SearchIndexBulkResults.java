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

import java.util.List;
import java.util.stream.Stream;

/**
 * The results of a search index bulk operation: successes and failures.
 *
 * @param <T> the type of the items indexed by the bulk operation.
 */
@Getter
@AllArgsConstructor
public class SearchIndexBulkResults<T> {
    /**
     * The search index results.
     */
    private List<SearchIndexBulkResult<T>> results;

    /**
     * Gets a stream of the successful operations in this results set.
     *
     * @return a stream of the successful operations
     */
    public Stream<SearchIndexBulkResult<T>> getSuccessfulStream() {
        return getResults().stream().filter(SearchIndexBulkResult::isSuccessful);
    }

    /**
     * Gets a stream of the failed operations in this results set.
     *
     * @return a stream of the failed operations
     */
    public Stream<SearchIndexBulkResult<T>> getFailureStream() {
        return getResults().stream().filter(SearchIndexBulkResult::isFailure);
    }

    /**
     * Determines the count of successful operations in this results set.
     *
     * @return a successful operations count
     */
    public int getSuccessfulCount() {
        return (int) getSuccessfulStream().count();
    }

    /**
     * Determines the count of failed operations in this results set.
     *
     * @return a failed operations count
     */
    public int getFailureCount() {
        return (int) getFailureStream().count();
    }

    /**
     * Determines whether the operation at the given index in this results set was successful.
     *
     * @param itemIndex the index of the item whose operational success is to be determined.
     * @return true if the operation for the item at the given index was successful, false otherwise.
     */
    public boolean isSuccessful(int itemIndex) {
        return results.get(itemIndex).isSuccessful();
    }

    /**
     * Determines whether the operation at the given index in this results set failed.
     *
     * @param itemIndex the index of the item whose operational failure is to be determined.
     * @return true if the operation for the item at the given index was a failure, false otherwise.
     */
    public boolean isFailure(int itemIndex) {
        return !results.get(itemIndex).isSuccessful();
    }

    /**
     * Gets the number of results in this results set.
     *
     * @return a count of the number of results in this results set.
     */
    public int size() {
        return results.size();
    }
}
