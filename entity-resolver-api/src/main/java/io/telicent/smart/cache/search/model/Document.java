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
package io.telicent.smart.cache.search.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import io.telicent.smart.cache.search.model.utils.DocumentUtils;
import io.telicent.smart.cache.search.security.SecureSearchContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Represents a document in the search results.
 * <p>
 * This class is essentially just a container class upon which arbitrary key value pairs may be set.
 * </p>
 * <p>
 * This is by design so that the API itself does not impose any document structure and can freely exchange documents
 * regardless of their actual structure.  Actual applications using these APIs will likely have a document structure
 * that they expect.  <strong>But</strong> by not baking it into this low level of the API we allow applications to be
 * freely built with their own document structures with the Core API itself being agnostic to this.
 * </p>
 */
public class Document {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Map<String, Object> properties = new HashMap<>();

    /**
     * Creates a new blank document
     */
    public Document() {
    }

    /**
     * Creates a document with the given properties
     *
     * @param properties Properties
     */
    public Document(Map<String, Object> properties) {
        Objects.requireNonNull(properties, "Properties cannot be null");
        this.properties.putAll(properties);
    }

    /**
     * Takes a copy of the document
     *
     * @param original Original document
     * @return Copy of the document
     */
    public static Document copy(Document original) {
        return new Document(DocumentUtils.deepCopyMap(original.getProperties()));
    }

