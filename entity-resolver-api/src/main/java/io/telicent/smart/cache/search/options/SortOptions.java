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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for sorting search results.
 **/
public final class SortOptions {

    /**
     * Constant type sort options for when no specific sorting
     */
    public static final SortOptions NONE = new SortOptions();

    private final List<SortField> fields = new ArrayList<>();

    private SortOptions() {
    }

    /**
     * Parse a String representing field names to use for sorting search results. Field names are separated by a comma
     * and prefixed by an optional '&gt;' or '&lt;' to indicate whether they are respectively in decreasing or
     * increasing order. The former is used by default. For instance the string "&lt;start,&lt;end,importance" will
     * cause the documents to be sorted first by increasing start value, then by increasing end value and finally by
     * decreasing importance.
     *
     * @param fieldRepresentation representation of the fields to use
     * @return sort options instance
     **/
    public static SortOptions parse(final String fieldRepresentation) {
        if (StringUtils.isEmpty(fieldRepresentation)) {
            return NONE;
        }
        SortOptions opts = new SortOptions();
        String[] fields = fieldRepresentation.split("\\s*,\\s*");
        for (String f : fields) {
            String fieldName = f;
            SortField.Direction direction = SortField.Direction.DESCENDING;
            if (f.startsWith("<")) {
                fieldName = f.substring(1);
                direction = SortField.Direction.ASCENDING;
            } else if (f.startsWith(">")) {
                fieldName = f.substring(1);
            }
            opts.fields.add(new SortField(fieldName, direction));
        }
        return opts;
    }

    /**
     * Returns the SortFields to be used for ranking the documents from a search
     *
     * @return sort fields
     **/
    public List<SortField> getFields() {
        return fields;
    }
}
