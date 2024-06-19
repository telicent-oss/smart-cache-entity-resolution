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
package io.telicent.smart.cache.entity;

import io.telicent.smart.cache.entity.collectors.DirectLiteralsCollector;
import io.telicent.smart.cache.entity.collectors.OutRelationshipsCollector;
import io.telicent.smart.cache.entity.config.EntityProjectionConfig;
import io.telicent.smart.cache.entity.selectors.SimpleTypeSelector;
import io.telicent.smart.cache.entity.sinks.EntityToMapSink;
import io.telicent.smart.cache.entity.sinks.converters.*;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import io.telicent.smart.cache.entity.vocabulary.Telicent;
import io.telicent.smart.cache.observability.metrics.MetricTestUtils;
import io.telicent.smart.cache.payloads.RdfPayload;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.projectors.sinks.*;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.Header;
import io.telicent.smart.cache.sources.TelicentHeaders;
import io.telicent.smart.cache.sources.memory.SimpleEvent;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdfpatch.changes.RDFChangesCollector;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TestEntityCentricProjector extends AbstractEntityCollectorTests {

    public static final Node FRED = NodeFactory.createURI(FRED_URI);

    @BeforeMethod
    public void setupMetrics() {
        MetricTestUtils.enableMetricsCapture();
    }

    @AfterMethod
    public void teardownMetrics() {
        MetricTestUtils.disableMetricsCapture();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void entity_centric_projector_bad_01() {
        new EntityCentricProjector<>(null);
    }

    private void verifyMetrics(long expected) {
        verifyMetrics(expected, 0);
    }

    private void verifyMetrics(long expected, long errors) {
        try {
            Double reportedMetric = MetricTestUtils.getReportedMetric(EntityMetricNames.PROJECTION_ENTITIES_TOTAL);
            Assert.assertEquals(reportedMetric.longValue(), expected);
        } catch (IllegalStateException e) {
            Assert.assertEquals(0, expected);
        }

        try {
            Double reportedMetric = MetricTestUtils.getReportedMetric(EntityMetricNames.PROJECTION_ENTITIES_ERRORS);
            Assert.assertEquals(reportedMetric.longValue(), errors);
        } catch (IllegalStateException e) {
            Assert.assertEquals(0, errors);
        }
    }

    @Test
    public void entity_centric_projector_01() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, null, RdfPayload.of(DatasetGraphFactory.create())), collector);
        Assert.assertEquals(collector.get().size(), 0);
        verifyMetrics(0);
    }

    @Test
    public void entity_centric_projector_02() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(DatasetGraphFactory.wrap(testData))),
                          collector);
        Assert.assertEquals(collector.get().size(), 23);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(23);
    }

    @Test
    public void entity_centric_projector_03() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(),
                                           Collections.singletonList(new OutRelationshipsCollector())));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, "another-test", RdfPayload.of(DatasetGraphFactory.wrap(testData))),
                          collector);
        Assert.assertEquals(collector.get().size(), 23);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData("outRels")));
        verifyMetrics(23);
    }

    @Test
    public void entity_centric_projector_04() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(),
                                           Collections.singletonList(new OutRelationshipsCollector())));
        ErrorSink<Event<String, Entity>> sink = new ErrorSink<>();
        ThroughputSink<Event<String, Entity>> throughput = Sinks.<Event<String, Entity>>throughput()
                                                                .tracker(t -> t.reportBatchSize(1)
                                                                               .logger(TestEntityCentricProjector.class))
                                                                .destination(sink)
                                                                .build();
        projector.project(new SimpleEvent<>(null, null, RdfPayload.of(DatasetGraphFactory.wrap(testData))), throughput);
        Assert.assertEquals(throughput.receivedCount(), 23);
        Assert.assertEquals(throughput.processedCount(), 0);
        verifyMetrics(23, 23);
    }

    @Test
    public void entity_centric_projector_05() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        // Projector should project from all graphs
        DatasetGraph dataset = DatasetGraphFactory.create();
        dataset.addGraph(NodeFactory.createURI("urn:test:graph"), testData);
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(dataset)), collector);
        Assert.assertEquals(collector.get().size(), 23);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(23);
    }

    @Test
    public void entity_centric_projector_06() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        // Projector should project from all graphs
        DatasetGraph dataset = DatasetGraphFactory.create();
        GraphUtil.addInto(dataset.getDefaultGraph(), testData);
        dataset.addGraph(NodeFactory.createURI("urn:test:graph"), testData);
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(dataset)), collector);
        Assert.assertEquals(collector.get().size(), 46);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(46);
    }

    @Test
    public void entity_centric_projector_07() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        // Projector should not project from security labels graph
        DatasetGraph dataset = DatasetGraphFactory.create();
        dataset.addGraph(Telicent.SECURITY_LABELS_GRAPH_URI, testData);
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(dataset)), collector);
        Assert.assertEquals(collector.get().size(), 0);
        verifyMetrics(0);
    }

    @Test
    public void entity_centric_projector_09() {
        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        // Projector should project from all graphs
        DatasetGraph dataset = DatasetGraphFactory.create();
        GraphUtil.addInto(dataset.getDefaultGraph(), testData);
        projector.project(
                new SimpleEvent<>(Collections.singletonList(new Header(TelicentHeaders.SECURITY_LABEL, "admin")),
                                  "test", RdfPayload.of(dataset)), collector);
        Assert.assertEquals(collector.get().size(), 23);
        collector.get().forEach(e -> {
            Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP));
            Assert.assertTrue(e.value().hasSecurityLabels());
            Assert.assertNotNull(e.value().getDefaultSecurityLabels());
            Assert.assertNull(e.value().getSecurityLabels());
        });
        verifyMetrics(23);
    }

    @Test
    public void entity_centric_projector_10() throws IOException {
        try (InputStream input = TestEntityCentricProjector.class.getResourceAsStream("/fred.trig")) {
            Assert.assertNotNull(input);

            RDFParser parser = RDFParserBuilder.create().lang(Lang.TRIG).source(input).build();
            DatasetGraph dataset = DatasetGraphFactory.create();
            parser.parse(dataset);

            EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                    new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
            CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
            projector.project(
                    new SimpleEvent<>(List.of(new Header(TelicentHeaders.SECURITY_LABEL, "default")), "test",
                                      RdfPayload.of(dataset)),
                    collector);

            Assert.assertEquals(collector.get().size(), 1);
            Entity e = collector.get().get(0).value();
            Set<String> uniqueLabels = new HashSet<>();
            e.getData(Rdf.TYPE_GROUP).forEach(d -> verifyAllLabelled(d, uniqueLabels));
            e.getData(DefaultOutputFields.LITERALS).forEach(d -> verifyAllLabelled(d, uniqueLabels));

            Assert.assertEquals(uniqueLabels.size(), 5);
        }
    }

    @Test
    public void entity_centric_projector_11() throws IOException {
        try (InputStream input = TestEntityCentricProjector.class.getResourceAsStream("/fred-bad-prefix.trig")) {
            Assert.assertNotNull(input);

            RDFParser parser = RDFParserBuilder.create().lang(Lang.TRIG).source(input).build();
            DatasetGraph dataset = DatasetGraphFactory.create();
            parser.parse(dataset);

            EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                    new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
            CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
            projector.project(
                    new SimpleEvent<>(List.of(new Header(TelicentHeaders.SECURITY_LABEL, "default")), "test",
                                      RdfPayload.of(dataset)),
                    collector);

            Assert.assertEquals(collector.get().size(), 1);
            Entity e = collector.get().get(0).value();
            Set<String> uniqueLabels = new HashSet<>();
            e.getData(Rdf.TYPE_GROUP).forEach(d -> verifyAllLabelled(d, uniqueLabels));
            e.getData(DefaultOutputFields.LITERALS).forEach(d -> verifyAllLabelled(d, uniqueLabels));

            // Because we redefined the foaf prefix to the wrong URL when the labels graph is processed the rules
            // pertaining to foaf triples won't match the data so everything falls back to the default label
            Assert.assertEquals(uniqueLabels.size(), 1);
        }
    }

    private void verifyAllLabelled(EntityData data, Set<String> uniqueLabels) {
        data.keys().forEach(k -> data.get(k).forEach(n -> {
            Assert.assertTrue(n.hasSecurityLabel(),
                              String.format("Node %s for key %s is not security labelled", n.getNode(), k));
            uniqueLabels.add(n.getSecurityLabel());
        }));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void entity_centric_projector_12() throws IOException {
        try (InputStream input = TestEntityToMapOutputConverters.class.getResourceAsStream("/fred.trig")) {
            Assert.assertNotNull(input);

            RDFParser parser = RDFParserBuilder.create().lang(Lang.TRIG).source(input).build();
            DatasetGraph dataset = DatasetGraphFactory.create();
            parser.parse(dataset);

            EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                    new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
            CollectorSink<Event<Entity, Map<String, Object>>> collector = CollectorSink.of();
            Sink<Event<String, Entity>> sink = EntityToMapSink.<String>create()
                                                              .withConverters(new UriConverter(),
                                                                              new PrimaryNameConverter(),
                                                                              new DataToSimpleList(
                                                                                      DefaultOutputFields.TYPES,
                                                                                      Rdf.TYPE_GROUP, false),
                                                                              new DataToSimpleMap(
                                                                                      DefaultOutputFields.LITERALS,
                                                                                      true, false,
                                                                                      DefaultOutputFields.LITERALS))
                                                              .destination(collector)
                                                              .build();
            projector.project(
                    new SimpleEvent<>(List.of(new Header(TelicentHeaders.SECURITY_LABEL, "default")), "test",
                                      RdfPayload.of(dataset)),
                    sink);

            Assert.assertEquals(collector.get().size(), 1);
            Map<String, Object> doc = collector.get().get(0).value();
            AbstractEntityToMapOutputConverterTests.dumpOutput(doc);
            Assert.assertNotNull(doc);

            Assert.assertTrue(doc.containsKey(DefaultOutputFields.SECURITY_LABELS));
            Map<String, Object> securityLabels = (Map<String, Object>) doc.get(DefaultOutputFields.SECURITY_LABELS);
            Assert.assertTrue(securityLabels.containsKey(DefaultOutputFields.PRIMARY_NAME));
            Assert.assertEquals(securityLabels.get(DefaultOutputFields.PRIMARY_NAME), "default");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void entity_centric_projector_13() throws IOException {
        try (InputStream input = TestEntityToMapOutputConverters.class.getResourceAsStream("/fred.trig")) {
            Assert.assertNotNull(input);

            RDFParser parser = RDFParserBuilder.create().lang(Lang.TRIG).source(input).build();
            DatasetGraph dataset = DatasetGraphFactory.create();
            parser.parse(dataset);

            EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                    new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
            CollectorSink<Event<Entity, Map<String, Object>>> collector = CollectorSink.of();
            Sink<Event<String, Entity>> sink = EntityToMapSink.<String>create()
                                                              .withConverters(new UriConverter(),
                                                                              new PrimaryNameConverter(),
                                                                              new DataToUpsertableMap(
                                                                                      DefaultOutputFields.TYPES,
                                                                                      Rdf.TYPE_GROUP,
                                                                                      UpsertableKeyFormat.COMPACTED,
                                                                                      true, false),
                                                                              new DataToUpsertableComplexMap(
                                                                                      DefaultOutputFields.LITERALS,
                                                                                      DefaultOutputFields.LITERALS,
                                                                                      UpsertableKeyFormat.HASHED, true,
                                                                                      false))
                                                              .destination(collector)
                                                              .build();
            projector.project(
                    new SimpleEvent<>(List.of(new Header(TelicentHeaders.SECURITY_LABEL, "default")), "test",
                                      RdfPayload.of(dataset)),
                    sink);

            Assert.assertEquals(collector.get().size(), 1);
            Map<String, Object> doc = collector.get().get(0).value();
            AbstractEntityToMapOutputConverterTests.dumpOutput(doc);
            Assert.assertNotNull(doc);

            Assert.assertTrue(doc.containsKey(DefaultOutputFields.SECURITY_LABELS));
            Map<String, Object> securityLabels = (Map<String, Object>) doc.get(DefaultOutputFields.SECURITY_LABELS);
            Assert.assertTrue(securityLabels.containsKey(DefaultOutputFields.PRIMARY_NAME));
            Assert.assertEquals(securityLabels.get(DefaultOutputFields.PRIMARY_NAME), "default");
        }
    }

    @Test
    public void entity_centric_projector_14() {
        DatasetGraph dsg = new DatasetGraphNullGraphs();

        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
        NullSink<Event<String, Entity>> sink = NullSink.of();
        projector.project(new SimpleEvent<>(null, "Test", RdfPayload.of(dsg)), sink);
        Assert.assertEquals(sink.count(), 0L);
    }

    @Test
    public void entity_centric_projector_15() {
        DatasetGraph dsg = new DatasetGraphEmptyGraphs(Arrays.asList(new Node[]{null}));

        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
        NullSink<Event<String, Entity>> sink = NullSink.of();
        projector.project(new SimpleEvent<>(null, "Test", RdfPayload.of(dsg)), sink);
        Assert.assertEquals(sink.count(), 0L);
    }

    @Test
    public void entity_centric_projector_patches_01() {
        RDFChangesCollector changes = new RDFChangesCollector();
        changes.start();
        changes.add(Quad.defaultGraphIRI, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.finish();

        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(changes.getRDFPatch())),
                          collector);
        Assert.assertEquals(collector.get().size(), 1);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(1);
    }

    @Test
    public void entity_centric_projector_patches_02() {
        RDFChangesCollector changes = new RDFChangesCollector();
        changes.start();
        changes.add(Quad.defaultGraphIRI, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.delete(Quad.defaultGraphIRI, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.finish();

        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(changes.getRDFPatch())),
                          collector);
        Assert.assertEquals(collector.get().size(), 2);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(2);

        Assert.assertFalse(collector.get().get(0).value().isDeletion());
        Assert.assertTrue(collector.get().get(1).value().isDeletion());
    }

    @Test
    public void entity_centric_projector_patches_03() {
        Node graphName = NodeFactory.createURI("https://graphs/1");
        RDFChangesCollector changes = new RDFChangesCollector();
        changes.start();
        changes.add(graphName, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.delete(graphName, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.finish();

        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), Collections.emptyList()));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(changes.getRDFPatch())),
                          collector);
        Assert.assertEquals(collector.get().size(), 2);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(2);

        Assert.assertFalse(collector.get().get(0).value().isDeletion());
        Assert.assertTrue(collector.get().get(1).value().isDeletion());
    }

    @Test
    public void entity_centric_projector_patches_04() {
        Node graphName = NodeFactory.createURI("https://graphs/1");
        RDFChangesCollector changes = new RDFChangesCollector();
        changes.start();
        changes.add(graphName, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.add(graphName, FRED, FOAF.name.asNode(), FRED_FULL_NAME);
        changes.add(graphName, FRED, FOAF.nick.asNode(), FRED_SHORT_NAME);
        changes.delete(graphName, FRED, Rdf.TYPE, PERSON_TYPE);
        changes.finish();

        EntityCentricProjector<String> projector = new EntityCentricProjector<>(
                new EntityProjectionConfig(new SimpleTypeSelector(), List.of(new DirectLiteralsCollector())));
        CollectorSink<Event<String, Entity>> collector = CollectorSink.of();
        projector.project(new SimpleEvent<>(null, "test", RdfPayload.of(changes.getRDFPatch())),
                          collector);
        Assert.assertEquals(collector.get().size(), 2);
        collector.get().forEach(e -> Assert.assertTrue(e.value().hasData(Rdf.TYPE_GROUP)));
        verifyMetrics(2);

        Assert.assertFalse(collector.get().get(0).value().isDeletion());
        Assert.assertTrue(collector.get().get(1).value().isDeletion());

        Event<String, Entity> addition = collector.get().get(0);
        Assert.assertTrue(addition.value().hasAnyLiterals());

        Event<String, Entity> deletion = collector.get().get(1);
        Assert.assertFalse(deletion.value().hasAnyLiterals());
    }

}
