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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.telicent.smart.cache.entity.AbstractEntityCollectorTests;
import io.telicent.smart.cache.entity.Entity;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.graph.GraphFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class AbstractEntityToMapOutputConverterTests extends AbstractEntityCollectorTests {
    @SuppressWarnings("unused")
    public static void dumpOutput(Map<String, Object> map) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(System.out, map);
    }

    /**
     * Verifies that a converter produces some output
     *
     * @param converter Converter
     * @param entity    Entity
     * @return Output map
     */
    protected Map<String, Object> verifyConverterProducesOutput(EntityToMapOutputConverter converter, Entity entity) {
        Map<String, Object> map = new HashMap<>();

        Assert.assertTrue(map.isEmpty());
        converter.output(entity, map);
        Assert.assertFalse(map.isEmpty());
        return map;
    }

    protected void verifyConverterHasNoOutput(EntityToMapOutputConverter converter, Entity entity) {
        Map<String, Object> map = new HashMap<>();

        Assert.assertTrue(map.isEmpty());
        converter.output(entity, map);
        Assert.assertTrue(map.isEmpty());
    }

    protected void verifyTypes(List<String> data) {
        Assert.assertEquals(data.get(0), PERSON_TYPE.getURI());
        Assert.assertEquals(data.get(1), EVENT_PARTICIPANT_TYPE.getURI());
    }

    protected void verifyCompactedTypes(List<String> data) {
        Assert.assertEquals(data.get(0), "ies:Person");
        Assert.assertEquals(data.get(1), "ies:EventParticipant");
    }

    protected List<String> verifyListOutput(Map<String, Object> map) {
        Assert.assertTrue(map.get("test") instanceof List<?>);
        List<String> data = (List<String>) map.get("test");
        Assert.assertEquals(data.size(), 2);
        return data;
    }

    protected void verifySecurityLabelledListOutput(Map<String, Object> output, String expectedLabel) {
        verifySecurityLabelledListOutput(output, List.of(expectedLabel));
    }

    protected void verifySecurityLabelledListOutput(Map<String, Object> output, List<String> expectedLabels) {
        Assert.assertTrue(output.containsKey("test"));
        Assert.assertTrue(output.containsKey(DefaultOutputFields.SECURITY_LABELS));
        Assert.assertTrue(output.get(DefaultOutputFields.SECURITY_LABELS) instanceof Map<?, ?>);
        Map<String, Object> securityLabels = (Map<String, Object>) output.get(DefaultOutputFields.SECURITY_LABELS);
        List<String> labels = (List<String>) securityLabels.get("test");
        Assert.assertEquals(labels.size(), expectedLabels.size());
        Assert.assertEquals(labels, expectedLabels);
    }

    protected Map<String, List<String>> verifyMapOutput(Map<String, Object> map) {
        return verifyMapOutput(map, 1);
    }

    protected Map<String, List<String>> verifyMapOutput(Map<String, Object> map, int expectedSize) {
        Assert.assertNotNull(map);
        Assert.assertTrue(map.get("test") instanceof Map<?, ?>);
        Map<String, List<String>> data = (Map<String, List<String>>) map.get("test");
        Assert.assertEquals(data.size(), expectedSize);
        return data;
    }

    protected void verifySecurityLabelledMapOutput(Map<String, Object> map,
                                                   Map<String, List<String>> expectedLabels) {
        Assert.assertNotNull(map);
        Assert.assertTrue(map.get("test") instanceof Map<?, ?>);
        Map<String, Object> fieldMap = (Map<String, Object>) map.get("test");
        Map<String, Object> securityLabels =
                (Map<String, Object>) fieldMap.getOrDefault(DefaultOutputFields.SECURITY_LABELS,
                                                            Collections.emptyMap());

        for (String key : expectedLabels.keySet()) {
            if (!expectedLabels.get(key).isEmpty()) {
                Assert.assertTrue(securityLabels.containsKey(key));
                Assert.assertTrue(securityLabels.get(key) instanceof List<?>);
                List<String> labels = (List<String>) securityLabels.get(key);
                Assert.assertEquals(labels, expectedLabels.get(key));
            } else {
                Assert.assertFalse(securityLabels.containsKey(key));
            }
        }
    }

    protected Map<String, Object> verifySecurityLabels(String defaultLabels, Graph securityLabels,
                                                       Map<String, Object> output,
                                                       String outputField) {
        Map<String, Object> labelsMap = (Map<String, Object>) output.get(outputField);
        String labels = (String) labelsMap.get(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_GRAPH);
        if (securityLabels == null) {
            Assert.assertNull(labels);
        } else {
            Assert.assertTrue(StringUtils.isNotBlank(labels));
            RDFParser parser = RDFParser.create().lang(Lang.TURTLE).source(new StringReader(labels)).build();
            Graph decoded = GraphFactory.createDefaultGraph();
            parser.parse(decoded);
            Assert.assertTrue(decoded.isIsomorphicWith(securityLabels));
        }

        String actualDefaultLabels = (String) labelsMap.get(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS);
        Assert.assertEquals(actualDefaultLabels, defaultLabels);

        return labelsMap;
    }

    @DataProvider(name = "keyFormats")
    public Object[][] upsertableKeyFormats() {
        return new Object[][]{
                {UpsertableKeyFormat.AS_IS},
                {UpsertableKeyFormat.COMPACTED},
                {UpsertableKeyFormat.HASHED}
        };
    }

    protected void verifyUpsertableMapOutput(UpsertableKeyFormat keyFormat, boolean compactValues, Entity e,
                                             Map<String, Object> output, Node... expected) {
        if (expected.length == 0) {
            Assert.assertTrue(MapUtils.isEmpty(output));
        } else {
            //dumpOutput(output);

            for (Node n : expected) {
                String key = null;
                switch (keyFormat) {
                    case HASHED:
                        if (n.isURI()) {
                            key = UpsertableKeyFormat.hashKey(n.getURI());
                        } else if (n.isBlank()) {
                            key = UpsertableKeyFormat.hashKey(n.getBlankNodeLabel());
                        } else if (n.isLiteral()) {
                            key = UpsertableKeyFormat.hashKey(n.getLiteralLexicalForm());
                        }
                        break;
                    case COMPACTED:
                    case AS_IS:
                    default:
                        if (n.isURI()) {
                            key = compactValues && e.hasPrefixes() ? e.getPrefixes().shortForm(n.getURI()) : n.getURI();
                        } else if (n.isBlank()) {
                            key = n.getBlankNodeLabel();
                        } else if (n.isLiteral()) {
                            key = n.getLiteralLexicalForm();
                        }
                }
                Assert.assertNotNull(key);
                Assert.assertTrue(output.containsKey(key), "Expected output to contain key " + key + " for Node " + n);

                String expectedValue = null;
                if (n.isURI()) {
                    expectedValue =
                            compactValues && e.hasPrefixes() ? e.getPrefixes().shortForm(n.getURI()) : n.getURI();
                } else if (n.isBlank()) {
                    expectedValue = n.getBlankNodeLabel();
                } else if (n.isLiteral()) {
                    expectedValue = n.getLiteralLexicalForm();
                }
                Assert.assertNotNull(expectedValue);
                Assert.assertEquals(output.get(key), expectedValue);
            }
        }
    }

    protected void verifySecurityLabelledUpsertableMapOutput(UpsertableKeyFormat keyFormat, boolean compactValues,
                                                             Entity e,
                                                             Map<String, Object> output, List<Node> expected,
                                                             List<String> expectedSecurityLabels) {
        if (expected.isEmpty()) {
            Assert.assertTrue(MapUtils.isEmpty(output));
        } else {
            //dumpOutput(output);

            Assert.assertTrue(output.keySet().size() <= expected.size(),
                              "Too many keys in security labels map, expected at most " + expected.size() + " but found " + output.keySet()
                                                                                                                                  .size());

            for (int i = 0; i < expected.size(); i++) {
                Node n = expected.get(i);
                String key = null;
                switch (keyFormat) {
                    case HASHED:
                        if (n.isURI()) {
                            key = UpsertableKeyFormat.hashKey(n.getURI());
                        } else if (n.isBlank()) {
                            key = UpsertableKeyFormat.hashKey(n.getBlankNodeLabel());
                        } else if (n.isLiteral()) {
                            key = UpsertableKeyFormat.hashKey(n.getLiteralLexicalForm());
                        }
                        break;
                    case COMPACTED:
                    case AS_IS:
                    default:
                        if (n.isURI()) {
                            key = compactValues && e.hasPrefixes() ? e.getPrefixes().shortForm(n.getURI()) : n.getURI();
                        } else if (n.isBlank()) {
                            key = n.getBlankNodeLabel();
                        } else if (n.isLiteral()) {
                            key = n.getLiteralLexicalForm();
                        }
                }
                Assert.assertNotNull(key);

                String expectedLabel = expectedSecurityLabels.get(i);
                if (StringUtils.isBlank(expectedLabel)) {
                    Assert.assertFalse(output.containsKey(key),
                                       "Non-security labelled node " + n + " with key " + key + " should not appear in security labels map");
                } else {
                    Assert.assertTrue(output.containsKey(key),
                                      "Expected output to contain key " + key + " for Node " + n);
                    Assert.assertNotNull(expectedLabel);
                    Assert.assertEquals(output.get(key), expectedLabel);
                }
            }
        }
    }

    protected Map<String, Object> verifyUpsertableMapOutputHasSecurityLabels(Map<String, Object> output,
                                                                             String field) {
        Assert.assertTrue(output.containsKey(field));
        output = (Map<String, Object>) output.get(field);
        Assert.assertTrue(output.containsKey(DefaultOutputFields.SECURITY_LABELS));
        return (Map<String, Object>) output.get(DefaultOutputFields.SECURITY_LABELS);
    }

    protected Set<String> verifyNoListsInOutput(Map<String, Object> map) {
        return verifyNoListsInOutput(map, new HashSet<>());
    }

    protected Set<String> verifyNoListsInOutput(Map<String, Object> map, Set<String> leafValues) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?>) {
                verifyNoListsInOutput((Map<String, Object>) entry.getValue(), leafValues);
            } else if (entry.getValue() instanceof List<?>) {
                Assert.fail("Field " + entry.getKey() + " has unexpected list value");
            } else {
                leafValues.add(entry.getValue().toString());
            }
        }
        return leafValues;
    }

    protected void verifyAllDataWasOutput(Set<String> values, Entity entity, boolean compactValues,
                                          List<Node> expected) {
        expected.forEach(n -> {
            String expectedValue;
            if (n.isURI()) {
                expectedValue =
                        compactValues && entity.hasPrefixes() ? entity.getPrefixes().shortForm(n.getURI()) : n.getURI();
            } else if (n.isBlank()) {
                expectedValue = n.getBlankNodeLabel();
            } else if (n.isLiteral()) {
                expectedValue = n.getLiteralLexicalForm();
            } else {
                expectedValue = null;
            }
            if (StringUtils.isNotBlank(expectedValue)) {
                Assert.assertTrue(values.contains(expectedValue),
                                  "Output leaf values did not contain expected value " + expectedValue + " for Node " + n);
            }
        });
    }
}
