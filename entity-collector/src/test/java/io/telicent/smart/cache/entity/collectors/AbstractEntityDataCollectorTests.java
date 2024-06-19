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
import io.telicent.smart.cache.entity.AbstractEntityCollectorTests;
import io.telicent.smart.cache.entity.Entity;
import io.telicent.smart.cache.entity.EntityData;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public abstract class AbstractEntityDataCollectorTests extends AbstractEntityCollectorTests {

    protected abstract EntityDataCollector createCollector();

    public abstract boolean expectDataCollected(Node subject);

    public abstract void verifyCollected(Node subject, List<EntityData> collected, List<String> expectedSecurityLabels);

    public final void verifyCollected(Node subject, List<EntityData> collected) {
        verifyCollected(subject, collected, null);
    }

    public List<String> getExpectedSecurityLabels(Node subject) {
        return null;
    }

    @Test
    public void entity_data_collector_success() {
        EntityDataCollector collector = createCollector();
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null);

        List<EntityData> data = collector.collect(this.testData, entity.getUri(), null).toList();
        if (expectDataCollected(entity.getUri())) {
            Assert.assertNotEquals(data.size(), 0);
            verifyCollected(entity.getUri(), data);
        } else {
            Assert.assertEquals(data.size(), 0);
        }
    }

    @Test
    public void entity_data_collector_security_labelled() {
        EntityDataCollector collector = createCollector();
        Entity entity = new Entity(NodeFactory.createURI(FRED_URI), null, null, this.testLabels);

        List<EntityData> data =
                collector.collect(this.testData, entity.getUri(), Labels.createLabelsStoreMem(this.testLabels))
                         .toList();
        if (expectDataCollected(entity.getUri())) {
            Assert.assertNotEquals(data.size(), 0);
            verifyCollected(entity.getUri(), data, getExpectedSecurityLabels(entity.getUri()));
        } else {
            Assert.assertEquals(data.size(), 0);
        }
    }

    @Test
    public void entity_data_collector_failure() {
        EntityDataCollector collector = createCollector();
        Entity entity = new Entity(NodeFactory.createURI("http://no-such-person"), null);

        List<EntityData> data = collector.collect(this.testData, entity.getUri(), null).toList();
        Assert.assertEquals(data.size(), 0);
    }

    @Test
    public void entity_data_collector_group() {
        EntityDataCollector collector = createCollector();
        Assert.assertNotNull(collector.getGroup());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void entity_data_collector_bad_01() {
        EntityDataCollector collector = createCollector();
        collector.collect(this.testData, null, null).toList();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void entity_data_collector_bad_02() {
        EntityDataCollector collector = createCollector();
        collector.collect(null, NodeFactory.createURI(FRED_URI), null).toList();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void entity_data_collector_bad_03() {
        EntityDataCollector collector = createCollector();
        collector.collect(this.testData, NodeFactory.createVariable("var"), null).toList();
    }
}
