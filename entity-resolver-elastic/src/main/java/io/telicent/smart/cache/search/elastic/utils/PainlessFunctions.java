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

/**
 * Provides some useful Painless functions for working with the document structures we generate.
 * <p>
 * In particular there are a number of functions for manipulating lists within the document structure so that we
 * effectively treat lists as being sets that we mutate.  For example see {@link #ADD_TO_COMPLEX_LIST} and
 * {@link #REMOVE_FROM_COMPLEX_LIST}.
 * </p>
 */
public final class PainlessFunctions {

    /**
     * The name of the {@link #ADD_TO_LIST} function
     */
    public static final String ADD_TO_LIST_NAME = "addToList";

    /**
     * A Painless function that adds a simple item to a list
     */
    public static final String ADD_TO_LIST = """
            void addToList(def list, def item) {
              if (!list.contains(item)) {
                list.add(item);
              }
            }
            """;

    /**
     * The name of the {@link #REMOVE_FROM_LIST} function
     */
    public static final String REMOVE_FROM_LIST_NAME = "removeFromList";

    /**
     * A Painless function that removes a simple item from a list
     */
    public static final String REMOVE_FROM_LIST = """
            void removeFromList(def list, def item) {
              if (list.contains(item)) {
                list.remove(list.indexOf(item));
              }
            }
            """;

    /**
     * The name of the {@link #ADD_TO_COMPLEX_LIST} function
     */
    public static final String ADD_TO_COMPLEX_LIST_NAME = "addToComplexList";

    /**
     * A Painless function that adds/replaces an item in a complex list
     */
    public static final String ADD_TO_COMPLEX_LIST = """
            void addToComplexList(def list, def item, def keyField) {
              int foundIdx = -1;
              for (int i = 0; i < list.size(); i++) {
                if (list[i][keyField] == item[keyField]) {
                  foundIdx = i;
                  break;
                }
              }
              if (foundIdx == -1) {
                list.add(item);
              } else {
                list[foundIdx] = item;
              }
            }
            """;

    /**
     * The name of the {@link #REMOVE_FROM_COMPLEX_LIST} function
     */
    public static final String REMOVE_FROM_COMPLEX_LIST_NAME = "removeFromComplexList";

    /**
     * A Painless function that removes an item from a complex list
     */
    public static final String REMOVE_FROM_COMPLEX_LIST = """
            void removeFromComplexList(def list, def item, def keyField) {
              int foundIdx = -1;
              for (int i = 0; i < list.size(); i++) {
                if (list[i][keyField] == item[keyField]) {
                  foundIdx = i;
                  break;
                }
              }
              if (foundIdx != -1) {
                list.remove(foundIdx);
              }
            }
            """;

    /**
     * Name of the {@link #HAS_FIELD} function
     */
    public static final String HAS_FIELD_NAME = "hasField";

    /**
     * A Painless function that checks whether a given field exists
     */
    public static final String HAS_FIELD = """
            boolean hasField(def map, List keys) {
              for (int i = 0; i < keys.size(); i++) {
                if (map[keys[i]] == null) {
                    return false;
                }
                map = map[keys[i]];
              }
              return map != null;
            }
            """;

    /**
     * Name of the {@link #ENSURE_MAP_FIELD} function
     */
    public static final String ENSURE_MAP_FIELD_NAME = "ensureMapField";

    /**
     * A Painless function that ensures that a map field exists
     */
    public static final String ENSURE_MAP_FIELD = """
            def ensureMapField(def map, List keys) {
              for (int i = 0; i < keys.size(); i++) {
                if (map[keys[i]] == null) {
                    map[keys[i]] = [:];
                }
                map = map[keys[i]];
              }
              return map;
            }
            """;

    /**
     * The name of the {@link #ENSURE_LIST_FIELD} function
     */
    public static final String ENSURE_LIST_FIELD_NAME = "ensureListField";

    /**
     * A Painless function that ensures that a leaf list field exists
     */
    public static final String ENSURE_LIST_FIELD = """
            def ensureListField(def map, List keys) {
              for (int i = 0; i < keys.size(); i++) {
                if (map[keys[i]] == null) {
                  if (i == keys.size() - 1) {
                    map[keys[i]] = [];
                  } else {
                    map[keys[i]] = [:];
                  }
                }
                map = map[keys[i]];
              }
              return map;
            }
            """;

    private PainlessFunctions() {
    }
}
