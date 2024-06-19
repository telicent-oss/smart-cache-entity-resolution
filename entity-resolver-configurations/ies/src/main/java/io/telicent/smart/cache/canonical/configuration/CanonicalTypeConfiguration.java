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
package io.telicent.smart.cache.canonical.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import io.telicent.smart.cache.canonical.QueryVisitor;
import io.telicent.smart.cache.canonical.utility.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representation of the various canonical types supported.
 * NOTE - might split out into individual classes to spare the clutter.
 */
public class CanonicalTypeConfiguration {
    /**
     * Type representing Canonical type
     */
    public static final String TYPE = "canonicaltype";
    private static final Logger LOGGER = LoggerFactory.getLogger(CanonicalTypeConfiguration.class);

    /**
     * Load configuration from file
     * @param path the path to the configuration file
     * @return the loaded configuration or null if there's an error
     */
    public static CanonicalTypeConfiguration loadFromConfigFile(String path) {
        return Mapper.loadFromConfigFile(CanonicalTypeConfiguration.class, path);
    }

    /**
     * Load configuration from a string
     * @param value the string representation of the configuration
     * @return the loaded configuration or null if there's an error
     */
    public static CanonicalTypeConfiguration loadFromString(String value) {
        return Mapper.loadFromString(CanonicalTypeConfiguration.class, value);
    }

    /**
     * Load configuration from a JSON Node
     * @param value the json node representation of the configuration
     * @return the loaded configuration or null if there's an error
     */
    public static CanonicalTypeConfiguration loadFromNode(JsonNode value) {
        return Mapper.loadFromString(CanonicalTypeConfiguration.class, value.asText());
    }


    /**
     * For a given field-name return the relevant config
     * @param fieldName the name of the fieldName we are interested in.
     * @return the given fieldName or null if we don't recognise it
     */
    public SimilarityField getField(String fieldName) {
        return fields.stream().filter(f -> f.name.equalsIgnoreCase(fieldName)).findFirst().orElse(null);
    }

    /**
     * The type of the config in question:
     * keyword, text, number (in various flavours), geo-point and date.
     */
    @JsonProperty(required = true)
    public String type="";
    /**
     * The ElasticSearch index to map to.
     */
    @JsonProperty(required = true)
    public String index="";

