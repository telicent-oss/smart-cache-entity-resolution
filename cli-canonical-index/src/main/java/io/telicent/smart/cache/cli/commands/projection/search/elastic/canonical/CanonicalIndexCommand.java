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
package io.telicent.smart.cache.cli.commands.projection.search.elastic.canonical;

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration;
import io.telicent.smart.cache.cli.commands.SmartCacheCommand;
import io.telicent.smart.cache.cli.commands.projection.AbstractKafkaProjectorCommand;
import io.telicent.smart.cache.cli.options.search.elastic.ElasticSearchOptions;
import io.telicent.smart.cache.cli.options.search.elastic.IndexingOptions;
import io.telicent.smart.cache.live.model.IODescriptor;
import io.telicent.smart.cache.projectors.Projector;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.search.sinks.BulkSearchIndexerSink;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.EventSource;
import io.telicent.smart.cache.sources.kafka.KafkaEventSource;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;

import java.util.UUID;

/**
 * A command for indexing canonical records into ElasticSearch
 */
@Command(name = "elastic-can-index.sh", description = "Indexes records from the canonical topic and builds a similarity index in ElasticSearch")
public class CanonicalIndexCommand extends
        AbstractKafkaProjectorCommand<Bytes, CanonicalFormMap, Event<Bytes, CanonicalFormMap>> {

    /**
     * ElasticSearch related options
     */
    @AirlineModule
    protected final ElasticSearchOptions elastic = new ElasticSearchOptions();

    /**
     * Indexing related options
     */
    @AirlineModule
    protected final IndexingOptions indexing =
            new IndexingOptions(CanonicalSearchConfiguration.CONFIG_NAME_V1, CanonicalSearchConfiguration.DOCUMENT_FORMAT_IES4_V3);

    /**
     * Dynamic Rule mapping configuration
     */
    @Option(name = "--canonical-config", title = "CanonicalTypeConfiguration", description = "Path of configuration file for setting up dynamic rules for processing canonical types")
    protected String canonicalConfig;
    @Override
    protected IODescriptor getLiveReporterOutputDescriptor() {
        return new IODescriptor("elasticsearch", "smartcache");
    }

    @Override
    protected Serializer<Bytes> keySerializer() {
        return new BytesSerializer();
    }

    @Override
    protected Deserializer<Bytes> keyDeserializer() {
        return new BytesDeserializer();
    }

    @Override
    protected Serializer<CanonicalFormMap> valueSerializer() {
        return null;
    }

    @Override
    protected Deserializer<CanonicalFormMap> valueDeserializer() {
        return new CanonicalFormMapDeserializer();
    }

    @Override
    protected String getThroughputItemsName() {
        return "Canonical Forms";
    }

    @Override
    protected EventSource<Bytes, CanonicalFormMap> getSource() {
        // TODO - slotting in here. Not ideal though
        CanonicalSearchConfiguration.loadDynamicMappingRules(this.canonicalConfig);

        return KafkaEventSource.<Bytes, CanonicalFormMap>create()
                               .bootstrapServers(this.kafka.bootstrapServers)
                               .topics(this.kafka.topics)
                               .consumerGroup(this.kafka.getConsumerGroup())
                               .keyDeserializer(BytesDeserializer.class)
                               .valueDeserializer(CanonicalFormMapDeserializer.class)
                               .maxPollRecords(100)
                               .readPolicy(this.kafka.readPolicy.toReadPolicy())
                               .autoCommit(this.useAutoCommit())
                               .build();
    }

    @Override
    protected Projector<Event<Bytes, CanonicalFormMap>, Event<Bytes, CanonicalFormMap>> getProjector() {
        return (event, sink) -> sink.send(event);
    }

    private String obtainID(CanonicalFormMap map) {
        return map.entityMapping().getOrDefault("id", UUID.randomUUID().toString()).toString();
    }

    @Override
    protected Sink<Event<Bytes, CanonicalFormMap>> prepareWorkSink() {
        return BulkSearchIndexerSink.<Bytes, CanonicalFormMap>createBulk()
                                    .indexer(this.elastic.prepareElasticIndexer(this.indexing.getConfigName(), null,
                                                                                null))
                                    .idProvider(this::obtainID)
                                    .batchSize(this.indexing.indexBatchSize)
                                    .flushPerBatches(this.indexing.flushPerBatches)
                                    .maxIdleTime(this.indexing.selectMaxIdleTime())
                                    .build();
    }

    /**
     * Entry point for running the ElasticSearch Indexer as a single command
     *
     * @param args Command options and arguments
     */
    public static void main(String[] args) {
        SmartCacheCommand.runAsSingleCommand(CanonicalIndexCommand.class, args);
    }
}
