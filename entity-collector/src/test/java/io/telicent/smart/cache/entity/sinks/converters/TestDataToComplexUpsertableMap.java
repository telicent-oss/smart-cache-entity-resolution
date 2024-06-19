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
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestDataToComplexUpsertableMap extends AbstractEntityToMapOutputConverterTests {

    @Override
    protected Entity createSecureFred(PrefixMapping prefixes) {
        Entity fred = super.createSecureFred(prefixes);

        // Add an uber-group with everything in it
        Set<String> groups = fred.getDataGroups().collect(Collectors.toSet());
        groups.stream().flatMap(fred::getData).forEach(d -> fred.addData("uber", d));
        return fred;
    }

    @Test(dataProvider = "keyFormats")
    public void data_to_complex_upsertable_map_01(UpsertableKeyFormat keyFormat) {
        Entity fred = createSecureFred(null);

        DataToUpsertableComplexMap converter = new DataToUpsertableComplexMap("test", "uber", keyFormat, true, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey("test"));
        Set<String> values = verifyNoListsInOutput(output);
        verifyAllDataWasOutput(values, fred, false, SECURE_FRED_VALUES);
    }

    @Test(dataProvider = "keyFormats")
    public void data_to_complex_upsertable_map_02(UpsertableKeyFormat keyFormat) throws IOException {
        PrefixMapping mapping = PrefixMapping.Factory.create();
        mapping.setNsPrefix("ies", IES_NAMESPACE);
        mapping.setNsPrefix("rdf", RDF.uri);
        mapping.setNsPrefix("foaf", FOAF.NS);
        Entity fred = createSecureFred(mapping);

        DataToUpsertableComplexMap converter = new DataToUpsertableComplexMap("test", "uber", keyFormat, true, false);
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        dumpOutput(output);
        Assert.assertTrue(output.containsKey("test"));
        Set<String> values = verifyNoListsInOutput(output);
        verifyAllDataWasOutput(values, fred, false, SECURE_FRED_VALUES);
    }

    @Test(dataProvider = "keyFormats")
    public void data_to_complex_upsertable_map_03(UpsertableKeyFormat keyFormat) throws IOException {
        PrefixMapping mapping = PrefixMapping.Factory.create();
        mapping.setNsPrefix("ies", IES_NAMESPACE);
        mapping.setNsPrefix("rdf", RDF.uri);
        mapping.setNsPrefix("foaf", FOAF.NS);
        Entity fred = createSecureFred(mapping);

        DataToUpsertableComplexMap converter = new DataToUpsertableComplexMap("test", "uber", keyFormat, true, true);
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        dumpOutput(output);
        Assert.assertTrue(output.containsKey("test"));
        Set<String> values = verifyNoListsInOutput(output);
        verifyAllDataWasOutput(values, fred, true, SECURE_FRED_VALUES);
    }

    @Test(dataProvider = "keyFormats")
    public void data_to_complex_upsertable_map_04(UpsertableKeyFormat keyFormat) {
        Entity fred = createSecureFred(null);
        // Not a data group so expect no output
        DataToUpsertableComplexMap converter = new DataToUpsertableComplexMap("test", "foo", keyFormat, true, false);
        verifyConverterHasNoOutput(converter, fred);
    }

}