    public List<SimilarityField> fields = Collections.emptyList();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }

        if (o instanceof CanonicalTypeConfiguration c) {
            // Compare each field for equality
            if (!index.equals(c.index)) {
                return false;
            }
            if (!type.equals(c.type)) {
                return false;
            }
            return fields.equals(c.fields);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + index.hashCode();
        result = 31 * result + fields.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return Mapper.writeValueAsString(this);
    }

    /**
     * The fields that correspond to the type - with classes to match.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = KeywordField.class, name = "keyword"),
            @JsonSubTypes.Type(value = TextField.class, name = "text"),
            @JsonSubTypes.Type(value = NumberField.class, name = "number"),
            @JsonSubTypes.Type(value = NumberField.class, name = "integer"),
            @JsonSubTypes.Type(value = NumberField.class, name = "long"),
            @JsonSubTypes.Type(value = NumberField.class, name = "float"),
            @JsonSubTypes.Type(value = NumberField.class, name = "double"),
            @JsonSubTypes.Type(value = LocationField.class, name = "geo-point"),
            @JsonSubTypes.Type(value = DateField.class, name = "date"),
            @JsonSubTypes.Type(value = BooleanField.class, name = "boolean")
    })
    /**
     * Representing a specific field within a Canonical Type definition.
     */
    public abstract static class SimilarityField {
        /**
         * The name of the field
         */
        @JsonProperty(required = true)
        public String name = "";
        /**
         * The type of the field (see above for options)
         */
        @JsonProperty(required = true)
        public String type = "";
        /**
         * Indicates whether the field is required for searching (false = no)
         */
        @JsonProperty(required = true)
        public boolean required;
        /**
         * The boost to give the field when scoring.
         * Defaults to 1.0 if not set.
         */
        @JsonProperty
        public float boost = 1.0F;
        /**
         * Indicates whether to use an exact match (false = no)
         */
        @JsonProperty
        public boolean exactMatch;

        /**
         * As part of the pattern we "accept" the visitor to then apply it to the given object
         * @param visitor Visitor used to generate a query for the given field
         * @param object The given document for querying with.
         */
        public abstract void accept(QueryVisitor visitor, Object object);

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o instanceof SimilarityField s) {
                // Compare each field for equality
                if (!name.equals(s.name)) {
                    return false;
                }
                if (!type.equals(s.type)) {
                    return false;
                }
                if (required != s.required) {
                    return false;
                }
                if (Float.compare(s.boost, boost) != 0) {
                    return false;
                }
                return exactMatch == s.exactMatch;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + (required ? 1 : 0);
            result = 31 * result + (boost != 0.0f ? Float.floatToIntBits(boost) : 0);
            result = 31 * result + (exactMatch ? 1 : 0);
            return result;
        }

    }

    /**
     * Representing a keyword for which only an exact match will do.
     */
    public static class KeywordField extends SimilarityField {
        // An exact match is required
        @Override
        public void accept(QueryVisitor visitor, Object object)
        {
            visitor.buildQuery(this, object);
        }

        @Override
        public boolean equals(Object o) {
            if(super.equals(o)) {
                return o instanceof KeywordField;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    /**
     * Representing numbers and the decay function required for "closeness"
     */
    public static class NumberField extends SimilarityField {
        /**
         * The decay function use to calculate how close other numbers can be.
         */
        public Decay decay;
        @Override
        public void accept(QueryVisitor visitor, Object object)
        {
            visitor.buildQuery(this, object);
        }

        @Override
        public boolean equals(Object o) {
            if(super.equals(o)) {
                if (o instanceof NumberField n) {
                    return Objects.equals(decay, n.decay);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (decay != null ? decay.hashCode() : 0);
            return result;
        }

    }

    /**
     * Representing a date field.
     * TODO - technically we could also use a decay function. Is it worth implementing as an alternative option?
     */
    public static class DateField extends SimilarityField {
        /**
         * The required distance parameters
         */
        public Distance distance;
        @Override
        public void accept(QueryVisitor visitor, Object object)
        {
            visitor.buildQuery(this, object);
        }

        @Override
        public boolean equals(Object o) {
            if(super.equals(o)) {
                if (o instanceof DateField d) {
                    return Objects.equals(distance, d.distance);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (distance != null ? distance.hashCode() : 0);
            return result;
        }
    }

    /**
     * Represents a location (geo-point) field
     */
    public static class LocationField extends SimilarityField {
        /**
         * The required distance parameters
         */
        public Distance distance;
        @Override
        public void accept(QueryVisitor visitor, Object object)
        {
            visitor.buildQuery(this, object);
        }

        @Override
        public boolean equals(Object o) {
            if(super.equals(o)) {
                if (o instanceof LocationField l) {
                    return Objects.equals(distance, l.distance);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (distance != null ? distance.hashCode() : 0);
            return result;
        }
    }

    /**
     * Represents a text field (i.e. a String).
     * This is the default go-to field as everything can be represented this way.
     */
    public static class TextField extends SimilarityField {
        /**
         * Determines the fuzziness of the search
         * If not set, an exact match will be used in queries.
         */
        public Fuzziness fuzziness;
        @Override
        public void accept(QueryVisitor visitor, Object object)
        {
            visitor.buildQuery(this, object);
        }

        @Override
        public boolean equals(Object o) {
            if(super.equals(o)) {
                if (o instanceof TextField t) {
                    return Objects.equals(fuzziness, t.fuzziness);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (fuzziness != null ? fuzziness.hashCode() : 0);
            return result;
        }
    }

    /**
     * Representing a boolean for which only an exact match will do.
     */
    public static class BooleanField extends SimilarityField {
        // An exact match is required
        @Override
        public void accept(QueryVisitor visitor, Object object)
        {
            visitor.buildQuery(this, object);
        }

        @Override
        public boolean equals(Object o) {
            if(super.equals(o)) {
                return o instanceof BooleanField;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }


    /**
     * Represents the parameters or a fuzzy query as per the
     * Levenshtein edit difference (i.e. how many edits would it take to get from
     * one word to another)
     * Note: Both min and max need to be set or both need to be null.
     * It will create the value "AUTO:[min],[max]"
     * If null the parameter "AUTO" will be used (which is the equivalent of "AUTO:3,6")
     */
    public static class Fuzziness {
        /**
         * Indicates whether to use a fuzzy search.
         * If false, an exact match query is to be used.
         */
        public boolean enabled;
        /**
         * Indicates the minimum n-gram length
         */
        public Integer min;
        /**
         * Mapping
         */
        public Integer max;


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof Fuzziness f) {
                // Compare each field for equality
                if (enabled != f.enabled){
                    return false;
                }
                if (!Objects.equals(min, f.min)){
                    return false;
                }
                return Objects.equals(max, f.max);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = enabled ? 1 : 0;
            result = 31 * result + (min != null ? min.hashCode() : 0);
            result = 31 * result + (max != null ? max.hashCode() : 0);
            return result;
        }
    }

    /**
     * Represents the parameters for defining the distance from.
     * The origin will be the incoming value on the search.
     */
    public static class Distance {
        /**
         * The pivot is the value at which point scores receive half of the score.
         */
        @JsonProperty(required = true)
        public String pivot;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof Distance d) {
                // Compare the pivot field for equality
                return pivot.equals(d.pivot);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return pivot.hashCode();
        }
    }

    /**
     * Represents the parameters for a Decay function.
     * The origin will be the incoming value on the search.
     * For example, if we had an origin of 50, a decay of 0.5, an offset of 10 and a scale of 10.
     * For the given values we would get scores as follows:
     * 50 -> 1.0 (obviously)
     * 40-60 -> 1.0 (because they are within the offset)
     * 30,60 -> 0.5 (because it is 1 scale from the offset).
     * 35,55 -> 0.75 (because it's a linear progression from 1.0 -> 0.5 so would be 0.75)
     * 25,65 -> 0.25 (because it's a linear progression from 0.5 -> 0.0 so would be 0.25)
     * lte20, gte70 -> 0.0
     */
    public static class Decay {
        /**
         * Defines how documents are scored at the distance given by scale.
         * Defaults to 0.5
         */
        @JsonProperty
        public double decay = 0.5;

        /**
         * If defined, the decay function will only be computed for documents outside this range.
         * Defaults to 0
         */
        @JsonProperty
        public String offset = "0";
        /**
         * The scale defines the distance from the origin + offset for which the score will match the decay value.
         */
        @JsonProperty(required = true)
        public String scale;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o instanceof Decay d) {
                // Compare each field for equality
                if (Double.compare(d.decay, decay) != 0) {
                    return false;
                }
                if (!offset.equals(d.offset)) {
                    return false;
                }
                return scale.equals(d.scale);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result;
            result = Double.hashCode(decay);
            result = 31 * result + offset.hashCode();
            result = 31 * result + scale.hashCode();
            return result;
        }
    }
}
