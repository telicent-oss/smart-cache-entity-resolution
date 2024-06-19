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

import ch.qos.logback.classic.Level;
import com.github.rvesse.airline.parser.ParseResult;
import io.telicent.smart.cache.cli.commands.SmartCacheCommandTester;
import io.telicent.smart.cache.cli.commands.projection.AbstractProjectorCommand;
import io.telicent.smart.cache.cli.commands.projection.search.elastic.AbstractElasticIndexCommandTests;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.sources.kafka.KafkaTestCluster;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class DockerTestCanonicalIndexCommandBadOptions extends AbstractElasticIndexCommandTests {

    public static final String IOW_TEST_DATA_FOLDER = "test-data" + File.separator + "iow" + File.separator;

    public static final String[] IOW_TEST_FILES = {
            IOW_TEST_DATA_FOLDER + "1-iow.ttl"
    };

    @Override
    protected void setupLogging() {
        enableSpecificLogging(CanonicalFormMapDeserializer.class, Level.WARN);
        enableSpecificLogging(AbstractProjectorCommand.class, Level.ERROR);
    }

    @Test
    public void canonical_index_help_01() {
        // Providing no arguments at all
        CanonicalIndexCommand.main(new String[]{"--help"});

        verifyUnsuccessfulParsing(2, "Required option '--index'",
                                  "At least one of the following",
                                  "Required option '--elastic-host'");
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 2);

        verifyStandardOut("SYNOPSIS", "OPTIONS", "EXIT CODES",
                          "COPYRIGHT");
    }

    @Test
    public void canonical_index_bad_01() {
        // Providing no arguments at all
        CanonicalIndexCommand.main(new String[0]);

        verifyUnsuccessfulParsing(127, "Required option '--index'",
                                  "At least one of the following",
                                  "Required option '--elastic-host'");
    }

    @Test
    public void canonical_index_bad_02() {
        // Providing an ElasticSearch index but no Kafka instance
        CanonicalIndexCommand.main(new String[]{
                "--elastic-host",
                this.elastic.getHost(),
                "--elastic-port",
                Integer.toString(this.elastic.getPort()),
                "--index",
                SearchTestClusters.DEFAULT_TEST_INDEX
        });

        verifyUnsuccessfulParsing(127, "At least one of the following");
    }

    @Test
    public void canonical_index_bad_03() {
        // Providing a Kafka instance but no ElasticSearch host/index
        CanonicalIndexCommand.main(new String[]{
                "--bootstrap-server", this.kafka.getBootstrapServers(), "--topic", KafkaTestCluster.DEFAULT_TOPIC
        });

        verifyUnsuccessfulParsing(127, "Required option '--elastic-host'", "Required option '--index' ");
    }

    @Test
    public void canonical_index_bad_04() {
        // Invalid MaxConnectionAttempts
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments("--elastic-max-connect-attempts", "0");
        builder.invoke();

        verifyUnsuccessfulParsing(127,
                                  "Value for option 'MaxConnectionAttempts' was given as '0' which is not in the acceptable range");
    }

    @Test
    public void canonical_index_bad_05() {
        // Invalid MinConnectInterval
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments("--elastic-min-connect-interval", "0");
        builder.invoke();

        verifyUnsuccessfulParsing(127,
                                  "Value for option 'MinConnectInterval' was given as '0' which is not in the acceptable range");
    }

    @Test
    public void canonical_index_bad_06() {
        // Invalid MaxConnectInterval
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments("--elastic-max-connect-interval", "0");
        builder.invoke();

        verifyUnsuccessfulParsing(127,
                                  "Value for option 'MaxConnectInterval' was given as '0' which is not in the acceptable range");
    }

    @Test
    public void canonical_index_bad_07() {
        // Invalid FlushPerBatches
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments("--flush-per-batches", "0");
        builder.invoke();

        verifyUnsuccessfulParsing(127,
                                  "Value for option 'FlushPerBatches' was given as '0' which is not in the acceptable range");
    }

    @Test
    public void canonical_index_bad_08() {
        // Invalid MaxIdleTime
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments("--max-idle-time", "0");
        builder.invoke();

        verifyUnsuccessfulParsing(127,
                                  "Value for option 'MaxIdleTime' was given as '0' which is not in the acceptable range");
    }

    @Test
    public void canonical_index_bad_09() {
        // Invalid ElasticSearchPort
        CanonicalIndexCommand.main(new String[]{
                "--bootstrap-server",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--elastic-host",
                this.elastic.getHost(),
                "--elastic-port",
                "abcd",
                "--index",
                SearchTestClusters.DEFAULT_TEST_INDEX
        });

        verifyUnsuccessfulParsing(127, "ElasticSearchPort: can not convert \"abcd\" to a int", "unexpected parameter");
    }

    @Test
    public void canonical_index_bad_10() {
        // Invalid IndexBatchSize
        CanonicalIndexCommand.main(new String[]{
                "--bootstrap-server",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--elastic-host",
                this.elastic.getHost(),
                "--index",
                SearchTestClusters.DEFAULT_TEST_INDEX,
                "--index-batch-size",
                "abcd"
        });

        verifyUnsuccessfulParsing(127, "IndexBatchSize: can not convert \"abcd\" to a int", "unexpected parameter");
    }

    @Test
    public void canonical_index_bad_11() {
        // Invalid DuplicateCacheSize
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments("--duplicate-cache-size", "abcd");
        builder.invoke();

        verifyUnsuccessfulParsing(127, "DuplicateCacheSize: can not convert \"abcd\" to a int", "unexpected parameter");
    }


    @Test
    public void canonical_index_bad_12() {
        // Insert non-JSON events (RDF in the case) onto Kafka
        insertRdfPayloadEvents(IOW_TEST_FILES);

        // Fully configuring Kafka and ElasticSearch instances to communicate with, recreate index set
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments(
                // Use --recreate-index, this causes the index to be dropped and recreated prior to beginning indexing
                "--recreate-index");
        builder.invoke();
        ParseResult<CanonicalIndexCommand> result = verifySuccessfulParsing();
        verifyExitStatus(result, 1);
        verifyStandardError("Unexpected error", "Malformed record encountered");
    }

    @Test
    public void canonical_index_bad_13() {
        // Fully configuring Kafka and ElasticSearch instances to communicate with, recreate index set
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments(
                // Use --recreate-index, this causes the index to be dropped and recreated prior to beginning indexing
                "--recreate-index",
                // Invalid configuration
                "--index-configuration", "no-such-config");
        builder.invoke();

        ParseResult<CanonicalIndexCommand> result = verifySuccessfulParsing();
        verifyExitStatus(result, 1);
        verifyStandardError("Unexpected error",
                            "Specified index configuration no-such-config is not supported");
    }

    @Test
    public void canonical_index_bad_14() {
        // Fully configuring Kafka and ElasticSearch instances to communicate with, recreate index set
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments(
                // Use --recreate-index, this causes the index to be dropped and recreated prior to beginning indexing
                "--recreate-index",
                // Invalid configuration
                "--index-configuration", "no-such-config");
        builder.invoke();
        ParseResult<CanonicalIndexCommand> result = verifySuccessfulParsing();
        verifyExitStatus(result, 1);

        verifyStandardError("Unexpected error", "Specified index configuration no-such-config is not supported");
    }

    @Test
    public void canonical_index_bad_15() {
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(null, this.elastic);
        //@formatter:off
        builder.arguments("--source-file",
                          new File("test-data/canonical", "no_file_exists.txt").getAbsolutePath());
        //@formatter:on
        builder.invoke();

        verifyUnsuccessfulParsing(127, "Option value 'Option 'SourceFile'' was given value");
        verifyUnsuccessfulParsing(127, "which is not a path to an existing file/directory");
    }

    @Test
    public void canonical_index_bad_16() {
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(null, this.elastic);
        //@formatter:off
        builder.arguments("--source-file",
                          new File("test-data/canonical", "test_entity_bad_format.txt").getAbsolutePath());
        //@formatter:on
        builder.invoke();

        ParseResult<CanonicalIndexCommand> result = verifySuccessfulParsing();
        verifyExitStatus(result, 1);
        verifyStandardError("Unexpected error", " Invalid Event in file");
    }

    @Test
    public void canonical_index_bad_17() throws IOException {
        // Insert test events into Kafka
        insertMultipleJsonToKafkaTopic("test-data/canonical/test_entity_list.json");

        // Fully configuring Kafka and ElasticSearch instances to communicate with, recreate index set
        CanonicalIndexCommandBuilder builder = new CanonicalIndexCommandBuilder(this.kafka, this.elastic);
        builder.arguments(
                // Use --recreate-index, this causes the index to be dropped and recreated prior to beginning indexing
                "--recreate-index");
        builder.invoke();

        ParseResult<CanonicalIndexCommand> result = verifySuccessfulParsing();
        verifyExitStatus(result, 0);
    }
}
