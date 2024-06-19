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
package io.telicent.smart.cache.search.elastic.utils;

import io.telicent.smart.cache.projectors.utils.PeriodicAction;
import io.telicent.smart.cache.search.elastic.ElasticIndexManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.logging.FmtLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A monitor that can provide an indicator as to when an underlying ElasticSearch index has been mutated.  This is
 * achieved by tracking the internal UUID that ElasticSearch assigns to indices and noting when it changes.
 */
public class ElasticIndexMonitor implements Supplier<Boolean>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticIndexMonitor.class);

    private String lastKnownId;
    private long lastCheckedAt = -1;
    private final long interval;
    private final PeriodicAction asyncMonitor;
    private final boolean async;
    private final ElasticIndexManager manager;
    private final String indexName;
    private final AtomicBoolean hasIndexChanged = new AtomicBoolean(false);

    /**
     * Creates a new index monitor
     *
     * @param manager       Index manager
     * @param index         Index to monitor
     * @param async         Whether monitoring should be asynchronous
     * @param checkInterval How often should we query ElasticSearch to verify whether the index has changed?
     */
    public ElasticIndexMonitor(ElasticIndexManager manager, String index, boolean async, Duration checkInterval) {
        Objects.requireNonNull(manager, "Index manager cannot be null");
        if (StringUtils.isBlank(index)) {
            throw new IllegalArgumentException("Index name cannot be null/blank");
        }
        Objects.requireNonNull(checkInterval, "Check interval cannot be null");
        if (Duration.ZERO.compareTo(checkInterval) >= 0) {
            throw new IllegalArgumentException("Check interval must be a positive duration");
        } else if (async && PeriodicAction.MINIMUM_INTERVAL.compareTo(checkInterval) > 0) {
            throw new IllegalArgumentException("Check interval must be at least 1 seconds for asynchronous monitoring");
        }
        this.manager = manager;
        this.indexName = index;

        this.async = async;
        this.asyncMonitor = this.async ? new PeriodicAction(this::checkIndex, checkInterval) : null;
        this.interval = checkInterval.toMillis();

        // Do our initial check to get the internal ID of the index (if any).  This will always set the changed flag so
        // need to reset it
        checkIndex();
        this.hasIndexChanged.set(false);

        FmtLog.info(LOGGER,
                    "Configured to monitor ElasticSearch index %s at %,d millisecond intervals.  Current internal ID is %s",
                    this.indexName, this.interval, this.lastKnownId);

        if (this.asyncMonitor != null) {
            this.asyncMonitor.autoTrigger();
        }
    }

    private void checkIndex() {
        String currentId = this.manager.getInternalId(this.indexName);
        if (!StringUtils.equals(currentId, this.lastKnownId)) {
            this.hasIndexChanged.set(true);
            this.lastKnownId = currentId;

            // Unless this is our very first time being called log a warning that the internal ID has changed
            if (this.lastCheckedAt != -1) {
                if (currentId == null) {
                    // It's possible that this could occur due to a temporary loss of connectivity to ElasticSearch.  We
                    // can discount that situation by asking the manager whether the index actually exists
                    Boolean exists = this.manager.hasIndex(this.indexName);
                    if (Boolean.FALSE.equals(exists)) {
                        LOGGER.warn("ElasticSearch reports that index {} does not currently exist", this.indexName);
                    } else {
                        LOGGER.warn(
                                "Unable to connect to ElasticSearch to determine whether index {} has been modified",
                                this.indexName);
                    }
                } else {
                    LOGGER.warn("ElasticSearch index {} has been modified, new internal ID is {}", this.indexName,
                                currentId);
                }
            }
        }

        this.lastCheckedAt = System.currentTimeMillis();
    }

    @Override
    public Boolean get() {
        // If we're not doing asynchronous checking, and we've not been called in longer than our check interval, we now
        // trigger a new synchronous check
        if (!this.async) {
            long elapsed = System.currentTimeMillis() - this.lastCheckedAt;
            if (elapsed > this.interval) {
                this.checkIndex();
            }
        }

        Boolean changed = this.hasIndexChanged.get();
        if (changed) {
            // Reset the changed state to unchanged once we've read it and know that we are about to report it
            this.hasIndexChanged.set(false);
        }
        return changed;
    }

    @Override
    public void close() {
        if (this.asyncMonitor != null) {
            this.asyncMonitor.cancelAutoTrigger();
        }
    }
}
