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

import io.telicent.smart.cache.search.model.TypeFilterMode;
import org.apache.commons.lang3.StringUtils;

/**
 * Type filtering options for search
 */
public class TypeFilterOptions {

    /**
     * Constant type filtering options for when you want type filtering disabled
     */
    public static final TypeFilterOptions DISABLED = new TypeFilterOptions();

    private final String typeFilter;
    private final TypeFilterMode typeFilterMode;


    /**
     * Creates default type filtering options
     */
    public TypeFilterOptions() {
        this(null, TypeFilterMode.ANY);
    }

    /**
     * Creates new type filtering options
     *
     * @param typeFilter     Text used to match entity/identifier type
     * @param typeFilterMode Type filtering mode to use
     */
    public TypeFilterOptions(String typeFilter, TypeFilterMode typeFilterMode) {
        this.typeFilter = StringUtils.isNotBlank(typeFilter) ? typeFilter : null;
        this.typeFilterMode = typeFilterMode == null ? TypeFilterMode.ANY : typeFilterMode;
    }

    /**
     * Gets the type to filter by
     *
     * @return type or {@code null}
     */
    public String getTypeFilter() {
        return this.typeFilter;
    }

    /**
     * Gets the type filter mode
     *
     * @return type
     */
    public TypeFilterMode getTypeFilterMode() {
        return this.typeFilterMode;
    }

    /**
     * Gets whether type filtering is enabled
     *
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return StringUtils.isNotBlank(this.typeFilter);
    }
}
