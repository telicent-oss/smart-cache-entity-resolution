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

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestEntityProjectionProviders {

    @Test
    public void providers_01() {
        List<EntityProjectionProvider> providers = new ArrayList<>(EntityProjectionConfigurations.available());
        Assert.assertFalse(providers.isEmpty());
        Assert.assertEquals(providers.size(), 1);
    }

    @Test
    public void providers_02() {
        Assert.assertTrue(EntityProjectionConfigurations.isProjection(SimpleProjection.NAME));
    }

    @Test
    public void providers_03() {
        EntityProjectionProvider provider = EntityProjectionConfigurations.get(SimpleProjection.NAME);
        Assert.assertNotNull(provider);
        Assert.assertEquals(provider.name(), SimpleProjection.NAME);
        Assert.assertTrue(StringUtils.isNotBlank(provider.description()));
        Assert.assertNotNull(provider.create());
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*not a recognised.*")
    public void providers_04() {
        EntityProjectionConfigurations.get("no-such-provider");
    }
}
