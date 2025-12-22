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

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TestDefaultingLabelsStore extends AbstractEntityCollectorTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void defaulting_bad_01() {
        new DefaultingLabelsStore(null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void defaulting_bad_02() {
        new DefaultingLabelsStore(Labels.emptyStore(), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void defaulting_bad_03() {
        new DefaultingLabelsStore(Labels.emptyStore(), "");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void defaulting_bad_04() {
        new DefaultingLabelsStore(Labels.emptyStore(), "    ");
    }

    private Graph testLabels() {
        Graph graph = GraphFactory.createDefaultGraph();
        GraphUtil.addInto(graph, this.testLabels);
        return graph;
    }

    @Test
    public void defaulting_01() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", testLabels());
        Assert.assertNotNull(store);
        Triple t = Triple.create(NodeFactory.createURI(FRED_URI), Rdf.TYPE, PERSON_TYPE);
        verifyLabels(store, t, List.of("default"));

        store.add(t, Label.fromText("foo"));

        verifyLabels(store, t, List.of("foo"));

        store.add(t, List.of(Label.fromText("foo"), Label.fromText("bar")));
        verifyLabels(store, t, List.of("bar", "foo"));
    }

    @Test
    public void defaulting_01a() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", GraphFactory.createDefaultGraph());
        Assert.assertNotNull(store);
        Assert.assertTrue(store.isEmpty());
        store.addGraph(this.testLabels);

        Assert.assertFalse(store.isEmpty());

        Triple t = Triple.create(NodeFactory.createURI(FRED_URI), Rdf.TYPE, PERSON_TYPE);
        verifyLabels(store, t, List.of("default"));
    }

    @Test
    public void defaulting_01b() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", testLabels());
        Assert.assertNotNull(store);
        Triple t = Triple.create(NodeFactory.createURI(FRED_URI), Rdf.TYPE, PERSON_TYPE);
        verifyLabels(store, t, List.of("default"));

        store.add(t.getSubject(), t.getPredicate(), t.getObject(), Label.fromText("foo"));
        verifyLabels(store, t, List.of("foo"));

        store.add(t.getSubject(), t.getPredicate(), t.getObject(), List.of(Label.fromText("foo"), Label.fromText("bar")));
        verifyLabels(store, t, List.of("bar", "foo"));
    }

    @Test
    public void defaulting_02() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", testLabels());
        Assert.assertNotNull(store);
        Triple t = Triple.create(NodeFactory.createURI(FRED_URI), FOAF.name.asNode(), FRED_FULL_NAME);

        verifyLabels(store, t, List.of("public"));
    }

    @Test
    public void defaulting_03() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", testLabels());
        Assert.assertNotNull(store);
        Triple t = Triple.create(NodeFactory.createURI(FRED_URI), FOAF.age.asNode(), FRED_AGE);

        verifyLabels(store, t, List.of("gdpr"));
    }

    @Test
    public void defaulting_04() {
        Graph labels = testLabels();
        RDFDataMgr.write(System.out, labels, Lang.TTL);
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", labels);
        Assert.assertNotNull(store);
        RDFDataMgr.write(System.out, store.getGraph(), Lang.TTL);
        Assert.assertFalse(labels.isIsomorphicWith(store.getGraph()));
    }

    @Test(enabled = false)
    public void givenALabelStore_whenPropertiesIsInvoked_thenTheDelegatePropertiesAreReturned() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", testLabels());
        Assert.assertNotNull(store);
        Assert.assertNotNull(store.getProperties());
        Assert.assertEquals(store.getProperties().get("size"), String.valueOf(testLabels().size()));
    }

    @Test
    public void givenALabelStore_whenTransactionalIsInvoked_thenTheTransactionAttributeReturned() {
        LabelsStore store = EntityCentricProjector.prepareLabelsStore("default", testLabels());
        Assert.assertNotNull(store);
        Assert.assertNotNull(store.getTransactional());
    }

    private static void verifyLabels(LabelsStore store, Triple t, List<String> expected) {
        List<Label> expectedLabels = expected.stream().map(Label::fromText).toList();
        verifyLabelList(store, t, expectedLabels);
    }

    private static void verifyLabelList(LabelsStore store, Triple t, List<Label> expected) {
        // NB - No guarantee what order the underlying LabelsStore returns the labels in so sort them into lexical order
        //      for reliable comparison
        List<Label> labels = new ArrayList<>(store.labelsForTriples(t));
        labels.sort(Comparator.comparing(Label::getText));
        Assert.assertFalse(labels.isEmpty());
        Assert.assertEquals(labels, expected);
    }
}
