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
package io.telicent.smart.cache.entity.resolver;


import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.SearchBackend;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.options.SecurityOptions;

import java.util.List;

/**
 * An entity resolver allows matching input documents representing entities against previously indexed entities in order
 * to perform data deduplication, ID resolution etc.
 */
public interface EntityResolver extends SearchBackend {
    /**
     * Find documents similar to the input one
     *
     * @param doc             input document
     * @param maxResults      max number of results per input
     * @param minScore        minimum score for a match
     * @param securityOptions security options
     * @param overrides       string representation of config override
     * @return SimilarityResult
     **/
    SimilarityResult findSimilar(Document doc, int maxResults, float minScore,
                                 SecurityOptions securityOptions, String overrides);
    /**
     * Find documents similar to the ones passed as input
     *
     * @param docs            input documents
     * @param maxResults      max number of results per input
     * @param minScore        minimum score for a match
     * @param withinInput     whether to return similarities between input documents
     * @param securityOptions security options
     * @param overrides       string representation of config override
     * @return SimilarityResult
     **/
    SimilarityResults findSimilar(List<Document> docs, int maxResults, float minScore,
                                  boolean withinInput, SecurityOptions securityOptions, String overrides);


    SimilarityResult findSimilarV2(Document doc, int maxResults, float minScore,
                                   SecurityOptions securityOptions, String modelId);

    SimilarityResults findSimilarV2(List<Document> docs, int maxResults, float minScore,
                                    boolean withinInput, SecurityOptions securityOptions, String modelId);


    /**
     * Add config entry
     *
     * @param type      type of config
     * @param entry     json string representation
     * @param id        unique id
     **/
    void addConfig(String type, String entry, String id);
    /**
     * Update config entry
     *
     * @param type      type of config
     * @param entry     json string representation
     * @param id        unique id
     **/
    void updateConfig(String type, String entry, String id);
    /**
     * Delete config entry
     *
     * @param type      type of config
     * @param id        unique id
     **/

    void deleteConfig(String type, String id);
    /**
     * Get config entry
     *
     * @param type      type of config
     * @param id        unique id
     * @return String   json representation of config
     **/
    String readConfig(String type, String id);

    /**
     * Get all config entries
     *
     * @param type      type of config
     * @return A list of JSON Strings representation of a list of config
     **/
    String readAllConfig(String type);

    /**
     * Where possible validate the given configuration
     * @param type  type of config
     * @param id    unique id of config
     * @param index index to which config may be applied
     * @return String representation of config details
     */
    String validateConfig(String type, String id, String index);
}
