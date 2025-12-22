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
package io.telicent.smart.cache.entity.resolver.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.telicent.smart.cache.cli.commands.AbstractCommandTests;
import io.telicent.smart.cache.cli.commands.SmartCacheCommandTester;
import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.configuration.sources.NullSource;
import io.telicent.smart.cache.live.LiveReporter;
import io.telicent.smart.cache.live.model.LiveHeartbeat;
import io.telicent.smart.cache.live.model.LiveStatus;
import io.telicent.smart.cache.live.serializers.LiveHeartbeatDeserializer;
import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.kafka.BasicKafkaTestCluster;
import io.telicent.smart.cache.sources.kafka.KafkaEventSource;
import io.telicent.smart.cache.sources.kafka.KafkaTestCluster;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DockerSearchCommandTests extends AbstractCommandTests {

    private final KafkaTestCluster kafka = new BasicKafkaTestCluster();

    private static final Client client = ClientBuilder.newClient();


    protected void enableSpecificLogging(Class loggerClass, Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(loggerClass).setLevel(level);
    }

    protected void disableSpecificLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
    }

    @BeforeClass
    @Override
    public void setup() {
        Configurator.setSingleSource(NullSource.INSTANCE);
        SmartCacheCommandTester.TEE_TO_ORIGINAL_STREAMS = true;
        super.setup();
        this.kafka.setup();
    }

    @AfterMethod
    @Override
    public void testCleanup() {
        Configurator.reset();
        this.kafka.resetTestTopic();
        super.testCleanup();
    }

    @AfterClass
    @Override
    public void teardown() {
        this.kafka.teardown();

        super.teardown();
    }

    @Test
    public void api_server_live_reporting_01() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT),
                "--live-bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--live-reporter-topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--live-reporter-interval",
                "1"
        }));
        verifyServerStartup(future);

        // Finally check some heartbeats were produced
        KafkaEventSource<Bytes, LiveHeartbeat> source = getLiveReportsSource(KafkaTestCluster.DEFAULT_TOPIC);
        verifyLiveReports(source);
    }

    @Test
    public void api_server_live_reporting_02() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT),
                "--live-bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--live-reporter-topic",
                KafkaTestCluster.DEFAULT_TOPIC
        }));
        verifyServerStartup(future);

        // Finally check some heartbeats were produced
        KafkaEventSource<Bytes, LiveHeartbeat> source = getLiveReportsSource(KafkaTestCluster.DEFAULT_TOPIC);
        verifyLiveReports(source);
    }

    @Test
    public void api_server_live_reporting_03() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT),
                "--live-bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--live-reporter-interval",
                "1"
        }));
        verifyServerStartup(future);

        // Finally check some heartbeats were produced
        KafkaEventSource<Bytes, LiveHeartbeat> source = getLiveReportsSource(LiveReporter.DEFAULT_LIVE_TOPIC);
        verifyLiveReports(source);

        this.kafka.resetTopic(LiveReporter.DEFAULT_LIVE_TOPIC);
    }

    @Test
    public void api_server_live_reporting_04() throws InterruptedException {
        enableSpecificLogging(LiveReporter.class, Level.WARN);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT),
                "--live-reporter-interval",
                "1"
        }));
        verifyServerStartup(future);

        // No bootstrap servers configured then no heartbeats go anywhere
        KafkaEventSource<Bytes, LiveHeartbeat> source = getLiveReportsSource(KafkaTestCluster.DEFAULT_TOPIC);
        Assert.assertNull(source.remaining());
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));

        String stdErr = SmartCacheCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "No sink specified"));
        disableSpecificLogging();
    }

    private static void verifyServerStartup(Future<?> future) throws InterruptedException {
        // Should be a blocking command so no exit status produced immediately
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), Integer.MIN_VALUE);

        // Wait a few seconds for some live heartbeats to be produced
        Thread.sleep(5000);

        // Ensure server is still running
        Assert.assertFalse(future.isDone());

        // Force server to stop and check exit status is now zero
        future.cancel(true);
        try {
            future.get();
            Assert.fail("Expected server to have been cancelled");
        } catch (CancellationException e) {
            // Expected, this is fine
        } catch (Throwable e) {
            Assert.fail("Unexpected error " + e.getMessage());
        }
        // There needs to be a brief pause here between when the thread is cancelled and when it actually completes and
        // sets the exit code accordingly
        Thread.sleep(250);
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
    }

    private static void verifyLiveReports(KafkaEventSource<Bytes, LiveHeartbeat> source) {
        Assert.assertFalse(source.isExhausted());
        Event<Bytes, LiveHeartbeat> first = source.poll(Duration.ofSeconds(3));
        Assert.assertNotNull(first);
        Assert.assertNotNull(first.value());
        Assert.assertEquals(first.value().getStatus(), LiveStatus.STARTED);

        while (source.remaining() > 1) {
            Event<Bytes, LiveHeartbeat> next = source.poll(Duration.ofSeconds(3));
            Assert.assertNotNull(next);
            Assert.assertNotNull(next.value());
            Assert.assertEquals(next.value().getStatus(), LiveStatus.RUNNING);
        }

        Event<Bytes, LiveHeartbeat> last = source.poll(Duration.ofSeconds(3));
        Assert.assertNotNull(last);
        Assert.assertNotNull(last.value());
        Assert.assertEquals(last.value().getStatus(), LiveStatus.COMPLETED);

        source.close();
    }

    private KafkaEventSource<Bytes, LiveHeartbeat> getLiveReportsSource(String topic) {
        return KafkaEventSource.<Bytes, LiveHeartbeat>create()
                               .bootstrapServers(this.kafka.getBootstrapServers())
                               .topic(topic)
                               .consumerGroup("test-live-reports")
                               .keyDeserializer(BytesDeserializer.class)
                               .valueDeserializer(LiveHeartbeatDeserializer.class)
                               .fromBeginning()
                               .build();
    }

    @Test
    public void api_server_base_path_01() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT)
        }));
        verifyServerHealthy(future, "");
    }

    @Test
    public void api_server_base_path_02() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT),
                "--base-path",
                "/api/search"
        }));
        verifyServerHealthy(future, "/api/search");
    }

    @Test
    public void api_server_base_path_03() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> EntityResolutionApiCommand.main(new String[]{
                "--host",
                AbstractEntityResolutionApiServerTests.API_HOST,
                "--port",
                Integer.toString(AbstractEntityResolutionApiServerTests.API_PORT),
                "--base-path",
                "/a/b/c/d"
        }));
        verifyServerHealthy(future, "/a/b/c/d");
    }

    private static void verifyServerHealthy(Future<?> future, String basePath) throws InterruptedException {
        // Should be a blocking command so no exit status produced immediately
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), Integer.MIN_VALUE);

        // Wait a few seconds for server to start up
        Thread.sleep(5000);

        // Ensure server is still running
        Assert.assertFalse(future.isDone());

        // Make a request to the /healthz endpoint of the server
        WebTarget target = client.target("http://localhost:18081" + basePath + "/healthz");
        try (Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get()) {
            HealthStatus health = response.readEntity(HealthStatus.class);
            Assert.assertNotNull(health);
        } finally {

            // Force server to stop and check exit status is now zero
            future.cancel(true);
            try {
                future.get();
                Assert.fail("Expected server to have been cancelled");
            } catch (CancellationException e) {
                // Expected, this is fine
            } catch (Throwable e) {
                Assert.fail("Unexpected error " + e.getMessage());
            }
            // There needs to be a brief pause here between when the thread is cancelled and when it actually completes and
            // sets the exit code accordingly
            Thread.sleep(250);
            Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        }
    }
}
