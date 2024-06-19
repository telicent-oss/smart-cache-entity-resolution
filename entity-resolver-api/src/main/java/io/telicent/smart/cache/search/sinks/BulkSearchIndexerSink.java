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

import io.telicent.smart.cache.observability.events.EventListener;
import io.telicent.smart.cache.observability.events.MetricEvent;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.projectors.sinks.builder.SinkBuilder;
import io.telicent.smart.cache.search.SearchIndexer;
import io.telicent.smart.cache.search.model.SearchIndexBulkResult;
import io.telicent.smart.cache.search.model.SearchIndexBulkResults;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.EventSource;
import io.telicent.smart.cache.sources.Header;
import io.telicent.smart.cache.sources.TelicentHeaders;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.telicent.smart.cache.observability.events.CounterEvent.counterEvent;
import static io.telicent.smart.cache.observability.events.DurationEvent.durationEvent;
import static io.telicent.smart.cache.observability.events.EventUtil.emit;
import static java.util.Arrays.stream;

/**
 * A sink that sends items for search indexing in bulk using a {@link SearchIndexer}
 *
 * @param <TKey>   Event key type
 * @param <TValue> Event value type
 */
public class BulkSearchIndexerSink<TKey, TValue> extends SearchIndexerSink<TKey, TValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkSearchIndexerSink.class);

    /**
     * Minimum permitted idle time, currently <strong>1 second</strong>
     */
    public static final Duration MINIMUM_IDLE_TIME = Duration.ofSeconds(1);
    /**
     * Default idle time, currently <strong>1 minute</strong>
     */
    public static final Duration DEFAULT_IDLE_TIME = Duration.ofMinutes(1);

    private final ExecutorService executor;
    private final IndexingIdleTrigger<TKey, TValue> trigger;

    private final List<Event<TKey, TValue>> items = new ArrayList<>();
    private final int batchSize;
    private final int flushPerBatches;
    private int batches;
    private int idleTriggers;
    private long lastIndexedAt = System.currentTimeMillis();
    private boolean currentBatchIsForDeletion;

    /**
     * Creates a new bulk search indexing sink
     *
     * @param indexer         Search Indexer
     * @param idProvider      ID Provider function that generates Document IDs for items
     * @param isDeletion      Function that detects when an item represents a deletion
     * @param deletionAction  Action to perform when encountering an item that represents a deletion
     * @param indexBatchSize  Batch size for bulk indexing i.e. how many items to collect before submitting them for
     *                        bulk indexing.  This <strong>MUST</strong> be less than the {@code reportBatchSize}.
     * @param flushPerBatches Controls how often a {@link SearchIndexer#flush(boolean)} operation will be called in
     *                        terms of number of batches, so a value of {@code 10} means that after 10 batches have been
     *                        indexed a {@link SearchIndexer#flush(boolean)} will be called.
     * @param reportBatchSize Controls how often progress is reported to the logs.  This is expressed as a batch size
     *                        i.e. after how many items are seen should progress be reported.  This
     *                        <strong>MUST</strong> be a multiple of the {@code indexBatchSize}.
     * @param maxIdleTime     Controls the maximum amount of time the sink is allowed to be idle, i.e. not perform an
     *                        actual index operation, before one will be triggered regardless of whether the
     *                        {@code indexBatchSize} has been reached.  This ensures that documents are regularly
     *                        indexed even when the pipeline producing the documents may be slow, or contains minimal
     *                        amounts of data.  If {@code null} then {@link #DEFAULT_IDLE_TIME} is used.
     * @param deadLetterSink  A dead letter sink where events which cannot be indexed will be written, which may be null
     */
    BulkSearchIndexerSink(SearchIndexer<TValue> indexer,
                          Function<TValue, String> idProvider, Function<Event<TKey, TValue>, Boolean> isDeletion,
                          IndexDeletionAction deletionAction,
                          int indexBatchSize, int flushPerBatches,
                          long reportBatchSize, Duration maxIdleTime, final Sink<Event<TKey, TValue>> deadLetterSink) {
        super(indexer, idProvider, isDeletion, deletionAction, reportBatchSize, deadLetterSink);

        if (indexBatchSize <= 0) {
            throw new IllegalArgumentException("indexBatchSize must be >= 1");
        }
        if (indexBatchSize > reportBatchSize) {
            throw new IllegalArgumentException("indexBatchSize must be < reportBatchSize");
        }
        if (reportBatchSize % indexBatchSize != 0) {
            throw new IllegalArgumentException("reportBatchSize must be a multiple of the indexBatchSize");
        }
        if (flushPerBatches <= 0) {
            throw new IllegalArgumentException("flushPerBatches must be >= 1");
        }

        this.batchSize = indexBatchSize;
        this.flushPerBatches = flushPerBatches;

        // Configure and set up the background thread that's going to trigger indexing based on an idle time i.e. forces
        // an indexing operation to happen every so often regardless of whether the configured batch size has been
        // reached
        // The only scenario in which we don't enable this feature is when the configured batch size is 1 i.e. no
        // batching, because in that case we're always indexing stuff as soon as we receive it
        if (maxIdleTime == null) {
            maxIdleTime = DEFAULT_IDLE_TIME;
        }
        boolean enableIdleTrigger = indexBatchSize > 1;
        if (MINIMUM_IDLE_TIME.compareTo(maxIdleTime) >= 1) {
            // We do enforce a minimum idle time to prevent the triggering from happening too frequently.  The minimum
            // is very low as this makes writing fast test cases for this code easier.  However, real users of this code
            // should really set higher values (on the order of minutes/hours) because in production usage there should
            // be enough data flowing through the system that the index batches fill up regularly.
            throw new IllegalArgumentException(
                    String.format("maxIdleTime must be a minimum of %,d seconds", MINIMUM_IDLE_TIME.getSeconds()));
        }
        this.executor = enableIdleTrigger ? Executors.newSingleThreadExecutor() : null;
        this.trigger = enableIdleTrigger ? new IndexingIdleTrigger<>(this, maxIdleTime) : null;
        if (enableIdleTrigger) {
            this.executor.submit(this.trigger);
        }
    }

    @Override
    public void send(Event<TKey, TValue> item) {
        if (this.batchSize == 1) {
            super.send(item);
        } else {
            this.tracker.itemReceived();

            synchronized (this.items) {
                boolean isDeletion = this.isDeletion.apply(item);
                if (isDeletion != this.currentBatchIsForDeletion) {
                    if (!this.items.isEmpty()) {
                        // Existing batch represents the opposite operation from the current item.  Need to cause
                        // that batch to be processed and then start a new batch setting the operation type
                        this.bulkIndex();
                    }
                    this.currentBatchIsForDeletion = isDeletion;
                }

                this.items.add(item);

                if (this.items.size() == this.batchSize) {
                    this.bulkIndex();
                }
            }
        }
    }

    /**
     * Performs the bulk indexing, called whenever the batch size is reached or the sink is closed
     */
    private void bulkIndex() {
        synchronized (this.items) {
            LOGGER.debug("Bulk indexing {} items", this.items.size());

            final List<TValue> indexItems = this.items.stream().map(Event::value).toList();
            SearchIndexBulkResults<TValue> indexBulkResults;
            long startTime = System.currentTimeMillis();
            if (this.currentBatchIsForDeletion) {
                switch (this.deleteAction) {
                    case DOCUMENT:
                        indexBulkResults = this.indexer.bulkDeleteDocuments(this.idProvider, indexItems);
                        emit(eventSupport, metricsFor(indexBulkResults, "delete", startTime, System.currentTimeMillis()));
                        break;
                    case CONTENTS:
                    default:
                        indexBulkResults = this.indexer.bulkDeleteContents(this.idProvider, indexItems);
                        emit(eventSupport, metricsFor(indexBulkResults, "deletecontents", startTime, System.currentTimeMillis()));
                        break;
                }
            } else {
                indexBulkResults = this.indexer.bulkIndex(this.idProvider, indexItems);
                emit(eventSupport, metricsFor(indexBulkResults, "index", startTime, System.currentTimeMillis()));
            }

            if (indexBulkResults.getSuccessfulCount() > 0) {
                this.tracker.itemsProcessed(indexBulkResults.getSuccessfulCount());
            }

            // NB - Per Javadoc this part of the Events API is unchecked by design
            Map<EventSource, List<Event<TKey, TValue>>>
                    eventsBySource = IntStream.range(0, this.items.size())
                                              .filter(indexBulkResults::isSuccessful)
                                              .mapToObj(this.items::get)
                                              .filter(e -> e.source() != null).collect(Collectors.groupingBy(
                            Event::source));
            if (!eventsBySource.isEmpty()) {
                eventsBySource.forEach((key, value) -> {
                    // From #202 if the EventSource.processed() method threw an error we'd never clear the items that
                    // we just successfully indexed so next time bulkIndex() was called we'd over-report our processed
                    // items leading to an error in the ThroughputTracker
                    try {
                        key.processed(value);
                    } catch (Throwable t) {
                        // Failure to report processed items isn't considered fatal.  The worse case for Kafka is that
                        // fail to commit our very latest offsets and thus are forced to re-process some events when we
                        // are restarted.
                        LOGGER.warn("Failed to report items processed to event source {} ({}): {}",
                                    key.getClass().getSimpleName(), key, t.getMessage());
                    }
                });
            }

            if (deadLetterSink != null) {
                final SearchIndexBulkResults<TValue> finalIndexBulkResults = indexBulkResults;
                IntStream.range(0, this.items.size())
                         .filter(indexBulkResults::isFailure)
                         .mapToObj(i -> new ImmutablePair<>(this.items.get(i), finalIndexBulkResults.getResults().get(i)))
                         .forEach(indexResultEventPair -> {
                             final Event<TKey, TValue> deadLetterEvent =
                                     decorateDeadLetterEventWithMetadata(indexResultEventPair.getLeft(),
                                                                         indexResultEventPair.getRight());
                             try {
                                deadLetterSink.send(deadLetterEvent);
                            } catch (Throwable t) {
                                LOGGER.error("Failed to send index error event [" + deadLetterEvent + "] to dead letter sink: ", t);
                            }
                         });
            }

            this.items.clear();

            this.batches++;
            if (this.batches % this.flushPerBatches == 0) {
                this.indexer.flush(false);
            }

            this.lastIndexedAt = System.currentTimeMillis();
        }
    }

    private List<MetricEvent> metricsFor(final SearchIndexBulkResults<TValue> indexBulkResults,
                                         final String operation,
                                         final long startTime, final long endTime) {
        List<MetricEvent> metricEvents = new ArrayList<>();
        if (indexBulkResults.getSuccessfulCount() > 0) {
            metricEvents.add(counterEvent("search.indexer.bulksink." + operation + ".success", indexBulkResults.getSuccessfulCount()));
        }

        if (indexBulkResults.getFailureCount() > 0) {
            metricEvents.add(counterEvent("search.indexer.bulksink." + operation + ".failure", indexBulkResults.getFailureCount()));
        }

        if (indexBulkResults.size() > 0) {
            metricEvents.add(durationEvent("search.indexer.bulksink." + operation + ".duration", startTime, endTime));
        }

        return metricEvents;
    }

    private Event<TKey, TValue> decorateDeadLetterEventWithMetadata(final Event<TKey, TValue> event, final SearchIndexBulkResult<?> errorResult) {
        return event.addHeaders(Stream.of(new Header(TelicentHeaders.DEAD_LETTER_REASON, errorResult.getReason())));
    }

    /**
     * Gets the number of batches indexed
     *
     * @return Batched indexed
     */
    public int batchesIndexed() {
        return this.batches;
    }

    /**
     * Gets the number of times the idle time trigger caused a bulk indexing
     *
     * @return Number of times triggered
     */
    public int idleTriggers() {
        return this.idleTriggers;
    }

    @Override
    public void close() {
        synchronized (this.items) {
            if (!this.items.isEmpty()) {
                this.bulkIndex();
            }
        }
        if (this.trigger != null) {
            this.trigger.cancel();
            this.executor.shutdownNow();
        }
        this.tracker.reportThroughput();
        this.tracker.reset();
        this.batches = 0;
        this.lastIndexedAt = System.currentTimeMillis();

        this.indexer.flush(true);
    }

    private static final class IndexingIdleTrigger<TKey, TValue> implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(IndexingIdleTrigger.class);
        public static final long IDLE_TRIGGER_CHECK_INTERVAL = 250L;

        private final BulkSearchIndexerSink<TKey, TValue> sink;
        private final Duration maxIdleTime;
        private volatile boolean cancelled;

        public IndexingIdleTrigger(BulkSearchIndexerSink<TKey, TValue> sink, Duration maxIdleTime) {
            this.sink = sink;
            this.maxIdleTime = maxIdleTime;
        }

        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public void run() {
            LOGGER.debug("Bulk Indexing configured with max idle time of {}", this.maxIdleTime);
            while (!this.cancelled) {
                // Check whether the idle time has been exceeded
                // Do this first because we want to avoid unnecessarily taking the synchronisation lock on the items
                // list too frequently and impacting the main processing thread that is pushing incoming items onto that
                // list for bulk indexing.
                long now = System.currentTimeMillis();
                Duration idle = Duration.ofMillis(now - this.sink.lastIndexedAt);
                if (idle.compareTo(this.maxIdleTime) >= 1) {
                    synchronized (this.sink.items) {
                        // Only need to trigger if there's actually something to be indexed
                        if (!this.sink.items.isEmpty()) {
                            LOGGER.debug("Triggering a bulk index as max idle time of {} was exceeded",
                                         this.maxIdleTime);
                            this.sink.idleTriggers++;
                            try {
                                this.sink.bulkIndex();
                            } catch (Throwable e) {
                                // From #202 previously failed bulk indexing on the idle trigger thread would kill the
                                // thread rendering the trigger useless after the first failure.  We now instead log
                                // the failure for visibility.
                                // If it is a genuine persistent failure the main thread will encounter it in due course
                                // and crash out in a more user-friendly fashion than an invisible background thread
                                // dying.
                                LOGGER.error("Background idle time bulk indexing failed:", e);
                            }
                        }
                    }
                }

                // Check again at regular intervals
                try {
                    Thread.sleep(IDLE_TRIGGER_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    // Ignored, likely means we're cancelled which we'll spot as soon as we go round our loop and exit
                }
            }
            LOGGER.debug("Bulk Indexing idle trigger cancelled");
        }
    }

    /**
     * Creates a new builder for a bulk search indexer sink
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     * @return Builder
     */
    public static <TKey, TValue> Builder<TKey, TValue> createBulk() {
        return new Builder<>();
    }

    /**
     * A builder for bulk search indexer sinks
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     */
    public static final class Builder<TKey, TValue>
            implements SinkBuilder<Event<TKey, TValue>, BulkSearchIndexerSink<TKey, TValue>> {

        private SearchIndexer<TValue> indexer;
        private Function<TValue, String> idProvider;
        private Function<Event<TKey, TValue>, Boolean> isDeletion = e -> false;
        private IndexDeletionAction onDelete = IndexDeletionAction.CONTENTS;
        private Duration maxIdleTime = DEFAULT_IDLE_TIME;
        private int indexBatchSize = 1000;
        private int flushPerBatches = 10;
        private long reportBatchSize = 1000L;
        /**
         * The Dead Letter Sink where events which cannot be indexed will be written
         */
        private Sink<Event<TKey, TValue>> deadLetterSink;
        /**
         * The event listeners to register with the sink.
         */
        private EventListener<?>[] eventListeners;

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
         * Sets the indexing batch size for the sink
         *
         * @param batchSize Indexing batch size
         * @return Builder
         */
        public Builder<TKey, TValue> indexBatchSize(int batchSize) {
            this.indexBatchSize = batchSize;
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
         * @param deadLetterSink a dead letter sink where events which cannot be indexed will be written, which may be null
         * @return Builder the builder for chaining
         */
        public Builder<TKey, TValue> deadLetterSink(final Sink<?> deadLetterSink) {
            this.deadLetterSink = (Sink<Event<TKey, TValue>>) deadLetterSink;
            return this;
        }

        /**
         * Sets the indexing and reporting batch sizes for the sink
         *
         * @param batchSize Batch size
         * @return Builder
         */
        public Builder<TKey, TValue> batchSize(int batchSize) {
            return this.indexBatchSize(batchSize).reportBatchSize(batchSize);
        }

        /**
         * Sets how frequently the sink will flush the search indexer, expressed in terms of indexing batches
         *
         * @param flushPerBatches Flush per batches
         * @return Builder
         */
        public Builder<TKey, TValue> flushPerBatches(int flushPerBatches) {
            this.flushPerBatches = flushPerBatches;
            return this;
        }

        /**
         * Sets the maximum idle time for the sink
         *
         * @param maxIdleTime Maximum idle time
         * @return Builder
         */
        public Builder<TKey, TValue> maxIdleTime(Duration maxIdleTime) {
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        /**
         * Sets one or more event listeners to be added to the sink.
         *
         * @param eventListeners the event listeners to be added to the sink.
         * @return the builder for fluent chaining of build operations.
         */
        public Builder<TKey, TValue> eventListener(final EventListener<?> ... eventListeners) {
            this.eventListeners = eventListeners;
            return this;
        }

        @Override
        public BulkSearchIndexerSink<TKey, TValue> build() {
            BulkSearchIndexerSink<TKey, TValue> sink = new BulkSearchIndexerSink<>(this.indexer, this.idProvider, this.isDeletion, this.onDelete,
                                                                                   this.indexBatchSize, this.flushPerBatches,
                                                                                   this.reportBatchSize, this.maxIdleTime, this.deadLetterSink);
            if (eventListeners != null) {
                stream(eventListeners).forEach(sink::addListener);
            }

            return sink;
        }
    }
}
