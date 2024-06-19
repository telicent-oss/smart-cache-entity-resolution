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
package io.telicent.smart.cache.search.model.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * A field name matching expression whose implementation matches the logic commonly used by search indices in providing
 * field name patterns
 */
public final class FieldNameExpression {
    /**
     * The wildcard path match expression
     */
    private static final String WILDCARD = "*";
    private final String[] pathSegments;
    private final Pattern regex;

    /**
     * Creates a new field name expression
     *
     * @param expression Field name expression
     */
    public FieldNameExpression(String expression) {
        this.pathSegments = expression.split("\\.");
        for (String segment : this.pathSegments) {
            if (StringUtils.isBlank(segment)) {
                throw new IllegalArgumentException("Field name expression segments cannot be blank");
            }
        }

        // Build the regular expression we'll use to match the field names
        StringBuilder regexPattern = new StringBuilder();
        regexPattern.append("^");
        for (int i = 0; i < this.pathSegments.length; i++) {
            String segment = this.pathSegments[i];
            if (StringUtils.equals(segment, WILDCARD)) {
                regexPattern.append(".+");
            } else {
                regexPattern.append(segment);
            }
            if (i < this.pathSegments.length - 1) {
                regexPattern.append("\\.");
            }
        }
        regexPattern.append("$");
        this.regex = Pattern.compile(regexPattern.toString());
    }

    /**
     * Gets whether this expression matches the given field path
     *
     * @param path Field path
     * @return True if matches, false otherwise
     */
    public boolean matches(String[] path) {
        return this.regex.matcher(StringUtils.join(path, ".")).find();
    }

    @Override
    public String toString() {
        return StringUtils.join(this.pathSegments, ".");
    }
}
