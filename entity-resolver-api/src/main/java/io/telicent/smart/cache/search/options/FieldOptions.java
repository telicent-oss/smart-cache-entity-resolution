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

import io.telicent.smart.cache.configuration.Configurator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Options to lists the fields to use for search, a boost value can be assigned to a field. By default, all fields are
 * used (presented as a *). This can be changed through the environment variable 'FIELD_OPTIONS', e.g. export
 * FIELD_OPTIONS="primaryName^2, *" bearing in mind that the search will be restricted to what is listed in the value
 **/
public final class FieldOptions {

    private final List<String> fieldBoosts = new ArrayList<>();

    /**
     * All fields are used for the search by default, none are boosted
     **/
    public static final FieldOptions DEFAULT;

    /**
     * Environment variable key to specify the default field options
     **/
    public static final String ENV_VAR_FIELD_OPTS = "FIELD_OPTIONS";

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldOptions.class);

    static {
        // try to load an alternative default from the System properties
        String value = Configurator.get(ENV_VAR_FIELD_OPTS);
        if (StringUtils.isBlank(value)) {
            value = "*";
        }
        DEFAULT = parse(value);
        LOGGER.info("Using {} as default for the field options", DEFAULT.fieldBoosts);
    }

    private FieldOptions() {
    }

    /**
     * Parse a String representing the list of fields and their corresponding boost values. Fields are separated by
     * commas and the boost value is preceded by a ^ e.g. 'field1^2, field2^3'
     *
     * @param representation string to parse
     * @return FieldBoostOptions
     **/
    public static FieldOptions parse(final String representation) {
        if (StringUtils.isEmpty(representation)) {
            return DEFAULT;
        }
        final FieldOptions options = new FieldOptions();
        final String[] tokens = representation.split("\\s*,\\s*");
        for (String s : tokens) {
            // ignore anything non-compliant
            if (StringUtils.isNotBlank(s)) {
                options.fieldBoosts.add(s);
            }
        }

        if (options.fieldBoosts.isEmpty()) {
            LOGGER.info("Empty values for fields options, reverting to default {}", DEFAULT.getFields());
            return DEFAULT;
        }

        return options;
    }

    /**
     * Returns a list of field names and associated boost values
     *
     * @return field names and their boost values
     **/
    public List<String> getFields() {
        return fieldBoosts;
    }

}
