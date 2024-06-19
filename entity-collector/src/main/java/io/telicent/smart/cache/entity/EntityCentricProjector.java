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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.entity.config.EntityProjectionConfig;
import io.telicent.smart.cache.entity.patches.PatchOperation;
import io.telicent.smart.cache.entity.patches.RDFChangesAsDatasets;
import io.telicent.smart.cache.entity.vocabulary.Telicent;
import io.telicent.smart.cache.observability.AttributeNames;
import io.telicent.smart.cache.observability.LibraryVersion;
import io.telicent.smart.cache.observability.TelicentMetrics;
import io.telicent.smart.cache.payloads.RdfPayload;
import io.telicent.smart.cache.projectors.Library;
import io.telicent.smart.cache.projectors.Projector;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.projectors.SinkException;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.TelicentHeaders;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfpatch.RDFPatch;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * A Projector that creates Entity-centric representations of the incoming RDF data and passes that onto a sink for
 * further processing.
 */
public class EntityCentricProjector<TKey> implements Projector<Event<TKey, RdfPayload>, Event<TKey, Entity>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityCentricProjector.class);

    private final EntityProjectionConfig config;
    private final Attributes metricAttributes;
    private final LongCounter totalEntities;
    private final LongCounter totalErrors;
    private final LongHistogram entitiesPerGraph;
    private final LongHistogram graphSizes;

    /**
     * Creates a new projector
     *
     * @param config Configuration that controls how the entity-centric representations are created
     */
    public EntityCentricProjector(EntityProjectionConfig config) {
        Objects.requireNonNull(config, "Configuration cannot be null");

        this.config = config;
        this.metricAttributes =
                Attributes.of(AttributeKey.stringKey(AttributeNames.INSTANCE_ID), UUID.randomUUID().toString());
        Meter meter = TelicentMetrics.getMeter(Library.NAME, LibraryVersion.get(Library.NAME));
        //@formatter:off
        this.totalEntities
                = meter.counterBuilder(EntityMetricNames.PROJECTION_ENTITIES_TOTAL)
                       .setDescription(EntityMetricNames.PROJECTED_ENTITIES_TOTAL_DESCRIPTION)
                       .build();
        this.totalErrors
                = meter.counterBuilder(EntityMetricNames.PROJECTION_ENTITIES_ERRORS)
                       .setDescription(EntityMetricNames.PROJECTION_ENTITIES_ERROR_DESCRIPTION)
                       .build();
        //@formatter:on
        this.graphSizes = meter.histogramBuilder(EntityMetricNames.PROJECTION_ENTITIES_GRAPH_SIZES)
                               .setDescription(EntityMetricNames.PROJECTION_ENTITIES_GRAPH_SIZES_DESCRIPTION)
                               .ofLongs()
                               .build();
        this.entitiesPerGraph = meter.histogramBuilder(EntityMetricNames.PROJECTION_ENTITIES_PER_GRAPH)
                                     .setDescription(EntityMetricNames.PROJECTION_ENTITIES_PER_GRAPH_DESCRIPTION)
                                     .ofLongs()
                                     .build();

    }

    @Override
    public void project(Event<TKey, RdfPayload> event, Sink<Event<TKey, Entity>> sink) {
        // Prepare Security Labels
        String defaultSecurityLabels = event.lastHeader(TelicentHeaders.SECURITY_LABEL);

        if (event.value().isDataset()) {
            // Select the relevant entities
            DatasetGraph dataset = event.value().getDataset();
            projectForDataset(event, sink, false, defaultSecurityLabels, dataset);
        } else {
            // Convert into a sequence of DatasetGraph instances each of which consists only of additions or deletions
            RDFPatch patch = event.value().getPatch();
            RDFChangesAsDatasets changesAsDatasets = new RDFChangesAsDatasets();
            changesAsDatasets.start();
            patch.apply(changesAsDatasets);
            changesAsDatasets.finish();
            Graph securityLabels = changesAsDatasets.getSecurityLabels();

            changesAsDatasets.getOperations().forEach(op -> {
                // We track the Security Labels graph for the patch separately from the rest of the patch changes so
                // need to make sure that we copy that into each dataset we're going to process
                op.getValue().addGraph(Telicent.SECURITY_LABELS_GRAPH_URI, securityLabels);
                projectForDataset(event, sink, op.getKey() == PatchOperation.DELETE, defaultSecurityLabels,
                                  op.getValue());
            });
        }
    }

    private void projectForDataset(Event<TKey, RdfPayload> event, Sink<Event<TKey, Entity>> sink, boolean isDeletion,
                                   String defaultSecurityLabels, DatasetGraph dataset) {
        Graph securityLabels = dataset.getGraph(Telicent.SECURITY_LABELS_GRAPH_URI);
        if (securityLabels != null && securityLabels.isEmpty()) {
            securityLabels = null;
        }
        if (securityLabels != null) {
            // MUST propagate the prefixes from the dataset to the labels graph otherwise LabelsStore can fail to
            // successfully parse the label patterns if they've been defined using prefixed name form
            copyPrefixes(dataset, securityLabels);
        }
        LabelsStore labelsStore = prepareLabelsStore(defaultSecurityLabels, securityLabels);

        // Select out of the default graph (if any)
        Graph defaultGraph = dataset.getDefaultGraph();
        projectForGraph(event, sink, isDeletion, defaultSecurityLabels, securityLabels, labelsStore,
                        Quad.defaultGraphIRI, defaultGraph);

        // Select out of the named graphs (if any)
        Iterator<Node> graphNames = dataset.listGraphNodes();
        while (graphNames.hasNext()) {
            Node graphName = graphNames.next();

            // Don't select entities out of the security label graph itself
            if (Telicent.SECURITY_LABELS_GRAPH_URI.equals(graphName)) {
                continue;
            }

            Graph graph = dataset.getGraph(graphName);
            projectForGraph(event, sink, isDeletion, defaultSecurityLabels, securityLabels, labelsStore, graphName,
                            graph);
        }
    }

    /**
     * Copy prefix definitions from the dataset level to the security labels graph level respecting any pre-existing
     * prefixes on the graph level since those take precedence
     *
     * @param dataset        Dataset
     * @param securityLabels Security Labels Graph
     */
    protected final void copyPrefixes(DatasetGraph dataset, Graph securityLabels) {
        Map<String, String> graphPrefixes = securityLabels.getPrefixMapping().getNsPrefixMap();
        dataset.prefixes()
               .getMapping()
               .keySet()
               .forEach(prefix -> graphPrefixes.computeIfAbsent(prefix, k -> dataset.prefixes().get(k)));
        securityLabels.getPrefixMapping().setNsPrefixes(graphPrefixes);
    }

    /**
     * Prepares a label store for use in determining what labels apply to each entity and collected entity data
     *
     * @param defaultSecurityLabels Default security label
     * @param securityLabels        Security labels
     * @return Prepared labels store
     */
    public static LabelsStore prepareLabelsStore(String defaultSecurityLabels, Graph securityLabels) {
        if (securityLabels != null || StringUtils.isNotBlank(defaultSecurityLabels)) {
            LabelsStore labelsStore =
                    Labels.createLabelsStoreMem(securityLabels != null ? securityLabels : Graph.emptyGraph);
            if (StringUtils.isNotBlank(defaultSecurityLabels)) {
                return new DefaultingLabelsStore(labelsStore, defaultSecurityLabels);
            } else {
                return labelsStore;
            }
        } else {
            return null;
        }
    }

    /**
     * Projects the entities for a given graph to the sink
     *
     * @param event                 Event
     * @param sink                  Sink
     * @param defaultSecurityLabels Default security labels for the event
     * @param securityLabels        Security Labels graph for the event
     * @param labelsStore           Labels store for the event
     * @param graphName             Graph Name for the graph being projected from
     * @param graph                 Graph being projected from
     */
    private void projectForGraph(Event<TKey, RdfPayload> event, Sink<Event<TKey, Entity>> sink, boolean isDeletion,
                                 String defaultSecurityLabels, Graph securityLabels, LabelsStore labelsStore,
                                 Node graphName, Graph graph) {
        if (graph == null) {
            return;
        }


        Stream<Entity> entities = this.config.getSelector().select(graph, defaultSecurityLabels, securityLabels);
        AtomicLong count = new AtomicLong(0L);
        entities.forEach(e -> {
            LOGGER.trace("Processing discovered entity {}", e.getUri());

            // Convert to a deletion if necessary
            final Entity output;
            if (isDeletion) {
                output = e.asDeletion();
            } else {
                output = e;
            }

            // Apply entity data collectors
            this.config.getCollectors()
                       .forEach(x -> x.collect(graph, output.getUri(), labelsStore)
                                      .forEach(d -> output.addData(x.getGroup(), d)));

            // Output to sink
            try {
                sink.send(event.replaceValue(output));
            } catch (SinkException ex) {
                LOGGER.warn("Sink failed to accept entity {} - {}", output.getUri(), ex.getMessage());
                this.totalErrors.add(1, this.metricAttributes);
            }
            count.getAndIncrement();
        });

        int graphSize = graph.size();
        long entityCount = count.get();
        LOGGER.debug("Graph {} with {} triples produced {} entities",
                     graphName != null ? graphName.getURI() : Quad.defaultGraphIRI, graphSize, entityCount);
        this.totalEntities.add(entityCount, this.metricAttributes);
        this.graphSizes.record(graphSize, this.metricAttributes);
        this.entitiesPerGraph.record(entityCount, this.metricAttributes);
    }
}
