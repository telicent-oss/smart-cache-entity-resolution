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
package io.telicent.smart.cache.search.elastic;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.reload_search_analyzers.ReloadDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.telicent.smart.cache.search.SearchClient;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.elastic.utils.Highlighting;
import io.telicent.smart.cache.search.model.*;
import io.telicent.smart.cache.search.model.utils.PathMatchingVisitor;
import io.telicent.smart.cache.search.options.HighlightingOptions;
import io.telicent.smart.cache.search.options.SearchOptions;
import io.telicent.smart.cache.search.options.SecurityOptions;
import io.telicent.smart.cache.search.options.SortField.Direction;
import io.telicent.smart.cache.search.options.TypeFilterOptions;
import io.telicent.smart.cache.search.security.CaffeineRedactedDocumentsCache;
import io.telicent.smart.cache.search.security.RedactedDocumentsCache;
import io.telicent.smart.cache.search.security.SecureSearchContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.logging.FmtLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Arrays.asList;

/**
 * A search client backed by ElasticSearch
 */
public class ElasticSearchClient extends AbstractElasticClient implements SearchClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchClient.class);

    /**
     * This is the default page size we'll use for our queries to ElasticSearch.
     * <p>
     * Earlier versions of Search used a very small page size (100) but this led to performance problems because it
     * meant we were scrolling in tiny increments for very large results. This led to a lot of unnecessary network
     * traffic in production environments that killed performance.
     * </p>
     */
    public static final int DEFAULT_PAGE_SIZE = 2500;
    /**
     * This is the minimum page size that ElasticSearch permits
     */
    public static final int MINIMUM_PAGE_SIZE = 1;
    /**
     * This is the default maximum page size imposed by ElasticSearch on queries
     */
    public static final int DEFAULT_MAXIMUM_PAGE_SIZE = 10000;

    private final List<String> indices;

    /**
     * Make it configurable later on
     **/
    private final String synonymsIndex = "synonyms-plugin";

    /**
     * Make it configurable later on
     **/
    private static final String ID_FIELD = "fa_id";

    private final AtomicInteger serverMaxPageSize = new AtomicInteger(-1);

    private final RedactedDocumentsCache redactedDocumentsCache;

    /**
     * Builds a new ElasticSearch client.
     *
     * @return the newly built ElasticSearch client.
     */
    public static ElasticSearchClientBuilder<?, ?> builder() {
        return new ElasticSearchClientBuilderImpl();
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost ElasticSearch host
     * @param elasticPort ElasticSearch port
     * @param index       Index for searching on resolved RDF entities
     */
    ElasticSearchClient(String elasticHost, int elasticPort, String index) {
        this(elasticHost, elasticPort, index, null, null, false);
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost ElasticSearch host
     * @param elasticPort ElasticSearch port
     * @param indices     The indexes for searching.
     */
    ElasticSearchClient(String elasticHost, int elasticPort, final List<String> indices) {
        this(elasticHost, elasticPort, indices, null, null, null, false, null);
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param index                    Index for searching on resolved RDF entities
     * @param user                     ElasticSearch user
     * @param password                 ElasticSearch password
     * @param makeOpenSearchCompatible Whether to make the client "compatible", in so far as is possible, with
     *                                 OpenSearch servers
     */
    protected ElasticSearchClient(String elasticHost, int elasticPort, String index, String user, String password,
                                  boolean makeOpenSearchCompatible) {
        this(elasticHost, elasticPort, index == null ? null : List.of(index), user, password, null, makeOpenSearchCompatible,
             null);
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param indices                  The indexes for searching.
     * @param user                     ElasticSearch user
     * @param password                 ElasticSearch password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     * @param makeOpenSearchCompatible Whether to make the client "compatible", in so far as is possible, with
     *                                 OpenSearch servers
     * @param cache                    Redacted Documents Cache
     */
    protected ElasticSearchClient(String elasticHost, int elasticPort, final List<String> indices,
                                  String user, String password, String elasticTlsCaCert,
                                  boolean makeOpenSearchCompatible, RedactedDocumentsCache cache) {
        super(elasticHost, elasticPort, user, password, elasticTlsCaCert, makeOpenSearchCompatible);
        if (CollectionUtils.isEmpty(indices) || indices.stream().anyMatch(StringUtils::isBlank)) {
            throw new IllegalArgumentException("Indices to search cannot be null/empty");
        }
        this.indices = indices;
        this.redactedDocumentsCache = cache;
    }

    @Override
    public void close() throws Exception {
        super.close();

        if (this.redactedDocumentsCache != null) {
            this.redactedDocumentsCache.close();
        }
    }

    @Override
    public boolean supports(QueryType type) {
        // ElasticSearch supports everything but wildcard
        return switch (type) {
            case WILDCARD -> false;
            default -> true;
        };
    }

    @Override
    public boolean supports(SearchOptions options) {
        // ElasticSearch fully supports all the currently known search options provided
        // that if highlighting is enabled
        // both pre-tag and post-tag are specified
        if (options.getHighlighting().isEnabled()) {
            if (options.getHighlighting().usesCustomTags() && !options.getHighlighting().bothTagsSet()) {
                // If using custom tags MUST specify both a pre-tag and post-tag
                return false;
            }
        }
        return true;
    }


    @Override
    public Document getDocument(String id, SecurityOptions securityOptions) {
        return indices.stream().map(index -> {
            try {
                GetResponse<Document> response = this.client.get(g -> g.index(index).id(id), Document.class);
                if (response.found()) {
                    SecureSearchContext context =
                            buildSecureSearchContext(SearchOptions.create().withSecurity(securityOptions).build());
                    // NB - canViewDocument() will call filter() on the Document object returned by response.source(), this
                    // filtering happens in-place meaning its safe to return the source() value directly after this
                    if (context.canViewDocument(id, String.valueOf(response.version()), response.source())) {
                        return response.source();
                    }
                }
                return null;
            } catch (IOException e) {
                throw new SearchException(
                        String.format("Failed to get document with ID %s from ElasticSearch index %s: %s", id, index,
                                      e.getMessage()), e);
            }
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private SearchResults searchCommon(String rawQuery, SearchOptions options, QueryType type, Query query) {
        // Short Circuit - if the user asks for zero results bypass any search entirely and just return an empty
        // results.  This was added as of #62 since we no longer need to calculate and report a total so if the user
        // has asked for nothing just give them that straight back!
        if (options.getLimit() == 0) {
            LOGGER.info("Query with Limit 0 short-circuits any ElasticSearch query evaluation");
            return new SearchResults(true, options.getLimit(), options.getOffset(), rawQuery, type,
                                     Collections.emptyList());
        }

        try {
            // Potentially modify query by adding type filtering if enabled
            TypeFilterOptions typeFilterOpts = options.getTypeFilterOpts();
            if (typeFilterOpts.isEnabled()) {
                Query origQuery = query;
                //@formatter:off
                query = Query.of(q -> q.bool(b -> b.must(origQuery)
                                                   .filter(f ->
                                                       f.multiMatch(mm -> mm.fields(filterModeFields(typeFilterOpts.getTypeFilterMode()))
                                                                            .type(TextQueryType.Phrase)
                                                                            .query(typeFilterOpts.getTypeFilter())))));
                //@formatter:on
            }

            // Build our search request passing through the search options where possible
            // NB - We need ElasticSearch's internal version field to be returned as we use this for cache keying in the
            //      RedactedDocumentsCache if one is configured
            Time scrollTimeout = Time.of(t -> t.time("1m"));
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(indices).version(true).query(query);

            // Detect the servers configured maximum page size if we haven't yet done so
            detectServerMaxPageSize();

            //@formatter:off

            // ElasticSearch doesn't like returning large number of results in a single response, so we limit our page
            // size regardless of the limit. We can't ever set the size to 0, so we set the size to 1 in that case so
            // that we can still accurately report how many results are available
            //
            // Note that we can't use the "from" parameter in conjunction with scrolling, so we may have to apply offset
            // manually after the fact
            //
            // We'll make a decision whether to scroll or not based upon the actual limit in-use, if its small enough
            // we'll push both the limit and offset down to ElasticSearch
            //
            // When security is enabled we'll be filtering results as they come back from ElasticSearch so always want
            // to use scrolling, so we can continue scrolling until we have enough results to satisfy the other options
            long requestedWindow = calculateEffectiveLimit(options);
            boolean useScrolling
                    = options.getLimit() == SearchResults.UNLIMITED
                      || options.getLimit() > this.serverMaxPageSize.get()
                      || requestedWindow > DEFAULT_MAXIMUM_PAGE_SIZE
                      || options.getSecurity().isEnabled();

            if (useScrolling) {
                // Scroll with a reasonable page size otherwise we'll waste a lot of time hopping back and forth across
                // the network scrolling in tiny increments.  Prior to #205 we set this to the effective limit BUT that
                // meant we scrolled the entire result set in tiny chunks which led to network traffic dominating
                // performance.  Subsequently, #334 also demonstrated that doing too much work will also harm
                // performance, so we try to strike a balance.  This is especially true when security is enabled as
                // just because the user asked for the first 10 results doesn't mean their attributes permit them to
                // see the first 10 results that ES happens to give back.
                // See selectScrollPageSize() for more detail on the heuristics we use to choose this
                int scrollPageSize = selectScrollPageSize(options, requestedWindow);
                LOGGER.info("Selected scroll page size {} for {} query '{}'...", scrollPageSize, type, rawQuery);
                builder.scroll(scrollTimeout)
                       .size(scrollPageSize);
            } else {
                // Can apply limit and offset directly
                builder.size(Math.max((int) options.getLimit(), MINIMUM_PAGE_SIZE))
                       .from((int) (options.getOffset() - 1));
            }
            //@formatter:on

            builder.trackTotalHits(t -> t.enabled(true));

            // If highlighting is enabled then add options for that now
            if (options.getHighlighting().isEnabled()) {
                builder.highlight(h -> h.fields("*", buildHighlightingField(options.getHighlighting())));
            }

            // User specified sort options
            io.telicent.smart.cache.search.options.SortOptions sortOpts = options.getSortOptions();
            if (!sortOpts.equals(io.telicent.smart.cache.search.options.SortOptions.NONE)) {
                final List<SortOptions> esSortOpts = new ArrayList<>();
                sortOpts.getFields().forEach(t -> {
                    final SortOrder order =
                            t.getDirection().equals(Direction.ASCENDING) ? SortOrder.Asc : SortOrder.Desc;
                    final FieldSort fs = FieldSort.of(f -> f.field(t.getFieldName()).order(order));
                    esSortOpts.add(SortOptions.of(f -> f.field(fs)));
                });
                builder.sort(esSortOpts);
            }
            // If no sort options have been specifically set by the user, this is the default sort order. In order to
            // fulfill the SearchResults API contract that the contained results are in sorted order we need to
            // explicitly sort by score
            else {
                builder.sort(s -> s.score(ScoreSort.of(sc -> sc.order(SortOrder.Desc))))
                       .sort(s -> s.doc(d -> d.order(SortOrder.Asc)));
            }

            //@formatter:off
            //
            // Make the query and collect up the results
            //
            // We try to short-circuit evaluation to stop once we have retrieved and filtered sufficient results to
            // answer the users query.
            //
            //@formatter:on
            LOGGER.info("Starting search for {} query '{}'...", type, rawQuery);
            long start = System.currentTimeMillis();
            SearchResponse<Document> response = this.client.search(builder.build(), Document.class);
            FmtLog.info(LOGGER, "Retrieved %,d initial results in %,d milliseconds", response.hits().hits().size(),
                        System.currentTimeMillis() - start);

            HitsMetadata<Document> metadata = response.hits();
            AtomicLong total = new AtomicLong(metadata.total().value());
            List<SearchResult> results = new ArrayList<>();
            if (options.getLimit() != 0 && options.getOffset() <= metadata.total().value()) {
                SecureSearchContext context = buildSecureSearchContext(options);
                hitsToResults(options, context, total, response.hits().hits(), results);
                scrollAdditionalResults(options, context, scrollTimeout, total, results, response.scrollId());
            } else {
                FmtLog.info(LOGGER,
                            "%s Query '%s' with offset %,d short-circuits document filtering as ElasticSearch only has %,d hits for it",
                            type, rawQuery, options.getOffset(), metadata.total().value());
                clearScroll(response.scrollId());
            }

            // Apply any limit and/or offset that we could not fully push down
            if (options.getLimit() == 0) {
                results = Collections.emptyList();
            } else if (useScrolling && options.getOffset() > SearchResults.FIRST_OFFSET) {
                // Since we do have an offset which we didn't push down (because its incompatible with scrolling) we
                // need to go ahead and apply it now
                if (options.getOffset() <= results.size()) {
                    results = results.subList((int) (options.getOffset() - 1), results.size());
                } else {
                    results = Collections.emptyList();
                }
            }
            if (options.getLimit() != SearchResults.UNLIMITED && results.size() > options.getLimit()) {
                results = results.subList(0, (int) options.getLimit());
            }

            FmtLog.info(LOGGER, "Completed search for %s query '%s' in %,d total milliseconds", type, rawQuery,
                        System.currentTimeMillis() - start);
            return new SearchResults(isMaybeMore(total.longValue(), options), options.getLimit(), options.getOffset(),
                                     rawQuery, type, results);
        } catch (ElasticsearchException e) {
            // can't tell exactly why it failed but if there are sort fields
            // try again just in case one of the specified fields did not exist
            if (!options.getSortOptions().getFields().isEmpty()) {
                options.getSortOptions().getFields().clear();
                return searchCommon(rawQuery, options, type, query);
            }
            throw fromElasticException(e, "search indices " + indices);
        } catch (IOException e) {
            throw new SearchException(
                    String.format("Failed to search ElasticSearch indices %s: %s", this.indices, e.getMessage()), e);
        }
    }

    /**
     * Select a suitable scroll page size
     * <p>
     * Past issues have demonstrated that asking for too few results, or too many, can both harm performance.  Therefore
     * we try to strike a balance in how many results we ask for so that we both minimise the number of network requests
     * that might be needed to answer a query, and the amount of data that has to be communicated.
     * </p>
     * <p>
     * The selection takes into account the servers configured maximum page size, and the users requested window of
     * results, rounding up to a larger value where appropriate to avoid ever using too small a page size.
     * </p>
     *
     * @param options         Search options
     * @param requestedWindow Requested window of results, this is the limit plus the offset
     * @return Scroll page size
     */
    private int selectScrollPageSize(SearchOptions options, long requestedWindow) {
        // Safe to access, as searchCommon() will have called determineMaxServerPageSize() prior to calling us, which
        // guarantees to set this
        int serverMaxPageSize = this.serverMaxPageSize.get();

        // If asking for all results we always want to use the maximum page size possible to minimise network requests
        if (options.getLimit() == SearchResults.UNLIMITED) {
            return serverMaxPageSize;
        }

        // If the requested window is larger than the server maximum use the server maximum
        // This check comes before the subsequent one as in a worst case scenario the servers configured maximum page
        // size is less than our default page size
        if (requestedWindow >= serverMaxPageSize) {
            return serverMaxPageSize;
        }

        // As seen in #205 too small a page size will drastically reduce performance in a production environment as the
        // excessive network hops become too costly, thus for small page sizes always grab a decent chunk of results.
        // Since #334 we are going to lazily evaluate these anyway so while we might retrieve too much data we only have
        // to security filter enough of it to answer the users actual requested window
        if (requestedWindow <= DEFAULT_PAGE_SIZE) {
            return DEFAULT_PAGE_SIZE;
        }

        // For any other page size see how much a percentage of the server maximum page it is, if it's over 75% we round
        // up to the maximum, under that we use it as is.  We know that the request window is of a reasonable size, so
        // we're just making a judgement call whether to round up.
        double windowAsPercentageOfMax = (double) requestedWindow / (double) serverMaxPageSize;
        if (windowAsPercentageOfMax >= 0.75) {
            return serverMaxPageSize;
        } else {
            return (int) requestedWindow;
        }
    }

    /**
     * Detects the servers maximum page size i.e. the maximum number of results that may be retrieved in a single search
     * request.  The result is cached in the private member variable {@link #serverMaxPageSize} so this can be called
     * repeatedly and once the maximum page size has been detected this becomes a no-op.
     */
    private synchronized void detectServerMaxPageSize() {
        if (this.serverMaxPageSize.get() == -1) {
            final Integer maxPageSizeAcrossAllIndices = indices.stream().map(index -> {
                try {
                    GetIndexResponse response = this.client.indices().get(i -> i.index(index).includeDefaults(true));
                    Integer maxPageSize = response.result().get(index).settings().index().maxResultWindow();
                    if (maxPageSize == null) {
                        maxPageSize = response.result().get(index).defaults().index().maxResultWindow();
                    }

                    return maxPageSize;
                } catch (Throwable e) {
                    LOGGER.info(
                            "No maximum search page size for {} reported, using our default {} as failed to query server for this: {}",
                            this.name(), DEFAULT_PAGE_SIZE, e.getMessage());
                    return DEFAULT_PAGE_SIZE;
                }
            }).filter(Objects::nonNull).min(Integer::compare).orElse(null);

            if (maxPageSizeAcrossAllIndices != null && maxPageSizeAcrossAllIndices > 0) {
                LOGGER.info("Detected maximum search page size for {} indices {} is {}", this.name(), indices,
                            maxPageSizeAcrossAllIndices);
                this.serverMaxPageSize.set(maxPageSizeAcrossAllIndices);
            } else {
                LOGGER.info("No maximum search page size for {} reported, using our default {}", this.name(),
                            DEFAULT_PAGE_SIZE);
                this.serverMaxPageSize.set(DEFAULT_PAGE_SIZE);
            }
        }
    }

    /**
     * Calculates whether there are maybe more results available
     *
     * @param total   Total results as reported by ElasticSearch
     * @param options Search options
     * @return True if there may be more results if the user asked for a subsequent page/different limit etc., false if
     * there are definitely no more results
     */
    private boolean isMaybeMore(long total, SearchOptions options) {
        // If we've been asked for unlimited results then there inherently can't be any more because we've already
        // returned them all
        if (options.getLimit() == SearchResults.UNLIMITED) {
            return false;
        }

        // If the offset is beyond the total then there can't be any more results
        if (options.getOffset() >= total) {
            return false;
        }

        // If the offset plus the limit are less than the total then there are maybe more results
        return options.getOffset() + options.getLimit() < total;
    }

    /**
     * Builds the secure context for the search
     *
     * @param options Search options
     * @return Secure search context
     */
    private SecureSearchContext buildSecureSearchContext(SearchOptions options) {
        SecureSearchContext.Builder builder = SecureSearchContext.create().fromSearchOptions(options);
        if (this.redactedDocumentsCache != null) {
            builder.withRedactionCache(this.redactedDocumentsCache);
        }
        if (options.getTypeFilterOpts().isEnabled()) {
            // Precompile the field match expressions for the type filtering once for the search context
            //@formatter:off
            builder.typeFilterFields(PathMatchingVisitor.compileExpressions(
                                        filterModeFields(options.getTypeFilterOpts()
                                                                .getTypeFilterMode())
                   .stream()
                   // The filterModeFields() returns the keyword field names used internally within the index
                   // and which are created by our indexing rules.
                   // However, the document itself won't have this in the field names, so we remove it to get
                   // an expression that will work with PathMatchingVisitor
                   .map(f -> StringUtils.removeEnd(f, ".keyword"))
                   .toList()));
            //@formatter:on
        }
        return builder.build();
    }

    /**
     * Builds highlighting field options based on the provided highlighting options
     *
     * @param highlighting Highlighting fields
     * @return Highlight field
     */
    private HighlightField buildHighlightingField(HighlightingOptions highlighting) {
        HighlightField.Builder builder = new HighlightField.Builder().numberOfFragments(0);
        if (highlighting.bothTagsSet()) {
            builder.preTags(highlighting.getPreTag()).postTags(highlighting.getPostTag());
        } else if (highlighting.usesCustomTags() && !highlighting.bothTagsSet()) {
            throw new SearchException(
                    "Must set both highlighting pre-tag and post-tag when enabling Highlighting for ElasticSearch");
        }
        return builder.build();
    }

    /**
     * Scrolls through additional results
     *
     * @param options       Search options
     * @param context       Search Context
     * @param scrollTimeout Scroll timeout
     * @param total         Total results
     * @param results       Results array being built
     * @param scrollId      Scroll ID
     * @throws IOException Thrown if there's a problem scrolling further results
     */
    private void scrollAdditionalResults(SearchOptions options, SecureSearchContext context, Time scrollTimeout,
                                         AtomicLong total, List<SearchResult> results, String scrollId) throws
            IOException {
        // If we aren't using scrolling no need to scroll for additional results
        if (StringUtils.isBlank(scrollId)) {
            return;
        }

        long effectiveLimit = calculateEffectiveLimit(options);
        while (options.getLimit() == SearchResults.UNLIMITED || results.size() < effectiveLimit) {
            final String finalScrollId = scrollId;
            long start = System.currentTimeMillis();
            LOGGER.info("Scrolling for additional results...");
            ScrollResponse<Document> scrollResponse =
                    this.client.scroll(s -> s.scrollId(finalScrollId).scroll(scrollTimeout), Document.class);
            FmtLog.info(LOGGER, "Retrieved %,d additional results from scrolling in %,d milliseconds",
                        scrollResponse.hits().hits().size(), System.currentTimeMillis() - start);
            if (!scrollResponse.hits().hits().isEmpty()) {
                hitsToResults(options, context, total, scrollResponse.hits().hits(), results);
                scrollId = scrollResponse.scrollId();
            } else {
                // When a scroll response returns empty there are no further results
                break;
            }
        }
        clearScroll(scrollId);
    }

    private void clearScroll(final String scrollId) throws IOException {
        if (StringUtils.isNotBlank(scrollId)) {
            this.client.clearScroll(s -> s.scrollId(scrollId));
        }
    }

    private void hitsToResults(SearchOptions options, SecureSearchContext context, AtomicLong total,
                               List<Hit<Document>> hits, List<SearchResult> results) {
        int returned = 0;
        int considered = 0;
        long effectiveLimit = calculateEffectiveLimit(options);
        long start = System.currentTimeMillis();
        for (Hit<Document> hit : hits) {
            if (options.getLimit() != SearchResults.UNLIMITED && results.size() >= effectiveLimit) {
                // Can short-circuit as we've produced enough results to satisfy the page of results that is being
                // requested
                break;
            }
            considered++;
            // NB - canViewDocument() includes fine-grained filtering of the Document reference provided by the Hit
            // instance in-place thus if this method returns true then that reference will then represent the filtered
            // document and can safely be wrapped in hitToResult()
            if (context.canViewDocument(hit.id(), String.valueOf(hit.version()), hit.source())) {
                results.add(hitToResult(options, hit));
                returned++;
            } else {
                // Have to adjust the total results that we are using to determine the value we return for maybeMore in
                // the metadata
                total.decrementAndGet();
            }
        }
        if (options.getSecurity().isEnabled()) {
            FmtLog.info(LOGGER,
                        "Filtered %,d of %,d results against users security attributes producing %,d visible documents in %,d milliseconds",
                        considered, hits.size(), returned, System.currentTimeMillis() - start);
        }
    }

    /**
     * Calculates the effective limit i.e. number of results we need to return in order to be able to return the page of
     * results the user has requested
     * <p>
     * This is the limit plus the offset minus 1, since we use a 1 based offset
     * </p>
     *
     * @param options Search options
     * @return Effective Limit
     */
    private static long calculateEffectiveLimit(SearchOptions options) {
        return options.getLimit() + options.getOffset() - 1;
    }

    private SearchResult hitToResult(SearchOptions options, Hit<Document> hit) {
        double score = hit.score() != null ? hit.score() : 0d;
        // NB - At the point where this is called we've already determined that the user can see this document and have
        // filtered out anything from the document they are not permitted to see
        Document highlighted = Highlighting.getHighlights(options.getHighlighting(), hit);
        return new SearchResult(hit.id(), score, hit.source(), highlighted);
    }

    private List<String> filterModeFields(TypeFilterMode typeFilterMode) {
        return switch (typeFilterMode) {
            case ENTITY -> List.of("types.keyword");
            case IDENTIFIER -> List.of("*.types.keyword");
            default -> List.of("types.keyword", "*.types.keyword");
        };
    }

    @Override
    public SearchResults searchByQuery(String query, SearchOptions options) {
        return searchCommon(query, options, QueryType.QUERY, buildQuerystringQuery(query, options));
    }

    @Override
    public SearchResults searchByTerms(String terms, SearchOptions options) {
        // For Term Queries we use a multi-match query since that allows us to search
        // for the terms across multiple
        // fields within the documents.

        //@formatter:off
        return searchCommon(terms, options, QueryType.TERM,
                            buildTermsQuery(terms, options));
        //@formatter:on
    }

    @Override
    public SearchResults searchByPhrase(String phrase, SearchOptions options) {
        // For Term Queries we use a multi-match query since that allows us to search
        // for the terms across multiple
        // fields within the documents. The only
        // difference from a term query is that we explicitly set the query type to
        // Phrase.
        //@formatter:off
        return searchCommon(phrase, options, QueryType.PHRASE,
                            buildPhraseQuery(phrase, options));
        //@formatter:on
    }


    /**
     * Builds a querystring query
     *
     * @param query   Querystring
     * @param options Search options
     * @return Query
     */
    protected Query buildQuerystringQuery(String query, SearchOptions options) {
        return Query.of(q -> q.queryString(s -> s.query(query).fields(options.getFieldOptions().getFields())));
    }

    /**
     * Builds a terms query
     *
     * @param terms   Terms
     * @param options Search options
     * @return Query
     */
    protected Query buildTermsQuery(String terms, SearchOptions options) {
        return Query.of(q -> q.multiMatch(mm -> mm.fields(options.getFieldOptions().getFields()).query(terms)));
    }

    /**
     * Builds a phrase query
     *
     * @param phrase  Phrase
     * @param options Search options
     * @return Query
     */
    protected Query buildPhraseQuery(String phrase, SearchOptions options) {
        return Query.of(q -> q.multiMatch(
                mm -> mm.fields(options.getFieldOptions().getFields()).type(TextQueryType.Phrase).query(phrase)));
    }

    @Override
    public SearchResults searchByWildcard(String prefix, SearchOptions options) {
        // While Wildcard searches are supported by ElasticSearch they must be scoped to
        // a specific field, since for all
        // the other query types we are searching across all fields we choose not to
        // support wildcard queries currently
        throw new SearchException("Wildcard queries are not currently supported");
    }

    @Override
    public SearchResults typeahead(String phrase, List<String> fields, SearchOptions options) {
        if (CollectionUtils.isEmpty(fields)) {
            // Use our set of default fields to search which is just the Primary Name for
            // the entity, our test datasets
            // seem to use two different case variants of this so default to both for
            // portability
            fields = List.of("*PrimaryName", "*primaryName");
        }
        //@formatter:off
        List<String> finalFields = fields;
        return searchCommon(phrase, options, QueryType.PHRASE,
                            Query.of(q -> q.multiMatch(mm -> mm.fields(finalFields)
                                                             .type(TextQueryType.PhrasePrefix)
                                                             .query(phrase))));
        //@formatter:on
    }

    @Override
    public SearchResults getStates(String id, SearchOptions options) {
        // Build a filter query to retrieve only states which mention the doc's id,
        // match the ID as a whole because it
        // might be a URI which Elastic would tokenize and treat as a sequence of terms
        // and could cause us to match all
        // indexed states, not just those relevant to the entity with the given ID
        final Query mmatch = Query.of(q -> q.multiMatch(mm -> mm.fields("*.keyword").query(id)));
        final TermQuery tq = TermQuery.of(t -> t.field("isState").value(true));
        final Query query = Query.of(q -> q.bool(b -> b.must(mmatch).filter(f -> f.term(tq))));

        return searchCommon(id, options, QueryType.QUERY, query);
    }

    @Override
    public FacetResults facets(String query, QueryType type, String facet, SearchOptions options) {
        final String[] path = facet.split("/");
        long start = System.currentTimeMillis();
        LOGGER.info("Starting facet {} computation for {} query '{}'", facet, type, query);

        if (options.getOffset() != SearchResults.FIRST_OFFSET) {
            throw new SearchException(
                    "Facets are calculated via randomised sampling of documents so only offset=1 is permitted");
        }

        // Create the query
        // In order to avoid needing to get all the possible results to calculate the facets we will instead get a
        // random sample of results and calculate the facets from that
        // Our random sample is seeded based on the internal ES _id field for documents and the sample size, i.e. limit,
        // specified in the search options.  This ensures that calculated facets are reproducible as long as the index
        // is stable.
        Query baseQuery = switch (type) {
            case QUERY -> buildQuerystringQuery(query, options);
            case PHRASE -> buildPhraseQuery(query, options);
            case TERM -> buildTermsQuery(query, options);
            default -> throw new SearchException("Wildcard queries are not currently supported");
        };
        Query facetsQuery = Query.of(q -> q.functionScore(f -> f.query(baseQuery)
                                                                .functions(s -> s.randomScore(r -> r.field("_id")
                                                                                                    .seed(Long.toString(
                                                                                                            options.getLimit()))))));

        // Given the results of the underlying query count the occurrences of different values for the requested facet
        SearchResults results = searchCommon(query, options, type, facetsQuery);
        final Map<String, AtomicLong> counts = new HashMap<>();
        boolean unexpectedClassErrorAlreadyLogged = false;
        for (SearchResult result : results.getResults()) {
            Object val = result.getDocument().getProperty(path);
            if (val == null) continue;
            if (val instanceof List l) {
                l.forEach(v -> counts.computeIfAbsent(v.toString(), s -> new AtomicLong(0)).incrementAndGet());
            } else if (val instanceof String v) {
                counts.computeIfAbsent(v, s -> new AtomicLong(0)).incrementAndGet();
            } else {
                if (!unexpectedClassErrorAlreadyLogged) {
                    // Log only once per result set
                    // This error occurs when the provided facet identifies a field that is not a string/list
                    LOGGER.info("Unexpected type for facet {}: {}", facet, val.getClass().getCanonicalName());
                    unexpectedClassErrorAlreadyLogged = true;
                }
            }
        }

        // Convert the counts into facet results
        // We sort them by decreasing frequency then alphabetical order
        final List<FacetResult> facetResults = new ArrayList<>(counts.size());
        double sum = counts.values().stream().map(AtomicLong::longValue).reduce(0L, Long::sum);
        counts.forEach((l, c) -> facetResults.add(new FacetResult(l, c.longValue(), c.doubleValue() / sum)));
        Collections.sort(facetResults);
        FmtLog.info(LOGGER,
                    "Completed facet %s computation for %s query '%s' in %,d milliseconds producing %,d unique facet values with %,.0f total occurrences",
                    facet, type, query, System.currentTimeMillis() - start, facetResults.size(), sum);
        return new FacetResults(query, type, results.getResults().size(), facetResults);
    }

    @Override
    public String toString() {
        return String.format("%s:%d/%s", this.elasticHost, this.elasticPort, this.indices);
    }

    @Override
    public boolean putSynonyms(String[] synonyms, boolean delete) {
        try {
            if (delete) {
                // make sure the Elastic index for handling synonyms is deleted / recreate it
                client.indices().delete(DeleteIndexRequest.of(r -> r.index(synonymsIndex).ignoreUnavailable(true)));
            }

            // build a single document containing the synonyms
            final Map<String, List<String>> fields = new HashedMap<>();

            List<String> values = new ArrayList<>(10);
            int fieldCount = 0;
            int linesFound = 0;

            // put up to ten lines as values of a field
            for (String s : synonyms) {
                if (StringUtils.isBlank(s)) {
                    continue;
                }
                // needs a new field?
                if (values.size() == 10) {
                    fields.put("f_" + fieldCount, values);
                    values = new ArrayList<>(10);
                    fieldCount++;
                }
                values.add(s.trim());
                linesFound++;
            }

            // flush the rest
            if (!values.isEmpty()) {
                fields.put("f_" + fieldCount, values);
                fieldCount++;
            }

            LOGGER.info("Indexing {} synonym mappings into {} fields", linesFound, fieldCount);

            // index the documents in Elastic
            client.index(IndexRequest.of(r -> (r.document(fields).index(synonymsIndex).refresh(Refresh.True))));

            // reload the analyzers for the main index
            ReloadSearchAnalyzersResponse reloadResponse = client.indices()
                                                                 .reloadSearchAnalyzers(ReloadSearchAnalyzersRequest.of(
                                                                         r -> r.index(indices)
                                                                               .ignoreUnavailable(true)));

            List<ReloadDetails> reloadDetails = reloadResponse.reloadDetails();
            if (!reloadDetails.isEmpty()) {
                ReloadDetails details = reloadDetails.getFirst();
                String indexName = details.index();
                List<String> analyzers = details.reloadedAnalyzers();
                LOGGER.info("Reloaded {} analyzer(s) for index '{}'", analyzers.size(), indexName);
            }

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getSynonyms() {
        // get all the documents from the index
        // assuming there are only a handful of documents
        List<String> list = new ArrayList<>();
        try {
            int synonymsLoaded = 0;

            SearchResponse<ObjectNode> response = client.search(s -> s.index(synonymsIndex), ObjectNode.class);

            List<Hit<ObjectNode>> hits = response.hits().hits();
            for (Hit<ObjectNode> hit : hits) {
                // get the data from the source field
                Iterator<Entry<String, JsonNode>> fieldsIter = hit.source().fields();
                while (fieldsIter.hasNext()) {
                    Entry<String, JsonNode> node = fieldsIter.next();
                    if (node.getValue().isArray()) {
                        for (JsonNode jsonNode : node.getValue()) {
                            list.add(jsonNode.asText());
                            synonymsLoaded++;
                        }
                    } else {
                        list.add(node.getValue().asText());
                        synonymsLoaded++;
                    }
                }
            }

            LOGGER.info("{} synonyms loaded from index {}", synonymsLoaded, synonymsIndex);

        } catch (Exception e) {
            LOGGER.error("Exception caught when loading the synonyms from {}", synonymsIndex, e);
        }

        return list.toArray(new String[0]);
    }

    /**
     * An ElasticSearch client builder, suitable for extending.
     *
     * @param <C> the client subclass being built.
     * @param <B> the corresponding builder subclass.
     */
    public abstract static class ElasticSearchClientBuilder<C extends ElasticSearchClient, B extends ElasticSearchClientBuilder<C, B>>
            extends AbstractElasticClientBuilder<C, B> {
        /**
         * The search indices to be used.
         */
        protected List<String> indices;
        /**
         * The redacted documents cache to be used
         */
        protected RedactedDocumentsCache redactedDocumentsCache;

        /**
         * @param index configures on index on the builder, used to construct the client.
         * @return the builder for chaining.
         * @see #indices(String...)
         */
        public B index(String index) {
            return indices(index);
        }

        /**
         * @param indices configures one or more indices on the builder, used to construct the client.
         * @return the builder for chaining.
         * @see #indices(List)
         */
        public B indices(String... indices) {
            return indices(asList(indices));
        }

        /**
         * @param indices configures one or more indices on the builder, used to construct the client.
         * @return the builder for chaining.
         */
        public B indices(List<String> indices) {
            this.indices = indices;
            return self();
        }

        /**
         * Configures the default redacted documents
         *
         * @param maxUsers            Maximum users to cache redacted document results for
         * @param maxDocumentsPerUser Maximum document visibility results to cache per user
         * @param expiresAfter        Duration after which cache entries for users and documents expire
         * @return the builder for chaining.
         */
        public B redactedDocumentsCache(int maxUsers, int maxDocumentsPerUser, Duration expiresAfter) {
            return redactedDocumentsCache(
                    new CaffeineRedactedDocumentsCache(maxUsers, maxDocumentsPerUser, expiresAfter));
        }

        /**
         * Configures a redacted documents cache
         *
         * @param cache Redacted documents cache
         * @return the builder for chaining.
         */
        public B redactedDocumentsCache(RedactedDocumentsCache cache) {
            this.redactedDocumentsCache = cache;
            return self();
        }

        @Override
        protected abstract B self();

        /**
         * Builds the instance.
         *
         * @return the elastic search client subclass instance.
         */
        @Override
        public abstract C build();

        /**
         * Returns useful state information about the builder.
         *
         * @return the builder state.
         */
        @Override
        public String toString() {
            return "ElasticSearchClient.ElasticSearchClientBuilder(super=" + super.toString() + ", indices=" + this.indices + ", redactedDocumentsCache=" + this.redactedDocumentsCache + ")";
        }
    }

    private static final class ElasticSearchClientBuilderImpl
            extends ElasticSearchClientBuilder<ElasticSearchClient, ElasticSearchClientBuilderImpl> {
        private ElasticSearchClientBuilderImpl() {
        }

        @Override
        protected ElasticSearchClientBuilderImpl self() {
            return this;
        }

        @Override
        public ElasticSearchClient build() {
            return new ElasticSearchClient(elasticHost, elasticPort, indices, username, password, elasticTlsCaCert,
                                           makeOpenSearchCompatible, redactedDocumentsCache);
        }
    }
}
