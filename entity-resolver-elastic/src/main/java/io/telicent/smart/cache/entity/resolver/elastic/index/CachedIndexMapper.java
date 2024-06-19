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
package io.telicent.smart.cache.entity.resolver.elastic.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.telicent.smart.cache.canonical.configuration.*;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import io.telicent.smart.cache.canonical.utility.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.telicent.smart.cache.canonical.utility.Mapper.writeValueAsString;

/**
 * An internal mapping of the config to stop making calls to ES every time.
 */
public final class CachedIndexMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedIndexMapper.class);
    private static final List<String> configList =
            Arrays.asList(Model.TYPE, Relation.TYPE, Scorer.TYPE, CanonicalTypeConfiguration.TYPE);
    /**
     * Internal map of config types
     */
    static Map<String, Map<String, Object>> cacheMap = initializeMapOfMaps(configList);

    static boolean initialLoad = true;

    private CachedIndexMapper() {}

    /**
     * Pre-populate the map of maps of config
     * @param keys the config types to use
     * @return the initialised map
     */
    public static Map<String, Map<String, Object>> initializeMapOfMaps(List<String> keys) {
        return keys.stream().collect(
                HashMap::new,
                (map, key) -> map.put(key, new HashMap<>()),
                HashMap::putAll
        );
    }

    /**
     * Load the CT Config from a file straight into the internal map
     * @param path the initial config file
     */
    public static void load(String path) {
        CanonicalTypeConfigurationMap ctMap = CanonicalTypeConfigurationMap.loadFromConfigFile(path);
        if (null != ctMap) {
            cacheMap.get(CanonicalTypeConfiguration.TYPE).putAll(ctMap);
        }
    }

    /**
     * As part of the transition from CT Map to internal mapping.
     * @param ctMap the map to load
     */
    public static void loadCTMapFromMap(CanonicalTypeConfigurationMap ctMap) {
        if (null != ctMap && !ctMap.isEmpty()) {
            cacheMap.get(CanonicalTypeConfiguration.TYPE).putAll(ctMap);
        }
    }

    /**
     * Return CanonicalTypeConfiguration from internal map
     * @param id to look-up
     * @return relevant config
     */
    public static CanonicalTypeConfiguration getCanonicalTypeConfiguration(String id) {
        Object object = cacheMap.get(CanonicalTypeConfiguration.TYPE).get(id);
        if (object instanceof CanonicalTypeConfiguration canonicalTypeConfiguration) {
            return canonicalTypeConfiguration;
        } else {
            LOGGER.error(String.format("Map contains entry (%s) that is not of CanonicalTypeConfiguration for ID (%s)", object, id));
        }
        return null;
    }

    /**
     * Get entry from relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     * @return String representation of given configuration class.
     */
    public static String getIndexEntry(ElasticsearchClient client, String type, String id) {
        return getIndexTypeEntry(client, type, id);
    }

    private static String getIndexTypeEntry(ElasticsearchClient client, String type, String id) {
        Object item = getIndexTypEntryObject(client, type, id);
        if (null != item) {
            return writeValueAsString(item);
        }
        return "";
    }

    public static Object getIndexTypEntryObject(ElasticsearchClient client, String type, String id) {
        initialLoadIfNecessary(client);
        Map<String, Object> itemMap = cacheMap.getOrDefault(type, Collections.emptyMap());
        if (itemMap.containsKey(id)) {
            return itemMap.get(id);
        }
        return null;
    }

    /**
     * Initially populate the internal memory map
     * @param client ES client to do look-up
     */
    public static void populateEmptyMap(ElasticsearchClient client) {
        for (String config : configList) {
            cacheMap.get(config).putAll(IndexMapper.getAllIndexEntriesAsMap(client, config));
        }
    }

    /**
     * Add given config to the relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     * @param value  the configuration value
     */
    public static void addIndexEntry(ElasticsearchClient client, String type, String id, String value) {
        IndexMapper.addIndexEntry(client, type, id, value);
        addIndexTypeEntry(client, type, id, value);
    }

    private static void addIndexTypeEntry(ElasticsearchClient client, String type, String id, String value) {
        addIndexTypeEntryObject(client, type, id, IndexMapper.validateEntry(type, id, value));
    }

    public static void addIndexTypeEntryObject(ElasticsearchClient client, String type, String id, Object value) {
        initialLoadIfNecessary(client);
        cacheMap.get(type).put(id, value);
    }

    public static void updateIndexEntry(ElasticsearchClient client, String type, String id, String value) {
        Object existing = CachedIndexMapper.getIndexTypEntryObject(client, type, id);
        if (existing != null) {
            Object updated = Mapper.updateFieldsFromJSON(existing, value);
            addIndexTypeEntryObject(client, type, id, updated);
        } else {
            throw new ValidationException("Type " + type + " does not exist for " + id);
        }
    }

    /**
     * Delete given entry from relevant index.
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @param id     the name of the config
     */
    public static void deleteIndexEntry(ElasticsearchClient client, String type, String id) {
        IndexMapper.deleteIndexEntry(client, type, id);
        deleteIndexTypeEntry(type, id);
    }

    private static void deleteIndexTypeEntry(String type, String id) {
        cacheMap.get(type).remove(id);
    }

    /**
     * Get all entries from relevant index
     *
     * @param client elastic search client
     * @param type   the type of config (i.e.  Resolver, Scorer, Model)
     * @return String representation of given configuration class.
     */
    public static String getAllIndexEntriesAsString(ElasticsearchClient client, String type) {
        initialLoadIfNecessary(client);
        return writeValueAsString(cacheMap.get(type));
    }

    static void initialLoadIfNecessary(ElasticsearchClient client) {
        if (initialLoad) {
            populateEmptyMap(client);
            initialLoad = false;
        }
    }

    /**
     * Clear out the cached mapping
     * NOTE: used for testing purposes only
     */
    public static void clearCache() {
        for (String config : configList) {
            cacheMap.get(config).clear();
        }
    }
}
