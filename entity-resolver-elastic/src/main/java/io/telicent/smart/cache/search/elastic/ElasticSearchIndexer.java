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
package io.telicent.smart.cache.search.elastic;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.*;
import co.elastic.clients.elasticsearch.indices.FlushResponse;
import co.elastic.clients.elasticsearch.indices.ForcemergeResponse;
import io.github.resilience4j.retry.RetryConfig;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.SearchIndexer;
import io.telicent.smart.cache.search.SearchUtils;
import io.telicent.smart.cache.search.elastic.utils.ContentDeletion;
import io.telicent.smart.cache.search.elastic.utils.ContentUpdate;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.SearchIndexBulkResult;
import io.telicent.smart.cache.search.model.SearchIndexBulkResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;

/**
 * A search indexer backed by ElasticSearch
 *
 * @param <T> Item type
 */
public class ElasticSearchIndexer<T> extends AbstractElasticClient implements SearchIndexer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchIndexer.class);
    /**
     * Maximum number of retries used for indexing operations
     */
    public static final int DEFAULT_MAX_RETRIES = 3;
    /**
     * Default minimum operation retry interval
     */
    public static final Duration DEFAULT_MIN_RETRY_INTERVAL = Duration.ofSeconds(4);
    /**
     * Default maximum operation retry interval
     */
    public static final Duration DEFAULT_MAX_RETRY_INTERVAL = Duration.ofSeconds(15);
    /**
     * Default minimum retry interval for index operations
     */
    public static final Duration DEFAULT_MIN_INDEX_RETRY_INTERVAL = Duration.ofSeconds(10);
    /**
     * Default maximum retry interval for index operations
     */
    public static final Duration DEFAULT_MAX_INDEX_RETRY_INTERVAL = Duration.ofSeconds(60);
    /**
     * Function used when no errors are permitted in a bulk response
     */
    private static final Function<BulkResponseItem, Boolean> NO_BULK_ERRORS_ACCEPTED = i -> false;
    /**
     * Function used when no errors are permitted in the write response
     */
    private static final Function<WriteResponseBase, Boolean> NO_ERRORS_ACCEPTED = r -> false;
    /**
     * Function used when a 404 error is permitted in the delete response
     */
    private static final Function<WriteResponseBase, Boolean> DELETE_ON_NON_EXISTENT_DOCUMENT =
            r -> r.shards().failures().stream().allMatch(f -> StringUtils.equals(f.status(), "404"));

    private final String index;
    private final boolean upsert;
    private final RetryConfig flushRetryConfig;
    private final RetryConfig forceMergeRetryConfig;
    private final RetryConfig indexRetryConfig;
    private final Function<T, Script> itemToUpdateScript;
    private final Function<T, Script> itemToDeleteScript;

    /**
     * Creates a new ElasticSearch Indexer
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param index                    The index into which documents should be indexed
     * @param upsert                   Whether documents are ingested into the index via upserts i.e. each document that
     *                                 is submitting for indexing is either indexed for the first time, or if a document
     *                                 with the ID already exists acts as an update to that document.
     * @param updateScriptBuilder      A function that given a document translates it into a script to update its
     *                                 contents from an existing ElasticSearch document. This function is not necessary
     *                                 for updates to function but may result in them functioning more accurately and
     *                                 reliably.
     * @param deleteScriptBuilder      A function that given a document translates it into a script to delete its
     *                                 contents from an existing ElasticSearch document.
     * @param maxRetries               Maximum number of retries for ElasticSearch operations
     * @param minRetryInterval         Minimum interval between retries for general ElasticSearch operations
     * @param maxRetryInterval         Maximum interval between retries for general ElasticSearch operations
     * @param minIndexRetryInterval    Minimum interval between retries for ElasticSearch indexing operations i.e.
     *                                 operations that involve data
     * @param maxIndexRetryInterval    Minimum interval between retries for ElasticSearch indexing operations i.e. *
     *                                 operations that involve data
     * @param user                     Username for connecting to Elasticsearch
     * @param password                 Password for connecting to Elasticsearch
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     * @param makeOpenSearchCompatible Whether to try to make the client OpenSearch "compatible", in so far as is
     *                                 possible
     */
    ElasticSearchIndexer(String elasticHost, int elasticPort, String index, boolean upsert,
                         Function<T, Script> updateScriptBuilder, Function<T, Script> deleteScriptBuilder,
                         int maxRetries, Duration minRetryInterval, Duration maxRetryInterval,
                         Duration minIndexRetryInterval, Duration maxIndexRetryInterval, String user, String password,
                         String elasticTlsCaCert, boolean makeOpenSearchCompatible) {
        super(elasticHost, elasticPort, user, password, elasticTlsCaCert, makeOpenSearchCompatible);

        if (StringUtils.isBlank(index)) {
            throw new IllegalArgumentException("Target ElasticSearch Index cannot be null/empty");
        }
        this.index = index;
        this.upsert = upsert;
        this.itemToUpdateScript = updateScriptBuilder;
        this.itemToDeleteScript = deleteScriptBuilder;

        // Prepare the various retry configurations using suitable defaults if not explicitly configured
        int actualMaxRetries = maxRetries >= 1 ? maxRetries : DEFAULT_MAX_RETRIES;
        Duration actualMinRetryInterval = minRetryInterval != null ? minRetryInterval : DEFAULT_MIN_RETRY_INTERVAL;
        Duration actualMaxRetryInterval = maxRetryInterval != null ? maxRetryInterval : DEFAULT_MAX_RETRY_INTERVAL;
        Duration actualMinIndexRetryInterval =
                minIndexRetryInterval != null ? minIndexRetryInterval : DEFAULT_MIN_INDEX_RETRY_INTERVAL;
        Duration actualMaxIndexRetryInterval =
                maxIndexRetryInterval != null ? maxIndexRetryInterval : DEFAULT_MAX_INDEX_RETRY_INTERVAL;

        this.flushRetryConfig = SearchUtils.<FlushResponse>prepareRetry(actualMaxRetries, actualMinRetryInterval,
                                                                        actualMaxRetryInterval)
                                           .retryExceptions(IOException.class, ElasticsearchException.class)
                                           .retryOnResult(ElasticSearchIndexer.createRetryTest())
                                           .build();
        this.forceMergeRetryConfig =
                SearchUtils.<ForcemergeResponse>prepareRetry(actualMaxRetries, actualMinRetryInterval,
                                                             actualMaxRetryInterval)
                           .retryExceptions(IOException.class, ElasticsearchException.class)
                           .retryOnResult(ElasticSearchIndexer.createRetryTest())
                           .build();
        this.indexRetryConfig =
                SearchUtils.prepareRetry(actualMaxRetries, actualMinIndexRetryInterval, actualMaxIndexRetryInterval)
                           .retryExceptions(IOException.class, ElasticsearchException.class, SearchException.class)
                           .failAfterMaxAttempts(true)
                           .build();
    }

    private static <T extends ShardsOperationResponseBase> Predicate<T> createRetryTest() {
        return r -> !r.shards().failures().isEmpty();
    }

    @Override
    public Boolean isIndexed(String id) {
        Objects.requireNonNull(id, "id cannot be null");
        try {
            GetResponse<Document> response = this.client.get(g -> g.index(this.index).id(id), Document.class);
            return response.found();
        } catch (IOException e) {
            LOGGER.warn("Error determining if a document with ID {} is in index {}: {}", id, this.index,
                        e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean isIndexed(Function<T, String> idProvider, T item) {
        return isIndexed(idProvider.apply(item));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void index(Function<T, String> idProvider, T item) {
        String id = idProvider.apply(item);
        if (StringUtils.isBlank(id)) {
            throw new SearchException("Calculated ID for document is null/empty which is not permitted");
        }
        SearchUtils.retryIndexOperation(this, this.index, String.format("Index document %s into", id),
                                        this.indexRetryConfig, () -> {
                    try {
                        if (this.upsert) {
                            UpdateResponse<T> response;
                            if (this.itemToUpdateScript != null) {
                                response = client.update(u -> u.index(this.index)
                                                                     .id(id)
                                                                     .upsert(item)
                                                                     .script(this.itemToUpdateScript.apply(item)),
                                                               (Class<T>) item.getClass());
                                checkResponse(id, response, NO_ERRORS_ACCEPTED,
                                              "Indexed document (via scripted update)",
                                              "Failed to index document (via scripted update)");
                            } else {
                                response =
                                        client.update(u -> u.index(this.index)
                                                                  .id(id)
                                                                  .doc(item)
                                                                  .docAsUpsert(true),
                                                            (Class<T>) item.getClass());
                                checkResponse(id, response, NO_ERRORS_ACCEPTED,
                                              "Indexed document (via upsert)",
                                              "Failed to index document (via upsert)");
                            }
                        } else {
                            IndexResponse response = client.index(i -> i.index(this.index)
                                                                        .id(id)
                                                                        .document(item));
                            checkResponse(id, response, NO_ERRORS_ACCEPTED, "Indexed document",
                                          "Failed to index document");
                        }
                    } catch (ElasticsearchException e) {
                        throw fromElasticException(e, "index document with ID " + id);
                    } catch (IOException e) {
                        throw new SearchException(
                                String.format("Failed to index document with ID %s into ElasticSearch index %s: %s",
                                              id,
                                              this.index, e.getMessage()), e);
                    }
                });
    }

    @Override
    public SearchIndexBulkResults<T> bulkIndex(Function<T, String> idProvider, Collection<T> items) {
        // Firstly build the bulk operations we want to send to ElasticSearch
        String actionModifier =
                this.upsert ? (this.itemToUpdateScript != null ? " (via scripted update)" : " (via upsert)") : "";
        final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> allItemOps =
                prepareBulkOperations(idProvider, items, (document, id) -> {
                    if (this.upsert) {
                        if (this.itemToUpdateScript != null) {
                            return new BulkOperation(UpdateOperation.of(b -> b.index(this.index)
                                                                              .id(id)
                                                                              .action(UpdateAction.of(
                                                                                      u -> u.upsert(document)
                                                                                            .script(this.itemToUpdateScript.apply(
                                                                                                    document))))));
                        } else {
                            return new BulkOperation(UpdateOperation.of(b -> b.index(this.index)
                                                                              .id(id)
                                                                              .action(UpdateAction.of(
                                                                                      u -> u.doc(document)
                                                                                            .docAsUpsert(
                                                                                                    true)))));
                        }
                    } else {
                        return new BulkOperation(IndexOperation.of(b -> b.index(this.index).id(id).document(document)));
                    }
                });

        // Then attempt the bulk indexing with retries enabled
        SearchUtils.retryIndexOperation(this, this.index, "Bulk index documents" + actionModifier + " into",
                                        this.indexRetryConfig, () -> {
                    final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> retryOps =
                            determineRetryOps(allItemOps);
                    try {
                        BulkResponse response = client.bulk(b -> b.index(this.index)
                                                                  .operations(retryOps.stream()
                                                                                      .map(Pair::getRight)
                                                                                      .map(Pair::getLeft)
                                                                                      .toList()));

                        // Figure out how many of the documents were successfully indexed and log any errors
                        checkBulkResponse(retryOps, response, NO_BULK_ERRORS_ACCEPTED,
                                          "Bulk Indexed" + actionModifier,
                                          "Failed to index" + actionModifier);
                    } catch (IOException e) {
                        throw new SearchException(
                                String.format("Failed to index %,d documents into ElasticSearch index %s: %s",
                                              retryOps.size(), this.index, e
                                                      .getMessage()),
                                e);
                    }
                });

        // Update the index results for any operations that were unable to be tried
        allItemOps.stream()
                  .filter(op -> isNull(op.getRight().getRight()))
                  .forEach(op -> op.getRight()
                               .setRight(new SearchIndexBulkResult<>(false, op.getLeft(),
                                                          "Index " + actionModifier + " was not attempted as preceding item(s) failed to index")));
        return new SearchIndexBulkResults<>(
                allItemOps.stream().map(Pair::getRight).map(Pair::getRight).collect(Collectors.toList()));
    }

    private static <T> List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> determineRetryOps(
            List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> allItemOps) {
        return allItemOps.subList(determineRetryIndexFrom(allItemOps), allItemOps.size());
    }

    private static <T> int determineRetryIndexFrom(
            List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> allItemOps) {
        return IntStream.range(0, allItemOps.size())
                        .filter(i -> isNull(allItemOps.get(i).getRight().getRight()) || !allItemOps.get(i)
                                                                                                   .getRight()
                                                                                                   .getRight()
                                                                                                   .isSuccessful())
                        .findFirst().orElse(0);
    }

    private void checkBulkResponse(List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> ops,
                                   BulkResponse response,
                                   Function<BulkResponseItem, Boolean> isAcceptableError, String successAction,
                                   String failureAction) {
        int successful = 0;
        int failures = 0;
        int responseItemIdx = 0;
        for (BulkResponseItem responseItem : response.items()) {
            final Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>> op = ops.get(responseItemIdx++);

            if (responseItem.error() != null) {
                if (isAcceptableError.apply(responseItem)) {
                    LOGGER.warn(
                            "{} document {} but this is an acceptable error for this operation.  ElasticSearch reported error: {}",
                            failureAction, responseItem.id(), findErrorReason(responseItem));
                    successful++;
                    op.getRight().setRight(new SearchIndexBulkResult<>(true, op.getLeft()));
                } else {
                    LOGGER.warn("{} document {}: {}", failureAction, responseItem.id(), findErrorReason(responseItem));
                    op.getRight()
                      .setRight(new SearchIndexBulkResult<>(false, op.getLeft(),
                                                            String.format("%s document %s: %s", failureAction,
                                                                          responseItem.id(),
                                                                          findErrorReason(responseItem))));
                    failures++;
                }
            } else if (responseItem.shards().successful().longValue() > 0) {
                successful++;
                op.getRight().setRight(new SearchIndexBulkResult<>(true, op.getLeft()));
            } else {
                LOGGER.warn("{} document {} and no error information was provided by ElasticSearch", failureAction,
                            responseItem.id());
                op.getRight()
                  .setRight(new SearchIndexBulkResult<>(false, op.getLeft(), String.format(
                          "%s document %s and no error information was provided by ElasticSearch", failureAction,
                          responseItem.id())));
                failures++;
            }
        }

        if (successful > 0) {
            // Bulk indexing succeeded, at least in part, depending on whether it's a complete/partial success
            // we may throw an error which leads to a retry
            if (failures == 0) {
                // Everything succeeded
                LOGGER.debug("{} {} documents into ElasticSearch index {}", successAction, successful, this.index);
            } else {
                // A mix of successes and failures
                LOGGER.warn("{} {} documents into ElasticSearch index {}, {} {} documents", successAction, successful,
                            this.index, failureAction, failures);
            }
        } else {
            // Everything failed
            LOGGER.error("{} {} documents into ElasticSearch index {}, please review logs for errors", failureAction,
                         ops.size(), this.index);
            throw new SearchException(
                    String.format("%s %,d documents into ElasticSearch index %s", failureAction, ops
                                          .size(),
                                  this.index));
        }
    }

    /**
     * Transforms a collection of items into a sequence of ElasticSearch Bulk Operations that can be submitted as a
     * single bulk request
     *
     * @param idProvider      Function that calculates the ID for an item
     * @param items           Items
     * @param itemToOperation Function that transforms an item and its ID into an ElasticSearch operation
     * @return Bulk operations
     */
    protected final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> prepareBulkOperations(
            Function<T, String> idProvider, Collection<T> items,
            BiFunction<T, String, BulkOperation> itemToOperation) {
        if (items.isEmpty()) {
            throw new SearchException("No bulk operations were generated");
        }

        return items.stream()
                    .map(i -> {
                        String id = idProvider.apply(i);
                        if (StringUtils.isBlank(id)) {
                            throw new SearchException(
                                    "Calculated ID for document is null/empty which is not permitted");
                        }
                        return Pair.of(i,
                                       MutablePair.of(itemToOperation.apply(i, id), (SearchIndexBulkResult<T>) null));
                    })
                    .collect(Collectors.toList());
    }

    @Override
    public void deleteDocument(String id) {
        if (StringUtils.isBlank(id)) {
            throw new SearchException("ID for document is null/empty which is not permitted");
        }

        SearchUtils.retryIndexOperation(this, this.index, "Delete Document from", this.indexRetryConfig, () -> {
            try {
                DeleteResponse response = client.delete(d -> d.index(this.index).id(id));
                checkResponse(id, response, DELETE_ON_NON_EXISTENT_DOCUMENT, "Deleted document",
                              "Failed to delete document");
            } catch (IOException e) {
                throw new SearchException(
                        String.format("Failed to delete document with ID %s from ElasticSearch index %s: %s",
                                      id,
                                      this.index, e.getMessage()), e);
            }
        });
    }

    @Override
    public void deleteContents(Function<T, String> idProvider, T item) {
        if (this.itemToDeleteScript == null) {
            throw new SearchException(
                    "This indexer implementation has not been configured to support deleting document contents.");
        }

        String id = idProvider.apply(item);
        if (StringUtils.isBlank(id)) {
            throw new SearchException("Calculated ID for document is null/empty which is not permitted");
        }

        SearchUtils.retryIndexOperation(this, this.index, "Delete Document Contents from", this.indexRetryConfig,
                                        () -> {
                                            try {
                                                @SuppressWarnings("unchecked") UpdateResponse<T> response =
                                                        client.update(UpdateRequest.of(u -> u.index(this.index)
                                                                                             .id(id)
                                                                                             .script(itemToDeleteScript.apply(
                                                                                                     item))),
                                                                      (Class<T>) item.getClass());

                                                checkResponse(id, response, DELETE_ON_NON_EXISTENT_DOCUMENT,
                                                              "Deleted contents", "Failed to delete contents");
                                            } catch (ElasticsearchException e) {
                                                if (e.status() == HttpStatus.SC_NOT_FOUND) {
                                                    LOGGER.warn(
                                                            "Failed to delete contents for document {} but this is an acceptable error for this operation.  ElasticSearch reported error: {}",
                                                            id, e.response().error().reason());
                                                } else {
                                                    throw failedToDeleteContents(id, e);
                                                }
                                            } catch (IOException e) {
                                                throw failedToDeleteContents(id, e);
                                            }
                                        });
    }

    private SearchException failedToDeleteContents(String id, Throwable e) {
        return new SearchException(
                String.format("Failed to delete contents of document with ID %s from ElasticSearch index %s: %s",
                              id,
                              this.index, e.getMessage()), e);
    }

    private void checkResponse(String id, WriteResponseBase response,
                               Function<WriteResponseBase, Boolean> isAcceptableError, String successAction,
                               String failureAction) {
        // For update based operations ElasticSearch may detect that no update actually is required and thus return a
        // result of NoOp which we treat as success because it means our update was either irrelevant or already applied
        // This might be the case because we were forced to replay and reprocess some events.
        if (response.shards().successful().longValue() > 0 || response.result() == Result.NoOp) {
            LOGGER.info("{} for document ID {} in ElasticSearchIndex {}", successAction, id, this.index);
        } else {
            if (isAcceptableError.apply(response)) {
                LOGGER.warn(
                        "{} for document {} but this is an acceptable error for this operation.  ElasticSearch reported error: {}",
                        failureAction, id, findErrorReason(response));
            } else {
                LOGGER.warn("{} for document ID {}: {}", failureAction, id, findErrorReason(response));
                throw new SearchException(
                        String.format("%s for document ID %s in ElasticSearch index %s",
                                      failureAction, id,
                                      this.index));
            }
        }
    }

    @Override
    public SearchIndexBulkResults<T> bulkDeleteDocuments(Function<T, String> idProvider, Collection<T> items) {
        // Firstly build the bulk operations we want to send to ElasticSearch
        final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> allItemOps =
                prepareBulkOperations(idProvider, items, (item, id) -> new BulkOperation(
                        DeleteOperation.of(d -> d.index(this.index)
                                                 .id(id))));

        SearchUtils.retryIndexOperation(this, this.index, "Bulk Delete Documents from", this.indexRetryConfig, () -> {
            final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> retryOps =
                    determineRetryOps(allItemOps);
            try {
                BulkResponse response = client.bulk(b -> b.index(this.index)
                                                          .operations(retryOps.stream()
                                                                              .map(Pair::getRight)
                                                                              .map(Pair::getLeft)
                                                                              .toList()));
                checkBulkResponse(retryOps, response, i -> i.status() == HttpStatus.SC_NOT_FOUND, "Bulk Deleted",
                                  "Failed to bulk delete");
            } catch (IOException e) {
                throw new SearchException(
                        String.format("Failed to delete %,d documents from ElasticSearch index %s: %s",
                                      retryOps.size(),
                                      this.index, e.getMessage()), e);
            }
        });

        // Update the index results for any operations that were unable to be tried
        allItemOps.stream()
                  .filter(op -> isNull(op.getRight().getRight()))
                  .forEach(op -> op.getRight()
                                   .setRight(new SearchIndexBulkResult<>(false, op.getLeft(),
                                                                         "Deletion from index was not attempted as preceding item(s) failed to index")));
        return new SearchIndexBulkResults<>(
                allItemOps.stream().map(Pair::getRight).map(Pair::getRight).collect(Collectors.toList()));
    }

    @Override
    public SearchIndexBulkResults<T> bulkDeleteContents(Function<T, String> idProvider, Collection<T> items) {
        if (this.itemToDeleteScript == null) {
            throw new SearchException(
                    "This indexer implementation has not been configured to support deleting document contents.");
        }

        // Firstly build the bulk operations we want to send to ElasticSearch
        final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>> allItemOps =
                prepareBulkOperations(idProvider, items, (item, id) -> new BulkOperation(
                        UpdateOperation.of(u -> u.index(this.index)
                                                 .id(id)
                                                 .action(UpdateAction.of(
                                                         a -> a.script(itemToDeleteScript.apply(item)))))));


        SearchUtils.retryIndexOperation(this, this.index, "Bulk Delete Document Contents from", this.indexRetryConfig,
                                        () -> {
                                            final List<Pair<T, MutablePair<BulkOperation, SearchIndexBulkResult<T>>>>
                                                    retryOps = determineRetryOps(allItemOps);
                                            try {
                                                BulkResponse response =
                                                        client.bulk(b -> b.index(this.index)
                                                                          .operations(retryOps.stream()
                                                                                              .map(Pair::getRight)
                                                                                              .map(Pair::getLeft)
                                                                                              .toList()));
                                                checkBulkResponse(retryOps, response,
                                                                  i -> i.status() == HttpStatus.SC_NOT_FOUND,
                                                                  "Bulk deleted content from",
                                                                  "Failed to bulk delete content from");
                                            } catch (IOException e) {
                                                throw new SearchException(String.format(
                                                        "Failed to delete contents from %,d documents from ElasticSearch index %s: %s",
                                                        retryOps.size(), this.index, e
                                                                .getMessage()),
                                                                          e);
                                            }
                                        });

        // Update the index results for any operations that were unable to be tried
        allItemOps.stream()
                  .filter(op -> isNull(op.getRight().getRight()))
                  .forEach(op -> op.getRight()
                                   .setRight(new SearchIndexBulkResult<>(false, op.getLeft(),
                                                                         "Deletion of contents from index was not attempted as preceding item(s) failed to index")));
        return new SearchIndexBulkResults<>(
                allItemOps.stream().map(Pair::getRight).map(Pair::getRight).collect(Collectors.toList()));
    }

    @Override
    public void flush(boolean finished) {
        FlushResponse flushResponse = SearchUtils.retryIndexOperation(this, this.index, "flush", this.flushRetryConfig,
                                                                      () -> this.client.indices()
                                                                                       .flush(f -> f.index(
                                                                                               this.index)));
        if (!flushResponse.shards().failures().isEmpty()) {
            throw new SearchException(
                    String.format("ElasticSearch reported shard failures while attempting to flush index %s",
                                  this.index));
        }

        if (finished) {
            ForcemergeResponse mergeResponse =
                    SearchUtils.retryIndexOperation(this, this.index, "force merge", this.forceMergeRetryConfig,
                                                    () -> this.client.indices()
                                                                     .forcemerge(f -> f.index(this.index)
                                                                                       .maxNumSegments(1L)));
            if (!mergeResponse.shards().failures().isEmpty()) {
                throw new SearchException(
                        String.format("ElasticSearch reported shard failures while attempting to force merge index %s",
                                      this.index));
            }
        }
    }

    /**
     * Creates a new builder for building an ElasticSearch indexer
     *
     * @param <T> Item type
     * @return Builder
     */
    public static <T> ElasticSearchIndexerBuilder<T, ?, ?> create() {
        return new ElasticSearchIndexerBuilderImpl<>();
    }

    /**
     * A builder for {@link ElasticSearchIndexer} instances
     *
     * @param <T> Item type
     * @param <C> Indexer
     * @param <B> Builder
     */
    public abstract static class ElasticSearchIndexerBuilder<T, C extends ElasticSearchIndexer<T>, B extends ElasticSearchIndexerBuilder<T, C, B>> extends AbstractElasticClientBuilder<C, B> {

        /** The search index to be used. */
        protected String index;
        /** Whether upserts should be used in indexing operations. */
        protected boolean upsert;
        /** The maximum number of retries of indexing operations.*/
        protected int maxRetries = DEFAULT_MAX_RETRIES;
        /** The minimum retry interval between failing indexing operations. */
        protected Duration minRetryInterval;
        /** The maximum retry interval between failing operations. */
        protected Duration maxRetryInterval;
        /** The minimum retry interval between failing operations. */

        protected Duration minIndexRetryInterval;
        /** The maximum retry interval between failing indexing operations. */
        protected Duration maxIndexRetryInterval;
        /** The delete script strategy builder. */
        protected Function<T, Script> deleteScriptBuilder;
        /** The update script strategy builder. */
        protected Function<T, Script> updateScriptBuilder;
        /** The search user to be used. */
        protected String user;
        /** The search password to be used. */
        protected String password;
        /** Whether the indexer should be OpenSearch "compatible". */
        protected boolean makeOpenSearchCompatible;

        /**
         * Sets the index upon which this indexer operates
         *
         * @param index Index
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> index(String index) {
            this.index = index;
            return this;
        }

        /**
         * Sets the indexing behaviour for documents
         *
         * @param upsert If true then index via upserts i.e. update existing document (if any), if false index via
         *               overwrites i.e. overwrite existing document (if any)
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> onIndexBehaviour(boolean upsert) {
            return upsert ? this.usingUpserts() : this.usingOverwrites();
        }

        /**
         * Sets that the indexer should not use upserts when indexing a document that has previously been indexed i.e.
         * any index operation overwrites the previous state of that document
         *
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> usingOverwrites() {
            this.upsert = false;
            return this;
        }

        /**
         * Sets that the indexer should use upserts when indexing a document that has previously been indexed i.e. any
         * index operation updates the previous state of that document (if any) rather than overwriting it.
         * <p>
         * When enabling this you should also consider configuring an update script generator via the
         * {@link #updatingContentsWith(Function)} method as that will provide more reliable application of updates.
         * </p>
         *
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> usingUpserts() {
            this.upsert = true;
            return this;
        }

        /**
         * Sets the maximum retries for ElasticSearch operations
         *
         * @param max Maximum retries
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> maxRetries(int max) {
            this.maxRetries = max;
            return this;
        }

        /**
         * Sets the minimum retry interval for ElasticSearch operations
         *
         * @param general      Minimum interval for general operations
         * @param dataIndexing Minimum interval for data indexing operations
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> withMinimumRetryInterval(Duration general, Duration dataIndexing) {
            this.minRetryInterval = general;
            this.minIndexRetryInterval = dataIndexing;
            return this;
        }

        /**
         * Sets the maximum retry interval for ElasticSearch operations
         *
         * @param general      Maximum interval for general operations
         * @param dataIndexing Maximum interval for data indexing operations
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> withMaximumRetryInterval(Duration general, Duration dataIndexing) {
            this.maxRetryInterval = general;
            this.maxIndexRetryInterval = dataIndexing;
            return this;
        }

        /**
         * Sets the deletion script builder that can be used to generate ElasticSearch scripts that delete contents from
         * an existing document without deleting the entire document.
         * <p>
         * If this is not specified then calling either the {@link SearchIndexer#deleteContents(Function, Object)} or
         * {@link SearchIndexer#bulkDeleteContents(Function, Collection)} function will result in a
         * {@link SearchException} as ElasticSearch only supports content deletion via scripting.
         * </p>
         * <p>
         * For example if you are indexing {@link Document} instances then you could provide the
         * {@link ContentDeletion#forDocument(Document)} function.
         * </p>
         *
         * @param deleteScriptBuilder Delete script builder function
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> deletingContentsWith(Function<T, Script> deleteScriptBuilder) {
            this.deleteScriptBuilder = deleteScriptBuilder;
            return this;
        }

        /**
         * Sets the update script builder that can be used to generate ElasticSearch scripts that update contents within
         * an existing document.
         * <p>
         * If this is not provided then an <em>Document as Upsert</em> operation is used to apply updates, however this
         * operation has known deficiencies when handling list fields within documents. Therefore, if this function is
         * provided then the generated {@link Script}'s are always used for a scripted update in preference of the
         * Upsert operation.
         * </p>
         * <p>
         * Note that you <strong>MUST</strong> also call {@link #usingUpserts()} on your builder instance to enable
         * updates otherwise all indexing is done via overwrites i.e. as if {@link #usingOverwrites()}, which is the
         * default, was set.
         * </p>
         * <p>
         * For example if you are indexing {@link Document} instances then you could provide the
         * {@link ContentUpdate#forDocument(Document)} function.
         * </p>
         *
         * @param updateScriptBuilder Update script builder function
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> updatingContentsWith(Function<T, Script> updateScriptBuilder) {
            this.updateScriptBuilder = updateScriptBuilder;
            return this;
        }


        /**
         * Sets the authentication for connecting to Elasticsearch
         *
         * @param user     Elastic username
         * @param password Elastic password
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> withCredentials(String user, String password, String elasticTlsCaCert) {
            this.username = user;
            this.password = password;
            this.elasticTlsCaCert = elasticTlsCaCert;
            return this;
        }

        /**
         * Tries to make the indexer "compatible", in so far as is possible, with OpenSearch servers
         *
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> withOpenSearchCompatibility() {
            return withOpenSearchCompatibility(true);
        }

        /**
         * Configures whether to make the indexer "compatible", in so far as is possible, with OpenSearch servers
         *
         * @param makeCompatible Whether to make the indexer "compatible" with OpenSearch
         * @return Builder
         */
        public ElasticSearchIndexerBuilder<T, C, B> withOpenSearchCompatibility(boolean makeCompatible) {
            this.makeOpenSearchCompatible = makeCompatible;
            return this;
        }

        @Override
        protected abstract B self();

        @Override
        public abstract C build();
    }

    private static final class ElasticSearchIndexerBuilderImpl<T>
            extends
            ElasticSearchIndexerBuilder<T, ElasticSearchIndexer<T>, ElasticSearchIndexerBuilderImpl<T>> {
        private ElasticSearchIndexerBuilderImpl() {
        }

        @Override
        protected ElasticSearchIndexerBuilderImpl<T> self() {
            return this;
        }

        @Override
        public ElasticSearchIndexer<T> build() {
            return new ElasticSearchIndexer<>(this.elasticHost, this.elasticPort, this.index, this.upsert,
                                              this.updateScriptBuilder,
                                              this.deleteScriptBuilder, this.maxRetries, this.minRetryInterval,
                                              this.maxRetryInterval, this.minIndexRetryInterval,
                                              this.maxIndexRetryInterval,
                                              this.username, this.password, this.elasticTlsCaCert,
                                              this.makeOpenSearchCompatible);
        }
    }

}
