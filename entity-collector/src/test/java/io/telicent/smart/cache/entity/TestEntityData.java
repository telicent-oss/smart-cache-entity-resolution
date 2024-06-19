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

import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TestEntityData extends AbstractEntityCollectorTests {

    @Test
    public void entity_data_empty_01() {
        EntityData data = EntityData.create().build();
        Assert.assertTrue(data.isEmpty());
        Assert.assertFalse(data.isSimple());
        Assert.assertNull(data.get());
        Assert.assertNull(data.key());
    }

    @Test
    public void entity_data_simple_01() {
        EntityData data = EntityData.of(Rdf.TYPE, PERSON_TYPE);
        Assert.assertFalse(data.isEmpty());
        Assert.assertTrue(data.isSimple());
        Assert.assertEquals(data.key(), Rdf.TYPE);
        Assert.assertEquals(data.get().getNode(), PERSON_TYPE);
    }

    @Test
    public void entity_data_complex_01() {
        EntityData data = createMultiTyped();
        Assert.assertFalse(data.isEmpty());
        Assert.assertFalse(data.isSimple());
        Assert.assertEquals(data.key(), Rdf.TYPE);
        List<Node> types = data.get(Rdf.TYPE).stream().map(SecurityLabelledNode::getNode).toList();
        Assert.assertEquals(types.size(), 2);
    }

    private EntityData createMultiTyped() {
        return EntityData.of(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                             .map(SecurityLabelledNode::new));
    }

    @Test
    public void entity_data_complex_02() {
        EntityData data = createJaneDoe();
        Assert.assertFalse(data.isEmpty());
        Assert.assertFalse(data.isSimple());
        List<Node> keys = data.keys().toList();
        Assert.assertEquals(keys.size(), 3);

        List<SecurityLabelledNode> ages = data.get(FOAF.age.asNode());
        Assert.assertEquals(ages.size(), 1);
        Assert.assertEquals(ages.get(0).getSecurityLabel(), "gdpr");
    }

    private EntityData createJaneDoe() {
        return EntityData.create()
                         .add(Rdf.TYPE, PERSON_TYPE)
                         .add(FOAF.name.asNode(), NodeFactory.createLiteral("Jane Doe"))
                         .add(FOAF.age.asNode(), NodeFactory.createLiteral("34", XSDDatatype.XSDinteger), "gdpr")
                         .build();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*multiple keys.*")
    public void entity_data_complex_bad_01() {
        EntityData data = createJaneDoe();
        data.get();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*multiple keys.*")
    public void entity_data_complex_bad_02() {
        EntityData data = createJaneDoe();
        data.key();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*multiple values.*")
    public void entity_data_complex_bad_03() {
        EntityData data = createMultiTyped();
        data.get();
    }

    @Test
    public void entity_data_builder_01() {
        // Either key or value being null is not permitted
        EntityData data =
                EntityData.create()
                          .add(null, FRED_FULL_NAME)
                          .add(Rdf.TYPE, (Node) null)
                          .add((Node) null, (Node) null)
                          .add(null, FRED_FULL_NAME, "label")
                          .add(Rdf.TYPE, (Node) null, "label")
                          .add((Node) null, (Node) null, "label")
                          .add(Rdf.TYPE, (SecurityLabelledNode) null)
                          .add(null, (SecurityLabelledNode) null)
                          .add(null, new SecurityLabelledNode(FRED_FULL_NAME),
                               new SecurityLabelledNode(FRED_SHORT_NAME, "public"))
                          .add(Rdf.TYPE, (SecurityLabelledNode) null, null)
                          .add(null, Stream.of(new SecurityLabelledNode(PERSON_TYPE),
                                               new SecurityLabelledNode(EVENT_PARTICIPANT_TYPE)))
                          .add(Rdf.TYPE, new ArrayList<SecurityLabelledNode>(Arrays.asList(null, null)).stream())
                          .build();
        Assert.assertTrue(data.isEmpty());
    }

    @Test
    public void entity_data_builder_02() {
        EntityData data = EntityData.create()
                                    .add(Rdf.TYPE, new SecurityLabelledNode(PERSON_TYPE),
                                         new SecurityLabelledNode(EVENT_PARTICIPANT_TYPE))
                                    .build();
        Assert.assertFalse(data.isEmpty());
        Assert.assertNotNull(data.get(Rdf.TYPE));
    }
}
