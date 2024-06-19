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
package io.telicent.smart.cache.cli.options.search.elastic;

import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.ranges.IntegerRange;
import com.github.rvesse.airline.annotations.restrictions.ranges.LongRange;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/**
 * Provides common indexing options for search indexing commands
 */
public class IndexingOptions {

    /**
     * Creates new indexing options
     *
     * @param defaultConfigName Default index configuration name
     * @param defaultFormatName Default document format name
     */
    public IndexingOptions(String defaultConfigName, String defaultFormatName) {
        if (StringUtils.isBlank(defaultConfigName)) {
            throw new IllegalArgumentException("Default configuration name must not be blank");
        }
        if (StringUtils.isBlank(defaultFormatName)) {
            throw new IllegalArgumentException("Default document format name must not be blank");
        }
        this.configName = defaultConfigName;
        this.formatName = defaultFormatName;
    }

    /**
     * The indexing batch size
     */
    @Option(name = "--index-batch-size", title = "IndexBatchSize",
            description = "Specifies the batch size for search indexing i.e. how many documents to index in one bulk indexing request.")
    public int indexBatchSize = 1000;

    /**
     * How frequently the search index should be flushed
     */
    @Option(name = "--flush-per-batches", title = "FlushPerBatches",
            description = "Specifies how often the search index will be flushed.  This is expressed in terms of a number of batches, where the index batch size is separately controlled by the --index-batch-size option.  So if this option were set to 5 and the index batch size was 100 then the index would be flushed every 500 documents.")
    @IntegerRange(min = 1)
    public int flushPerBatches = 10;

    /**
     * The duplicate suppression cache size
     */
    @Option(name = "--duplicate-cache-size", title = "DuplicateCacheSize",
            description = "Specifies the cache size used for suppressing duplicate documents to avoid having the search index do unnecessary work.")
    public int cacheSize = 1_000_000;

    /**
     * The maximum idle time without an indexing operation happening (assuming there is any documents ready for
     * indexing)
     */
    @Option(name = "--max-idle-time", title = "MaxIdleTime",
            description = "Specifies the maximum time (in seconds) that the indexing pipeline may be idle for before an index operation will occur.  Setting a low value for this ensures that new documents are regularly pushed to the index even if the configured index batch size has not been reached.")
    @LongRange(min = 1)
    public long maxIdleTime;

    @Option(name = "--index-configuration", title = "ConfigurationName", description = "Specifies the name of an Indexing Configuration to use, see Available Indexing Configurations in the help for this command for more details.")
    private String configName;

    @Option(name = "--index-document-format", title = "DocumentFormat", description = "Specifies the name of a Document Format to use, see Available Document Formats in the help for this command for more details.")
    private String formatName;

    /**
     * Selects the max idle time duration to use for bulk indexing
     * <p>
     * If the user has not supplied a value we'll be set to {@code 0} and we'll pass back {@code null}. 
     * </p>
     *
     * @return Max idle time
     */
    public Duration selectMaxIdleTime() {
        if (this.maxIdleTime == 0) {
            return null;
        } else {
            return Duration.ofSeconds(this.maxIdleTime);
        }
    }

    /**
     * Gets the requested index configuration name, which may be the default for this command if the user did not
     * override it via the relevant option
     *
     * @return Index configuration name
     */
    public String getConfigName() {
        return this.configName;
    }

    /**
     * Gets the requested index document format, which may be the default for this command if the user did not override
     * it via the relevant option
     *
     * @return Index document format name
     */
    public String getFormatName() {
        return this.formatName;
    }
}
