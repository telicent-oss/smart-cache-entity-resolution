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

import io.telicent.smart.cache.search.model.Hit;

/**
 * Represents a list of entities considered to be similar to some input one
 */
public class SimilarityResult {

    private static final Hit[] EMPTY_HIT = new Hit[]{};

    private String IDSourceEntity;
    private Hit[] hits;

    /**
     * Creates a new empty similarity result
     */
    public SimilarityResult() {
    }

    /**
     * Creates a new similarity result
     *
     * @param id ID of the source entity
     * @param h  hits
     */
    public SimilarityResult(String id, Hit[] h) {
        this.IDSourceEntity = id;
        this.hits = h;
    }

    /**
     * Get the ID of the source entity
     *
     * @return IDSourceEntity
     */
    public String getIDSourceEntity() {
        return IDSourceEntity;
    }

    /**
     * Set the ID of the source entity
     *
     * @param IDSourceEntity ID of the source entity
     */
    public void setIDSourceEntity(String IDSourceEntity) {
        this.IDSourceEntity = IDSourceEntity;
    }

    /**
     * Get the hits
     *
     * @return hits
     */
    public Hit[] getHits() {
        if (hits == null) {
            return EMPTY_HIT;
        }
        return hits;
    }

    /**
     * Set the hits
     *
     * @param hits hits
     */
    public void setHits(Hit[] hits) {
        this.hits = hits;
    }
}
