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
import org.apache.jena.graph.NodeFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUriConverter extends AbstractEntityToMapOutputConverterTests {

    @Test
    public void entity_data_converter_uri_01() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        UriConverter converter = new UriConverter();
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Assert.assertEquals(map.get(DefaultOutputFields.URI), FRED_URI);
    }

    @Test
    public void entity_data_converter_uri_02() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        UriConverter converter = new UriConverter("test");
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Assert.assertEquals(map.get("test"), FRED_URI);
    }

    @Test
    public void entity_data_converter_uri_03() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        UriConverter converter = new UriConverter("test");
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Assert.assertEquals(map.get("test"), FRED_URI);

        // Converting another entity should overwrite the existing field
        Entity other = new Entity(NodeFactory.createURI("https://other"), null);
        converter.output(other, map);
        Assert.assertNotEquals(map.get("test"), FRED_URI);
        Assert.assertEquals(map.get("test"), "https://other");
    }

    @Test
    public void entity_data_converter_uri_04() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        UriConverter converter = new UriConverter("test");
        Map<String, Object> map = new HashMap<>();
        map.put("test", Map.of("foo", "bar"));

        // Conversion should override existing fields of different types
        converter.output(entity, map);
        Assert.assertEquals(map.get("test"), FRED_URI);
    }

    @Test
    public void entity_data_converter_uri_05() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        UriConverter converter = new UriConverter("test");
        Map<String, Object> map = new HashMap<>();
        map.put("test", List.of("foo", "bar"));

        // Conversion should override existing fields of different types
        converter.output(entity, map);
        Assert.assertEquals(map.get("test"), FRED_URI);
    }
}
