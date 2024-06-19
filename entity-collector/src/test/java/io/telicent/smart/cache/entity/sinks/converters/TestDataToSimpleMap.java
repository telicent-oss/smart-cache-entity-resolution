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
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestDataToSimpleMap extends AbstractEntityToMapOutputConverterTests {

    @Test
    public void entity_data_converter_map_01() {
        Entity entity = createFredWithTypes();
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, Rdf.TYPE_GROUP);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map);
        Assert.assertTrue(data.containsKey(RDF.type.getURI()));
        List<String> types = data.get(RDF.type.getURI());
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        verifyTypes(types);
    }

    @Test
    public void entity_data_converter_map_02() {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefix("ies", IES_NAMESPACE);
        Entity entity = createFredWithTypes(prefixes);
        DataToSimpleMap converter = new DataToSimpleMap("test", false, true, Rdf.TYPE_GROUP);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map);
        Assert.assertTrue(data.containsKey(RDF.type.getURI()));
        List<String> types = data.get(RDF.type.getURI());
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        verifyCompactedTypes(types);
    }

    @Test
    public void entity_data_converter_map_03() {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefix("rdf", RDF.uri);
        prefixes.setNsPrefix("ies", IES_NAMESPACE);
        Entity entity = createFredWithTypes(prefixes);
        DataToSimpleMap converter = new DataToSimpleMap("test", true, true, Rdf.TYPE_GROUP);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map);
        Assert.assertTrue(data.containsKey("rdf:type"));
        List<String> types = data.get("rdf:type");
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        verifyCompactedTypes(types);
    }

    @Test
    public void entity_data_converter_map_04() {
        Entity entity = createFredWithTypes();
        DataToSimpleMap converter = new DataToSimpleMap("test", true, true, Rdf.TYPE_GROUP);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map);
        Assert.assertTrue(data.containsKey(RDF.type.getURI()));
        List<String> types = data.get(RDF.type.getURI());
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        verifyTypes(types);
    }

    @Test
    public void entity_data_converter_map_05() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(NodeFactory.createLiteral("a"), PERSON_TYPE));
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(NodeFactory.createLiteral("a"), EVENT_PARTICIPANT_TYPE));
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, Rdf.TYPE_GROUP);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map);
        Assert.assertTrue(data.containsKey("a"));
        List<String> types = data.get("a");
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        verifyTypes(types);
    }

    @Test
    public void entity_data_converter_map_06() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP,
                       EntityData.of(NodeFactory.createLiteral("a"), PERSON_TYPE));
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(NodeFactory.createLiteral("a"),
                                                     EVENT_PARTICIPANT_TYPE));
        // Group has no data so should be no output
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, "foo");
        verifyConverterHasNoOutput(converter, entity);
    }

    @Test
    public void entity_data_converter_map_07() {
        Entity entity = createSecureFred(null);
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, Rdf.TYPE_GROUP);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map);
        Assert.assertTrue(data.containsKey(RDF.type.getURI()));
        List<String> types = data.get(RDF.type.getURI());
        Assert.assertNotNull(types);
        Assert.assertEquals(types.size(), 2);
        verifyTypes(types);
    }

    @Test
    public void entity_data_converter_map_08() {
        Entity entity = createSecureFred(null);
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, "age");
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map, 2);
        Assert.assertTrue(data.containsKey(FOAF.age.getURI()));
        List<String> ages = data.get(FOAF.age.getURI());
        Assert.assertNotNull(ages);
        Assert.assertEquals(ages.size(), 1);

        //dumpOutput(map);
        verifySecurityLabelledMapOutput(map, Map.of(FOAF.age.getURI(), List.of("gdpr")));
    }

    @Test
    public void entity_data_converter_map_09() {
        Entity entity = createSecureFred(null);
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, "email");
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map, 2);
        Assert.assertTrue(data.containsKey(FOAF.mbox.getURI()));
        List<String> emails = data.get(FOAF.mbox.getURI());
        Assert.assertNotNull(emails);
        Assert.assertEquals(emails.size(), 3);

        //dumpOutput(map);
        verifySecurityLabelledMapOutput(map, Map.of(FOAF.mbox.getURI(), List.of("", "work", "private")));
    }

    @Test
    public void entity_data_converter_map_10() {
        Entity entity = createSecureFred(null);
        DataToSimpleMap converter = new DataToSimpleMap("test", false, false, "nickname");
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Map<String, List<String>> data = verifyMapOutput(map, 1);
        Assert.assertTrue(data.containsKey(FOAF.nick.getURI()));
        List<String> nicknames = data.get(FOAF.nick.getURI());
        Assert.assertNotNull(nicknames);
        Assert.assertEquals(nicknames.size(), 2);

        //dumpOutput(map);
        verifySecurityLabelledMapOutput(map, Map.of(FOAF.nick.getURI(), Collections.emptyList()));
    }

}
