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
package io.telicent.smart.cache.entity.resolver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents results for a similarity search
 */

public class SimilarityResults {

    private final List<SimilarityResult> results = new ArrayList<>();

    /**
     * Creates new empty facet results
     */
    public SimilarityResults() {
    }

    /**
     * Creates new similarity results
     *
     * @param results The similarity results themselves
     */
    public SimilarityResults(
            Collection<SimilarityResult> results) {

        Objects.requireNonNull(results, "Results cannot be null");

        this.results.addAll(results);
    }

    /**
     * Gets the similarity results
     *
     * @return similarity results
     */
    public List<SimilarityResult> getResults() {
        return results;
    }

}
