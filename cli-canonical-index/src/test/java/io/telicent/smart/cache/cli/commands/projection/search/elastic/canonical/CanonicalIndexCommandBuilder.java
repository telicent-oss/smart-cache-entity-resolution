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

import io.telicent.smart.cache.search.clusters.test.AbstractSearchCluster;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.sources.kafka.KafkaTestCluster;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper for building test invocations of the {@link CanonicalIndexCommand}
 */
public class CanonicalIndexCommandBuilder {

    private final List<String> arguments = new ArrayList<>();

    /**
     * Creates a new builder
     */
    public CanonicalIndexCommandBuilder() {
        // The following options are necessary so that the command aborts promptly and doesn't wait around too
        // long for additional events that will never arrive
        // Additionally they ensure we don't wait around for Elastic retries
        CollectionUtils.addAll(this.arguments,
                               "--poll-timeout",
                               Integer.toString(1),
                               "--max-stalls",
                               Integer.toString(1),
                               "--elastic-max-retries",
                               Integer.toString(1));
    }

    /**
     * Creates a new builder which automatically adds the arguments for connecting to the test Kafka and Elastic
     * clusters
     *
     * @param kafka   Test Kafka cluster
     * @param elastic Test Elastic cluster
     */
    @SuppressWarnings("rawtypes")
    public CanonicalIndexCommandBuilder(KafkaTestCluster kafka, AbstractSearchCluster elastic) {
        this();
        if (kafka != null) {
            CollectionUtils.addAll(this.arguments,
                                   "--bootstrap-server",
                                   kafka.getBootstrapServers(),
                                   "--topic",
                                   KafkaTestCluster.DEFAULT_TOPIC, "--read-policy",
                                   "BEGINNING");
        }
        if (elastic != null) {
            CollectionUtils.addAll(this.arguments, "--elastic-host",
                                   elastic.getHost(),
                                   "--elastic-port",
                                   Integer.toString(elastic.getPort()),
                                   "--index",
                                   SearchTestClusters.DEFAULT_TEST_SIMILARITY_INDEX
            );
        }
    }

    /**
     * Adds a single argument to the command builder
     *
     * @param argument Argument
     * @return Builder
     */
    public CanonicalIndexCommandBuilder argument(String argument) {
        this.arguments.add(argument);
        return this;
    }

    /**
     * Adds multiple arguments to the command builder
     *
     * @param arguments Arguments
     * @return Builder
     */
    public CanonicalIndexCommandBuilder arguments(String... arguments) {
        CollectionUtils.addAll(this.arguments, arguments);
        return this;
    }

    /**
     * Invokes the built command i.e. calls {@link CanonicalIndexCommand#main(String[])}
     */
    public void invoke() {
        CanonicalIndexCommand.main(this.arguments.toArray(new String[0]));
    }
}