    /**
     * Sets a property
     *
     * @param key   Key
     * @param value Value
     */
    @JsonAnySetter
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    /**
     * Gets the defined properties
     *
     * @return Properties
     */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Gets the value of a property that is potentially deeply nested in the document as identified by a key sequence.
     * <p>
     * For example {@code "a", "b", "c"} would find the value of the {@code c} key that is nested under the {@code b}
     * key, that is itself nested under the top level {@code a} key.
     * </p>
     * <p>
     * This need not identify a leaf of the document, e.g. {@code "a"}, would retrieve the entire section of the
     * document under the top level {@code a} key.
     * </p>
     *
     * @param keys A key sequence indicating the portion of the document you want to retrieve.
     * @return Value of the property, or {@code null} if the given key sequence does not identify a valid portion of the
     * document.
     */
    public Object getProperty(String... keys) {
        Map<String, Object> map = this.properties;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            Object temp = map.get(key);
            if (temp == null) {
                return null;
            }
            if (temp instanceof Map<?, ?>) {
                try {
                    map = (Map<String, Object>) temp;
                } catch (ClassCastException e) {
                    return null;
                }
            } else if (i == keys.length - 1) {
                return temp;
            } else {
                return null;
            }
        }
        return map;
    }

    /**
     * Gets whether the document is empty
     *
     * @return True if empty, false otherwise
     */
    @JsonIgnore
    public boolean isEmpty() {
        return MapUtils.isEmpty(this.properties);
    }

    /**
     * Removes all security labels from the document without applying them, only intended for usage when security is
     * disabled.
     * <p>
     * To apply security labels you <strong>MUST</strong> call {@link #filter(SecureSearchContext, boolean)} instead.
     * </p>
     */
    public void trimSecurityLabels() {
        trimSecurityLabels(this.properties);
    }

    private void trimSecurityLabels(Map<String, Object> map) {
        // Trim any security labels at this level
        map.entrySet().removeIf(Document::isSecurityLabelsField);

        // Recurse down to trim any finer grained security labels
        for (Map.Entry<String, Object> field : map.entrySet()) {
            if (field.getValue() instanceof Map<?, ?>) {
                trimSecurityLabels((Map<String, Object>) field.getValue());
            } else if (field.getValue() instanceof List<?>) {
                trimSecurityLabels((List<Object>) field.getValue());
            }
        }
    }

    private void trimSecurityLabels(List<Object> list) {
        for (Object item : list) {
            if (item instanceof Map<?, ?>) {
                trimSecurityLabels((Map<String, Object>) item);
            }
        }
    }

    /**
     * Filters the document based upon fine-grained security labels in the document (security labels hidden)
     *
     * @param context         Secure search context
     * @param defaultDecision Default decision to use for fields that don't have a more specific label.  This is
     *                        typically the result of evaluating the default labels for the document.  A default
     *                        decision of {@code true} means the user can see anything that doesn't have more specific
     *                        labels, whereas {@code false} means they cannot see anything <strong>UNLESS</strong> the
     *                        specific labels permit it.
     */
    public void filter(SecureSearchContext context, boolean defaultDecision) {
        filterMap(context, defaultDecision, this.properties, false);
    }

    /**
     * Filters the document based upon fine-grained security labels in the document
     *
     * @param context            Secure search context
     * @param defaultDecision    Default decision to use for fields that don't have a more specific label.  This is
     *                           typically the result of evaluating the default labels for the document.  A default
     *                           decision of {@code true} means the user can see anything that doesn't have more
     *                           specific labels, whereas {@code false} means they cannot see anything
     *                           <strong>UNLESS</strong> the specific labels permit it.
     * @param showSecurityLabels Show security labels flag
     */
    public void filter(SecureSearchContext context, boolean defaultDecision, boolean showSecurityLabels) {
        filterMap(context, defaultDecision, this.properties, showSecurityLabels);
    }

    private void filterMap(SecureSearchContext context, boolean defaultDecision, Map<String, Object> map,
                           boolean showSecurityLabels) {
        // Recurse down to do inner filtering i.e. filtering the complex objects deeper in the document
        for (Map.Entry<String, Object> field : map.entrySet()) {
            if (isSecurityLabelsField(field)) {
                // Ignore for now, we'll strip these out of the map later once we've used the
                continue;
            }
            if (field.getValue() instanceof Map<?, ?>) {
                filterMap(context, defaultDecision, (Map<String, Object>) field.getValue(), showSecurityLabels);
            } else if (field.getValue() instanceof List<?>) {
                filterList(context, defaultDecision, field.getKey(), (List<Object>) field.getValue(), map,
                           showSecurityLabels);
            }
        }

        // Filter fields based on the outcome of inner filtering operations
        map.entrySet().removeIf(field -> {
            if (field.getValue() instanceof Map<?, ?>) {
                return MapUtils.isEmpty((Map<?, ?>) field.getValue());
            } else if (field.getValue() instanceof List<?>) {
                return CollectionUtils.isEmpty((Collection<?>) field.getValue());
            } else {
                String securityLabelsForField = getSecurityLabelsForField(map, field.getKey());
                if (securityLabelsForField == null) {
                    return !defaultDecision;
                } else {
                    List<AttributeExpr> labels = context.parseLabelExpressions(null, securityLabelsForField);
                    if (CollectionUtils.isEmpty(labels)) {
                        return true;
                    } else {
                        return !context.evaluate(labels);
                    }
                }
            }
        });

        // Finally throw out any leftover security labels field if option set
        if (!showSecurityLabels) {
            map.entrySet().removeIf(Document::isSecurityLabelsField);
        }
    }

    private void filterList(SecureSearchContext context, boolean defaultDecision, String listField, List<Object> list,
                            Map<String, Object> parentMap, boolean showSecurityLabels) {
        boolean allComplex = true;
        for (Object item : list) {
            if (item instanceof Map<?, ?>) {
                filterMap(context, defaultDecision, (Map<String, Object>) item, showSecurityLabels);
            } else {
                allComplex = false;
            }
        }
        if (allComplex) {
            // If all the items in the list are complex, i.e. maps, that we've done inner filtering on we can now throw
            // out any that have ended up empty as a result of that filtering
            list.removeIf(item -> MapUtils.isEmpty((Map<?, ?>) item));
            return;
        }

        // It's a simple list i.e. it doesn't contain complex objects that need inner filtering so filter the list
        // entries based upon the labels list that accompanies the list in the parent map
        List<String> securityLabelsForList = getSecurityLabelsForList(parentMap, listField);
        if (securityLabelsForList != null) {
            int index = 0;
            while (index < list.size()) {
                String rawLabelsForListItem = securityLabelsForList.get(index);
                if (StringUtils.isBlank(rawLabelsForListItem)) {
                    // No more specific labels for the item so use the default decision
                    if (!defaultDecision) {
                        list.remove(index);
                        securityLabelsForList.remove(index);
                    } else {
                        index++;
                    }
                } else {
                    // Apply more specific labels for the item
                    List<AttributeExpr> labelsForListItem =
                            context.parseLabelExpressions(null, securityLabelsForList.get(index));
                    if (CollectionUtils.isEmpty(labelsForListItem) || !context.evaluate(labelsForListItem)) {
                        list.remove(index);
                        securityLabelsForList.remove(index);
                    } else {
                        index++;
                    }
                }
            }
        } else {
            // No more specific labels for the list so use the default decision
            if (!defaultDecision) {
                list.clear();
            }
        }
    }

    private String getSecurityLabelsForField(Map<String, Object> map, String field) {
        return (String) ((Map<String, Object>) map.getOrDefault(DefaultOutputFields.SECURITY_LABELS,
                                                                Collections.emptyMap())).getOrDefault(field, null);
    }

    private List<String> getSecurityLabelsForList(Map<String, Object> map, String field) {
        return (List<String>) ((Map<String, Object>) map.getOrDefault(DefaultOutputFields.SECURITY_LABELS,
                                                                      Collections.emptyMap())).getOrDefault(field,
                                                                                                            null);
    }

    private static boolean isSecurityLabelsField(Map.Entry<String, Object> field) {
        return StringUtils.equals(field.getKey(), DefaultOutputFields.SECURITY_LABELS) || StringUtils.endsWith(
                field.getKey(), DefaultOutputFields.SECURITY_LABELS);
    }

    @Override
    public String toString() {
        try {
            return JSON.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (!(obj instanceof Document)) {
            return false;
        }

        Document other = (Document) obj;
        return DocumentUtils.deepEqualsMap(this.properties, other.properties);
    }

}
