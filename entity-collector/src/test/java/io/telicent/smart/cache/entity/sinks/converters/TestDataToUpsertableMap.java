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

public class TestDataToUpsertableMap extends AbstractEntityToMapOutputConverterTests {

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_01(UpsertableKeyFormat keyFormat) {
        Entity entity = createFredWithTypes();
        DataToUpsertableMap converter = new DataToUpsertableMap("test", Rdf.TYPE_GROUP, keyFormat, false, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);
        verifyUpsertableMapOutput(keyFormat, false, entity, (Map<String, Object>) map.get("test"), PERSON_TYPE,
                                  EVENT_PARTICIPANT_TYPE);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_02(UpsertableKeyFormat keyFormat) {
        Entity entity = createFredWithTypes();
        DataToUpsertableMap converter = new DataToUpsertableMap("test", Rdf.TYPE_GROUP, keyFormat, true, true);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);
        verifyUpsertableMapOutput(keyFormat, true, entity, (Map<String, Object>) map.get("test"), PERSON_TYPE,
                                  EVENT_PARTICIPANT_TYPE);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_03(UpsertableKeyFormat keyFormat) {
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefix("ies", IES_NAMESPACE);
        Entity entity = createFredWithTypes(prefixes);
        DataToUpsertableMap converter = new DataToUpsertableMap("test", Rdf.TYPE_GROUP, keyFormat, true, true);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        verifyUpsertableMapOutput(keyFormat, true, entity, (Map<String, Object>) map.get("test"), PERSON_TYPE,
                                  EVENT_PARTICIPANT_TYPE);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_04(UpsertableKeyFormat keyFormat) {
        Entity entity = createFredWithNames();
        DataToUpsertableMap converter = new DataToUpsertableMap("test", "names", keyFormat, false, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        verifyUpsertableMapOutput(keyFormat, false, entity, (Map<String, Object>) map.get("test"),
                                  NodeFactory.createLiteral("Fred Test"),
                                  NodeFactory.createLiteral("Frederick A. Test"));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_05(UpsertableKeyFormat keyFormat) {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Node a = NodeFactory.createBlankNode();
        entity.addData("test", EntityData.of(Rdf.TYPE, a));
        Node b = NodeFactory.createBlankNode();
        entity.addData("test", EntityData.of(Rdf.TYPE, b));
        DataToUpsertableMap converter = new DataToUpsertableMap("test", "test", keyFormat, false, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        verifyUpsertableMapOutput(keyFormat, false, entity, (Map<String, Object>) map.get("test"), a, b);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_07(UpsertableKeyFormat keyFormat) {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        Node a = NodeFactory.createVariable("a");
        entity.addData("test", EntityData.of(Rdf.TYPE, a));
        Node b = NodeFactory.createVariable("b");
        entity.addData("test", EntityData.of(Rdf.TYPE, b));
        DataToUpsertableMap converter = new DataToUpsertableMap("test", "test", keyFormat, false, false);
        verifyConverterHasNoOutput(converter, entity);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_08(UpsertableKeyFormat keyFormat) {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                     .map(SecurityLabelledNode::new)));
        DataToUpsertableMap converter = new DataToUpsertableMap("test", Rdf.TYPE_GROUP, keyFormat, false, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        verifyUpsertableMapOutput(keyFormat, true, entity, (Map<String, Object>) map.get("test"), PERSON_TYPE,
                                  EVENT_PARTICIPANT_TYPE);
    }

    @Test(dataProvider = "keyFormats", expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*containing a single key")
    public void entity_data_converter_upsertable_map_09(UpsertableKeyFormat keyFormat) {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.create()
                                                 .add(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                      .map(SecurityLabelledNode::new))
                                                 .add(FOAF.name.asNode(), NodeFactory.createLiteral("Fred"))
                                                 .build());

        DataToUpsertableMap converter = new DataToUpsertableMap("test", Rdf.TYPE_GROUP, keyFormat, false, false);
        verifyConverterProducesOutput(converter, entity);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_10(UpsertableKeyFormat keyFormat) {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        entity.addData(Rdf.TYPE_GROUP, EntityData.of(Rdf.TYPE, Stream.of(PERSON_TYPE, EVENT_PARTICIPANT_TYPE)
                                                                     .map(SecurityLabelledNode::new)));
        // No such group on the entity so expect no output
        DataToUpsertableMap converter = new DataToUpsertableMap("test", "foo", keyFormat, false, false);
        verifyConverterHasNoOutput(converter, entity);
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_11(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", Rdf.TYPE_GROUP, keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Assert.assertTrue(output.containsKey("test"));
        Assert.assertFalse(output.containsKey(DefaultOutputFields.SECURITY_LABELS));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_12(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "names", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Assert.assertTrue(output.containsKey("test"));
        Assert.assertFalse(output.containsKey(DefaultOutputFields.SECURITY_LABELS));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_13(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "age", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Map<String, Object> securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels, List.of(FRED_AGE),
                                                  List.of("gdpr"));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_14(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "phone", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Map<String, Object> securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels, List.of(FRED_PHONE),
                                                  List.of("public"));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_15(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "age", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Map<String, Object> securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels, List.of(FRED_AGE),
                                                  List.of("gdpr"));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_upsertable_map_16(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "email", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Map<String, Object> securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels,
                                                  List.of(FRED_PUBLIC_EMAIL, FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL),
                                                  List.of("", "work", "private"));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_list_18(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "email", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Map<String, Object> securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels,
                                                  List.of(FRED_PUBLIC_EMAIL, FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL),
                                                  List.of("", "work", "private"));

        // Should merge with existing output so there's effectively no change
        converter.output(entity, output);
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels,
                                                  List.of(FRED_PUBLIC_EMAIL, FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL),
                                                  List.of("", "work", "private"));
    }

    @Test(dataProvider = "keyFormats")
    public void entity_data_converter_list_19(UpsertableKeyFormat keyFormat) {
        Entity entity = createSecureFred(null);

        DataToUpsertableMap converter = new DataToUpsertableMap("test", "email", keyFormat, false, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        Map<String, Object> securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifyUpsertableMapOutput(keyFormat, false, entity, (Map<String, Object>) output.get("test"), FRED_PUBLIC_EMAIL,
                                  FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL);
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels,
                                                  List.of(FRED_PUBLIC_EMAIL, FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL),
                                                  List.of("", "work", "private"));

        // Should merge with existing output so there's effectively no change
        converter = new DataToUpsertableMap("test", "age", keyFormat, false, false);
        converter.output(entity, output);
        verifyUpsertableMapOutput(keyFormat, false, entity, (Map<String, Object>) output.get("test"), FRED_PUBLIC_EMAIL,
                                  FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL, FRED_AGE);
        securityLabels = verifyUpsertableMapOutputHasSecurityLabels(output, "test");
        verifySecurityLabelledUpsertableMapOutput(keyFormat, false, entity, securityLabels,
                                                  List.of(FRED_PUBLIC_EMAIL, FRED_WORK_EMAIL, FRED_PRIVATE_EMAIL,
                                                          FRED_AGE), List.of("", "work", "private", "gdpr"));
    }
}
