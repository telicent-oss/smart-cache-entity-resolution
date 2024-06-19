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

public class TestCanonicalTypeConfigurationNumberField {

    @Test
    public void equals_test_fieldMismatch() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);

        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("age");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.NumberField);
        CanonicalTypeConfiguration.NumberField numberField = (CanonicalTypeConfiguration.NumberField) field;

        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("date-field");
        otherField.name = "age";
        otherField.type = "number";
        otherField.boost = 5.0f;
        otherField.required = true;

        // when
        boolean otherTypeMismatch = numberField.equals(otherField);

        // then
        Assert.assertFalse(otherTypeMismatch);
    }


    @Test
    public void equals_test_fieldValueMatch() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);

        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("age");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.NumberField);
        CanonicalTypeConfiguration.NumberField numberField = (CanonicalTypeConfiguration.NumberField) field;

        CanonicalTypeConfiguration
                sameConfiguration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        Assert.assertNotNull(sameConfiguration);
        CanonicalTypeConfiguration.SimilarityField sameField = configuration.getField("age");
        Assert.assertTrue(sameField instanceof CanonicalTypeConfiguration.NumberField);
        CanonicalTypeConfiguration.NumberField sameNumberField = (CanonicalTypeConfiguration.NumberField) sameField;

        // when
        boolean sameValueMatch = numberField.equals(sameNumberField);

        // then
        Assert.assertTrue(sameValueMatch);
    }


    @Test
    @SuppressWarnings("SelfEquals")
    public void equals_test_fieldSame() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("age");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.NumberField);
        CanonicalTypeConfiguration.NumberField numberField = (CanonicalTypeConfiguration.NumberField) field;

        boolean sameMatch = numberField.equals(numberField);

        // then
        Assert.assertTrue(sameMatch);
    }

    @Test
    public void equals_test_valueMatch() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("age");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.NumberField);
        CanonicalTypeConfiguration.NumberField numberField = (CanonicalTypeConfiguration.NumberField) field;

        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("date-field");
        otherField.name = "age";
        otherField.type = "number";
        otherField.boost = 5.0f;
        otherField.required = false;
        boolean otherTypeMismatch = numberField.equals(otherField);

        // then
        Assert.assertFalse(otherTypeMismatch);
    }


    @Test
    public void equals_test_decayMismatch() {
        // given
        CanonicalTypeConfiguration
                configuration = CanonicalTypeConfiguration.loadFromString(TestCanonicalTypeConfiguration.HAPPY_STRING);
        // when
        Assert.assertNotNull(configuration);
        CanonicalTypeConfiguration.SimilarityField field = configuration.getField("age");
        Assert.assertTrue(field instanceof CanonicalTypeConfiguration.NumberField);
        CanonicalTypeConfiguration.NumberField numberField = (CanonicalTypeConfiguration.NumberField) field;

        CanonicalTypeConfiguration.SimilarityField otherField = configuration.getField("date-field");
        otherField.name = "age";
        otherField.type = "number";
        otherField.boost = 5.0f;
        otherField.required = false;
        boolean otherTypeMismatch = numberField.equals(otherField);
        boolean resultDecayMismatch = numberField.decay.equals(new Object());

        // then
        Assert.assertFalse(otherTypeMismatch);
        Assert.assertFalse(resultDecayMismatch);
    }


    @Test
    @SuppressWarnings("SelfEquals")
    public void decay_equals_same() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        // when
        boolean sameObjectResult = decay.equals(decay);
        // then
        Assert.assertTrue(sameObjectResult);
    }

    @Test
    public void decay_equals_null() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        // when
        boolean nullObjectResult = decay.equals(null);
        // then
        Assert.assertFalse(nullObjectResult);
    }

    @Test
    public void decay_equals_object() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        // when
        boolean differentObjectResult = decay.equals(new Object());
        // then
        Assert.assertFalse(differentObjectResult);
    }

    @Test
    public void decay_equals_decay() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        decay.decay = 0.1f;
        CanonicalTypeConfiguration.Decay otherDecay = new CanonicalTypeConfiguration.Decay();
        otherDecay.decay = 0.2f;
        // when
        boolean differentDecayResult = decay.equals(otherDecay);
        // then
        Assert.assertFalse(differentDecayResult);
    }

    @Test
    public void decay_equals_offset() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        decay.decay = 0.1f;
        decay.offset = "1";
        CanonicalTypeConfiguration.Decay otherDecay = new CanonicalTypeConfiguration.Decay();
        otherDecay.decay = 0.1f;
        // when
        boolean differentOffsetResult = decay.equals(otherDecay);
        // then
        Assert.assertFalse(differentOffsetResult);
    }

    @Test
    public void decay_equals_scale() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        decay.decay = 0.1f;
        decay.offset = "1";
        decay.scale = "1";
        CanonicalTypeConfiguration.Decay otherDecay = new CanonicalTypeConfiguration.Decay();
        otherDecay.decay = 0.1f;
        otherDecay.offset = "1";
        otherDecay.scale = "2";
        // when
        boolean differentScaleResult = decay.equals(otherDecay);
        // then
        Assert.assertFalse(differentScaleResult);
    }

    @Test
    public void decay_equals_same_values() {
        // given
        CanonicalTypeConfiguration.Decay decay = new CanonicalTypeConfiguration.Decay();
        decay.decay = 0.1f;
        decay.offset = "1";
        decay.scale = "1";
        CanonicalTypeConfiguration.Decay otherDecay = new CanonicalTypeConfiguration.Decay();
        otherDecay.decay = 0.1f;
        otherDecay.offset = "1";
        otherDecay.scale = "1";
        // when
        boolean sameValuesResult = decay.equals(otherDecay);
        // then
        Assert.assertTrue(sameValuesResult);
    }
}
