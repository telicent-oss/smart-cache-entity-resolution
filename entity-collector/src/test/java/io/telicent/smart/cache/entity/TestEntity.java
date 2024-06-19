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

import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class TestEntity extends AbstractEntityCollectorTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void entity_bad_01() {
        new Entity(null, null);
    }

    @Test
    public void entity_01() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Assert.assertFalse(entity.hasPrefixes());
        Assert.assertNull(entity.getPrefixes());
        Assert.assertFalse(entity.hasAnyData());
        Assert.assertFalse(entity.hasData("test"));
        Assert.assertFalse(entity.hasAnyLiterals());
    }

    @Test
    public void entity_02() {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), prefixes);
        Assert.assertTrue(entity.hasPrefixes());
        Assert.assertNotNull(entity.getPrefixes());
        Assert.assertTrue(entity.getPrefixes().hasNoMappings());
        Assert.assertFalse(entity.hasAnyData());
        Assert.assertFalse(entity.hasData("test"));
        Assert.assertFalse(entity.hasAnyLiterals());
    }

    @Test
    public void entity_03() {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefix("rdf", RDF.uri);
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), prefixes);
        Assert.assertTrue(entity.hasPrefixes());
        Assert.assertNotNull(entity.getPrefixes());
        Assert.assertFalse(entity.getPrefixes().hasNoMappings());
        Assert.assertEquals(entity.getPrefixes().getNsPrefixURI("rdf"), RDF.uri);
        Assert.assertFalse(entity.hasAnyData());
        Assert.assertFalse(entity.hasData("test"));
        List<EntityData> data = entity.getData("test").toList();
        Assert.assertEquals(data.size(), 0);
        Assert.assertFalse(entity.hasAnyLiterals());
    }

    @Test
    public void entity_04() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Assert.assertFalse(entity.hasPrefixes());
        Assert.assertNull(entity.getPrefixes());
        Assert.assertFalse(entity.hasAnyData());
        Assert.assertFalse(entity.hasData(Rdf.TYPE_GROUP));
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, PERSON_TYPE));
        Assert.assertTrue(entity.hasData(Rdf.TYPE_GROUP));
        Assert.assertTrue(entity.hasAnyData());
        List<EntityData> data = entity.getData(Rdf.TYPE_GROUP).toList();
        Assert.assertEquals(data.size(), 1);
        Assert.assertEquals(data.get(0).get().getNode(), PERSON_TYPE);
        Assert.assertFalse(entity.hasAnyLiterals());
    }

    @Test
    public void entity_05() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Assert.assertFalse(entity.hasPrefixes());
        Assert.assertNull(entity.getPrefixes());
        Assert.assertFalse(entity.hasAnyData());
        Assert.assertFalse(entity.hasData(Rdf.TYPE_GROUP));
        Node name = NodeFactory.createLiteral("Fred");
        entity.addData(DefaultOutputFields.LITERALS, EntityData.of(FOAF.name.asNode(), name));
        Assert.assertTrue(entity.hasData(DefaultOutputFields.LITERALS));
        Assert.assertTrue(entity.hasAnyData());
        List<EntityData> data = entity.getData(DefaultOutputFields.LITERALS).toList();
        Assert.assertEquals(data.size(), 1);
        Assert.assertEquals(data.get(0).get().getNode(), name);
        Assert.assertTrue(entity.hasAnyLiterals());
    }

    @Test
    public void entity_06() {
        Entity e = new Entity(NodeFactory.createURI(FRED_URI), null);
        Assert.assertFalse(e.isDeletion());

        Entity deletion = e.asDeletion();
        Assert.assertTrue(deletion.isDeletion());

        Entity deletion2 = deletion.asDeletion();
        Assert.assertSame(deletion, deletion2);
    }
}
