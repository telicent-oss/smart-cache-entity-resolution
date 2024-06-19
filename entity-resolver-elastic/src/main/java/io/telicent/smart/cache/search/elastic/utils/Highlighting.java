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
package io.telicent.smart.cache.search.elastic.utils;

import co.elastic.clients.elasticsearch.core.search.Hit;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.options.HighlightingOptions;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides utility methods related to applying ElasticSearch result highlighting to a returned {@link Document}
 */
public final class Highlighting {
    private static final String KEYWORD = ".keyword";

    /**
     * Private constructor prevents instantiation
     */
    private Highlighting() {
    }

    /**
     * Populates/updates a list entry with the highlighted version of the content
     *
     * @param options   Highlighting options
     * @param key       Key (optional)
     * @param value     Highlighted value to insert
     * @param listOrMap List or map to modify
     */
    @SuppressWarnings("unchecked")
    private static void populateListEntry(HighlightingOptions options, String key, String value, Object listOrMap) {
        List<Object> list = (List<Object>) listOrMap;
        listOrMap = list.get(0);
        if (listOrMap instanceof Map<?, ?>) {
            // Need to find the appropriate Map object to insert highlighted field into
            if (list.size() == 1) {
                ((Map<String, Object>) listOrMap).put(key, value);
            } else {
                String rawValue =
                        StringUtils.replace(value, options.getPreTag() != null ? options.getPreTag() : "<em>", "");
                rawValue = StringUtils.replace(rawValue, options.getPostTag() != null ? options.getPostTag() : "</em>",
                                               "");
                for (Object listEntry : list) {
                    Map<String, Object> map = (Map<String, Object>) listEntry;
                    if (Objects.equals(map.get(key), rawValue)) {
                        map.put(key, value);
                        break;
                    }
                }
            }
        } else {
            // Need to find appropriate list entry to insert highlighted field into
            if (list.size() == 1) {
                list.set(0, value);
            } else {
                String rawValue =
                        StringUtils.replace(value, options.getPreTag() != null ? options.getPreTag() : "<em>", "");
                rawValue = StringUtils.replace(rawValue, options.getPostTag() != null ? options.getPostTag() : "</em>",
                                               "");
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i), rawValue)) {
                        list.set(i, value);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets the highlighted version of the document
     *
     * @param options Highlighting options
     * @param hit     ElasticSearch hit
     * @return Highlighted document, may be {@code null} if no highlighting available for this hit
     */
    public static Document getHighlights(HighlightingOptions options, Hit<Document> hit) {
        if (MapUtils.isEmpty(hit.highlight()) || hit.source() == null || !options.isEnabled()) {
            return null;
        }

        // highlight is an unmodifiable map so create modifiable map
        final Map<String, List<String>> highlight = new HashMap<>();
        for (Map.Entry<String, List<String>> hitEntry : hit.highlight().entrySet()) {
            String key = hitEntry.getKey();
            List<String> value = hit.highlight().get(key);

            // If this is a keyword key
            if (key.endsWith(KEYWORD)) {
                String altKey = key.substring(0, key.lastIndexOf(KEYWORD));

                // If a matching key without a keyword does not exist
                // or if it does exist and the value is longer than the matching keyword value,
                // store the keyword value under the non-keyword key
                if (!hit.highlight().containsKey(altKey)
                        || (hit.highlight().containsKey(altKey)
                                && hit.highlight().get(altKey).get(0).length() > value.get(0).length())) {
                    highlight.put(altKey, value);
                }
            }
            // Add if not already added
            if (!highlight.containsKey(key)) {
                highlight.put(key, value);
            }
        }

        Document highlights = Document.copy(hit.source());
        for (Map.Entry<String, List<String>> entry : highlight.entrySet()) {
            // Highlights should only contain fields with highlighted content but skip any that don't just in case
            // ElasticSearch behaviour changes in future
            if (entry.getValue().isEmpty()) {
                continue;
            }

            // Highlighted results use the internal ElasticSearch field names as their keys so need to convert back into
            // the original document structure
            String dottedFieldName = entry.getKey();
            String[] path = dottedFieldName.split("\\.");
            Object listOrMap = highlights.getProperties();
            Map<String, Object> map = highlights.getProperties();
            if (path.length > 1) {
                for (int i = 0; i < path.length - 1; i++) {
                    listOrMap = map.computeIfAbsent(path[i], k -> new HashMap<String, Object>());
                    if (listOrMap instanceof Map<?, ?>) {
                        map = (Map<String, Object>) listOrMap;
                    } else {
                        break;
                    }
                }
            }

            // Depending on the field it could be multivalued, so we may encounter a list or map here
            if (listOrMap instanceof List<?>) {
                populateListEntry(options, path[path.length - 1], entry.getValue().get(0), listOrMap);
            } else if (listOrMap instanceof Map<?, ?>) {
                // We're setting number_of_fragments to 0 per
                // https://www.elastic.co/guide/en/elasticsearch/reference/current/highlighting.html#control-highlighted-frags
                // This means that for each highlighted field we're guaranteed to receive a single fragment containing
                // the whole field with the highlighting applied to it
                Object pathTerminal = ((Map<String, Object>) listOrMap).get(path[path.length - 1]);
                if (pathTerminal == null) {
                    // This can be the case if the field to be highlighted has been filtered out due to fine-grained
                    // security labels, in this case we don't want to reintroduce the field, so we just ignore
                } else if (pathTerminal instanceof List<?>) {
                    populateListEntry(options, path[path.length - 1], entry.getValue().get(0), pathTerminal);
                } else {
                    map.put(path[path.length - 1], entry.getValue().get(0));
                }
            }
        }
        return highlights;
    }
}
