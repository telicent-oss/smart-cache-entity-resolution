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

import io.telicent.smart.cache.canonical.QueryVisitor;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCanonicalTypeConfigurationFields {

    private static final String JSON_STRING = """
         {
                "fields":
                [
                    {
                    "name": "%s",
                    "type": "%s"
                    }
                ]
        }
        """;

    @Test
    public void accept_visitors() {
        // given
        TestVisitor visitor = new TestVisitor();
        for (String type : TYPE_LIST) {
            CanonicalTypeConfiguration configuration =
                    CanonicalTypeConfiguration.loadFromString(String.format(JSON_STRING, type, type));
            Assert.assertNotNull(configuration);
            // when
            CanonicalTypeConfiguration.SimilarityField field = configuration.getField(type);
            field.accept(visitor, null);
            // then
            Assert.assertEquals(visitor.getCallCount(type), 1, type);
            visitor.clear();
        }
    }

    private static class TestVisitor implements QueryVisitor {

        private static final Map<String,Integer> callCount = new HashMap<>();
        public int getCallCount(String key) {
            return callCount.getOrDefault(key, 0);
        }

        public void clear() {
            callCount.clear();
        }
        private void update(String key) {
            callCount.put(key, callCount.getOrDefault(key, 0) + 1);
        }
        @Override
        public void buildQuery(CanonicalTypeConfiguration.KeywordField field, Object document) {
            update("keyword");
        }

        @Override
        public void buildQuery(CanonicalTypeConfiguration.TextField field, Object document) {
            update("text");
        }

        @Override
        public void buildQuery(CanonicalTypeConfiguration.NumberField field, Object document) {
            update("number");
            update("integer");
            update("long");
            update("float");
            update("double");
        }

        @Override
        public void buildQuery(CanonicalTypeConfiguration.DateField field, Object document) {
            update("date");
        }

        @Override
        public void buildQuery(CanonicalTypeConfiguration.LocationField field, Object document) {
            update("geo-point");
        }

        @Override
        public void buildQuery(CanonicalTypeConfiguration.BooleanField field, Object document) {
            update("boolean");
        }
    }

    @Test(dataProvider = "mismatchedTypes")
    public void equals_test_mismatches(String type, List<String> otherTypes) {
        // given
        String matchingName = RandomStringUtils.secure().nextAscii(6);

        CanonicalTypeConfiguration
                originalConfiguration = CanonicalTypeConfiguration.loadFromString(String.format(JSON_STRING, matchingName, type));
        Assert.assertNotNull(originalConfiguration);
        for (String otherType : otherTypes) {
            CanonicalTypeConfiguration otherConfiguration =
                    CanonicalTypeConfiguration.loadFromString(String.format(JSON_STRING, matchingName, otherType));
            // when
            // then
            Assert.assertNotNull(otherConfiguration);
            Assert.assertNotEquals(originalConfiguration, otherConfiguration, type + " shouldn't match " + otherType);
        }
    }

    @Test
    public void equals_test_mismatch() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration
                misMatchTypeConfiguration = CanonicalTypeConfiguration.loadFromString(
                TestCanonicalTypeConfiguration.HAPPY_STRING);
        misMatchTypeConfiguration.type = "OTHER";
        // when
        boolean resultObjectMismatch = configuration.equals(new Object());
        boolean resultNullMismatch = configuration.equals(null);
        boolean resultTypeMismatch = configuration.equals(misMatchTypeConfiguration);
        // then
        Assert.assertFalse(resultObjectMismatch);
        Assert.assertFalse(resultNullMismatch);
        Assert.assertFalse(resultTypeMismatch);
    }

    @Test
    @SuppressWarnings("SelfEquals")
    public void equals_test_mismatch_similarity() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        int hashcode = field.hashCode();

        CanonicalTypeConfiguration
                misMatchingConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        CanonicalTypeConfiguration.SimilarityField mismatchField;
        // when
        boolean resultObjectMismatch = field.equals(new Object());
        boolean resultNullMismatch = field.equals(null);

        mismatchField = misMatchingConfiguration.getField("text-field-fuzzy");
        mismatchField.name = "different";
        boolean resultNameMismatch = field.equals(mismatchField);
        int resultNameMismatchHashCode = mismatchField.hashCode();

        misMatchingConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        mismatchField = misMatchingConfiguration.getField("text-field-fuzzy");
        mismatchField.type = "keyword";
        boolean resultTypeMismatch = field.equals(mismatchField);
        int resultTypeMismatchHashCode = mismatchField.hashCode();

        misMatchingConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        mismatchField = misMatchingConfiguration.getField("text-field-fuzzy");
        mismatchField.required = false;
        boolean resultRequiredMismatch = field.equals(mismatchField);
        int resultRequiredMismatchHashCode = mismatchField.hashCode();

        misMatchingConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        mismatchField = misMatchingConfiguration.getField("text-field-fuzzy");
        mismatchField.boost = 0.0f;
        boolean resultBoostMismatch = field.equals(mismatchField);
        int resultBoostMismatchHashCode = mismatchField.hashCode();

        misMatchingConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        mismatchField = misMatchingConfiguration.getField("text-field-fuzzy");
        mismatchField.exactMatch = true;
        boolean resultExactMismatch = field.equals(mismatchField);
        int resultExactMismatchHashCode = mismatchField.hashCode();

        misMatchingConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        mismatchField = misMatchingConfiguration.getField("text-field-fuzzy");
        boolean resultMatch = field.equals(mismatchField);
        int resultMatchHashCode = mismatchField.hashCode();

        boolean resultSame = field.equals(field);

        // then
        Assert.assertFalse(resultObjectMismatch);
        Assert.assertFalse(resultNullMismatch);
        Assert.assertFalse(resultNameMismatch);
        Assert.assertNotEquals(hashcode, resultNameMismatchHashCode);
        Assert.assertFalse(resultTypeMismatch);
        Assert.assertNotEquals(hashcode, resultTypeMismatchHashCode);
        Assert.assertFalse(resultRequiredMismatch);
        Assert.assertNotEquals(hashcode, resultRequiredMismatchHashCode);
        Assert.assertFalse(resultBoostMismatch);
        Assert.assertNotEquals(hashcode, resultBoostMismatchHashCode);
        Assert.assertFalse(resultExactMismatch);
        Assert.assertNotEquals(hashcode, resultExactMismatchHashCode);
        Assert.assertTrue(resultMatch);
        Assert.assertEquals(hashcode, resultMatchHashCode);
        Assert.assertTrue(resultSame);
    }





    @DataProvider(name = "mismatchedTypes")
    public Object[][] mismatchedTypes() {
        return new Object[][]{
                {"keyword", getOtherTypes("keyword")},
                {"text", getOtherTypes("text")},
                {"number", getOtherTypes("number")},
                {"integer", getOtherTypes("integer")},
                {"long", getOtherTypes("long")},
                {"float", getOtherTypes("float")},
                {"double", getOtherTypes("double")},
                {"geo-point", getOtherTypes("geo-point")},
                {"date", getOtherTypes("date")},
                {"boolean", getOtherTypes("boolean")}
        };
    }

    private static final List<String> TYPE_LIST = List.of(
            "keyword",
            "text",
            "number",
            "integer",
            "long",
            "float",
            "double",
            "geo-point",
            "date",
            "boolean");

    private static List<String> getOtherTypes(String type) {
        return TYPE_LIST.stream().filter(t -> !type.equals(t)).collect(Collectors.toList());
    }
}
