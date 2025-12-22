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
package io.telicent.smart.cache.cli.commands.projection.search.elastic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.ParseException;
import io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration;
import io.telicent.smart.cache.cli.commands.AbstractCommandTests;
import io.telicent.smart.cache.cli.commands.SmartCacheCommand;
import io.telicent.smart.cache.cli.commands.SmartCacheCommandTester;
import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.configuration.sources.NullSource;
import io.telicent.smart.cache.payloads.RdfPayload;
import io.telicent.smart.cache.projectors.sinks.events.file.EventCapturingSink;
import io.telicent.smart.cache.search.clusters.test.AbstractSearchCluster;
import io.telicent.smart.cache.search.clusters.test.SearchTestClusters;
import io.telicent.smart.cache.search.elastic.ESTestCluster;
import io.telicent.smart.cache.search.elastic.ElasticSearchClient;
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.SearchResults;
import io.telicent.smart.cache.search.options.SearchOptions;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.EventHeader;
import io.telicent.smart.cache.sources.Header;
import io.telicent.smart.cache.sources.file.FileEventFormatProvider;
import io.telicent.smart.cache.sources.file.FileEventSource;
import io.telicent.smart.cache.sources.kafka.BasicKafkaTestCluster;
import io.telicent.smart.cache.sources.kafka.KafkaTestCluster;
import io.telicent.smart.cache.sources.kafka.serializers.DatasetGraphDeserializer;
import io.telicent.smart.cache.sources.kafka.serializers.DatasetGraphSerializer;
import io.telicent.smart.cache.sources.kafka.serializers.RdfPayloadSerializer;
import io.telicent.smart.cache.sources.kafka.sinks.KafkaSink;
import io.telicent.smart.cache.sources.memory.SimpleEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.jena.rdfpatch.RDFPatchOps;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

public class AbstractElasticIndexCommandTests extends AbstractCommandTests {

    @SuppressWarnings("rawtypes")
    protected final AbstractSearchCluster elastic;
    protected final KafkaTestCluster kafka = new BasicKafkaTestCluster();

    public AbstractElasticIndexCommandTests() {
        elastic = createTestCluster(null);
    }

    public AbstractElasticIndexCommandTests(String elasticPassword) {
        elastic = createTestCluster(elasticPassword);
    }

    @SuppressWarnings("rawtypes")
    protected AbstractSearchCluster createTestCluster(String elasticPassword) {
        return new ESTestCluster(-1, null, elasticPassword);
    }

    protected void enableSpecificLogging(Class loggerClass, Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(loggerClass).setLevel(level);
    }

