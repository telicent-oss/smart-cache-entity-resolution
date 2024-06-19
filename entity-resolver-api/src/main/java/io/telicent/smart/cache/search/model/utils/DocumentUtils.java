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

import java.util.*;

/**
 * Utility methods related to working with Documents
 */
public final class DocumentUtils {

    /**
     * Private constructor to prevent direct instantiation
     */
    private DocumentUtils() {

    }

    /**
     * Takes a deep copy of a map
     * <p>
     * A deep copy means that child maps and lists are deep copied, individual leaf values in the map and list are not
     * necessarily deep copied.  However generally deep copying the container data structures is sufficient to create a
     * separate copy of the document since generally leaf values are replaced rather than manipulated in place.
     * </p>
     *
     * @param original Original map
     * @return Deep copy
     */
    public static Map<String, Object> deepCopyMap(Map<String, Object> original) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> m) {
                copy.put(entry.getKey(), deepCopyMap((Map<String, Object>) m));
            } else if (entry.getValue() instanceof List<?> l) {
                copy.put(entry.getKey(), deepCopyList((List<Object>) l));
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    /**
     * Takes a deep copy of a list
     * <p>
     * A deep copy means that child maps and lists are deep copied, individual leaf values in the map and list are not
     * necessarily deep copied.  However generally deep copying the container data structures is sufficient to create a
     * separate copy of the document since generally leaf values are replaced rather than manipulated in place.
     * </p>
     *
     * @param original Original map
     * @return Deep copy
     */
    private static List<Object> deepCopyList(List<Object> original) {
        List<Object> copy = new ArrayList<>();
        for (Object value : original) {
            if (value instanceof Map<?, ?>) {
                copy.add(deepCopyMap((Map<String, Object>) value));
            } else if (value instanceof List<?>) {
                copy.add(deepCopyList((List<Object>) value));
            } else {
                copy.add(value);
            }
        }
        return copy;
    }

    /**
     * Performs deep equality checking on the given values.
     * <p>
     * Deep equals means that if the values are maps/lists then they must be deeply equals as determined by calling
     * {@link #deepEqualsMap(Map, Map)} or {@link #deepEqualsList(List, List)}.  Any other values fall through to
     * checking for equality via {@link Objects#equals(Object, Object)}.
     * </p>
     *
     * @param a A value
     * @param b Another value
     * @return True if deeply equals, false otherwise
     */
    private static boolean deepEqualsValue(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            // Can just return false here, if both a and b are null then the reference equality check in the prior
            // branch has already detected this
            return false;
        } else if (a instanceof Map<?, ?>) {
            if (b instanceof Map<?, ?>) {
                return deepEqualsMap((Map<String, Object>) a, (Map<String, Object>) b);
            } else {
                return false;
            }
        } else if (a instanceof List<?>) {
            if (b instanceof List<?>) {
                return deepEqualsList((List<Object>) a, (List<Object>) b);
            } else {
                return false;
            }
        } else {
            return Objects.equals(a, b);
        }
    }

    /**
     * Performs deep equality checking on a map
     * <p>
     * Deep equality means that any child maps and lists are deeply compared for equality by recursively calling this
     * method or {@link #deepEqualsList(List, List)}.  Values that are not a Map/List are compared for equality via
     * {@link Objects#equals(Object, Object)} which calls the {@code equals(Object)} of the actual value instances.
     * </p>
     *
     * @param a Map
     * @param b Another map
     * @return True if deeply equals, false otherwise
     */
    public static boolean deepEqualsMap(Map<String, Object> a, Map<String, Object> b) {
        if (a == b) {
            return true;
        } else if (a.size() != b.size()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : a.entrySet()) {
            if (!b.containsKey(entry.getKey())) {
                return false;
            }

            Object aValue = entry.getValue();
            Object bValue = b.get(entry.getKey());
            if (!deepEqualsValue(aValue, bValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs deep equality checking on a list
     * <p>
     * Deep equality means that any child maps and lists are deeply compared for equality by recursively calling this
     * method or {@link #deepEqualsMap(Map, Map)}.  Values that are not a Map/List are compared for equality via
     * {@link Objects#equals(Object, Object)} which calls the {@code equals(Object)} of the actual value instances.
     * </p>
     *
     * @param a List
     * @param b Another List
     * @return True if deeply equals, false otherwise
     */
    private static boolean deepEqualsList(List<Object> a, List<Object> b) {
        if (a == b) {
            return true;
        } else if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {
            Object aValue = a.get(i);
            Object bValue = b.get(i);

            if (!deepEqualsValue(aValue, bValue)) {
                return false;
            }
        }
        return true;
    }
}
