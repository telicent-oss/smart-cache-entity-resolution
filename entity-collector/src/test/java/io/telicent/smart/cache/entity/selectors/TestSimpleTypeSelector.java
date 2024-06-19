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
package io.telicent.smart.cache.entity.selectors;

import io.telicent.smart.cache.entity.AbstractEntityCollectorTests;
import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import io.telicent.smart.cache.entity.vocabulary.Telicent;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.RDF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestSimpleTypeSelector extends AbstractEntityCollectorTests {

    @Test
    public void simple_types_all() {
        SimpleTypeSelector selector =
                new SimpleTypeSelector(true, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 23);
    }

    @Test
    public void simple_types_all_with_ignored_01() {
        List<Node> ignored = Collections.singletonList(PARTICULAR_PERIOD_TYPE);
        SimpleTypeSelector selector =
                new SimpleTypeSelector(true, Collections.emptyList(), ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 19);
    }

    @Test
    public void simple_types_all_with_ignored_02() {
        List<Node> ignored = Collections.singletonList(NodeFactory.createURI(DATA_NAMESPACE + "HospitalBed"));
        SimpleTypeSelector selector =
                new SimpleTypeSelector(true, Collections.emptyList(), ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 22);
    }

    @Test
    public void simple_types_all_with_ignored_03() {
        List<Node> ignored = Collections.singletonList(NodeFactory.createURI(IES_NAMESPACE + "PartOfFacility"));
        SimpleTypeSelector selector =
                new SimpleTypeSelector(true, Collections.emptyList(), ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 20);
    }

    @Test
    public void simple_types_included_01() {
        List<Node> included = Collections.singletonList(PERSON_TYPE);
        List<Node> ignored = Collections.singletonList(PARTICULAR_PERIOD_TYPE);
        SimpleTypeSelector selector =
                new SimpleTypeSelector(false, included, ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 1);
    }

    @Test
    public void simple_types_included_02() {
        List<Node> included = Arrays.asList(PERSON_TYPE, PARTICULAR_PERIOD_TYPE, EVENT_PARTICIPANT_TYPE);
        List<Node> ignored = Collections.singletonList(PARTICULAR_PERIOD_TYPE);
        SimpleTypeSelector selector =
                new SimpleTypeSelector(false, included, ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 2);
    }

    @Test
    public void simple_types_included_and_ignored_01() {
        List<Node> ignored = Collections.singletonList(PARTICULAR_PERIOD_TYPE);
        SimpleTypeSelector selector =
                new SimpleTypeSelector(false, ignored, ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 0);
    }

    @Test
    public void simple_types_included_and_ignored_02() {
        List<Node> included = Collections.singletonList(NodeFactory.createURI(DATA_NAMESPACE + "HospitalBed"));
        List<Node> ignored = Collections.singletonList(NodeFactory.createURI(IES_NAMESPACE + "PartOfFacility"));
        SimpleTypeSelector selector =
                new SimpleTypeSelector(false, included, ignored, Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 0);
    }

    @Test
    public void simple_types_none() {
        SimpleTypeSelector selector =
                new SimpleTypeSelector(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, null).toList();
        Assert.assertEquals(entities.size(), 0);
    }

    @Test
    public void simple_types_security_labelled_01() {
        SimpleTypeSelector selector =
                new SimpleTypeSelector(true, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, "admin,manager", null).toList();
        Assert.assertEquals(entities.size(), 23);
        entities.forEach(e -> {
            Assert.assertTrue(e.hasSecurityLabels());
            Assert.assertNotNull(e.getDefaultSecurityLabels());
            Assert.assertNull(e.getSecurityLabels());
        });
    }

    @Test
    public void simple_types_security_labelled_02() {
        Graph labels = GraphFactory.createDefaultGraph();
        this.testData.stream(Node.ANY, Rdf.TYPE, Node.ANY).forEach(type -> {
            Node rule = NodeFactory.createBlankNode();
            labels.add(rule, Telicent.SECURITY_LABELS_PATTERN, NodeFactory.createLiteralString(
                    "<" + type.getSubject().getURI() + "> <" + RDF.type.getURI() + "> <" + type.getObject()
                                                                                               .getURI() + ">"));
            labels.add(rule, Telicent.SECURITY_LABELS_LABEL, NodeFactory.createLiteralString("admin"));
        });

        SimpleTypeSelector selector =
                new SimpleTypeSelector(true, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        List<Entity> entities = selector.select(this.testData, null, labels).toList();
        Assert.assertEquals(entities.size(), 23);
        entities.forEach(e -> {
            Assert.assertTrue(e.hasSecurityLabels());
            Assert.assertNull(e.getDefaultSecurityLabels());
            Assert.assertNotNull(e.getSecurityLabels());
        });
    }
}
