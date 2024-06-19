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

import co.elastic.clients.elasticsearch._types.Script;
import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import io.telicent.smart.cache.search.model.Document;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class TestContentDeletion extends AbstractContentModificationTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void delete_bad_01() {
        ContentDeletion.forDocument(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void delete_bad_02() {
        ContentDeletion.forDocument(new Document());
    }

    @Test
    public void delete_top_level_field_01() {
        Document doc = new Document();
        doc.getProperties().put("foo", "bar");

        Script script = ContentDeletion.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, ".remove('foo')");
    }

    @Test
    public void delete_top_level_field_02() {
        Document doc = new Document();
        doc.getProperties().put("foo", "bar");
        doc.getProperties().put("a", "12345");

        Script script = ContentDeletion.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, ".remove('foo')", ".remove('a')");
    }

    @Test
    public void delete_nested_field_01() {
        Document doc = new Document();
        doc.getProperties().put("foo", Map.of("a", 123));

        Script script = ContentDeletion.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "['foo'].remove('a')");
    }

    @Test
    public void delete_nested_field_02() {
        Document doc = new Document();
        doc.getProperties().put("foo", Map.of("bar", Map.of("a", 123)));

        Script script = ContentDeletion.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "['foo']['bar'].remove('a')");
    }

    @Test
    public void delete_list_item_01() {
        Document doc = new Document();
        doc.getProperties().put("foo", List.of("a"));

        Script script = ContentDeletion.forDocument(doc);
        verifyScriptHasParameters(script, "a");
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "if", "{", "}", PainlessFunctions.REMOVE_FROM_LIST_NAME,
                            "params.a",
                            "contains(item)",
                            "indexOf(item)");
    }

    @Test
    public void delete_list_item_02() {
        Document doc = new Document();
        doc.getProperties().put("foo", List.of("a", "b", "c"));

        Script script = ContentDeletion.forDocument(doc);
        verifyScriptHasParameters(script, "a", "b", "c");
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "if", "{", "}", PainlessFunctions.REMOVE_FROM_LIST_NAME,
                            "contains(item)", "indexOf(item)",
                            "params.a", "params.b", "params.c");
    }

    @Test
    public void delete_ignored_01() {
        Document doc = new Document();
        doc.getProperties().put(DefaultOutputFields.URI, "http://test");

        Script script = ContentDeletion.forDocument(doc);
        verifyEmptyScriptGenerated(script);
    }

    @Test
    public void delete_ignored_02() {
        Document doc = new Document();
        // Security labels are ignored for deletion
        doc.getProperties()
           .put(DefaultOutputFields.SECURITY_LABELS,
                Map.of(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS, "nationality=UK"));

        Script script = ContentDeletion.forDocument(doc);
        verifyEmptyScriptGenerated(script);
    }

    @Test
    public void delete_ignored_03() {
        Document doc = new Document();
        // Security labels are ignored for deletion
        doc.getProperties()
           .put(DefaultOutputFields.SECURITY_LABELS, Map.of(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_GRAPH, ""));

        Script script = ContentDeletion.forDocument(doc);
        verifyEmptyScriptGenerated(script);
    }

    @Test
    public void delete_ignored_04() {
        Document doc = new Document();
        doc.getProperties().put(DefaultOutputFields.URI, "http://test");
        doc.getProperties().put("hello", "world");
        // Security labels are ignored for deletion
        doc.getProperties()
           .put(DefaultOutputFields.SECURITY_LABELS,
                Map.of(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS, "nationality=UK", "hello", "*"));

        Script script = ContentDeletion.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);
        verifyScriptContent(generatedScript, ".remove('hello')", "['securityLabels'].remove('hello')");
    }

    @Test
    public void delete_from_complex_list_01() {
        Document doc = new Document();
        doc.getProperties().put("list", List.of(Map.of("instance", "1")));

        Script script = ContentDeletion.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);
        verifyScriptContent(generatedScript, PainlessFunctions.REMOVE_FROM_COMPLEX_LIST_NAME + "(",
                            SOURCE_ACCESSED + "['list']", "params.a", "instance");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*list of lists")
    public void delete_from_complex_list_02() {
        Document doc = new Document();
        doc.getProperties().put("list", List.of(List.of("a", "b")));

        ContentDeletion.forDocument(doc);
    }
}
