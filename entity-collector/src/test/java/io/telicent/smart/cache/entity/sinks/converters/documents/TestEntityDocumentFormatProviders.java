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
package io.telicent.smart.cache.entity.sinks.converters.documents;

import io.telicent.smart.cache.entity.sinks.converters.EntityToMapOutputConverter;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestEntityDocumentFormatProviders {

    @Test
    public void available_01() {
        List<EntityDocumentFormatProvider> providers = new ArrayList<>(EntityDocumentFormats.availableFormats());
        Assert.assertFalse(providers.isEmpty());
        Assert.assertEquals(providers.size(), 1);

        EntityDocumentFormatProvider provider = providers.get(0);
        verifyFakeProvider(provider);
    }

    private static void verifyFakeProvider(EntityDocumentFormatProvider provider) {
        Assert.assertNotNull(provider);
        Assert.assertEquals(provider.name(), FakeDocumentFormat.NAME);
        Assert.assertTrue(StringUtils.isNotBlank(provider.description()));
        List<EntityToMapOutputConverter> converters = provider.loadConverters(true, true);
        Assert.assertFalse(converters.isEmpty());
        Assert.assertEquals(converters.size(), 3);
    }

    @Test
    public void is_format_01() {
        Assert.assertTrue(EntityDocumentFormats.isFormat(FakeDocumentFormat.NAME));
        Assert.assertFalse(EntityDocumentFormats.isFormat("no-such-format"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*not a recognised document format")
    public void get_format_01() {
        EntityDocumentFormats.getFormat("no-such-format");
    }

    @Test
    public void get_format_02() {
        EntityDocumentFormatProvider provider = EntityDocumentFormats.getFormat(FakeDocumentFormat.NAME);
        verifyFakeProvider(provider);
    }
}
