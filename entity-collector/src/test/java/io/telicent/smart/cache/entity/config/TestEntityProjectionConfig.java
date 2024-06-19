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
package io.telicent.smart.cache.entity.config;

import io.telicent.smart.cache.entity.collectors.DirectLiteralsCollector;
import io.telicent.smart.cache.entity.collectors.EntityDataCollector;
import io.telicent.smart.cache.entity.collectors.InRelationshipsCollector;
import io.telicent.smart.cache.entity.collectors.OutRelationshipsCollector;
import io.telicent.smart.cache.entity.selectors.SimpleTypeSelector;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestEntityProjectionConfig {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void config_bad_01() {
        new EntityProjectionConfig(null, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void config_bad_02() {
        new EntityProjectionConfig(new SimpleTypeSelector(), null);
    }

    @Test
    public void config_01() {
        EntityProjectionConfig config = new EntityProjectionConfig(new SimpleTypeSelector(), Collections.singletonList(
                new DirectLiteralsCollector()));

        Assert.assertTrue(config.getSelector() instanceof SimpleTypeSelector);
        List<EntityDataCollector> collectors = config.getCollectors().toList();
        Assert.assertEquals(collectors.size(), 1);
        Assert.assertTrue(collectors.get(0) instanceof DirectLiteralsCollector);
    }

    @Test
    public void config_02() {
        EntityProjectionConfig config = new EntityProjectionConfig(new SimpleTypeSelector(), Arrays.asList(
                new DirectLiteralsCollector(), new InRelationshipsCollector(), new OutRelationshipsCollector()));

        Assert.assertTrue(config.getSelector() instanceof SimpleTypeSelector);
        List<EntityDataCollector> collectors = config.getCollectors().toList();
        Assert.assertEquals(collectors.size(), 3);
        Assert.assertTrue(collectors.get(0) instanceof DirectLiteralsCollector);
        Assert.assertTrue(collectors.get(1) instanceof InRelationshipsCollector);
        Assert.assertTrue(collectors.get(2) instanceof OutRelationshipsCollector);
    }
}
