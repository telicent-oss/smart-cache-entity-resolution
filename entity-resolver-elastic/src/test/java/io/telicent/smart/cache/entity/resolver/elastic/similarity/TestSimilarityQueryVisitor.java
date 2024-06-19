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
package io.telicent.smart.cache.entity.resolver.elastic.similarity;

import io.telicent.smart.cache.canonical.configuration.CanonicalTypeConfiguration;
import io.telicent.smart.cache.search.model.Document;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSimilarityQueryVisitor {

    private static String processString(String entry) {
        return StringUtils.deleteWhitespace(StringEscapeUtils.unescapeJava(entry));
    }
    @Test
    public void test_buildQuery_blankTextField() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        CanonicalTypeConfiguration.TextField textField = new CanonicalTypeConfiguration.TextField();
        String expectedQueryString = "Query:{\"match\":{\"\":{\"boost\":1.0,\"query\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\"}}}";
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(textField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }

    @Test
    public void test_buildQuery_textField_blankFuzziness() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        CanonicalTypeConfiguration.TextField textField = new CanonicalTypeConfiguration.TextField();
        textField.fuzziness = new CanonicalTypeConfiguration.Fuzziness();
        String expectedQueryString = "Query:{\"match\":{\"\":{\"boost\":1.0,\"fuzziness\":\"AUTO\",\"query\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\"}}}";
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(textField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }


    @Test
    public void test_buildQuery_textField_noMin() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        CanonicalTypeConfiguration.TextField textField = new CanonicalTypeConfiguration.TextField();
        textField.fuzziness = new CanonicalTypeConfiguration.Fuzziness();
        textField.fuzziness.max = 5;
        String expectedQueryString = "Query:{\"match\":{\"\":{\"boost\":1.0,\"fuzziness\":\"AUTO\",\"query\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\"}}}";
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(textField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }

    @Test
    public void test_buildQuery_textField_noMax() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        CanonicalTypeConfiguration.TextField textField = new CanonicalTypeConfiguration.TextField();
        textField.fuzziness = new CanonicalTypeConfiguration.Fuzziness();
        textField.fuzziness.min = 5;
        String expectedQueryString = "Query:{\"match\":{\"\":{\"boost\":1.0,\"fuzziness\":\"AUTO\",\"query\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\"}}}";
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(textField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }

    @Test
    public void test_buildQuery_blankDateField() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        CanonicalTypeConfiguration.DateField dateField = new CanonicalTypeConfiguration.DateField();
        String expectedQueryString = "Query:{\"match\":{\"\":{\"boost\":1.0,\"query\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\"}}}";
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(dateField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }

    @Test
    public void test_buildQuery_DateField_noPivot() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        CanonicalTypeConfiguration.DateField dateField = new CanonicalTypeConfiguration.DateField();
        dateField.distance = new CanonicalTypeConfiguration.Distance();
        String expectedQueryString = "Query:{\"match\":{\"\":{\"boost\":1.0,\"query\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\"}}}";
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(dateField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }

    @Test
    public void test_buildQuery_blankLocationField() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        String expectedQueryString = "Query:{\"distance_feature\":{\"boost\":1.0,\"origin\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\",\"pivot\":\"1in\",\"field\":\"\"}}";
        CanonicalTypeConfiguration.LocationField locationField = new CanonicalTypeConfiguration.LocationField();
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(locationField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }

    @Test
    public void test_buildQuery_locationField_noPivot() {
        // given
        SimilarityQueryVisitor visitor = new SimilarityQueryVisitor();
        String expectedQueryString = "Query:{\"distance_feature\":{\"boost\":1.0,\"origin\":\"{\\n\\\"field\\\":\\\"value\\\"\\n}\",\"pivot\":\"1in\",\"field\":\"\"}}";
        CanonicalTypeConfiguration.LocationField locationField = new CanonicalTypeConfiguration.LocationField();
        locationField.distance = new CanonicalTypeConfiguration.Distance();
        Document doc = new Document();
        doc.setProperty("field", "value");
        // when
        visitor.buildQuery(locationField, doc);
        // then
        Assert.assertEquals(processString(visitor.getQuery().toString()), processString(expectedQueryString));
    }
}
