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
package io.telicent.smart.cache.search.security;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.AttributeParser;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.smart.cache.entity.sinks.converters.DefaultOutputFields;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.utils.FieldNameExpression;
import io.telicent.smart.cache.search.model.utils.PathMatchingVisitor;
import io.telicent.smart.cache.search.options.SearchOptions;
import io.telicent.smart.cache.search.options.SecurityOptions;
import io.telicent.smart.cache.search.options.TypeFilterOptions;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * A context for secure searches
 * <p>
 * Provides an evaluation cache for the lifetime of a single search request which avoids repeatedly parsing the labels
 * into expressions, and repeatedly evaluating those expressions.  This takes advantage of the fact that often the same
 * label expressions are used across much of the data.
 * </p>
 */
public final class SecureSearchContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureSearchContext.class);

    @Getter
    private final SearchOptions searchOptions;
    @Getter
    private final AttributeValueSet userAttributes;
    private final AttributesStore store;
    private final CxtABAC abacContext;
    private final Cache<String, List<AttributeExpr>> labelsToExpressions;

    private final RedactedDocumentsCache redactedDocumentsCache;
    private final Map<AttributeExpr, Boolean> evaluations = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final List<FieldNameExpression> typeFilterFields = new ArrayList<>();

    /**
     * Creates a new context
     *
     * @param searchOptions       Search options
     * @param userAttributes      User attributes
     * @param store               Attributes store, used to lookup attribute hierarchies
     * @param labelsToExpressions Label expressions
     * @param typeFilterFields    Type filtering field expressions
     */
    private SecureSearchContext(SearchOptions searchOptions, AttributeValueSet userAttributes, AttributesStore store,
                                Cache<String, List<AttributeExpr>> labelsToExpressions,
                                RedactedDocumentsCache documentsCache, List<FieldNameExpression> typeFilterFields) {
        this.searchOptions = searchOptions != null ? searchOptions : SearchOptions.defaults();
        this.userAttributes = userAttributes != null ? userAttributes : AttributeValueSet.EMPTY;
        this.redactedDocumentsCache = documentsCache;
        this.abacContext = CxtABAC.context(this.userAttributes, this::lookupHierarchy, DatasetGraphZero.create());
        this.store = store;
        this.labelsToExpressions = labelsToExpressions;
        if (labelsToExpressions == null) {
            LOGGER.warn("Security labels cache missing, label enforcement performance may be reduced as a result");
        }
        this.typeFilterFields.addAll(typeFilterFields);
    }

    /**
     * Gets the username of the user this search context is for (if any)
     *
     * @return Username, or {@code null} if security is disabled
     */
    public String getUsername() {
        return searchOptions.getSecurity().isEnabled() ? searchOptions.getSecurity().getUsername() : null;
    }

    /**
     * Tries to parse label expressions into a list of expressions
     *
     * @param id        ID of the document to which these labels are applied
     * @param rawLabels Raw labels
     * @return Parsed label expressions, or an empty list if the labels list is malformed
     */
    static List<AttributeExpr> tryParseExpressions(String id, String rawLabels) {
        try {
            return AttributeParser.parseAttrExprList(rawLabels);
        } catch (AttributeSyntaxError e) {
            // If security labels as a whole are malformed unclear what the intent was, fallback to being secure
            // by treating malformed labels as denying access
            LOGGER.warn("Security labels ({}) on document {} are malformed: {}", rawLabels, id, e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses raw labels, which may consist of many label expressions, into a list of expressions
     * <p>
     * This maintains a cache of raw labels into split expressions, actual parsed expressions are separately cached when
     * using the {@link #evaluate(List)} method.
     * </p>
     *
     * @param id        ID of the document to which these labels are applied
     * @param rawLabels Raw labels
     * @return Parsed label expressions, or an empty list if the labels list is malformed
     */
    public List<AttributeExpr> parseLabelExpressions(String id, String rawLabels) {
        if (this.labelsToExpressions != null) {
            return this.labelsToExpressions.get(rawLabels, x -> tryParseExpressions(id, rawLabels));
        } else {
            return tryParseExpressions(id, rawLabels);
        }
    }

    /**
     * Evaluates the list of label expressions returning {@code true} if all expressions are satisfied by the users
     * attributes
     *
     * @param labels Label expressions
     * @return True if all expressions are satisfied by the users attributes for this context, false otherwise
     */
    public boolean evaluate(List<AttributeExpr> labels) {
        return labels.stream()
                     .allMatch(e -> evaluations.computeIfAbsent(e, x -> x.eval(this.abacContext).getBoolean()));
    }

    /**
     * Lookups an attribute hierarchy (if any)
     *
     * @param attribute Attribute
     * @return Hierarchy, or {@code null} if no such hierarchy
     */
    private Hierarchy lookupHierarchy(Attribute attribute) {
        if (this.store == null || !this.store.hasHierarchy(attribute)) {
            return null;
        }
        return this.store.getHierarchy(attribute);
    }

    /**
     * Determines whether a given document can be viewed within this search context
     *
     * @param id       Document ID
     * @param document Document
     * @return True if the document can be viewed, false otherwise
     * @deprecated Use {@link #canViewDocument(String, String, Document)} instead
     */
    @Deprecated(since = "0.11.0", forRemoval = true)
    public boolean canViewDocument(String id, Document document) {
        return canViewDocument(id, "1", document);
    }

    /**
     * Determines whether a given document can be viewed within this search context
     * <p>
     * This replaces the old two argument version of the method, the document version information is important from a
     * caching perspective to know whether the document has changed and thus an existing cached redaction decision needs
     * to be recalculated.
     * </p>
     *
     * @param id       Document ID
     * @param version  Document Version
     * @param document Document
     * @return True if the document can be viewed, false otherwise
     */
    public boolean canViewDocument(String id, String version, Document document) {
        SecurityOptions securityOptions = this.searchOptions.getSecurity();
        //@formatter:off
        if (securityOptions.isEnabled()) {
            // If a redacted documents cache is available go ahead and use it
            if (this.redactedDocumentsCache != null) {
                Boolean visibility = this.redactedDocumentsCache.isVisible(this, id, version);
                if (visibility == null) {
                    // Redacted documents cache has not yet cached the visibility for this document so calculate and
                    // cache it now (subject to below comments)
                    visibility = this.canViewFilteredDocument(id, document);
                    if (!visibility) {
                        // We only store false results, i.e. fully redacted documents, in the cache as the point of the
                        // cache is to skip the more expensive filtering step for documents we aren't going to return.
                        // However, for any document whose visibility is true we ALWAYS have to filter regardless
                        // because the user may not be permitted to see the full document, perhaps only part of it, and
                        // the cache is intentionally designed to only store an indicator of a documents' visibility.
                        this.redactedDocumentsCache.setVisible(this, id, version, false);
                    }
                    return visibility;
                } else if (!visibility) {
                    // Short-circuit filtering.  The redacted documents cache indicates the user cannot see this
                    // document at all so no need to filter it in detail, just return false i.e. not visible
                    return false;
                }
            }

            // If no redacted documents cache, OR the cache indicated the document is visible we need to filter it as
            // the user may only be able to see parts of the document
            return canViewFilteredDocument(id, document);
        } else if (!securityOptions.getShowSecurityLabels()) {
            // If show security label is disabled just trim the labels from the document (if any) before returning it
            document.trimSecurityLabels();
        }
        //@formatter:on
        return true;
    }

    /**
     * Determines if a given document can be viewed in this search context after security label filtering has been
     * applied
     *
     * @param id       Document ID
     * @param document Document
     * @return True if the document can be viewed, false otherwise
     */
    boolean canViewFilteredDocument(String id, Document document) {
        String rawLabels = (String) document.getProperty(DefaultOutputFields.SECURITY_LABELS,
                                                         DefaultOutputFields.SECURITY_LABELS_SUB_FIELD_DEFAULTS);
        if (StringUtils.isNotBlank(rawLabels)) {
            // Check whether user can see this document
            List<AttributeExpr> labels = this.parseLabelExpressions(id, rawLabels);
            if (CollectionUtils.isEmpty(labels)) {
                // Default labels are malformed so unable to determine the intent of those labels, for security
                // purposes default to a decision of false in this case i.e. the user will only be able to see
                // fields where fine-grained labels specifically permit them to do so.
                return filterDocument(id, document, false) && filterByType(id, document,
                                                                           this.searchOptions.getTypeFilterOpts());
            }
            return filterDocument(id, document, this.evaluate(labels)) && filterByType(id, document,
                                                                                       this.searchOptions.getTypeFilterOpts());
        } else {
            // No default labels but may still be fine-grained labels, assume user is allowed to see anything that
            // doesn't have more restrictive labels
            return filterDocument(id, document, true) && filterByType(id, document,
                                                                      this.searchOptions.getTypeFilterOpts());
        }
    }

    /**
     * Filters a documents contents based on the users security labels
     *
     * @param id              Document ID
     * @param document        Document
     * @param defaultDecision The default decision whether the user can see portions of the document based upon the
     *                        default labels of the document
     * @return True if the result of filtering is a non-empty document i.e. the user is permitted to see the document,
     * False otherwise
     */
    private boolean filterDocument(String id, Document document, boolean defaultDecision) {
        SecurityOptions securityOptions = this.searchOptions.getSecurity();
        document.filter(this, defaultDecision, securityOptions.getShowSecurityLabels());

        if (document.isEmpty()) {
            LOGGER.warn("Security labels prevented user {} from accessing document {}", securityOptions.getUsername(),
                        id);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Document {} had security labels which were not satisfied by user {}'s attribute set: {}",
                             id, securityOptions.getUsername(), securityOptions.getAttributes().toString());
            }
        }
        return !document.isEmpty();
    }

    /**
     * Filters a document to ensure that it meets any type filtering criteria present in the search options
     *
     * @param id                Document ID
     * @param document          Document
     * @param typeFilterOptions Type Filtering options
     * @return True if the document should be visible in the results, false if it should be removed from the results
     */
    private boolean filterByType(String id, Document document, TypeFilterOptions typeFilterOptions) {
        if (!typeFilterOptions.isEnabled()) {
            // No type filters so can view this document assuming other document filtering was passed
            return true;
        } else {
            AtomicBoolean matchFound = new AtomicBoolean(false);
            PathMatchingVisitor visitor =
                    new PathMatchingVisitor(createTypeFilterFunction(typeFilterOptions, matchFound),
                                            this.typeFilterFields);
            visitor.visit(document.getProperties());
            return matchFound.get();
        }
    }

    private static BiConsumer<String[], Object> createTypeFilterFunction(TypeFilterOptions typeFilterOptions,
                                                                         AtomicBoolean matchFound) {
        return (path, item) -> {
            if (item instanceof List<?>) {
                if (CollectionUtils.containsAny((Collection<?>) item, typeFilterOptions.getTypeFilter())) {
                    matchFound.set(true);
                }
            } else if (item instanceof String) {
                if (StringUtils.equals((String) item, typeFilterOptions.getTypeFilter())) {
                    matchFound.set(true);
                }
            }
        };
    }

    /**
     * Creates a builder for a new secure search context
     *
     * @return Builder
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * A builder for secure search contexts
     */
    public static class Builder {

        private SearchOptions searchOptions;
        private AttributeValueSet userAttributes = AttributeValueSet.EMPTY;
        private AttributesStore store;
        private Cache<String, List<AttributeExpr>> labelsToExpressions;
        private RedactedDocumentsCache redactedDocumentsCache;
        private List<FieldNameExpression> typeFilterFields = new ArrayList<>();

        /**
         * Sets the users attributes
         *
         * @param attributes User attributes
         * @return Builder
         */
        public Builder userAttributes(AttributeValueSet attributes) {
            this.userAttributes = attributes != null ? attributes : AttributeValueSet.EMPTY;
            return this;
        }

        /**
         * Sets that the user has no attributes, this is equivalent to calling
         * {@link #userAttributes(AttributeValueSet)} with {@link AttributeValueSet#EMPTY}
         *
         * @return Builder
         */
        public Builder noUserAttributes() {
            return userAttributes(AttributeValueSet.EMPTY);
        }

        /**
         * Sets the attribute store that is used for resolving attribute hierarchies
         *
         * @param store Attributes store
         * @return Builder
         */
        public Builder attributesStore(AttributesStore store) {
            this.store = store;
            return this;
        }

        /**
         * Sets that no attributes store is available for resolving attribute hierarchies
         *
         * @return Builder
         */
        public Builder noAttributesStore() {
            return attributesStore(null);
        }

        /**
         * Sets the parser cache used to cache the parsing of raw label expressions into parsed label expressions
         *
         * @param cache Parser cache
         * @return Builder
         */
        public Builder withParserCache(Cache<String, List<AttributeExpr>> cache) {
            this.labelsToExpressions = cache;
            return this;
        }

        /**
         * Sets that the parser cache is disabled
         *
         * @return Builder
         */
        public Builder withoutParserCache() {
            return withParserCache(null);
        }

        /**
         * Configures the default redacted documents cache implementation
         *
         * @param maxUsers            Maximum users to cache redacted document results for
         * @param maxDocumentsPerUser Maximum documents to cache per-user
         * @param expiresAfter        Specifies after how long cache entries expire
         * @return Builder
         * @throws IllegalArgumentException Thrown if the cache parameters are invalid
         */
        public Builder withRedactionCache(int maxUsers, int maxDocumentsPerUser, Duration expiresAfter) {
            return withRedactionCache(
                    RedactedDocumentsConfiguration.create(maxUsers, maxDocumentsPerUser, expiresAfter));
        }

        /**
         * Configures a redacted documents cache
         *
         * @param cache Cache
         * @return Builder
         */
        public Builder withRedactionCache(RedactedDocumentsCache cache) {
            this.redactedDocumentsCache = cache;
            return this;
        }

        /**
         * Disables the redacted documents cache
         *
         * @return Builder
         */
        public Builder withoutRedactionCache() {
            return withRedactionCache(null);
        }

        /**
         * Sets that type filtering is in use for this search context
         *
         * @param typeFields The field name expressions indicating the fields upon which type filtering applies
         * @return Builder
         */
        public Builder typeFilterFields(List<FieldNameExpression> typeFields) {
            this.typeFilterFields.clear();
            this.typeFilterFields.addAll(typeFields);
            return this;
        }

        /**
         * Sets that type filtering is not in use for this search context
         *
         * @return Builder
         */
        public Builder withoutTypeFiltering() {
            this.typeFilterFields.clear();
            return this;
        }

        /**
         * Populates the builder based upon the given search options
         *
         * @param options Search Options
         * @return Builder
         */
        public Builder fromSearchOptions(SearchOptions options) {
            this.searchOptions = options;
            if (this.searchOptions != null) {
                SecurityOptions securityOptions = options.getSecurity();
                return fromSecurityOptions(securityOptions);
            } else {
                return this;
            }
        }

        /**
         * Populates the builder based upon the given security options
         *
         * @param securityOptions Security Options
         * @return Builder
         */
        public Builder fromSecurityOptions(SecurityOptions securityOptions) {
            return this.userAttributes(securityOptions.getAttributes())
                       .attributesStore(securityOptions.getAttributesStore())
                       .withParserCache(securityOptions.getLabelsToExpressions());
        }

        /**
         * Builds the secure search context
         *
         * @return Secure search context
         */
        public SecureSearchContext build() {
            return new SecureSearchContext(this.searchOptions, this.userAttributes, this.store,
                                           this.labelsToExpressions, this.redactedDocumentsCache,
                                           this.typeFilterFields);
        }
    }

}
