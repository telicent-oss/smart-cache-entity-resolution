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

import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;

import java.util.Map;

/**
 * A factory that converts from mapping rules into Elastic mappings
 */
public interface ElasticMappingFactory {

    /**
     * Determines whether this given mapping factory supports the given mapping rule
     *
     * @param rule Mapping rule
     * @return True if supported i.e. can be turned into an Elastic mapping by this factory, false otherwise
     */
    boolean supports(IndexMappingRule rule);

    /**
     * Converts the given rule into an Elastic <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/8.2/dynamic-templates.html">Dynamic Template</a>
     * which allows document fields to be mapped into the index without knowing their exact field names ahead of time
     *
     * @param rule Mapping rule
     * @return Dynamic template
     */
    Map<String, DynamicTemplate> toDynamicTemplate(IndexMappingRule rule);

    /**
     * Converts the given rule into an Elastic Property, also called an <a
     * href="https://www.elastic.co/guide/en/elasticsearch/reference/8.2/explicit-mapping.html">explicit mapping</a> in
     * the Elastic documentation.
     *
     * @param rule Mapping rule
     * @return Property mapping
     */
    Property toProperty(IndexMappingRule rule);
}
