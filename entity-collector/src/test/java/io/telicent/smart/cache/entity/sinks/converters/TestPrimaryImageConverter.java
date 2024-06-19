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

public class TestPrimaryImageConverter extends AbstractEntityToMapOutputConverterTests {

    protected void addPrimaryImage(Entity entity, Node imageId, String securityLabels) {
        entity.addData(DefaultOutputFields.LITERALS, EntityData.of(Telicent.PRIMARY_IMAGE, imageId, securityLabels));
    }

    @Test
    public void primary_image_converter_01() {
        Entity fred = createSecureFred(null);
        PrimaryImageConverter converter = new PrimaryImageConverter();
        // No primary image in the generated entity
        verifyConverterHasNoOutput(converter, fred);
    }

    @Test
    public void primary_image_converter_02() {
        Entity fred = createSecureFred(null);
        addPrimaryImage(fred, FRED_IMAGE_ID, null);
        PrimaryImageConverter converter = new PrimaryImageConverter();
        // Primary image added to generated entity
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.PRIMARY_IMAGE));
        Assert.assertEquals(output.get(DefaultOutputFields.PRIMARY_IMAGE), FRED_IMAGE_ID.getLiteralLexicalForm());
    }

    @Test
    public void primary_image_converter_03() {
        Entity fred = createSecureFred(null);
        PrimaryImageConverter converter = new PrimaryImageConverter();
        // Add an unsupported primary image i.e. a non-literal
        addPrimaryImage(fred, PERSON_TYPE, null);
        verifyConverterHasNoOutput(converter, fred);
    }

    @Test
    public void primary_image_converter_04() {
        Entity fred = createSecureFred(null);
        PrimaryImageConverter converter = new PrimaryImageConverter();
        // Add an unsupported primary image i.e. a non-literal
        addPrimaryImage(fred, PERSON_TYPE, null);
        // Add a supported primary image
        addPrimaryImage(fred, FRED_IMAGE_ID, null);

        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.PRIMARY_IMAGE));
        Assert.assertEquals(output.get(DefaultOutputFields.PRIMARY_IMAGE), FRED_IMAGE_ID.getLiteralLexicalForm());
    }

    @Test
    public void primary_image_converter_05() {
        Entity fred = createSecureFred(null);
        addPrimaryImage(fred, FRED_IMAGE_ID, "public");
        PrimaryImageConverter converter = new PrimaryImageConverter();
        // Primary image added to generated entity
        Map<String, Object> output = verifyConverterProducesOutput(converter, fred);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.PRIMARY_IMAGE));
        Assert.assertEquals(output.get(DefaultOutputFields.PRIMARY_IMAGE), FRED_IMAGE_ID.getLiteralLexicalForm());
        Assert.assertEquals(((Map<String, Object>) output.get(DefaultOutputFields.SECURITY_LABELS)).get(
                DefaultOutputFields.PRIMARY_IMAGE), "public");
    }

}
