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

import lombok.*;

import java.util.Objects;

/**
 * Represents a facet result, which is the value for a specific field with the number of occurrences found in a result
 * set
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FacetResult implements Comparable<FacetResult> {

    private String value;
    private long count;
    private double percentage;

    @Override
    public int compareTo(FacetResult other) {
        int c = Long.compare(other.count, this.count);
        if (c != 0) {
            return c;
        }
        c = Double.compare(other.percentage, this.percentage);
        if (c != 0) {
            return c;
        }
        // same number of occurrences, and same percentage of results
        // use label
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FacetResult that = (FacetResult) o;
        //@formatter:off
        return count == that.count
               && Double.compare(percentage, that.percentage) == 0
               && Objects.equals(value, that.value);
        //@formatter:on
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, count, percentage);
    }
}
