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

public class TestContentUpdate extends AbstractContentModificationTests {

    @Test(expectedExceptions = NullPointerException.class)
    public void update_bad_01() {
        ContentUpdate.forDocument(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void update_bad_02() {
        ContentUpdate.forDocument(new Document());
    }

    @Test
    public void update_top_level_field_01() {
        Document doc = new Document();
        doc.getProperties().put("foo", "bar");

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "['foo'] = params.a");
        verifyScriptHasParameters(script, "a");
    }

    @Test
    public void update_top_level_field_02() {
        Document doc = new Document();
        doc.getProperties().put("foo", "bar");
        doc.getProperties().put("a", "12345");

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "['foo'] = params.", "['a'] = params.");
        verifyScriptHasParameters(script, "a", "b");
    }

    @Test
    public void update_nested_field_01() {
        Document doc = new Document();
        doc.getProperties().put("foo", Map.of("a", 123));

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "['foo']['a'] = params.a");
        verifyScriptHasParameters(script, "a");
    }

    @Test
    public void update_nested_field_02() {
        Document doc = new Document();
        doc.getProperties().put("foo", Map.of("bar", Map.of("a", 123)));

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "['foo']['bar']['a'] = params.a");
        verifyScriptHasParameters(script, "a");
    }

    @Test
    public void update_list_item_01() {
        Document doc = new Document();
        doc.getProperties().put("foo", List.of("a"));

        Script script = ContentUpdate.forDocument(doc);
        verifyScriptHasParameters(script, "a");
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "if", "{", "}", PainlessFunctions.ADD_TO_LIST_NAME,
                            "params.a",
                            "contains(item)",
                            "add(item)");
    }

    @Test
    public void update_list_item_02() {
        Document doc = new Document();
        doc.getProperties().put("foo", List.of("a", "b", "c"));

        Script script = ContentUpdate.forDocument(doc);
        verifyScriptHasParameters(script, "a", "b", "c");
        String generatedScript = verifyScriptWasGenerated(script);

        verifyScriptContent(generatedScript, SOURCE_ACCESSED, "if", "{", "}", PainlessFunctions.ADD_TO_LIST_NAME,
                            "contains(item)", "add(item)",
                            "params.a", "params.b", "params.c");
    }

    @Test
    public void update_ignored_01() {
        Document doc = new Document();
        doc.getProperties().put(DefaultOutputFields.URI, "http://test");

        Script script = ContentUpdate.forDocument(doc);
        verifyEmptyScriptGenerated(script);
    }

    @Test
    public void update_ignored_02() {
        // Security Labels are NOT ignored for updates
        Document doc = new Document();
        doc.getProperties()
           .put(DefaultOutputFields.SECURITY_LABELS,
                Map.of(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS, "nationality=UK"));

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);
        verifyScriptContent(generatedScript, "['securityLabels']['defaults'] = params.a");
        verifyScriptHasParameters(script, "a");
    }

    @Test
    public void update_ignored_03() {
        // Security labels are NOT ignored for updates
        Document doc = new Document();
        doc.getProperties()
           .put(DefaultOutputFields.SECURITY_LABELS, Map.of(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_GRAPH, ""));

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);
        verifyScriptContent(generatedScript, "['securityLabels']['graph'] = params.a");
        verifyScriptHasParameters(script, "a");
    }

    @Test
    public void update_ignored_04() {
        // Security labels are NOT ignored for updates
        Document doc = new Document();
        doc.getProperties().put(DefaultOutputFields.URI, "http://test");
        doc.getProperties().put("hello", "world");
        doc.getProperties()
           .put(DefaultOutputFields.SECURITY_LABELS,
                Map.of(DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS, "nationality=UK", "hello", "*"));

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);
        verifyScriptContent(generatedScript, "['hello'] = params.", "['securityLabels']['hello'] = params.");
        verifyScriptHasParameters(script, "a", "b");
    }

    @Test
    public void update_from_complex_list_01() {
        Document doc = new Document();
        doc.getProperties().put("list", List.of(Map.of("instance", "1")));

        Script script = ContentUpdate.forDocument(doc);
        String generatedScript = verifyScriptWasGenerated(script);
        verifyScriptContent(generatedScript, PainlessFunctions.ADD_TO_COMPLEX_LIST_NAME + "(",
                            SOURCE_ACCESSED, "'list'", "params.a", "instance");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*nested lists")
    public void update_from_complex_list_02() {
        Document doc = new Document();
        doc.getProperties().put("list", List.of(List.of("a", "b")));

        ContentUpdate.forDocument(doc);
    }
}
