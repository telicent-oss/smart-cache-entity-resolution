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
import io.telicent.smart.cache.entity.Library;
import io.telicent.smart.cache.entity.sinks.converters.documents.FakeDocumentFormat;
import io.telicent.smart.cache.entity.sinks.converters.metadata.GeneratorVersionConverter;
import io.telicent.smart.cache.entity.sinks.converters.metadata.MetadataConverter;
import io.telicent.smart.cache.entity.sinks.converters.metadata.StaticFieldConverter;
import io.telicent.smart.cache.observability.LibraryVersion;
import org.apache.jena.graph.NodeFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Map;

public class TestMetadataConverters extends AbstractEntityToMapOutputConverterTests {

    private void verifyExpectedMetadata(String expectedLibraryName, String expectedLibraryVersion,
                                        String expectedDocumentFormat, boolean includeGeneratedAt,
                                        Map<String, Object> actual) {
        Assert.assertTrue(actual.containsKey(DefaultOutputFields.GENERATED_BY),
                          "Missing expected " + DefaultOutputFields.GENERATED_BY + " field");
        Assert.assertEquals(actual.get(DefaultOutputFields.GENERATED_BY), expectedLibraryName);

        Assert.assertTrue(actual.containsKey(DefaultOutputFields.GENERATOR_VERSION),
                          "Missing expected " + DefaultOutputFields.GENERATOR_VERSION + " field");
        Assert.assertEquals(actual.get(DefaultOutputFields.GENERATOR_VERSION), expectedLibraryVersion);

        Assert.assertTrue(actual.containsKey(DefaultOutputFields.DOCUMENT_FORMAT),
                          "Missing expected " + DefaultOutputFields.DOCUMENT_FORMAT + " field");
        Assert.assertEquals(actual.get(DefaultOutputFields.DOCUMENT_FORMAT), expectedDocumentFormat);

        if (includeGeneratedAt) {
            Assert.assertTrue(actual.containsKey(DefaultOutputFields.GENERATED_AT),
                              "Missing expected " + DefaultOutputFields.GENERATED_AT + " field");
            // NB - Not validating this field values exact as it's a date, just validate that it's of the correct type
            Assert.assertTrue(actual.get(DefaultOutputFields.GENERATED_AT) instanceof Date);
        } else {
            Assert.assertFalse(actual.containsKey(DefaultOutputFields.GENERATED_AT));
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".* cannot be blank")
    public void entity_data_converter_metadata_bad_01() {
        new MetadataConverter(null, null, true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".* cannot be blank")
    public void entity_data_converter_metadata_bad_02() {
        new MetadataConverter(Library.NAME, null, true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".* cannot be blank")
    public void entity_data_converter_metadata_bad_03() {
        new GeneratorVersionConverter(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".* cannot be blank")
    public void entity_data_converter_metadata_bad_04() {
        new StaticFieldConverter("test", null);
    }

    @Test
    public void entity_data_converter_metadata_01() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        MetadataConverter converter = new MetadataConverter(Library.NAME, FakeDocumentFormat.NAME, true);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Assert.assertTrue(map.containsKey(DefaultOutputFields.METADATA));
        Assert.assertTrue(map.get(DefaultOutputFields.METADATA) instanceof Map<?, ?>);
        Map<String, Object> metadata = (Map<String, Object>) map.get(DefaultOutputFields.METADATA);

        verifyExpectedMetadata(Library.NAME, LibraryVersion.get(Library.NAME), FakeDocumentFormat.NAME, true, metadata);
    }

    @Test
    public void entity_data_converter_metadata_02() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        MetadataConverter converter = new MetadataConverter(Library.NAME, FakeDocumentFormat.NAME, false);
        Map<String, Object> map = verifyConverterProducesOutput(converter, entity);

        Assert.assertTrue(map.containsKey(DefaultOutputFields.METADATA));
        Assert.assertTrue(map.get(DefaultOutputFields.METADATA) instanceof Map<?, ?>);
        Map<String, Object> metadata = (Map<String, Object>) map.get(DefaultOutputFields.METADATA);

        verifyExpectedMetadata(Library.NAME, LibraryVersion.get(Library.NAME), FakeDocumentFormat.NAME, false,
                               metadata);
    }
}
