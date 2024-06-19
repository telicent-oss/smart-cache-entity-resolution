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
package io.telicent.smart.cache.search.sinks;

import io.telicent.smart.cache.observability.events.ComponentEventSource;
import io.telicent.smart.cache.observability.events.EventSourceSupport;
import io.telicent.smart.cache.observability.events.MetricEvent;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.projectors.sinks.builder.SinkBuilder;
import io.telicent.smart.cache.projectors.utils.ThroughputTracker;
import io.telicent.smart.cache.search.SearchIndexer;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.Header;
import io.telicent.smart.cache.sources.TelicentHeaders;
import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.telicent.smart.cache.observability.events.CounterEvent.counterEvent;
import static io.telicent.smart.cache.observability.events.DurationEvent.durationEvent;
import static io.telicent.smart.cache.observability.events.EventUtil.emit;
import static java.util.Objects.requireNonNull;

/**
 * A sink that sends items for search indexing via a {@link SearchIndexer}.
 * <p>
 * This sink does not batch the items in any way, use {@link BulkSearchIndexerSink} if you want to perform bulk indexing
 * of items.
 * </p>
 *
 * @param <TKey>   Event key type
 * @param <TValue> Event value type
 */
public class SearchIndexerSink<TKey, TValue> implements Sink<Event<TKey, TValue>>, ComponentEventSource<MetricEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexerSink.class);

    /**
     * The search indexer in-use
     */
    protected final SearchIndexer<TValue> indexer;
    /**
     * The function that calculates the IDs for the items
     */
    protected final Function<TValue, String> idProvider;
    /**
     * The function that calculates whether an event represents a deletion
     */
    protected final Function<Event<TKey, TValue>, Boolean> isDeletion;
    /**
     * The action to perform for deletions
     */
    protected final IndexDeletionAction deleteAction;
    /**
     * The throughput tracker used to report progress on indexing
     */
    protected final ThroughputTracker tracker;

    /**
     * The dead letter sink where events which cannot be indexed will be written
     */
    protected final Sink<Event<TKey, TValue>> deadLetterSink;

    /**
     * An event support delegate used for configuration of event listeners.
     */
    @Delegate(types = ComponentEventSource.class)
    protected final EventSourceSupport<MetricEvent> eventSupport = new EventSourceSupport<>();

    /**
     * Creates a new search indexer sink
     *
     * @param indexer         Search Indexer
     * @param idProvider      ID Provider function to generate Document IDs for items
     * @param isDeletion      Deletion detection function that indicates when an event represents a deletion
     * @param reportBatchSize Controls how often progress is reported by the sink i.e. how often it logs
     * @param deadLetterSink  A dead letter sink where events which cannot be indexed will be written, which may be
     *                        null
     */
    SearchIndexerSink(SearchIndexer<TValue> indexer, Function<TValue, String> idProvider,
                      Function<Event<TKey, TValue>, Boolean> isDeletion, IndexDeletionAction deleteAction,
                      long reportBatchSize,
                      final Sink<Event<TKey, TValue>> deadLetterSink) {
        requireNonNull(indexer, "Search Indexer cannot be null");
        requireNonNull(idProvider, "ID Provider function cannot be null");
        requireNonNull(isDeletion, "Deletion detection function cannot be null");

        this.indexer = indexer;
        this.idProvider = idProvider;
        this.isDeletion = isDeletion;
        this.deleteAction = deleteAction;
        this.tracker = ThroughputTracker.create()
                                        .logger(LOGGER)
                                        .reportBatchSize(reportBatchSize)
                                        .inSeconds()
                                        .action("Indexed")
                                        .itemsName("Documents")
                                        .metricsLabel("indexed_documents")
                                        .build();
        this.deadLetterSink = deadLetterSink;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void send(Event<TKey, TValue> event) {
        this.tracker.itemReceived();

        emit(eventSupport, counterEvent("search.indexer.sink.received"));
        long startTime = System.currentTimeMillis();
        try {
            if (this.isDeletion.apply(event)) {
                switch (this.deleteAction) {
                    case DOCUMENT:
                        this.indexer.deleteDocument(this.idProvider.apply(event.value()));
                        break;
                    case CONTENTS:
                    default:
                        this.indexer.deleteContents(this.idProvider, event.value());
                        break;
                }
            } else {
                this.indexer.index(this.idProvider, event.value());
            }
            this.tracker.itemProcessed();
            emit(eventSupport, counterEvent("search.indexer.sink.success"), durationEvent("search.indexer.sink.success.duration", startTime, System.currentTimeMillis()));

            if (event.source() != null) {
                // NB - Per Javadoc this part of the Event Source API intentionally unchecked
                event.source().processed(List.of(event));
            }
        } catch (RuntimeException ex) {
            emit(eventSupport, counterEvent("search.indexer.sink.failure"), durationEvent("search.indexer.sink.failure.duration", startTime, System.currentTimeMillis()));
            if (deadLetterSink == null) {
                throw ex; // We're not configured to dead letter errors

            }
            LOGGER.warn("Unable to send event [" + event + "] to indexer, and will be sent to dead letter sink: ", ex);
            deadLetterSink.send(decorateDeadLetterEventWithMetadata(event, ex));
        }
    }

    private Event<TKey, TValue> decorateDeadLetterEventWithMetadata(final Event<TKey, TValue> event,
                                                                    final Throwable cause) {
        return event.addHeaders(Stream.of(new Header(TelicentHeaders.DEAD_LETTER_REASON, cause.getMessage())));
    }

    @Override
    public void close() {
        this.tracker.reportThroughput();
        this.tracker.reset();

        this.indexer.flush(true);
    }

    /**
     * Creates a builder for an indexer sink
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     * @return Builder
     */
    public static <TKey, TValue> Builder<TKey, TValue> create() {
        return new Builder<>();
    }

    /**
     * A builder for bulk search indexer sinks
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     */
    public static final class Builder<TKey, TValue>
            implements SinkBuilder<Event<TKey, TValue>, SearchIndexerSink<TKey, TValue>> {

        private SearchIndexer<TValue> indexer;
        private Function<TValue, String> idProvider;
        private Function<Event<TKey, TValue>, Boolean> isDeletion = e -> false;
        private IndexDeletionAction onDelete = IndexDeletionAction.CONTENTS;
        private long reportBatchSize = 1000L;
        /**
         * The Dead Letter Sink where events which cannot be indexed will be written
         */
        private Sink<Event<TKey, TValue>> deadLetterSink;

        /**
         * Sets the search indexer for the sink
         *
         * @param indexer Search indexer
         * @return Builder
         */
        public Builder<TKey, TValue> indexer(SearchIndexer<TValue> indexer) {
            this.indexer = indexer;
            return this;
        }

        /**
         * Sets the ID provider for the sink
         *
         * @param idProvider ID provider function
         * @return Builder
         */
        public Builder<TKey, TValue> idProvider(Function<TValue, String> idProvider) {
            this.idProvider = idProvider;
            return this;
        }

        /**
         * Sets the function that detects when an item passed to the sink represents a deletion from the index.  If not
         * specified then all items are considered to additions to the index.
         *
         * @param isDeletion Function that detects items that represent deletions
         * @return Builder
         */
        public Builder<TKey, TValue> isDeletionWhen(Function<Event<TKey, TValue>, Boolean> isDeletion) {
            this.isDeletion = isDeletion;
            return this;
        }

        /**
         * Sets that the built sink will not process any deletes i.e. every item passed is assumed to be an item to be
         * added to the indexed, not deleted
         * <p>
         * If you want some items to be treated as deletions then you must call {@link #isDeletionWhen(Function)} and
         * supply a suitable function.
         * </p>
         *
         * @return Builder
         */
        public Builder<TKey, TValue> noDeletes() {
            this.isDeletion = e -> false;
            return this;
        }

        /**
         * Sets how the built sink will process deletions i.e. what action will it take upon encountering an item that
         * represents a deletion.
         *
         * @param action Deletion Action
         * @return Builder
         */
        public Builder<TKey, TValue> onDeletion(IndexDeletionAction action) {
            this.onDelete = action;
            return this;
        }

        /**
         * Sets the reporting batch size for the sink
         *
         * @param batchSize Reporting batch size
         * @return Builder
         */
        public Builder<TKey, TValue> reportBatchSize(long batchSize) {
            this.reportBatchSize = batchSize;
            return this;
        }

        /**
         * Sets the dead letter sink where events which cannot be indexed will be written
         *
         * @param deadLetterSink a dead letter sink where events which cannot be indexed will be written, which may be
         *                       null
         * @return Builder the builder for chaining
         */
        public Builder<TKey, TValue> deadLetterSink(final Sink<?> deadLetterSink) {
            this.deadLetterSink = (Sink<Event<TKey, TValue>>) deadLetterSink;
            return this;
        }

        @Override
        public SearchIndexerSink<TKey, TValue> build() {
            return new SearchIndexerSink<>(this.indexer, this.idProvider, this.isDeletion, this.onDelete,
                                           this.reportBatchSize, this.deadLetterSink);
        }
    }
}
