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
package io.telicent.smart.cache.canonical;

import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration.*;
/**
 * Interface for a visitor pattern tying the config to query generation
 * <p>
 * Stops the need to pull in the ElasticSearch libs into the search-configuration module.
 */
public interface QueryVisitor {
    /**
     * @param field Keyword configuration
     * @param document Document for searching with
     */
    void buildQuery(KeywordField field, Object document);
    /**
     * @param field Text configuration
     * @param document Document for searching with
     */
    void buildQuery(TextField field, Object document);
    /**
     * @param field Number configuration
     * @param document Document for searching with
     */
    void buildQuery(NumberField field, Object document);
    /**
     * @param field Date configuration
     * @param document Document for searching with
     */
    void buildQuery(DateField field, Object document);
    /**
     * @param field Location configuration
     * @param document Document for searching with
     */
    void buildQuery(LocationField field, Object document);

    /**
     * @param field Keyword configuration
     * @param document Document for searching with
     */
    void buildQuery(BooleanField field, Object document);
}
