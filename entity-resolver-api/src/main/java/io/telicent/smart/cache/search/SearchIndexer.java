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
package io.telicent.smart.cache.search;

import io.telicent.smart.cache.search.model.SearchIndexBulkResults;

import java.util.Collection;
import java.util.function.Function;

/**
 * Represents the ability to index documents for later searching
 *
 * @param <T> Item type
 */
public interface SearchIndexer<T> extends SearchBackend {

    /**
     * Determines whether an item with the given ID is already indexed
     *
     * @param id ID
     * @return True if already indexed, false otherwise, {@code null} if unable to determine
     */
    Boolean isIndexed(String id);

    /**
     * Determines whether an item is already indexed
     *
     * @param idProvider A function that calculates an ID for an item
     * @param item       Item
     * @return True if already indexed, false otherwise, {@code null} if unable to determine
     */
    Boolean isIndexed(Function<T, String> idProvider, T item);

    /**
     * Indexes a single document
     *
     * @param idProvider A function that calculates an ID for an item
     * @param item       Item
     * @throws SearchException Thrown if there is an indexing failure
     */
    void index(Function<T, String> idProvider, T item);

    /**
     * Indexes multiple documents in bulk
     *
     * @param idProvider A function that calculates an ID for an item
     * @param items      Items
     * @return the results of the bulk operation
     * @throws SearchException Thrown if there is an indexing failure
     */
    SearchIndexBulkResults<T> bulkIndex(Function<T, String> idProvider, Collection<T> items);

    /**
     * Deletes a document based upon its ID
     *
     * @param id ID
     */
    void deleteDocument(String id);

    /**
     * Deletes some document contents
     *
     * @param idProvider A function that calculates an ID for an item
     * @param item       Item to delete
     */
    void deleteContents(Function<T, String> idProvider, T item);

    /**
     * Deletes multiple documents in bulk
     *
     * @param idProvider A function that calculates an ID for an item
     * @param items      Items to delete
     * @return the results of the bulk operation
     */
    SearchIndexBulkResults<T> bulkDeleteDocuments(Function<T, String> idProvider, Collection<T> items);

    /**
     * Deletes some contents from multiple documents in bulk
     *
     * @param idProvider A function that calculates an ID for an item
     * @param items      Items to delete
     * @return the results of the bulk operation
     */
    SearchIndexBulkResults<T> bulkDeleteContents(Function<T, String> idProvider, Collection<T> items);

    /**
     * Requests that the indexer flush the index
     * <p>
     * Depending on the underlying implementation this may do nothing.
     * </p>
     *
     * @param finished Indicates whether search indexing has finished, if {@code true} then the implementation can
     *                 assume that no further calls to any of the indexing functions will be made.  This may allow
     *                 implementations to perform a more comprehensive flush operation at the end of indexing.
     * @throws SearchException Thrown if there is a flush failure
     */
    void flush(boolean finished);
}
