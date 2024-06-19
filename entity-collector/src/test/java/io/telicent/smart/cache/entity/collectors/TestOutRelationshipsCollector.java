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

import io.telicent.smart.cache.entity.EntityData;
import io.telicent.smart.cache.entity.SecurityLabelledNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.testng.Assert;

import java.util.List;

public class TestOutRelationshipsCollector extends AbstractEntityDataCollectorTests {
    @Override
    protected EntityDataCollector createCollector() {
        return new OutRelationshipsCollector();
    }

    @Override
    public boolean expectDataCollected(Node subject) {
        return StringUtils.equals(subject.getURI(), FRED_URI);
    }

    @Override
    public List<String> getExpectedSecurityLabels(Node subject) {
        if (StringUtils.equals(subject.getURI(), FRED_URI)) {
            return List.of("gdpr");
        } else {
            return null;
        }
    }

    @Override
    public void verifyCollected(Node subject, List<EntityData> collected, List<String> expectedSecurityLabels) {
        if (StringUtils.equals(subject.getURI(), FRED_URI)) {
            Assert.assertEquals(collected.size(), 3);

            for (EntityData data : collected) {
                if (data.key().equals(NodeFactory.createURI(IES_NAMESPACE + "isIdentifiedBy"))) {
                    SecurityLabelledNode node = data.get();
                    if (expectedSecurityLabels == null) {
                        Assert.assertTrue(StringUtils.isBlank(node.getSecurityLabel()));
                    } else {
                        Assert.assertEquals(node.getSecurityLabel(), expectedSecurityLabels.get(0));
                    }
                }
            }
        }
    }
}
