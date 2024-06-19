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
package io.telicent.smart.cache.search.elastic;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.telicent.smart.cache.search.IndexManager;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.configuration.IndexConfiguration;
import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import io.telicent.smart.cache.search.elastic.schema.ElasticMappings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration.DEFAULT_INDEX_SETTINGS_FILE_KEY;
import static io.telicent.smart.cache.canonical.configuration.CanonicalSearchConfiguration.DEFAULT_INDEX_SETTINGS_FILE_VALUE;

/**
 * An Index Manager backed by ElasticSearch
 */
public class ElasticIndexManager extends AbstractElasticClient implements IndexManager<SimpleMappingRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticIndexManager.class);

    private static final String USERNAME_PLACEHOLDER = "${elastic.username}";
    private static final String PASSWORD_PLACEHOLDER = "${elastic.password}";

    /**
     * Creates a new ElasticSearch backed index manager
     *
     * @param elasticHost ElasticSearch host
     * @param elasticPort ElasticSearch port
     */
    public ElasticIndexManager(String elasticHost, int elasticPort) {
        super(elasticHost, elasticPort, null, null, false);
    }

    /**
     * Creates a new ElasticSearch backed index manager
     *
     * @param elasticHost ElasticSearch host
     * @param elasticPort ElasticSearch port
     * @param username    ElasticSearch user
     * @param password    ElasticSearch password
     */
    public ElasticIndexManager(String elasticHost, int elasticPort, String username, String password) {
        super(elasticHost, elasticPort, username, password, false);
    }

    /**
     * Creates a new ElasticSearch backed index manager
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param username                 ElasticSearch user
     * @param password                 ElasticSearch password
     * @param makeOpenSearchCompatible Whether to attempt to make the client "compatible", in so far as is possible,
     *                                 with OpenSearch servers
     */
    protected ElasticIndexManager(String elasticHost, int elasticPort, String username, String password,
                                  boolean makeOpenSearchCompatible) {
        this(elasticHost, elasticPort, username, password, null, makeOpenSearchCompatible);
    }

    /**
     * Creates a new ElasticSearch backed index manager
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param username                 ElasticSearch user
     * @param password                 ElasticSearch password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     * @param makeOpenSearchCompatible Whether to attempt to make the client "compatible", in so far as is possible,
     *                                 with OpenSearch servers
     */
    protected ElasticIndexManager(String elasticHost, int elasticPort, String username, String password,
                                  String elasticTlsCaCert, boolean makeOpenSearchCompatible) {
        super(elasticHost, elasticPort, username, password, elasticTlsCaCert, makeOpenSearchCompatible);
    }

    @Override
    public Boolean hasIndex(String name) {
        // ElasticSearch indices cannot have empty/null names so short-circuit bothering to ask Elastic this
        if (StringUtils.isBlank(name)) {
            return false;
        }

        try {
            BooleanResponse response = this.client.indices().exists(e -> e.index(name));
            return response.value();
        } catch (IOException e) {
            LOGGER.warn("Unable to determine if ElasticSearch index {} exists", name);
            return null;
        }
    }

    /**
     * Gets the ElasticSearch internal ID for the index with the given name
     * <p>
     * Index names in ElasticSearch are basically pointers to an actual underlying index which is given a unique ID.  If
     * the index is dropped and recreated with the same name then it will be assigned a fresh ID.  Thus, knowledge of
     * this internal ID can be used to detect when the underlying index has been changed.
     * </p>
     *
     * @param name Index name
     * @return Internal ID of the index, or {@code null} if no such index
     */
    public String getInternalId(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        try {
            GetIndexResponse response = this.client.indices().get(i -> i.index(name));
            if (!response.result().containsKey(name)) {
                return null;
            }
            return response.result().get(name).settings().index().uuid();
        } catch (ElasticsearchException e) {
            if (e.response().status() == 404) {
                return null;
            } else {
                LOGGER.warn("Failed to retrieve internal ID for ElasticSearch index {}: {}", name, e.getMessage());
                return null;
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to find internal ID for ElasticSearch index {}", name);
            return null;
        }
    }

    /**
     * Gets the index mappings currently configured for the given index
     * <p>
     * This is public primarily for unit test purposes in being able to verify that the expected index mappings are
     * generated based on the abstract {@link IndexConfiguration} used to create an index via this manager.
     * </p>
     *
     * @param name Index name
     * @return Index mappings
     * @throws SearchException Thrown if the index mappings cannot be retrieved
     */
    public IndexMappingRecord getIndexMappings(String name) {
        try {
            GetMappingResponse response = client.indices().getMapping(f -> f.index(name));
            IndexMappingRecord mappings = response.get(name);
            if (mappings == null) {
                throw noIndexMappingsFound(name);
            }
            return mappings;
        } catch (IOException e) {
            throw noIndexMappingsFound(name);
        }
    }

    private static SearchException noIndexMappingsFound(String name) {
        return new SearchException(String.format("Unable to find Index Mappings for ElasticSearch index %s", name));
    }

    @Override
    public Boolean createIndex(String name, IndexConfiguration<SimpleMappingRule> configuration) {
        List<Map<String, DynamicTemplate>> templates = new ArrayList<>();
        Map<String, Property> properties = new HashMap<>();
        configuration.getRules().forEach(rule -> ElasticMappings.ruleToElasticMapping(rule, templates, properties));

        // load the setting file specified in the configuration's properties
        // or the default value if none has been set
        String resourceFile = (String) configuration.getProperties()
                                                    .getOrDefault(DEFAULT_INDEX_SETTINGS_FILE_KEY,
                                                                  DEFAULT_INDEX_SETTINGS_FILE_VALUE);

        Optional<Boolean> success = createIndexWithSettingsFromFile(name, resourceFile, templates, properties);

        // can't connect?
        if (success.isEmpty()) {
            return null;
        }

        if (success.isPresent() && success.get()) {
            LOGGER.info("Successfully created ElasticSearch index {} using resource file {}", name, resourceFile);
            return true;
        }

        resourceFile = "index_settings_backup.json";

        LOGGER.warn("Failed to create ElasticSearch index {} - trying resource file {}", name, resourceFile);

        // try a back-up version e.g. without the synonyms
        success = createIndexWithSettingsFromFile(name, resourceFile, templates, properties);

        // can't connect?
        if (success.isEmpty()) {
            return null;
        }

        if (success.isPresent() && success.get()) {
            LOGGER.info("Successfully created ElasticSearch index {} using resource file {}", name, resourceFile);
            return true;
        }

        LOGGER.warn("Failed to create ElasticSearch index {}", name);
        return success.get();
    }

    /**
     * Creates a new ElasticSearch index given a resource file
     *
     * @param indexName    Name of the index to generate
     * @param resourceFile Name of the file in the resources directory to use for the settings
     * @param templates    Templates
     * @param properties   Properties
     * @return boolean true if succeeded, false otherwise
     */
    private Optional<Boolean> createIndexWithSettingsFromFile(final String indexName, final String resourceFile,
                                                              final List<Map<String, DynamicTemplate>> templates,
                                                              final Map<String, Property> properties) {

        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceFile);

        if (is == null) {
            LOGGER.warn("Couldn't read resource file {} to build the index", resourceFile);
            return Optional.of(false);
        }

        try {
            final String s = processCredentialsInFile(is);
            is = new ByteArrayInputStream(s.getBytes());
        } catch (Exception e) {
            LOGGER.warn("Error reading resource file {} to build the index", resourceFile, e);
            return Optional.of(false);
        }

        final IndexSettings settings = new IndexSettings.Builder().withJson(is).build();

        try {
            CreateIndexResponse response = this.client.indices()
                                                      .create(i -> i.index(indexName)
                                                                    .settings(settings)
                                                                    .mappings(m -> m.dynamic(DynamicMapping.True)
                                                                                    .dynamicTemplates(templates)
                                                                                    .properties(properties)));
            return Optional.of(response.acknowledged());
        } catch (ElasticsearchException e) {
            LOGGER.warn("Failed to create ElasticSearch index {}: {}", indexName, e.getMessage());
            return Optional.of(false);
        } catch (IOException e) {
            LOGGER.warn("Unable to determine if ElasticSearch index {} was created", indexName);
            return Optional.empty();
        }
    }

    @Override
    public List<String> listIndices() {
        try {
            IndicesResponse response = this.client.cat().indices();

            return response.valueBody().stream().map(IndicesRecord::index).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.warn("Unable to list ElasticSearch indices successfully");
        }
        return Collections.emptyList();
    }

    @Override
    public Boolean deleteIndex(String name) {
        try {
            DeleteIndexResponse response = this.client.indices().delete(d -> d.index(name));
            if (Boolean.TRUE.equals(response.acknowledged())) {
                LOGGER.info("Successfully deleted ElasticSearch index {}", name);
                return true;
            } else {
                LOGGER.warn("Failed to delete ElasticSearch index {}", name);
                return false;
            }
        } catch (ElasticsearchException e) {
            LOGGER.warn("Failed to delete ElasticSearch index {}: {}", name, e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.warn("Unable to determine if ElasticSearch index {} was deleted successfully", name);
            return null;
        }
    }

    /**
     * Overrides placeholder values with credential values
     *
     * @param is InputStream of the resource file containing settings
     * @return String representation of updated stream
     */
    private String processCredentialsInFile(InputStream is) throws IOException {
        // specify the credentials for Elastic if needed e.g. in the
        // graph synonym filter
        // read the file line by line
        // if a line containing a placeholder is found but the value is null
        // then that line is skipped, otherwise replace with the value
        // see https://github.com/Telicent-io/smart-cache-search/issues/190

        final StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.contains(PASSWORD_PLACEHOLDER)) {
                    if (StringUtils.isBlank(password)) {
                        continue;
                    }
                    // substitute the value
                    line = line.replaceFirst(Pattern.quote(PASSWORD_PLACEHOLDER), password);
                } else if (line.contains(USERNAME_PLACEHOLDER)) {
                    if (StringUtils.isBlank(username)) {
                        continue;
                    }
                    // substitute the value
                    line = line.replaceFirst(Pattern.quote(USERNAME_PLACEHOLDER), username);
                }
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
