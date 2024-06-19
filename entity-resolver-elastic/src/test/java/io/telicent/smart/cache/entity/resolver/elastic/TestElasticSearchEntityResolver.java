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
package io.telicent.smart.cache.entity.resolver.elastic;

import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestElasticSearchEntityResolver {

    public static final String HAPPY_STRING = """
            type: CoreCanonicalTestType
            fields:
              - name: text-field-fuzzy
                type: text
                required: true
                boost: 1.2
                fuzziness:
                  enabled: true
                  min: 0
                  max: 3
            """;


    @Test
    public void test_getIndexToUse_chooseOverrideIfAvailable() {
        // given
        String randomDefaultIndex = RandomStringUtils.random(6);
        String randomOverrideIndex = RandomStringUtils.random(6);

        CanonicalTypeConfiguration
                override = CanonicalTypeConfiguration.loadFromString(HAPPY_STRING);
        Assert.assertNotNull(override);
        override.index = randomOverrideIndex;

        try (ElasticSearchEntityResolver resolver = new ElasticSearchEntityResolver("host", 0, randomDefaultIndex)) {
            // when
            String result = resolver.getIndexToUse(null, override);
            // then
            Assert.assertEquals(result, randomOverrideIndex);
        } catch (Exception e) {
            Assert.assertNull(e, "This should not occur");
        }
    }

    @Test
    public void test_getIndexToUse_nullOverride() {
        // given
        String randomDefaultIndex = RandomStringUtils.random(6);
        try (ElasticSearchEntityResolver resolver = new ElasticSearchEntityResolver("host", 0, randomDefaultIndex)) {
            // when
            String result = resolver.getIndexToUse(null, null);
            // then
            Assert.assertEquals(result, randomDefaultIndex);
        } catch (Exception e) {
            Assert.assertNull(e, "This should not occur");
        }
    }

    @Test
    public void test_getIndexToUse_nullOverrideIndex() {
        // given
        String randomDefaultIndex = RandomStringUtils.random(6);
        CanonicalTypeConfiguration
                override = CanonicalTypeConfiguration.loadFromString(HAPPY_STRING);
        Assert.assertNotNull(override);
        try (ElasticSearchEntityResolver resolver = new ElasticSearchEntityResolver("host", 0, randomDefaultIndex)) {
            // when
            String result = resolver.getIndexToUse(null, override);
            // then
            Assert.assertEquals(result, randomDefaultIndex);
        } catch (Exception e) {
            Assert.assertNull(e, "This should not occur");
        }
    }

    @Test
    public void test_elasticEntityResolverBuilder_toString() {
        // given
        String expected = "ElasticSearchEntityResolver.ElasticEntityResolverBuilder(super=AbstractElasticClient.AbstractElasticClientBuilder(elasticHost=null, elasticPort=0, username=null, password=null, makeOpenSearchCompatible=false), similarityIndex=null)";
        // when
        String actual = ElasticSearchEntityResolver.builder().toString();
        // then
        Assert.assertEquals(actual, expected);
    }
}