    protected void disableSpecificLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
    }

    protected void setupLogging() {}

    @NotNull
    protected static <T extends SmartCacheCommand> ParseResult<T> verifySuccessfulParsing() {
        ParseResult<T> result = SmartCacheCommandTester.getLastParseResult();
        Assert.assertNotNull(result, "Parsing was never invoked");
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertEquals(result.getErrors().size(), 0);
        return result;
    }

    protected static <T extends SmartCacheCommand> void verifyExitStatus(ParseResult<T> result,
                                                                         int expectedExitStatus) {
        Assert.assertNotEquals(SmartCacheCommandTester.getLastExitStatus(), Integer.MIN_VALUE, "Command was not run");
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), expectedExitStatus);
    }

    protected static void verifyStandardOut(String... expectedContents) {
        String stdOut = SmartCacheCommandTester.getLastStdOut();
        for (String expectedContent : expectedContents) {
            Assert.assertTrue(StringUtils.contains(stdOut, expectedContent),
                              String.format("Standard Out was missing expected content '%s'", expectedContent));
        }
    }

    protected static void verifyStandardError(String... expectedContents) {
        String stdErr = SmartCacheCommandTester.getLastStdErr();
        for (String expectedContent : expectedContents) {
            Assert.assertTrue(StringUtils.contains(stdErr, expectedContent),
                              String.format("Standard Error was missing expected content '%s'", expectedContent));
        }
    }

    protected static void verifyNoSearchResults(ElasticSearchClient client, String searchTerm) {
        SearchResults results = client.searchByTerms(searchTerm, SearchOptions.defaults());
        Assert.assertTrue(results.getResults().isEmpty(), "Expected no results for search " + searchTerm);
    }

    protected static void verifySearchResults(ElasticSearchClient client, String searchTerm,
                                              String... expectedDocumentIDs) {
        SearchResults results = client.searchByTerms(searchTerm, SearchOptions.create().build());
        Assert.assertFalse(results.getResults().isEmpty(), "Expected some results for search " + searchTerm);
        Arrays.stream(expectedDocumentIDs)
              .forEach(i -> Assert.assertTrue(
                      results.getResults()
                             .stream()
                             .anyMatch(r -> StringUtils.equals(r.getId(), i)),
                      String.format("Expected a result for Document ID %s", i)));
    }

    @BeforeClass
    @Override
    public void setup() {
        Configurator.setSingleSource(NullSource.INSTANCE);

        // Setup Kafka and Setup ElasticSearch
        this.kafka.setup();
        this.elastic.setup();

        // Set to true for easier debugging during test development
        SmartCacheCommandTester.TEE_TO_ORIGINAL_STREAMS = false;
        setupLogging();
        super.setup();
    }

    @AfterMethod
    @Override
    public void testCleanup() {
        super.testCleanup();

        // Reset Kafka
        this.kafka.resetTestTopic();

        // Reset ElasticSearch
        this.elastic.resetIndex(SearchTestClusters.DEFAULT_TEST_INDEX, CanonicalSearchConfiguration.CONFIG_NAME_V1);
    }

    @AfterClass
    @Override
    public void teardown() {
        Configurator.reset();

        super.teardown();

        this.kafka.teardown();
        disableSpecificLogging();
        try {
            this.elastic.teardown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends SmartCacheCommand> void verifyExpectedErrors(ParseResult<T> result,
                                                                    String... expectedErrorMessages) {
        List<String> actualErrors = new ArrayList<>();
        for (ParseException e : result.getErrors()) {
            actualErrors.add(e.getMessage());
        }

        for (String expected : expectedErrorMessages) {
            String failureMessage = "Failed to find expected error message: " + expected;
            Assert.assertTrue(actualErrors.stream().anyMatch(e -> StringUtils.contains(e, expected)), failureMessage);
        }
    }

    protected void verifyUnsuccessfulParsing(int expectedExitStatus, String... expectedErrorMessages) {
        ParseResult<SmartCacheCommand> result = SmartCacheCommandTester.getLastParseResult();
        Assert.assertNotNull(result, "Parsing was never invoked");
        Assert.assertFalse(result.wasSuccessful());
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), expectedExitStatus,
                            "Unexpected exit status produced");
        Assert.assertEquals(result.getErrors().size(), expectedErrorMessages.length,
                            "Unexpected number of errors produced");
        verifyExpectedErrors(result, expectedErrorMessages);
    }

    protected void verifyExpectedDocuments(String[] expectedDocumentIDs) throws Exception {
        try (ElasticSearchIndexer<Document> indexer = createIndexer()) {
            for (String id : expectedDocumentIDs) {
                Assert.assertTrue(indexer.isIndexed(id),
                                  "Expected document with ID " + id + " was not found in ElasticSearch index");
            }
        }
    }

    protected ElasticSearchIndexer<Document> createIndexer() {
        return ElasticSearchIndexer.<Document>create()
                                   .withCredentials(null, elastic.getPassword(), elastic.getElasticTlsCaCertString())
                                   .index(SearchTestClusters.DEFAULT_TEST_INDEX)
                                   .host(this.elastic.getHost())
                                   .port(this.elastic.getPort())
                                   .build();
    }

    protected void verifyNoSuchDocuments(String[] unexpectedDocumentIDs) throws Exception {
        try (ElasticSearchIndexer<Document> indexer = createIndexer()) {
            for (String id : unexpectedDocumentIDs) {
                Assert.assertFalse(indexer.isIndexed(id),
                                   "Unexpected document with ID " + id + " was present in ElasticSearch index");
            }
        }
    }

    /**
     * Inserts previously captured events into Kafka
     *
     * @param directory Source directory
     * @param format    File event format
     */
    protected void insertFromCapture(File directory, FileEventFormatProvider format) {
        FileEventSource<Bytes, Bytes> source =
                format.createSource(new BytesDeserializer(), new BytesDeserializer(), directory);
        try (KafkaSink<Bytes, Bytes> sink = KafkaSink.<Bytes, Bytes>create()
                                                     .bootstrapServers(this.kafka.getBootstrapServers())
                                                     .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                     .keySerializer(BytesSerializer.class)
                                                     .valueSerializer(BytesSerializer.class)
                                                     .build()) {
            while (!source.isExhausted()) {
                Event<Bytes, Bytes> event = source.poll(Duration.ofSeconds(1));
                if (event != null) {
                    sink.send(event);
                }
            }
            source.close();
        }
    }

    /**
     * Inserts the given array of files into the test Kafka topic as RDF events
     *
     * @param testFiles Test files
     * @param lang      RDF Language the test files are serialized as
     */
    protected void insertRdfEvents(String[] testFiles, Lang lang) {
        try (KafkaSink<Bytes, DatasetGraph> sink = KafkaSink.<Bytes, DatasetGraph>create()
                                                            .bootstrapServers(this.kafka.getBootstrapServers())
                                                            .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                            .keySerializer(BytesSerializer.class)
                                                            .valueSerializer(DatasetGraphSerializer.class)
                                                            .build()) {
            for (String file : testFiles) {
                DatasetGraph dsg = RDFParserBuilder.create().lang(lang).source(file).toDatasetGraph();
                sink.send(new SimpleEvent<>(Collections.emptyList(), null, dsg));
            }
        }
    }

    protected File writeRdfEventsToFile(String[] testFiles, Lang lang) throws IOException {
        File tempDir = Files.createTempDirectory("yaml-events").toFile();
        try (EventCapturingSink<Bytes, DatasetGraph> sink = EventCapturingSink.<Bytes, DatasetGraph>create()
                                                                              .directory(tempDir)
                                                                              .writeYaml(y -> y.keySerializer(
                                                                                                       new BytesSerializer())
                                                                                               .valueSerializer(
                                                                                                       new DatasetGraphSerializer())
                                                                                               .keyDeserializer(
                                                                                                       new BytesDeserializer())
                                                                                               .valueDeserializer(
                                                                                                       new DatasetGraphDeserializer()))
                                                                              .build()) {
            for (String file : testFiles) {
                DatasetGraph dsg = RDFParserBuilder.create().lang(lang).source(file).toDatasetGraph();
                sink.send(new SimpleEvent<>(Collections.emptyList(), null, dsg));
            }
        }
        return tempDir;
    }

    /**
     * Inserts the given array of files into the test Kafka topic as RDF Payload events
     *
     * @param testFiles Test files
     */
    protected void insertRdfPayloadEvents(String[] testFiles) {
        try (KafkaSink<Bytes, RdfPayload> sink = KafkaSink.<Bytes, RdfPayload>create()
                                                          .bootstrapServers(this.kafka.getBootstrapServers())
                                                          .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                          .keySerializer(BytesSerializer.class)
                                                          .valueSerializer(RdfPayloadSerializer.class)
                                                          .build()) {
            for (String file : testFiles) {
                RdfPayload payload;
                List<EventHeader> headers = new ArrayList<>();
                if (StringUtils.endsWith(file, ".patch")) {
                    payload = RdfPayload.of(RDFPatchOps.read(file));
                    headers.add(new Header(HttpHeaders.CONTENT_TYPE, WebContent.contentTypePatch));
                } else {
                    payload = RdfPayload.of(RDFParserBuilder.create().source(file).toDatasetGraph());
                    headers.add(new Header(HttpHeaders.CONTENT_TYPE, WebContent.contentTypeTriG));
                }
                sink.send(new SimpleEvent<>(headers, null, payload));
            }
        }
    }

    /**
     * Takes the single object contents of a given JSON file and submits it to Kafka as a single message.
     *
     * @param file the file containing the JSON object to be written to kafka
     * @throws IOException if an error occurs reading or writing the message content
     */
    public void insertSingleJsonToKafkaTopic(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File is = new File(file);
        Map<String, Object> message = mapper.readerFor(Map.class).readValue(is);

        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .build()) {
            String msg = mapper.writeValueAsString(message);
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, msg));
        }
    }

    /**
     * Takes the contents of a given JSON file and submits them to Kafka
     *
     * @param file the file containing the JSON array of objects to be written to kafka
     * @throws IOException if an error occurs reading or writing the message content
     */
    public void insertMultipleJsonToKafkaTopic(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File is = new File(file);
        List<Map<String, String>> list = mapper.readerForListOf(Map.class).readValue(is);

        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .build()) {
           for (Map<String, String> entry : list) {
                String msg = mapper.writeValueAsString(entry);
                sink.send(new SimpleEvent<>(Collections.emptyList(), null, msg));
            }
        }
    }

    public static Document generateDocumentFromString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // convert JSON string to Map
            Map<String, Object> map = mapper.readValue(json, new TypeReference<>() {
            });
            return new Document(map);
        } catch (IOException e) {
            return null;
        }
    }

    public static void makeBlankConfiguration() {
        Configurator.setSingleSource(NullSource.INSTANCE);
    }
}
