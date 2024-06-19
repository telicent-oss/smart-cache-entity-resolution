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
package io.telicent.smart.cache.search.configuration;

import io.telicent.smart.cache.search.configuration.rules.SimpleMappingRule;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Simple Index Configuration
 */
public class SimpleIndexConfiguration implements IndexConfiguration<SimpleMappingRule> {
    private Properties properties;
    private final List<SimpleMappingRule> rules = new ArrayList<>();

    /**
     * Creates a new simple configuration
     *
     * @param props Properties
     * @param rules Rules
     */
    public SimpleIndexConfiguration(Properties props, Collection<SimpleMappingRule> rules) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        if (props != null && !props.isEmpty()) {
            putProperties(props);
        }

        if (CollectionUtils.isNotEmpty(rules)) {
            this.rules.addAll(rules);
        }
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public void setProperties(Properties props) {
        this.properties = props;
    }

    @Override
    public void putProperties(Properties props) {
        this.properties.putAll(props);
    }

    @Override
    public Stream<SimpleMappingRule> getRules() {
        return this.rules.stream();
    }

    @Override
    public void addRule(SimpleMappingRule rule) {
        this.rules.add(rule);
    }

    @Override
    public void removeRule(SimpleMappingRule rule) {
        this.rules.remove(rule);
    }

    @Override
    public void removeRule(String name) {
        this.rules.removeIf(r -> StringUtils.equals(r.getName(), name));
    }
}
