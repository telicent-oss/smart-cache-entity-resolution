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
package io.telicent.smart.cache.entity.sinks.converters;

import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.EntityData;
import io.telicent.smart.cache.entity.SecurityLabelledNode;
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TestDataToSimpleList extends AbstractEntityToMapOutputConverterTests {

    @Test
    public void entity_data_converter_list_01() {
        Entity entity = createFredWithTypes();
        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        verifyTypes(data);
    }

    @Test
    public void entity_data_converter_list_02() {
        Entity entity = createFredWithTypes();
        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, true);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        verifyTypes(data);
    }

    @Test
    public void entity_data_converter_list_03() {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefix("ies", IES_NAMESPACE);
        Entity entity = createFredWithTypes(prefixes);
        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, true);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        verifyCompactedTypes(data);
    }

    @Test
    public void entity_data_converter_list_04() {
        Entity entity = createFredWithNames();
        DataToSimpleList converter = new DataToSimpleList("test", "names", false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        Assert.assertEquals(data.get(0), "Fred Test");
        Assert.assertEquals(data.get(1), "Frederick A. Test");
    }

    @Test
    public void entity_data_converter_list_05() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Node a = NodeFactory.createBlankNode();
        entity.addData("test", EntityData.of(Rdf.TYPE, a));
        Node b = NodeFactory.createBlankNode();
        entity.addData("test", EntityData.of(Rdf.TYPE, b));
        DataToSimpleList converter = new DataToSimpleList("test", "test", false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        Assert.assertEquals(data.get(0), a.getBlankNodeLabel());
        Assert.assertEquals(data.get(1), b.getBlankNodeLabel());
    }

    @Test
    public void entity_data_converter_list_07() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Node a = NodeFactory.createVariable("a");
        entity.addData("test", EntityData.of(Rdf.TYPE, a));
        Node b = NodeFactory.createVariable("b");
        entity.addData("test", EntityData.of(Rdf.TYPE, b));
        DataToSimpleList converter = new DataToSimpleList("test", "test", false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        Assert.assertEquals(data.get(0), null);
        Assert.assertEquals(data.get(1), null);
    }

    @Test
    public void entity_data_converter_list_08() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                     .map(SecurityLabelledNode::new)));
        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        Assert.assertEquals(data.get(0), PERSON_TYPE.getURI());
        Assert.assertEquals(data.get(1), EVENT_PARTICIPANT_TYPE.getURI());
    }

    @Test
    public void entity_data_converter_list_08b() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                     .map(n -> new SecurityLabelledNode(n, n.equals(
                                                                             PERSON_TYPE) ? "public" : null))));
        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        List<String> data = verifyListOutput(map);
        Assert.assertEquals(data.get(0), PERSON_TYPE.getURI());
        Assert.assertEquals(data.get(1), EVENT_PARTICIPANT_TYPE.getURI());
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*containing a single key")
    public void entity_data_converter_list_09() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.create()
                                                 .add(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                      .map(SecurityLabelledNode::new))
                                                 .add(FOAF.name.asNode(), NodeFactory.createLiteral("Fred"))
                                                 .build());

        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, false);
        verifyConverterProducesOutput(converter, entity);
    }

    @Test
    public void entity_data_converter_list_10() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                     .map(SecurityLabelledNode::new)));
        // No such group on the entity so expect no output
        DataToSimpleList converter = new DataToSimpleList("test", "foo", false);
        verifyConverterHasNoOutput(converter, entity);
    }

    @Test
    public void entity_data_converter_list_11() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", Rdf.TYPE_GROUP, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Assert.assertTrue(output.containsKey("test"));
        Assert.assertFalse(output.containsKey(DefaultOutputFields.SECURITY_LABELS));
    }

    @Test
    public void entity_data_converter_list_12() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", "names", false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Assert.assertTrue(output.containsKey("test"));
        Assert.assertFalse(output.containsKey(DefaultOutputFields.SECURITY_LABELS));
    }

    @Test
    public void entity_data_converter_list_13() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", "age", false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabelledListOutput(output, "gdpr");
    }

    @Test
    public void entity_data_converter_list_14() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", "phone", false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabelledListOutput(output, "public");
    }

    @Test
    public void entity_data_converter_list_15() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", "age", false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabelledListOutput(output, "gdpr");
    }

    @Test
    public void entity_data_converter_list_16() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", "email", false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabelledListOutput(output, List.of("", "work", "private"));
    }

    @Test
    public void entity_data_converter_list_17() {
        Entity entity = createSecureFred(null);

        DataToSimpleList converter = new DataToSimpleList("test", "email", false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabelledListOutput(output, List.of("", "work", "private"));

        // Should append to existing output
        converter.output(entity, output);
        Assert.assertEquals(((List<String>) output.get("test")).size(), 6);
        verifySecurityLabelledListOutput(output, List.of("", "work", "private", "", "work", "private"));
    }
}
