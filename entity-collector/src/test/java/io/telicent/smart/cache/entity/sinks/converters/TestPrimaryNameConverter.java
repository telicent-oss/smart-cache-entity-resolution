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
import io.telicent.smart.cache.entity.vocabulary.Telicent;
import org.apache.jena.graph.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class TestPrimaryNameConverter extends AbstractEntityToMapOutputConverterTests {

    protected void addPrimaryName(Entity entity, Node primaryName, String securityLabels) {
        entity.addData(DefaultOutputFields.LITERALS, EntityData.of(Telicent.PRIMARY_NAME, primaryName, securityLabels));
    }

    @Test
    public void primary_name_converter_01() {
        Entity fred = createSecureFred(null);
        PrimaryNameConverter converter = new PrimaryNameConverter();
        // No primary name in the generated entity
        verifyConverterHasNoOutput(converter, fred);
    }

    @Test
    public void primary_name_converter_02() {
        Entity fred = createSecureFred(null);
        addPrimaryName(fred, FRED_FULL_NAME, null);
        PrimaryNameConverter converter = new PrimaryNameConverter();
        // Primary Name added to generated entity
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.PRIMARY_NAME));
        Assert.assertEquals(output.get(DefaultOutputFields.PRIMARY_NAME), FRED_FULL_NAME.getLiteralLexicalForm());
    }

    @Test
    public void primary_name_converter_03() {
        Entity fred = createSecureFred(null);
        PrimaryNameConverter converter = new PrimaryNameConverter();
        // Add an unsupported primary name i.e. a non-literal
        addPrimaryName(fred, PERSON_TYPE, null);
        verifyConverterHasNoOutput(converter, fred);
    }

    @Test
    public void primary_name_converter_04() {
        Entity fred = createSecureFred(null);
        PrimaryNameConverter converter = new PrimaryNameConverter();
        // Add an unsupported primary name i.e. a non-literal
        addPrimaryName(fred, PERSON_TYPE, null);
        // Add a supported primary name
        addPrimaryName(fred, FRED_FULL_NAME, null);

        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.PRIMARY_NAME));
        Assert.assertEquals(output.get(DefaultOutputFields.PRIMARY_NAME), FRED_FULL_NAME.getLiteralLexicalForm());
    }

    @Test
    public void primary_name_converter_05() {
        Entity fred = createSecureFred(null);
        addPrimaryName(fred, FRED_FULL_NAME, "public");
        PrimaryNameConverter converter = new PrimaryNameConverter();
        // Primary Name added to generated entity
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.PRIMARY_NAME));
        Assert.assertEquals(output.get(DefaultOutputFields.PRIMARY_NAME), FRED_FULL_NAME.getLiteralLexicalForm());
        Assert.assertEquals(((Map<String, Object>) output.get(DefaultOutputFields.SECURITY_LABELS)).get(
                DefaultOutputFields.PRIMARY_NAME), "public");
    }

}
