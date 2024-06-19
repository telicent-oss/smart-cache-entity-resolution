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
package io.telicent.smart.cache.search.elastic.schema.factories;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import io.telicent.smart.cache.search.configuration.CommonFieldTypes;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;

/**
 * An Elastic mapping factory that handles rules for field type {@value CommonFieldTypes#URI}
 */
public class UriMappingFactory extends AbstractMappingFactory {

    /**
     * Creates a new factory
     */
    public UriMappingFactory() {
        super(CommonFieldTypes.URI, "string");
    }

    @Override
    public Property toProperty(IndexMappingRule rule) {
        // Note that we limit the keyword field to URIs that are at most 1024 characters since that seems like a
        // reasonable (and wholly arbitrary) limit to choose
        return Property.of(p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k.ignoreAbove(1024)))));
    }
}
