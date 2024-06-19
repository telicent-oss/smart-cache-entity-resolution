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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Represents supported type filtering modes
 * <p>
 * Specifies which mode of type filtering is used for refining searches.
 * </p>
 */
public enum TypeFilterMode {
    /**
     * Type filtering will be carried out based on both entity and identifier types.
     */
    ANY("any", "Any"),
    /**
     * Type filtering will be carried out based on only entity types.
     */
    ENTITY("entity", "Entity"),
    /**
     * Type filtering will be carried out based on only identifier types.
     */
    IDENTIFIER("identifier", "Identifier");

    private final String apiValue;
    private final String displayName;

    /**
     * Creates a type filtering mode
     *
     * @param apiValue    Type filtering mode API value
     * @param displayName Display name, used primarily for logging purposes
     */
    TypeFilterMode(String apiValue, String displayName) {
        this.apiValue = apiValue;
        this.displayName = displayName;
    }

    /**
     * Gets the value of this type filtering mode when used in the Search REST API
     *
     * @return API Value
     */
    @JsonValue
    public String getApiValue() {
        return this.apiValue;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

    /**
     * Parses a type filter mode from the REST API type filter mode value
     *
     * @param rawTypeFilterMode Raw type filter mode
     * @return Type filter mode
     */
    @JsonCreator
    public static TypeFilterMode parse(String rawTypeFilterMode) {
        return TypeFilterMode.valueOf(rawTypeFilterMode.toUpperCase(Locale.ROOT));
    }
}
