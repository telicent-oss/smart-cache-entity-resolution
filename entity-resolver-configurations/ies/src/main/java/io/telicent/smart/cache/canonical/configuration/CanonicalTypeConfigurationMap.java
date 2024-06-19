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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A mapping of differing canonical type configurations.
 * Implements the Map interface just to make things easier to understand and use.
 * NOTE - so far YAML is only implementation, if that changes we will need to refactor/rename this.
 */
public class CanonicalTypeConfigurationMap implements Map<String, CanonicalTypeConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalTypeConfigurationMap.class);
    /**
     * The underlying hashmap
     */
    @JsonProperty("canonicalConfig")
    public final Map<String, CanonicalTypeConfiguration> underlyingMap = new HashMap<>();

    /**
     * Mapping
     */
    private static final ObjectReader
            OBJECT_READER = new ObjectMapper(new YAMLFactory()).findAndRegisterModules().readerFor(CanonicalTypeConfigurationMap.class);


    /**
     * Mapping
     * @param path the path to the configuration file
     * @return the loaded configuration or null if there's an error - TODO what is the proper form here?
     */
    public static CanonicalTypeConfigurationMap loadFromConfigFile(String path) {
        try {
            return OBJECT_READER.readValue(new File(path));
        } catch (IllegalArgumentException | IOException | NullPointerException e) {
            LOGGER.error("Failed to load configuration from file", e);
            return null;
        }
    }

    /**
     * Mapping
     * @param value a string representation of the configuration
     * @return the loaded configuration or null if there's an error - TODO what is the proper form here?
     */
    public static CanonicalTypeConfigurationMap loadFromString(String value) {
        try {
            return OBJECT_READER.readValue(value);
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error("Failed to load configuration from file", e);
            return null;
        }
    }

    @Override
    public int size() {
        return underlyingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return underlyingMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return underlyingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return underlyingMap.containsValue(value);
    }

    @Override
    public CanonicalTypeConfiguration get(Object key) {
        return underlyingMap.get(key);
    }

    @Override
    public CanonicalTypeConfiguration put(String key, CanonicalTypeConfiguration value) {
        return underlyingMap.put(key, value);
    }

    @Override
    public CanonicalTypeConfiguration remove(Object key) {
        return underlyingMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends CanonicalTypeConfiguration> m) {
        underlyingMap.putAll(m);
    }

    @Override
    public void clear() {
        underlyingMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return underlyingMap.keySet();
    }

    @Override
    public Collection<CanonicalTypeConfiguration> values() {
        return underlyingMap.values();
    }

    @Override
    public Set<Entry<String, CanonicalTypeConfiguration>> entrySet() {
        return underlyingMap.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return underlyingMap.equals(o);
    }

    @Override
    public int hashCode() {
        return underlyingMap.hashCode();
    }
}
