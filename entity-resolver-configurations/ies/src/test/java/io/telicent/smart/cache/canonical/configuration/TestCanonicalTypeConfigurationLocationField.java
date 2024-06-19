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

public class TestCanonicalTypeConfigurationLocationField {
    @Test
    public void equals_test_mismatch_otherType() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("location-field");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.LocationField);
        CanonicalTypeConfiguration.LocationField locationField = (CanonicalTypeConfiguration.LocationField) field;

        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("text-field-fuzzy");
        otherField.name = "location-field";
        otherField.type = "geo-point";
        otherField.boost = 10.0f;
        otherField.required = false;
        boolean resultOtherType = locationField.equals(otherField);

        // then
        Assert.assertFalse(resultOtherType);
    }

    @Test
    public void hashcode_diff_nullDistance() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("location-field");
        CanonicalTypeConfiguration.SimilarityField otherDateField = configuration.getField("location-field-no-distance");
        // when
        int hashCode = field.hashCode();
        int resultOtherDateFieldHashCode = otherDateField.hashCode();

        // then
        Assert.assertNotEquals(hashCode, resultOtherDateFieldHashCode);
    }

    @Test
    @SuppressWarnings("SelfEquals")
    public void distance_equals_same() {
        // given
        CanonicalTypeConfiguration.Distance distance = new CanonicalTypeConfiguration.Distance();
        // when
        boolean sameObjectResult = distance.equals(distance);
        // then
        Assert.assertTrue(sameObjectResult);
    }

    @Test
    public void distance_equals_null() {
        // given
        CanonicalTypeConfiguration.Distance distance = new CanonicalTypeConfiguration.Distance();
        // when
        boolean nullObjectResult = distance.equals(null);
        // then
        Assert.assertFalse(nullObjectResult);
    }

    @Test
    public void distance_equals_object() {
        // given
        CanonicalTypeConfiguration.Distance distance = new CanonicalTypeConfiguration.Distance();
        // when
        boolean differentObjectResult = distance.equals(new Object());
        // then
        Assert.assertFalse(differentObjectResult);
    }

    @Test
    public void distance_equals_pivot() {
        // given
        CanonicalTypeConfiguration.Distance distance = new CanonicalTypeConfiguration.Distance();
        distance.pivot = "1m";
        CanonicalTypeConfiguration.Distance otherDistance = new CanonicalTypeConfiguration.Distance();
        otherDistance.pivot = "5m";
        // when
        boolean differentDistanceResult = distance.equals(otherDistance);
        // then
        Assert.assertFalse(differentDistanceResult);
    }

    @Test
    public void distance_equals_same_values() {
        // given
        CanonicalTypeConfiguration.Distance distance = new CanonicalTypeConfiguration.Distance();
        distance.pivot = "1m";
        CanonicalTypeConfiguration.Distance otherDistance = new CanonicalTypeConfiguration.Distance();
        otherDistance.pivot = "1m";
        // when
        boolean sameValuesResult = distance.equals(otherDistance);
        // then
        Assert.assertTrue(sameValuesResult);
    }
}
