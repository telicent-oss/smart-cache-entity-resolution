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

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCanonicalTypeConfigurationTextField {

    @Test
    @SuppressWarnings("SelfEquals")
    public void equals_test_mismatch_fuzzy() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;

        // when
        boolean resultFuzzyObjectMismatch = textField.fuzziness.equals(new Object());
        boolean resultFuzzyNullMismatch = textField.fuzziness.equals(null);
        boolean resultFuzzySame = textField.fuzziness.equals(textField.fuzziness);

        // then
        Assert.assertTrue(resultFuzzySame);
        Assert.assertFalse(resultFuzzyNullMismatch);
        Assert.assertFalse(resultFuzzyObjectMismatch);
    }

    @Test
    public void equals_test_mismatch_otherField() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;

        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("date-field");
        otherField.name = "text-field-fuzzy";
        otherField.type = "text";
        otherField.boost = 1.2f;
        otherField.required = true;
        // when
        boolean resultOtherTypeMismatch = textField.equals(otherField);
        // then
        Assert.assertFalse(resultOtherTypeMismatch);
    }

    @Test
    public void equals_test_mismatch_differentFuzzyField() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;

        CanonicalTypeConfiguration.SimilarityField nonFuzzyField = configuration.getField("text-field-nonfuzzy");
        nonFuzzyField.name = "text-field-fuzzy";
        nonFuzzyField.boost = 1.2f;
        nonFuzzyField.required = true;

        // when
        boolean resultFuzzyEnabledMismatch = textField.equals(nonFuzzyField);
        int hashCodeWithDifferentFuzz = nonFuzzyField.hashCode();

        // then
        Assert.assertFalse(resultFuzzyEnabledMismatch);
        Assert.assertNotEquals(textField.hashCode(), hashCodeWithDifferentFuzz);
    }

    @Test
    public void equals_test_mismatch_nonFuzzyField() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;

        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("date-field");
        otherField.name = "text-field-fuzzy";
        otherField.type = "text";
        otherField.boost = 1.2f;
        otherField.required = true;

        CanonicalTypeConfiguration.SimilarityField nonFuzzyField = configuration.getField("text-field-nonfuzzy");
        // when
        boolean resultNonFuzzyMismatch = textField.equals(nonFuzzyField);
        // then
        Assert.assertFalse(resultNonFuzzyMismatch);
    }

    @Test
    public void hashcode_mismatch_nonFuzzyField() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;

        CanonicalTypeConfiguration.SimilarityField nonFuzzyField = configuration.getField("text-field-nonfuzzy");
        nonFuzzyField.name = "text-field-fuzzy";
        nonFuzzyField.boost = 1.2f;
        nonFuzzyField.required = true;

        Assert.assertTrue(nonFuzzyField instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField nonFuzzyTextField = (CanonicalTypeConfiguration.TextField) nonFuzzyField;
        nonFuzzyTextField.fuzziness = null;
        // when
        int hashCodeWithNullFuzz = nonFuzzyField.hashCode();
        // then
        Assert.assertNotEquals(textField.hashCode(), hashCodeWithNullFuzz);
    }

    @Test
    public void equals_test_mismatch_fuzzyMin() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;

        CanonicalTypeConfiguration.SimilarityField nonFuzzyField = configuration.getField("text-field-nonfuzzy");
        nonFuzzyField.name = "text-field-fuzzy";
        nonFuzzyField.boost = 1.2f;
        nonFuzzyField.required = true;

        Assert.assertTrue(nonFuzzyField instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField fuzzyTextFieldDifMin =  (CanonicalTypeConfiguration.TextField) nonFuzzyField;
        fuzzyTextFieldDifMin.fuzziness = new CanonicalTypeConfiguration.Fuzziness();
        fuzzyTextFieldDifMin.fuzziness.enabled = true;
        fuzzyTextFieldDifMin.fuzziness.min = 1;
        // when
        boolean resultFuzzyMinMismatch = textField.equals(fuzzyTextFieldDifMin);
        int hashCodeFuzzyMinMismatch = fuzzyTextFieldDifMin.hashCode();

        // then
        Assert.assertFalse(resultFuzzyMinMismatch);
        Assert.assertNotEquals(textField.hashCode(), hashCodeFuzzyMinMismatch);
    }

    @Test
    public void equals_test_mismatch_fuzzyMax() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("text-field-fuzzy");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField textField = (CanonicalTypeConfiguration.TextField) field;
        CanonicalTypeConfiguration.SimilarityField nonFuzzyField = configuration.getField("text-field-nonfuzzy");
        nonFuzzyField.name = "text-field-fuzzy";
        nonFuzzyField.boost = 1.2f;
        nonFuzzyField.required = true;
        Assert.assertTrue(nonFuzzyField instanceof CanonicalTypeConfiguration.TextField);
        CanonicalTypeConfiguration.TextField fuzzyTextFieldDifMax = (CanonicalTypeConfiguration.TextField) nonFuzzyField;
        fuzzyTextFieldDifMax.fuzziness = new CanonicalTypeConfiguration.Fuzziness();
        fuzzyTextFieldDifMax.fuzziness.enabled = true;
        fuzzyTextFieldDifMax.fuzziness.min = 0;
        fuzzyTextFieldDifMax.fuzziness.max = 9;

        // when
        boolean resultFuzzyMaxMismatch = textField.equals(fuzzyTextFieldDifMax);
        int hashCodeFuzzyMaxMismatch = fuzzyTextFieldDifMax.hashCode();

        // then
        Assert.assertFalse(resultFuzzyMaxMismatch);
        Assert.assertNotEquals(textField.hashCode(), hashCodeFuzzyMaxMismatch);
    }
}
