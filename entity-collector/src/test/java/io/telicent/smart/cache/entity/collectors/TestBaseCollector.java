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
package io.telicent.smart.cache.entity.collectors;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBaseCollector extends AbstractEntityDataCollector {

    @Test
    public void find_security_labels_01() {
        LabelsStore store = Labels.emptyStore();
        Assert.assertTrue(StringUtils.isBlank(
                this.getSecurityLabels(store, NodeFactory.createBlankNode(), NodeFactory.createBlankNode(),
                                       NodeFactory.createBlankNode())));
    }

    @Test
    public void find_security_labels_02() {
        LabelsStore store = Labels.emptyStore();
        Assert.assertTrue(StringUtils.isBlank(
                this.getSecurityLabels(store,
                                       Triple.create(NodeFactory.createBlankNode(), NodeFactory.createBlankNode(),
                                                     NodeFactory.createBlankNode()))));
    }

    @Test
    public void null_store_01() {
        Assert.assertNull(this.getSecurityLabels(null, NodeFactory.createBlankNode(), NodeFactory.createBlankNode(),
                                                 NodeFactory.createBlankNode()));
    }

    @Test
    public void null_store_02() {
        Assert.assertNull(
                this.getSecurityLabels(null, Triple.create(NodeFactory.createBlankNode(), NodeFactory.createBlankNode(),
                                                           NodeFactory.createBlankNode())));
    }
}
