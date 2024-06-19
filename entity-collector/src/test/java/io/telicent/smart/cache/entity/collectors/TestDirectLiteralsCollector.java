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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.testng.Assert;

import java.util.List;

public class TestDirectLiteralsCollector extends AbstractEntityDataCollectorTests {
    @Override
    protected EntityDataCollector createCollector() {
        return new DirectLiteralsCollector();
    }

    @Override
    public boolean expectDataCollected(Node subject) {
        return StringUtils.equals(subject.getURI(), FRED_URI);
    }

    @Override
    public List<String> getExpectedSecurityLabels(Node subject) {
        if (StringUtils.equals(subject.getURI(), FRED_URI)) {
            return List.of("public", "gdpr");
        } else {
            return null;
        }
    }

    @Override
    public void verifyCollected(Node subject, List<EntityData> collected, List<String> expectedSecurityLabels) {
        if (StringUtils.equals(subject.getURI(), FRED_URI)) {
            Assert.assertEquals(collected.size(), 2);
            Node expectedName = NodeFactory.createLiteral("Fred Test");
            Node expectedAge = NodeFactory.createLiteral("34", XSDDatatype.XSDinteger);

            for (EntityData data : collected) {
                boolean isName = data.key().equals(FOAF.name.asNode());
                Assert.assertEquals(data.get().getNode(), isName ? expectedName : expectedAge);

                if (expectedSecurityLabels != null) {
                    Assert.assertEquals(data.get().getSecurityLabel(),
                                        isName ? expectedSecurityLabels.get(0) : expectedSecurityLabels.get(1),
                                        "Wrong security label for " + data.key().getURI());
                }
            }
        }
    }
}
