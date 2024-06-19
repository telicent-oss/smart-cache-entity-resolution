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
package io.telicent.smart.cache.search.elastic.utils;

import co.elastic.clients.elasticsearch.core.search.Hit;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.options.HighlightingOptions;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestHighlighting {

    private String highlight(String value, String toHighlight, HighlightingOptions options) {
        String preTag = options.getPreTag() != null ? options.getPreTag() : "<em>";
        String postTag = options.getPostTag() != null ? options.getPostTag() : "</em>";
        return value.replace(toHighlight, preTag + toHighlight + postTag);
    }

    @Test
    public void no_highlighting_01() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("class", this.getClass().getCanonicalName());
        HighlightingOptions options = new HighlightingOptions(true, "<em>", "</em>");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc));
        Assert.assertTrue(MapUtils.isEmpty(hit.highlight()));

        Assert.assertNull(Highlighting.getHighlights(options, hit));
    }

    @Test
    public void no_highlighting_02() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("class", this.getClass().getCanonicalName());
        HighlightingOptions options = new HighlightingOptions(true, "<em>", "</em>");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .highlight("class",
                                                    Collections.singletonList(
                                                            highlight(this.getClass().getCanonicalName(), "telicent",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Assert.assertNull(Highlighting.getHighlights(options, hit));
    }

    @Test
    public void no_highlighting_03() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("class", this.getClass().getCanonicalName());
        HighlightingOptions options = new HighlightingOptions(true, "<em>", "</em>");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("class",
                                                    Collections.emptyList()));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertEquals(highlighted.getProperties().get("class"), doc.getProperties().get("class"));
    }

    @Test
    public void no_highlighting_04() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items", Arrays.asList("foo", "bar"));
        HighlightingOptions options = new HighlightingOptions(true, "*", "*");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items",
                                                    Collections.singletonList(
                                                            highlight("nope", "nope",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));
    }

    @Test
    public void no_highlighting_05() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("class", this.getClass().getCanonicalName());
        HighlightingOptions options = HighlightingOptions.DISABLED;

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("class",
                                                    Collections.singletonList(
                                                            highlight(this.getClass().getCanonicalName(), "telicent",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNull(highlighted);
    }

    @Test
    public void highlighting_01() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("class", this.getClass().getCanonicalName());
        HighlightingOptions options = new HighlightingOptions(true, "<em>", "</em>");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("class",
                                                    Collections.singletonList(
                                                            highlight(this.getClass().getCanonicalName(), "telicent",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("class"), doc.getProperties().get("class"));

        String highlightedField = (String) highlighted.getProperties().get("class");
        Assert.assertTrue(highlightedField.contains(options.getPreTag()));
        Assert.assertTrue(highlightedField.contains(options.getPostTag()));
    }

    @Test
    public void highlighting_02() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items", Arrays.asList("foo", "bar"));
        HighlightingOptions options = new HighlightingOptions(true);

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));

        String highlightedField = ((List<String>) highlighted.getProperties().get("items")).get(0);
        Assert.assertTrue(highlightedField.contains("<em>"));
        Assert.assertTrue(highlightedField.contains("</em>"));
    }

    @Test
    public void highlighting_03() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items", Arrays.asList("foo", "bar"));
        HighlightingOptions options = new HighlightingOptions(true, "*", "*");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));

        String highlightedField = ((List<String>) highlighted.getProperties().get("items")).get(0);
        Assert.assertTrue(highlightedField.contains(options.getPreTag()));
        Assert.assertTrue(highlightedField.contains(options.getPostTag()));
    }

    @Test
    public void highlighting_04() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items", List.of("foo"));
        HighlightingOptions options = new HighlightingOptions(true, "*", "*");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));

        String highlightedField = ((List<String>) highlighted.getProperties().get("items")).get(0);
        Assert.assertTrue(highlightedField.contains(options.getPreTag()));
        Assert.assertTrue(highlightedField.contains(options.getPostTag()));
    }

    @Test
    public void highlighting_05() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items",
                        Arrays.asList(Map.of("name", "foo", "value", 12345), Map.of("name", "bar", "value", 6789)));
        HighlightingOptions options = new HighlightingOptions(true);

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items.name",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));

        String highlightedField =
                (String) ((List<Map<String, Object>>) highlighted.getProperties().get("items")).get(0).get("name");
        Assert.assertTrue(highlightedField.contains("<em>"));
        Assert.assertTrue(highlightedField.contains("</em>"));
    }

    @Test
    public void highlighting_06() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items",
                        Collections.singletonList(Map.of("name", "foo", "value", 12345)));
        HighlightingOptions options = new HighlightingOptions(true, "/*", "*/");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items.name",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));

        String highlightedField =
                (String) ((List<Map<String, Object>>) highlighted.getProperties().get("items")).get(0).get("name");
        Assert.assertTrue(highlightedField.contains("/*"));
        Assert.assertTrue(highlightedField.contains("*/"));
    }

    @Test
    public void highlighting_07() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("items",
                        Arrays.asList(Map.of("name", "foo", "value", 12345), Map.of("name", "bar", "value", 6789)));
        HighlightingOptions options = new HighlightingOptions(true);

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(1.23)
                                         .source(doc)
                                         .highlight("items.name",
                                                    // Injecting a highlight that doesn't actually correspond to any document field
                                                    Collections.singletonList(
                                                            highlight("test", "test",
                                                                      options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertEquals(highlighted.getProperties().get("items"), doc.getProperties().get("items"));

        String highlightedField =
                (String) ((List<Map<String, Object>>) highlighted.getProperties().get("items")).get(0).get("name");
        Assert.assertFalse(highlightedField.contains("<em>"));
        Assert.assertFalse(highlightedField.contains("</em>"));
    }

    @Test
    public void highlighting_08() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("a", Map.of("b", Map.of("c", "foo", "d", "bar")));
        HighlightingOptions options = new HighlightingOptions(true, "<pre>", null);

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(7.89)
                                         .source(doc)
                                         .highlight("a.b.c",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo", options)))
                                         .highlight("a.b.d",
                                                    Collections.singletonList(
                                                            highlight("bar", "bar", options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("a"), doc.getProperties().get("a"));

        String highlightedField = (String) highlighted.getProperty("a", "b", "c");
        Assert.assertTrue(highlightedField.contains("<pre>"));
        Assert.assertTrue(highlightedField.contains("</em>"));
        highlightedField = (String) highlighted.getProperty("a", "b", "d");
        Assert.assertTrue(highlightedField.contains("<pre>"));
        Assert.assertTrue(highlightedField.contains("</em>"));
    }

    @Test
    public void highlighting_09() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("a", Map.of("b", Map.of("c", "foo", "d", "bar")));
        HighlightingOptions options = new HighlightingOptions(true, null, "</post>");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(7.89)
                                         .source(doc)
                                         .highlight("a.b.c",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo", options)))
                                         .highlight("a.b.d",
                                                    Collections.singletonList(
                                                            highlight("bar", "bar", options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("a"), doc.getProperties().get("a"));

        String highlightedField = (String) highlighted.getProperty("a", "b", "c");
        Assert.assertTrue(highlightedField.contains("<em>"));
        Assert.assertTrue(highlightedField.contains("</post>"));
        highlightedField = (String) highlighted.getProperty("a", "b", "d");
        Assert.assertTrue(highlightedField.contains("<em>"));
        Assert.assertTrue(highlightedField.contains("</post>"));
    }

    @Test
    public void highlighting_10() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("a", Map.of("b", Arrays.asList(Map.of("c", "foo"), Map.of("d", "bar"))));
        HighlightingOptions options = new HighlightingOptions(true, "<pre>", null);

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(7.89)
                                         .source(doc)
                                         .highlight("a.b.c",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo", options)))
                                         .highlight("a.b.d",
                                                    Collections.singletonList(
                                                            highlight("bar", "bar", options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("a"), doc.getProperties().get("a"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) highlighted.getProperty("a", "b");

        String highlightedField = (String) items.get(0).get("c");
        Assert.assertTrue(highlightedField.contains("<pre>"));
        Assert.assertTrue(highlightedField.contains("</em>"));
        highlightedField = (String) items.get(1).get("d");
        Assert.assertTrue(highlightedField.contains("<pre>"));
        Assert.assertTrue(highlightedField.contains("</em>"));
    }

    @Test
    public void highlighting_11() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("a", Map.of("b", Arrays.asList(Map.of("c", "foo"), Map.of("d", "bar"))));
        HighlightingOptions options = new HighlightingOptions(true, null, "</post>");

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(7.89)
                                         .source(doc)
                                         .highlight("a.b.c",
                                                    Collections.singletonList(
                                                            highlight("foo", "foo", options)))
                                         .highlight("a.b.d",
                                                    Collections.singletonList(
                                                            highlight("bar", "bar", options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("a"), doc.getProperties().get("a"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) highlighted.getProperty("a", "b");

        String highlightedField = (String) items.get(0).get("c");
        Assert.assertTrue(highlightedField.contains("<em>"));
        Assert.assertTrue(highlightedField.contains("</post>"));
        highlightedField = (String) items.get(1).get("d");
        Assert.assertTrue(highlightedField.contains("<em>"));
        Assert.assertTrue(highlightedField.contains("</post>"));
    }

    @Test
    // Values including URLs are highlighted correctly, i.e. only one highlight per URL
    public void highlighting_12() {
        Document doc = new Document();
        doc.setProperty("test", true);
        doc.setProperty("a", Map.of("b", Arrays.asList(
                Map.of("c", "http://example.domain.com/example-folder/folder2/example-page.html"),
                Map.of("d", "Example text http://example.domain.com/example-folder/example-page.html more text."),
                Map.of("e", "Example text http://example.domain.com/example-folder/example-page.html more text."))));
        HighlightingOptions options = new HighlightingOptions(true, null, null);

        Hit<Document> hit = Hit.of(h -> h.id("test")
                                         .index("test")
                                         .score(7.89)
                                         .source(doc)
                                         .highlight("a.b.c",
                                                    Collections.singletonList(
                                                            highlight(
                                                                    "http://example.domain.com/example-folder/folder2/example-page.html",
                                                                    "http://example.domain.com/example-folder/folder2/example-page.html",
                                                                    options)))
                                         .highlight("a.b.d",
                                                    Collections.singletonList(
                                                            highlight(
                                                                    "Example text http://example.domain.com/example-folder/example-page.html more text.",
                                                                    "Example text http://example.domain.com/example-folder/example-page.html more text.",
                                                                    options)))
                                         .highlight("a.b.e",
                                                    Collections.singletonList(
                                                            highlight(
                                                                    "Example text http://example.domain.com/example-folder/example-page.html more text.",
                                                                    "http://example.domain.com/example-folder/example-page.html",
                                                                    options))));
        Assert.assertFalse(MapUtils.isEmpty(hit.highlight()));

        Document highlighted = Highlighting.getHighlights(options, hit);
        Assert.assertNotNull(highlighted);
        Assert.assertNotEquals(highlighted.getProperties().get("a"), doc.getProperties().get("a"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) highlighted.getProperty("a", "b");

        String highlightedField = (String) items.get(0).get("c");
        Assert.assertEquals(StringUtils.countMatches(highlightedField, "<em>"), 1);
        Assert.assertEquals(StringUtils.countMatches(highlightedField, "</em>"), 1);
        highlightedField = (String) items.get(1).get("d");
        Assert.assertEquals(StringUtils.countMatches(highlightedField, "<em>"), 1);
        Assert.assertEquals(StringUtils.countMatches(highlightedField, "</em>"), 1);
        highlightedField = (String) items.get(2).get("e");
        Assert.assertEquals(StringUtils.countMatches(highlightedField, "<em>"), 1);
        Assert.assertEquals(StringUtils.countMatches(highlightedField, "</em>"), 1);
    }
}
