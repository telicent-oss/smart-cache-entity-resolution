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

public class TestCanonicalTypeConfigurationDateField {
    @Test
    public void equals_test_distance_mismatch_object() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("date-field");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.DateField);
        CanonicalTypeConfiguration.DateField dateField = (CanonicalTypeConfiguration.DateField) field;

        // when
        boolean resultDistanceMismatch = dateField.distance.equals(new Object());

        // then
        Assert.assertFalse(resultDistanceMismatch);
    }

    @Test
    public void equals_test_distance_mismatch_null() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("date-field");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.DateField);
        CanonicalTypeConfiguration.DateField dateField = (CanonicalTypeConfiguration.DateField) field;

        // when
        boolean resultDistanceMismatch = dateField.distance.equals(null);

        // then
        Assert.assertFalse(resultDistanceMismatch);
    }

    @Test
    public void equals_test_mismatch_dateField() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("date-field");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.DateField);
        CanonicalTypeConfiguration.DateField dateField = (CanonicalTypeConfiguration.DateField) field;


        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("text-field-fuzzy");
        otherField.name = "date-field";
        otherField.type = "date";
        otherField.boost = 1.5f;

        // when
        boolean resultOtherType = dateField.equals(otherField);
        // then
        Assert.assertFalse(resultOtherType);
    }
}
