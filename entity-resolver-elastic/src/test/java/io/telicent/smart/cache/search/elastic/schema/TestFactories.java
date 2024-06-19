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
package io.telicent.smart.cache.search.elastic.schema;

import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import io.telicent.smart.cache.search.configuration.CommonFieldTypes;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFactories {

    @Test(dataProvider = "schemaFactoryPropertyTypes")
    public void test_factories_properties(SimpleMappingRule rule) {
        // given
        List<Map<String, DynamicTemplate>> templates = new ArrayList<>();
        Map<String, Property> properties = new HashMap<>();

        // when
        ElasticMappings.ruleToElasticMapping(rule, templates, properties);

        // then
        Assert.assertTrue((templates.isEmpty()));
        Assert.assertFalse(properties.isEmpty());
        Assert.assertTrue(properties.containsKey(rule.getMatchPattern()));
        Property property = properties.get(rule.getMatchPattern());
        Assert.assertEquals(property._kind().toString(), rule.getName());
    }

    @DataProvider(name = "schemaFactoryPropertyTypes")
    protected Object[][] propertyTypes() {
        return new Object[][]{
                {new SimpleMappingRule("Text", "pattern", CommonFieldTypes.ANY)},
                {new SimpleMappingRule("Boolean", "pattern", CommonFieldTypes.BOOLEAN)},
                {new SimpleMappingRule("Date", "pattern", CommonFieldTypes.DATE)},
                {new SimpleMappingRule("Double", "pattern", CommonFieldTypes.DOUBLE)},
                {new SimpleMappingRule("Float", "pattern", CommonFieldTypes.FLOAT)},
                {new SimpleMappingRule("GeoPoint", "pattern", CommonFieldTypes.GEO_POINT)},
                {new SimpleMappingRule("Integer", "pattern", CommonFieldTypes.INTEGER)},
                {new SimpleMappingRule("Keyword", "pattern", CommonFieldTypes.KEYWORD)},
                {new SimpleMappingRule("Long", "pattern", CommonFieldTypes.LONG)},
                {new SimpleMappingRule("Object", "pattern", CommonFieldTypes.NON_INDEXED)},
                {new SimpleMappingRule("Integer", "pattern", CommonFieldTypes.NUMBER)},
                {new SimpleMappingRule("Text", "pattern", CommonFieldTypes.TEXT)},
                {new SimpleMappingRule("Text", "pattern", CommonFieldTypes.URI)},
                };
    }

    @Test(dataProvider = "schemaFactoryTemplateTypes")
    public void test_factories_templates(SimpleMappingRule rule) {
        // given
        List<Map<String, DynamicTemplate>> templates = new ArrayList<>();
        Map<String, Property> properties = new HashMap<>();

        // when
        ElasticMappings.ruleToElasticMapping(rule, templates, properties);

        // then
        Assert.assertTrue((properties.isEmpty()));
        Assert.assertFalse(templates.isEmpty());
        Assert.assertFalse(templates.get(0).isEmpty());
        Assert.assertTrue(templates.get(0).containsKey(rule.getName()));
    }

    @DataProvider(name = "schemaFactoryTemplateTypes")
    protected Object[][] templateTypes() {
        return new Object[][]{
                {new SimpleMappingRule("Any", "*", CommonFieldTypes.ANY)},
                {new SimpleMappingRule("Boolean", "*", CommonFieldTypes.BOOLEAN)},
                {new SimpleMappingRule("Date", "*", CommonFieldTypes.DATE)},
                {new SimpleMappingRule("Double", "*", CommonFieldTypes.DOUBLE)},
                {new SimpleMappingRule("Float", "*", CommonFieldTypes.FLOAT)},
                {new SimpleMappingRule("geo-point", "*", CommonFieldTypes.GEO_POINT)},
                {new SimpleMappingRule("Integer", "*", CommonFieldTypes.INTEGER)},
                {new SimpleMappingRule("Integer", "*", CommonFieldTypes.KEYWORD)},
                {new SimpleMappingRule("Integer", "*", CommonFieldTypes.LONG)},
                {new SimpleMappingRule("Integer", "*", CommonFieldTypes.NON_INDEXED)},
                {new SimpleMappingRule("Integer", "*", CommonFieldTypes.NUMBER)},
                {new SimpleMappingRule("Text", "*", CommonFieldTypes.TEXT)},
                {new SimpleMappingRule("Integer", "*", CommonFieldTypes.URI)},
                };
    }
}
