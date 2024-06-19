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
import io.telicent.smart.cache.entity.vocabulary.Rdf;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.RDFS;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestSecurityLabelsConverter extends AbstractEntityToMapOutputConverterTests {

    @Test
    public void entity_security_labels_01() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);
        SecurityLabelsConverter converter = new SecurityLabelsConverter();
        verifyConverterHasNoOutput(converter, entity);
    }

    @Test
    public void entity_security_labels_02() {
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, null, GraphFactory.createDefaultGraph());
        SecurityLabelsConverter converter = new SecurityLabelsConverter();
        verifyConverterHasNoOutput(converter, entity);
    }

    @Test
    public void entity_security_labels_03() {
        Graph securityLabels = GraphFactory.createDefaultGraph();
        securityLabels.add(NodeFactory.createURI(FRED_URI), Rdf.TYPE, NodeFactory.createURI(IES_NAMESPACE + "Person"));
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, null, securityLabels);
        SecurityLabelsConverter converter = new SecurityLabelsConverter();
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabels(null, securityLabels, output, DefaultOutputFields.SECURITY_LABELS);
    }

    @Test
    public void entity_security_labels_04() {
        Graph securityLabels = GraphFactory.createDefaultGraph();
        securityLabels.add(NodeFactory.createURI(FRED_URI), RDFS.label.asNode(), NodeFactory.createLiteral("test"));
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, null, securityLabels);
        String customOutputField = "secretSquirrels";
        SecurityLabelsConverter converter = new SecurityLabelsConverter(customOutputField);
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabels(null, securityLabels, output, customOutputField);
    }

    @Test
    public void entity_security_labels_05() {
        String defaultLabels = "admin,manager";
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, defaultLabels, null);
        SecurityLabelsConverter converter = new SecurityLabelsConverter();
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabels(defaultLabels, null, output, DefaultOutputFields.SECURITY_LABELS);
    }

    @Test
    public void entity_security_labels_06() {
        String defaultLabels = "anyone";
        Graph securityLabels = GraphFactory.createDefaultGraph();
        securityLabels.add(NodeFactory.createURI(FRED_URI), Rdf.TYPE, NodeFactory.createURI(IES_NAMESPACE + "Person"));
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, defaultLabels, securityLabels);
        SecurityLabelsConverter converter = new SecurityLabelsConverter();
        Map<String, Object> output = verifyConverterProducesOutput(converter, entity);
        verifySecurityLabels(defaultLabels, securityLabels, output, DefaultOutputFields.SECURITY_LABELS);
    }

    @Test
    public void entity_security_labels_07() {
        String defaultLabels = "anyone";
        Graph securityLabels = GraphFactory.createDefaultGraph();
        securityLabels.add(NodeFactory.createURI(FRED_URI), Rdf.TYPE, NodeFactory.createURI(IES_NAMESPACE + "Person"));
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, defaultLabels, securityLabels);
        SecurityLabelsConverter converter = new SecurityLabelsConverter();
        Map<String, Object> output = new HashMap<>();
        output.put("securityLabels", new HashMap<>(Map.of("test", "secure")));

        // Outputting the defaults should maintain any pre-existing labels that have already been output
        converter.output(entity, output);
        Map<String, Object> labelsMap =
                verifySecurityLabels(defaultLabels, securityLabels, output, DefaultOutputFields.SECURITY_LABELS);
        Assert.assertTrue(labelsMap.containsKey("test"));
    }
}
