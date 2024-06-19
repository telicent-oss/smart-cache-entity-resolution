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

import io.telicent.smart.cache.search.configuration.rules.IndexMappingRule;

/**
 * Common field types for use with {@link IndexMappingRule}
 */
public final class CommonFieldTypes {

    /**
     * Private constructor prevents instantiation
     */
    private CommonFieldTypes() {
    }

    /**
     * A field that will contain URIs.  A field of this type will be indexed for both full text and keyword search.
     */
    public static final String URI = "uri";

    /**
     * A field that will contain a keyword i.e. a value who you only ever want to match as an exact value.  A field of
     * this type will be indexed for only keyword search.
     */
    public static final String KEYWORD = "keyword";

    /**
     * A field that will contain text and will be available for full text search, typically this means a field that
     * originates from an RDF Literal
     */
    public static final String TEXT = "text";

    /**
     * A field that won't be indexed i.e. it won't be matched by searches, but it will still be present in the
     * underlying index should it's value be needed internally e.g. applying security labels.
     */
    public static final String NON_INDEXED = "non-indexed";

    /**
     * A field that should be indexed as a geospatial point
     */
    public static final String GEO_POINT = "geo-point";

    /**
     * A field that should be indexed as a date
     */
    public static final String DATE = "date";

    /**
     * A field that should be indexed as a single precision floating point number
     */
    public static final String FLOAT = "float";

    /**
     * A field that should be indexed as a double precision floating point number
     */
    public static final String DOUBLE = "double";

    /**
     * A field that should be indexed as a 32-bit integer
     */
    public static final String INTEGER = "integer";

    /**
     * A field that should be indexed as a 64-bit integer
     */
    public static final String LONG = "long";

    /**
     * A field that represents a generic number format
     */
    public static final String NUMBER = "number";

    /**
     * A field that represents a boolean
     */
    public static final String BOOLEAN = "boolean";

    /**
     * A field that should be indexed as defined by the mapping rules, regardless of how Elastic thought it was
     */
    public static final String ANY = "any";

    /**
     * All defined common field types that search API implementations are expected to handle
     */
    public static String[] ALL =
            {URI, KEYWORD, TEXT, NON_INDEXED, GEO_POINT, DATE, FLOAT, DOUBLE, INTEGER, LONG, NUMBER, BOOLEAN, ANY};
}
