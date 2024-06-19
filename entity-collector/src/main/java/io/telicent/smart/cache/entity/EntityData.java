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
package io.telicent.smart.cache.entity;

import org.apache.jena.graph.Node;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents some data about an entity
 * <p>
 * This is in the form of a map of predicates to object(s).
 * </p>
 */
public final class EntityData {
    private final Map<Node, List<SecurityLabelledNode>> items;

    /**
     * Creates new entity data
     *
     * @param data Data
     */
    private EntityData(Map<Node, List<SecurityLabelledNode>> data) {
        this.items = Collections.unmodifiableMap(data);
    }

    /**
     * Gets whether this data is empty
     *
     * @return True if empty, false otherwise
     */
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    /**
     * Indicates whether this entity data is simple i.e. it has a single key with a single value
     * <p>
     * This impacts whether callers can use convenience {@link #get()} and {@link #key()} methods as opposed to using
     * the {@link #get(Node)} and {@link #keys()} methods.
     * </p>
     *
     * @return True if simple, false otherwise
     */
    public boolean isSimple() {
        if (this.items.size() > 1) {
            return false;
        }

        List<SecurityLabelledNode> nodes = this.items.values().stream().findFirst().orElse(null);
        return nodes != null && nodes.size() == 1;
    }

    /**
     * Gets the single value contained in this entity data if, and only if, there is only a single key with a single
     * value defined
     * <p>
     * Consumers can call {@link #isSimple()} to determine whether this method is usable
     * </p>
     *
     * @return Single value
     * @throws IllegalStateException Thrown if the entity data has multiple keys defined, or a single key with multiple
     *                               values
     */
    public SecurityLabelledNode get() {
        if (this.items.isEmpty()) {
            return null;
        } else if (this.items.size() == 1) {
            List<SecurityLabelledNode> nodes = this.items.values().stream().findFirst().orElse(Collections.emptyList());
            if (nodes.size() == 1) {
                return nodes.get(0);
            }
            throw new IllegalStateException(
                    "This EntityData contains multiple values for the given key, use get(Node key) to retrieve values");
        } else {
            throw new IllegalStateException(
                    "This EntityData contains multiple keys, use get(Node key) to retrieve values");
        }
    }

    /**
     * Gets all the data associated with a given key in this data
     *
     * @param key Key
     * @return Associated data
     */
    public List<SecurityLabelledNode> get(Node key) {
        return this.items.get(key);
    }

    /**
     * Gets the single key contained in this entity data if, and only if, there is only a single key defined
     * <p>
     * Consumers can call {@link #isSimple()} to determine whether this method is usable
     * </p>
     *
     * @return Single key
     * @throws IllegalStateException Thrown if the entity data has multiple keys defined
     */
    public Node key() {
        if (this.items.isEmpty()) {
            return null;
        } else if (this.items.size() == 1) {
            return this.items.keySet().stream().findFirst().orElse(null);
        } else {
            throw new IllegalStateException(
                    "This EntityData contains multiple keys, use keys() instead to obtain keys");
        }
    }

    /**
     * Gets all the keys contained in this entity data
     *
     * @return Keys
     */
    public Stream<Node> keys() {
        return this.items.keySet().stream();
    }

    /**
     * Gets whether the data has any fine-grained security labels associated with it
     *
     * @return Security Labels
     */
    public boolean hasSecurityLabels() {
        return this.items.values()
                         .stream()
                         .anyMatch(l -> l.stream()
                                         .filter(Objects::nonNull)
                                         .anyMatch(SecurityLabelledNode::hasSecurityLabel));
    }

    /**
     * Creates a new builder that provides a fluent builder API for building an {@link EntityData} instance
     * <p>
     * For simple entity data use {@link #of(Node, Node)} or {@link #of(Node, Node, String)} instead.
     * </p>
     *
     * @return Builder
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Creates new {@link EntityData} that contains a single key value pair.
     * <p>
     * For more complex entity data use {@link #create()} and the {@link Builder} API.
     * </p>
     *
     * @param key   Key
     * @param value Value
     * @return Entity Data
     */
    public static EntityData of(Node key, Node value) {
        return new Builder().add(key, value).build();
    }

    /**
     * Creates new {@link EntityData} that contains a single key value pair.
     * <p>
     * For more complex entity data use {@link #create()} and the {@link Builder} API.
     * </p>
     *
     * @param key            Key
     * @param value          Value
     * @param securityLabels Security labels for the value
     * @return Entity Data
     */
    public static EntityData of(Node key, Node value, String securityLabels) {
        return new Builder().add(key, value, securityLabels).build();
    }

    /**
     * Creates new {@link EntityData} with a single key and multiple values
     *
     * @param key    Key
     * @param values Security labelled values
     * @return Entity Data
     */
    public static EntityData of(Node key, Stream<SecurityLabelledNode> values) {
        return new Builder().add(key, values).build();
    }

    /**
     * Provides a builder interface for building {@link EntityData} instances
     */
    public static class Builder {
        private final Map<Node, List<SecurityLabelledNode>> items = new LinkedHashMap<>();

        /**
         * Adds a key value pair to the entity data.  The value will have no security labels associated with it.
         *
         * @param key   Key
         * @param value Value
         * @return Builder
         */
        public Builder add(Node key, Node value) {
            if (key == null || value == null) {
                return this;
            }
            return add(key, new SecurityLabelledNode(value));
        }

        /**
         * Adds a key value pair to the entity data where the value is security labelled
         *
         * @param key            Key
         * @param value          Value
         * @param securityLabels Security labels for the value
         * @return Builder
         */
        public Builder add(Node key, Node value, String securityLabels) {
            if (key == null || value == null) {
                return this;
            }
            return add(key, new SecurityLabelledNode(value, securityLabels));
        }

        /**
         * Adds a key value pair to the entity data where the value is already security labelled
         *
         * @param key   Key
         * @param value Security labelled value
         * @return Builder
         */
        public Builder add(Node key, SecurityLabelledNode value) {
            if (key == null || value == null) {
                return this;
            }
            items.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Adds multiple values to a given key where the values are already security labelled
         *
         * @param key    Key
         * @param values Security labelled values
         * @return Builder
         */
        public Builder add(Node key, SecurityLabelledNode... values) {
            if (key == null) {
                return this;
            }
            List<SecurityLabelledNode> valueNodes = items.computeIfAbsent(key, k -> new ArrayList<>());
            for (SecurityLabelledNode value : values) {
                if (value == null) {
                    continue;
                }
                valueNodes.add(value);
            }
            return this;
        }

        /**
         * Adds multiple values to a given key where the values are already security labelled
         *
         * @param key    Key
         * @param values Stream of security labelled values
         * @return Builder
         */
        public Builder add(Node key, Stream<SecurityLabelledNode> values) {
            if (key == null) {
                return this;
            }
            List<SecurityLabelledNode> valueNodes = items.computeIfAbsent(key, k -> new ArrayList<>());
            values.forEach(v -> {
                if (v != null) {
                    valueNodes.add(v);
                }
            });
            return this;
        }

        /**
         * Builds the final {@link EntityData} instance
         *
         * @return Entity Data instance
         */
        public EntityData build() {
            items.entrySet().removeIf(e -> e.getValue().isEmpty());
            return new EntityData(this.items);
        }
    }
}
