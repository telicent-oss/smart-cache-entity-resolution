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
package io.telicent.smart.cache.search.model.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * An abstract content visitor that walks a document content structure to visit all the leaf nodes of the document.  A
 * document structure is defined in terms of a {@code Map<String, Object>} with the actual actions upon visiting the
 * leafs of the structure defined by the derived concrete implementation.
 */
public abstract class ContentLeafVisitor {

    private final String[] ignoredFields;

    /**
     * Creates a new content visitor
     *
     * @param ignoredFields Ignored fields, these specify portions of the document structure that we will not walk
     */
    public ContentLeafVisitor(String[] ignoredFields) {
        this.ignoredFields = ignoredFields;
    }

    /**
     * Visits the provided content
     *
     * @param map Content
     */
    public void visit(Map<String, Object> map) {
        visitInternal(map, new Stack<>());
    }

    /**
     * Internal handler for walking the map portions of the document structure
     * <p>
     * This will either recurse further by calling the relevant {@link #visitInternal(Map, Stack)} or
     * {@link #visitInternal(List, Stack)} method, of for leaf fields call {@link #visitLeafField(String[], Object)}.
     * </p>
     *
     * @param fields Map that represents the current level of the document structure we are walking
     * @param keys   Path of keys to the level of the document structure we are walking
     */
    protected void visitInternal(Map<String, Object> fields, Stack<String> keys) {
        String[] path = new String[keys.size() + 1];
        keys.toArray(path);
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            keys.push(field.getKey());
            if (!isIgnoredField(keys)) {
                if (field.getValue() instanceof Map<?, ?>) {
                    visitInternal((Map<String, Object>) field.getValue(), keys);
                } else if (field.getValue() instanceof List<?>) {
                    visitInternal((List<Object>) field.getValue(), keys);
                } else {
                    path[path.length - 1] = field.getKey();
                    if (!isIgnoredField(path)) {
                        visitLeafField(path, field.getValue());
                    }
                }
            }
            keys.pop();
        }
    }

    /**
     * Checks whether a field is considered as ignored
     *
     * @param keys Path of keys to the field in question
     * @return True if ignored, false otherwise
     */
    protected boolean isIgnoredField(Stack<String> keys) {
        String fullFieldName = StringUtils.join(keys, ".");
        return StringUtils.equalsAnyIgnoreCase(keys.get(0), this.ignoredFields) || StringUtils.equalsAnyIgnoreCase(
                fullFieldName, this.ignoredFields);
    }

    /**
     * Checks whether a field is considered as ignored
     *
     * @param keys Path of keys to the field in question
     * @return True if ignored, false otherwise
     */
    protected boolean isIgnoredField(String[] keys) {
        String fullFieldName = StringUtils.join(keys, ".");
        return StringUtils.equalsAnyIgnoreCase(keys[0], this.ignoredFields) || StringUtils.equalsAnyIgnoreCase(
                fullFieldName, this.ignoredFields);
    }

    /**
     * Internal handler for walking the list portions of the document structure
     * <p>
     * Note that this method does not perform deep recursion into nested lists, instead it calls the appropriate
     * function for the list items it encounters.  This will be one of {@link #visitListItem(String[], Object)},
     * {@link #visitComplexListItem(String[], Object)} or {@link #visitNestedListItem(String[], List)}.
     * </p>
     *
     * @param items List that represents the current level of the document structure that is being walked
     * @param keys  Path of keys to the list that is currently being walked
     */
    protected void visitInternal(List<Object> items, Stack<String> keys) {
        String[] path = keys.toArray(new String[0]);
        if (this.isIgnoredField(path)) {
            return;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?>) {
                visitComplexListItem(path, item);
            } else if (item instanceof List<?>) {
                visitNestedListItem(path, (List<Object>) item);
            } else {
                visitListItem(path, item);
            }
        }
    }

    /**
     * Visits a leaf field
     *
     * @param path Path of keys to that leaf field
     * @param item Item
     */
    public abstract void visitLeafField(String[] path, Object item);

    /**
     * Visits a leaf field that is a complex item in a list
     *
     * @param path Path of keys to the leaf list that contains the item
     * @param item Item
     */
    public abstract void visitComplexListItem(String[] path, Object item);

    /**
     * Visits a leaf field that is itself a nested list
     *
     * @param path Path of keys to the leaf list that contains the nested list
     * @param item Nested list
     */
    public abstract void visitNestedListItem(String[] path, List<Object> item);

    /**
     * Visits a leaf field that is an item in a list
     *
     * @param path Path of keys to the leaf list that contains the item
     * @param item Item
     */
    public abstract void visitListItem(String[] path, Object item);
}
