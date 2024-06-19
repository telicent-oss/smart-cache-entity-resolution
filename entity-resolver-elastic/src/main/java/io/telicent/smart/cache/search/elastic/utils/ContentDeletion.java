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
import io.telicent.smart.cache.search.elastic.ElasticSearchIndexer;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.utils.ContentLeafVisitor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Utilities relating to deleting content from ElasticSearch documents
 */
public class ContentDeletion extends ContentLeafVisitor {

    /**
     * These are the ignored fields whose values we don't ever want to delete as part of a delete BUT whose values are
     * required in order to know which document in the underlying ElasticSearch index should be modified
     */
    private static final String[] IGNORED_FIELDS = {
            DefaultOutputFields.URI,
            DefaultOutputFields.SECURITY_LABELS + "." + DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS,
            DefaultOutputFields.SECURITY_LABELS + "." + DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_GRAPH,
            DefaultOutputFields.METADATA
    };

    private final PainlessScriptBuilder builder = new PainlessScriptBuilder();

    /**
     * Private constructor prevents instantiation
     */
    public ContentDeletion() {
        super(IGNORED_FIELDS);
    }

    /**
     * Generates a script that translates a document into delete operations
     * <p>
     * Intended for use as an input function to configure an
     * {@link ElasticSearchIndexer} via the
     * {@link
     * ElasticSearchIndexer.ElasticSearchIndexerBuilder#deletingContentsWith(Function)}
     * method.
     * </p>
     *
     * @param document Document
     * @return Script
     */
    public static Script forDocument(Document document) {
        Objects.requireNonNull(document, "Document cannot be null");
        if (document.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete contents based upon an empty document");
        }

        return forMap(document.getProperties());
    }

    /**
     * Generates a script that translates a map into delete operations
     * <p>
     * Intended for use as an input function to configure an
     * {@link ElasticSearchIndexer} via the
     * {@link
     * ElasticSearchIndexer.ElasticSearchIndexerBuilder#deletingContentsWith(Function)}
     * method.
     * </p>
     *
     * @param map Map
     * @return Script
     */
    public static Script forMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "Map cannot be null");
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete contents based upon an empty map");
        }

        ContentDeletion deletion = new ContentDeletion();
        deletion.visit(map);
        return deletion.builder.asScript();
    }

    @Override
    public void visitLeafField(String[] path, Object item) {
        builder.deleteField(path);
    }

    @Override
    public void visitComplexListItem(String[] path, Object item) {
        builder.deleteComplexListItem(item, "instance", path);
    }

    @Override
    public void visitNestedListItem(String[] path, List<Object> item) {
        // Not supporting deletions from nested lists
        throw new IllegalArgumentException("Cannot generate a script that deletes from a list of lists");
    }

    @Override
    public void visitListItem(String[] path, Object item) {
        builder.deleteListItem(item, path);
    }
}
