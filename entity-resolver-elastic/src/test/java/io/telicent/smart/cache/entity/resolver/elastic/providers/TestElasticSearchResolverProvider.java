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
package io.telicent.smart.cache.entity.resolver.elastic.providers;

import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.configuration.sources.ConfigurationSource;
import io.telicent.smart.cache.configuration.sources.NullSource;
import io.telicent.smart.cache.configuration.sources.PropertiesSource;
import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.entity.resolver.elastic.ElasticSearchEntityResolver;
import io.telicent.smart.cache.entity.resolver.providers.EntityResolverProvider;
import io.telicent.smart.cache.entity.resolver.providers.EntityResolvers;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.elastic.providers.ElasticsearchClientProvider;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.util.Properties;

public class TestElasticSearchResolverProvider {

    @AfterTest
    public void cleanup() {
        Configurator.reset();
    }

    @Test
    public void givenNoConfig_whenLoadingEntityResolver_thenNothingIsLoaded() {
        // Given
        Configurator.setSingleSource(NullSource.INSTANCE);

        // When
        EntityResolver resolver = EntityResolvers.load();

        // Then
        Assert.assertNull(resolver);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNoConfig_whenUsingElasticProviderDirectly_thenUnsupported_andErrorIsThrown() {
        // Given
        Configurator.setSingleSource(NullSource.INSTANCE);

        // When
        EntityResolverProvider provider = new ElasticSearchResolverProvider();

        // Then
        Assert.assertFalse(provider.supports());
        provider.load();
    }

    @Test
    public void givenValidConfig_whenLoadingEntityResolver_thenResolverIsLoaded() {
        // Given
        Properties props = new Properties();
        props.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_HOST), "localhost");
        props.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_SIMILARITY_INDEX), "test");
        PropertiesSource source = new PropertiesSource(props);
        Configurator.setSingleSource(source);

        // When
        EntityResolver resolver = EntityResolvers.load();

        // Then
        Assert.assertNotNull(resolver);
        Assert.assertTrue(resolver instanceof ElasticSearchEntityResolver);
    }

    @Test
    public void test_minimumRequiredConfiguration() {
        // Given
        EntityResolverProvider provider = new ElasticSearchResolverProvider();
        String[] expected = {"ELASTIC_HOST", "ELASTIC_SIMILARITY_INDEX"};
        // When
        String[] actual = provider.minimumRequiredConfiguration();
        // Then
        Assert.assertEquals(actual, expected);
    }

    @Test(expectedExceptions = SearchException.class)
    public void givenInvalidPortConfig_whenLoadingEntityResolver_thenThrowException() {
        // Given
        Properties props = new Properties();
        props.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_HOST), "localhost");
        props.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_SIMILARITY_INDEX), "test");
        props.put(ConfigurationSource.asSystemPropertyKey(ElasticsearchClientProvider.ENV_ELASTIC_PORT), "wrong");
        PropertiesSource source = new PropertiesSource(props);
        Configurator.setSingleSource(source);

        // When
        // Then
        EntityResolvers.load();
    }
}
