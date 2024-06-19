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
package io.telicent.smart.cache.canonical.configuration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Test class for specifically testing the map interface implementation.
 */
public class TestCanonicalTypeConfigurationMapMethods {

    private CanonicalTypeConfigurationMap map;

    @BeforeMethod
    void setUp() {
        // Initialize the map before each test
        map = new CanonicalTypeConfigurationMap();
    }

    @Test
    void testPutAndGet() {
        //given
        CanonicalTypeConfiguration first = new CanonicalTypeConfiguration();
        CanonicalTypeConfiguration second = new CanonicalTypeConfiguration();
        // when
        map.put("one", first);
        map.put("two", second);
        // then
        assertEquals(first, map.get("one"));
        assertEquals(second, map.get("two"));
    }

    @Test
    void testSize() {
        // given
        assertEquals(0, map.size());
        // when
        map.put("one", new CanonicalTypeConfiguration());
        map.put("two", new CanonicalTypeConfiguration());
        // then
        assertEquals(2, map.size());
    }

    @Test
    void testContainsKey() {
        // given
        // when
        map.put("apple", new CanonicalTypeConfiguration());
        map.put("banana", new CanonicalTypeConfiguration());
        // then
        assertTrue(map.containsKey("apple"));
        assertFalse(map.containsKey("cherry"));
    }

    @Test
    void testRemove() {
        // given
        map.put("one", new CanonicalTypeConfiguration());
        map.put("two", new CanonicalTypeConfiguration());
        // when
        map.remove("one");
        // then
        assertNull(map.get("one"));
    }

    @Test
    void testClear() {
        // given
        map.put("one", new CanonicalTypeConfiguration());
        map.put("two", new CanonicalTypeConfiguration());
        // when
        map.clear();
        // then
        assertTrue(map.isEmpty());
    }

    @Test
    void testContainsValue() {
        // given
        CanonicalTypeConfiguration apple = new CanonicalTypeConfiguration();
        apple.index = "Apple";
        CanonicalTypeConfiguration banana = new CanonicalTypeConfiguration();
        banana.index = "Banana";
        CanonicalTypeConfiguration cherry = new CanonicalTypeConfiguration();
        cherry.index = "Cherry";

        // when
        map.put("apple", apple);
        map.put("banana", banana);
        // then
        assertTrue(map.containsValue(apple));
        assertFalse(map.containsValue(cherry));
    }

    @Test
    void testPutAll() {
        // given
        CanonicalTypeConfiguration first = new CanonicalTypeConfiguration();
        CanonicalTypeConfiguration second = new CanonicalTypeConfiguration();
        map.put("one", first);
        map.put("two", second);

        // when
        CanonicalTypeConfigurationMap newMap = new CanonicalTypeConfigurationMap();
        newMap.putAll(map);


        CanonicalTypeConfigurationMap altMap = new CanonicalTypeConfigurationMap();
        altMap.put("one", new CanonicalTypeConfiguration());
        altMap.put("two", new CanonicalTypeConfiguration());
        // then
        assertEquals(map, newMap);
        assertEquals(map.hashCode(), newMap.hashCode());
        assertNotSame(map, altMap);
        assertNotSame(map.hashCode(), altMap.hashCode());
    }

    @Test
    public void testKeySetAndValueSet() {
        // given
        CanonicalTypeConfiguration first = buildConfig("first");
        CanonicalTypeConfiguration second = buildConfig("second");
        CanonicalTypeConfiguration third = buildConfig("third");
        map.put("one", first);
        map.put("two", second);
        // when
        Set<String> keys = map.keySet();
        Collection<CanonicalTypeConfiguration> values = map.values();
        // then
        assertTrue(keys.contains("one"));
        assertFalse(keys.contains("three"));
        assertTrue(values.contains(first));
        assertFalse(values.contains(third));
    }

    private static CanonicalTypeConfiguration buildConfig(String prefix) {
        CanonicalTypeConfiguration canonicalTypeConfiguration = new CanonicalTypeConfiguration();
        canonicalTypeConfiguration.type = prefix + "_type";
        canonicalTypeConfiguration.index = prefix + "_index";
        return canonicalTypeConfiguration;
    }
}
