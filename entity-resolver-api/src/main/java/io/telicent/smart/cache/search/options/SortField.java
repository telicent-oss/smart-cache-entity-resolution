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
package io.telicent.smart.cache.search.options;

/**
 * Representation of a field to use for sorting search results, back-end neutral representation
 **/
public class SortField {
    final String fieldName;
    final Direction direction;

    /**
     * Indicates whether a field should be used in ascending or descending order
     **/
    public enum Direction {
        /**
         * Ascending
         **/
        ASCENDING,
        /**
         * Descending
         **/
        DESCENDING
    }

    SortField(String name, Direction direction) {
        fieldName = name;
        this.direction = direction;
    }

    /**
     * Get the name of the field
     *
     * @return field name
     **/
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the direction of a field
     *
     * @return direction
     **/
    public Direction getDirection() {
        return direction;
    }
}
