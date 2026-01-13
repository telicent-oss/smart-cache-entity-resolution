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
package io.telicent.smart.cache.entity.resolver.elastic;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.telicent.smart.cache.canonical.configuration.*;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.entity.resolver.elastic.index.CachedIndexMapper;
import io.telicent.smart.cache.entity.resolver.elastic.index.IndexMapper;
import io.telicent.smart.cache.entity.resolver.elastic.similarity.CanonicalTypeConfigurationValidator;
import io.telicent.smart.cache.entity.resolver.elastic.similarity.QueryGeneratorResolver;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.SearchException;
import io.telicent.smart.cache.search.elastic.AbstractClientAdaptor;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.SearchResults;
import io.telicent.smart.cache.search.options.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * An entity resolver backed by ElasticSearch
 */
public class ElasticSearchEntityResolver extends AbstractClientAdaptor implements EntityResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchEntityResolver.class);

    private final String similarityIndex;

    /**
     * Make it configurable later on
     **/
    public static final String TEMP_INDEXING_SIMILARITY_FIELD = "tmp_similarity_input";

    /**
    * Default name for the similarity index
     **/
    public static final String DEFAULT_NAME_SIMILARITY_INDEX = "canonical";

    private static final int DELETE_ALL_COUNT = 2;
    private static int deleteCount;

    /**
     * Builds a new ElasticSearch client.
     *
     * @return the newly built ElasticSearch client.
     */
    public static ElasticEntityResolverBuilder<?, ?> builder() {
        return new ElasticEntityResolverBuilderImpl();
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost     ElasticSearch host
     * @param elasticPort     ElasticSearch port
     * @param similarityIndex Index for similarity
     */
    ElasticSearchEntityResolver(String elasticHost, int elasticPort, String similarityIndex) {
        this(elasticHost, elasticPort, similarityIndex, null, null, null, false);
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param similarityIndex          Index for similarity
     * @param user                     ElasticSearch user
     * @param password                 ElasticSearch password
     * @param makeOpenSearchCompatible Whether to make the client "compatible", in so far as is possible, with
     *                                 OpenSearch servers
     */
    protected ElasticSearchEntityResolver(String elasticHost, int elasticPort, String similarityIndex, String user,
                                          String password, boolean makeOpenSearchCompatible) {
        super(elasticHost, elasticPort, user, password, null, makeOpenSearchCompatible);

        this.similarityIndex = similarityIndex;
    }

    /**
     * Creates a new ElasticSearch Search client
     *
     * @param elasticHost              ElasticSearch host
     * @param elasticPort              ElasticSearch port
     * @param similarityIndex          Index for similarity
     * @param user                     ElasticSearch user
     * @param password                 ElasticSearch password
     * @param elasticTlsCaCert         the TLS/SSL trust base-64 encoded certificate of the Elasticsearch service for
     *                                 secure communication, which may be null if security features are disabled
     *                                 (i.e. plain text HTTP only).
     * @param makeOpenSearchCompatible Whether to make the client "compatible", in so far as is possible, with
     *                                 OpenSearch servers
     */
    protected ElasticSearchEntityResolver(String elasticHost, int elasticPort, String similarityIndex, String user,
                                          String password, String elasticTlsCaCert, boolean makeOpenSearchCompatible) {
        super(elasticHost, elasticPort, user, password, elasticTlsCaCert, makeOpenSearchCompatible);

        this.similarityIndex = similarityIndex;
    }

    @Override
    public String toString() {
        return String.format("%s:%d/%s", this.elasticHost, this.elasticPort, this.similarityIndex);
    }

    private SimilarityResult findSimilar(final Document doc, int maxResults, final float minScore,
                                         final boolean withinInput, final SecurityOptions securityOptions,
                                         CanonicalTypeConfiguration overrideConfiguration) {

        // the input document will have been indexed at this stage
        final String id = (String) doc.getProperty("id");
        final String originalId = (String) doc.getProperty("originalId");
        final SimilarityResult sr = new SimilarityResult();
        sr.setIDSourceEntity(originalId);

        final Query query = QueryGeneratorResolver.generateQuery(doc, overrideConfiguration);
        if (query == null) {
            throw new SearchException("Could not generate a query for doc " + doc);
        }

        final SearchRequest.Builder builder = new SearchRequest.Builder();
        String indexToUse = getIndexToUse(doc, overrideConfiguration);
        builder.index(indexToUse).query(query);

        // run the query
        LOGGER.info("Starting search for documents similar to {} in index {}", originalId, indexToUse);

        long start = System.currentTimeMillis();
        SearchResponse<Document> response;
        try {
            response = this.getClient().search(builder.build(), Document.class);
            LOGGER.info("Retrieved {} initial results in {} milliseconds", response.hits().hits().size(),
                        System.currentTimeMillis() - start);
        } catch (ElasticsearchException e) {
            LOGGER.error("ElasticsearchException caught when querying Elastic {}", query, e);
            throw AbstractClientAdaptor.fromElasticException(e, "Similarity search for doc " + doc);
        } catch (Exception e) {
            LOGGER.error("Exception caught when querying Elastic {}", query, e);
            throw new SearchException(e);
        }

        // return the top docs
        final List<Hit<Document>> hits = response.hits().hits();

        final List<io.telicent.smart.cache.search.model.Hit> similarHits = new ArrayList<>();

        double topScore = -1;
        boolean firstLoop = true;
        for(Hit<Document> hit : hits) {
            if (maxResults <= similarHits.size()) {
                break;
            }

            String hitId = hit.id();
            Double score = hit.score();
            Document source = hit.source();
            if (null == score || null == source) {
                LOGGER.error("Ignoring search hit given missing details: {}", hitId);
                continue;
            }

            final boolean fromTempSet = source.getProperties().containsKey(TEMP_INDEXING_SIMILARITY_FIELD);

            // source doc will come first
            if (firstLoop) {
                topScore = score;
                firstLoop = false;
                if (!hitId.equals(id)) {
                    LOGGER.info("Top score not self? {} {}", hitId, score);
                }
            }
            // do not include self in the results
            if (hitId.equals(id)) {
                continue;
            }
            // also remove matches if within input and not needed
            else if (!withinInput && fromTempSet) {
                continue;
            } else {
                // normalise the score
                double normalisedScore = score / topScore;
                if (normalisedScore < minScore) {
                    break;
                }
                similarHits.add(new io.telicent.smart.cache.search.model.Hit(hitId, normalisedScore, hit.source()));
            }
        }

        sr.setHits(similarHits.toArray(new io.telicent.smart.cache.search.model.Hit[0]));

        return sr;
    }

    /**
     * Find documents similar to the input one. (Overloading method for easier reading tests et al.)
     *
     * @param doc        input document
     * @param maxResults max number of results per input
     * @param minScore   minimum score for a match
     * @return SimilarityResult
     **/
    public SimilarityResult findSimilar(final Document doc, int maxResults, final float minScore) {
        return findSimilar(doc, maxResults, minScore, null, null);
    }

    @Override
    public SimilarityResult findSimilar(final Document doc, int maxResults, final float minScore,
                                        final SecurityOptions securityOptions, String overrides) {
        // parse and validate override mapping, if provided
        CanonicalTypeConfiguration overrideConfiguration = loadAndValidateConfigurationOverride(overrides, doc);
        // index the documents for which we need similarity
        String batchID = indexDocumentsTemporarilyIntoSimilarityIndex(Collections.singletonList(doc), overrideConfiguration);

        // do the similarity magic e.g. by querying the search
        // back-end
        final SimilarityResult res = findSimilar(doc, maxResults, minScore, false, securityOptions, overrideConfiguration);

        // delete temporary docs
        DeleteByQueryRequest request = generateDeleteRequest(getIndexToUse(doc, overrideConfiguration), batchID);
        try {
            this.getClient().deleteByQuery(request);
        } catch (Exception e) {
            LOGGER.error("Exception while deleting batch", e);
            // no need to propagate - it is not that crucial, but we should clean up after.
            flagFutureDeleteForCleanUp();
        }

        return res;
    }

    /**
     * Find documents similar to the ones passed as input (Overloading method for easier reading tests et al.)
     *
     * @param docs        input documents
     * @param maxResults  max number of results per input
     * @param minScore    minimum score for a match
     * @param withinInput whether to return similarities between input documents
     * @return SimilarityResult
     **/
    public SimilarityResults findSimilar(final List<Document> docs, int maxResults, final float minScore,
                                         final boolean withinInput) {
        return findSimilar(docs, maxResults, minScore, withinInput, null, null);
    }

    @Override
    public SimilarityResults findSimilar(final List<Document> docs, int maxResults, final float minScore,
                                         final boolean withinInput, final SecurityOptions securityOptions,
                                         String overrides) {

        // parse and validate override mapping, if provided
        CanonicalTypeConfiguration overrideConfiguration = loadAndValidateConfigurationOverride(overrides, docs.getFirst());

        // index the documents for which we need similarity
        String batchID = indexDocumentsTemporarilyIntoSimilarityIndex(docs, overrideConfiguration);

        final List<SimilarityResult> results = new ArrayList<>();


        // do the similarity magic e.g. by querying the search
        // back-end
        for (Document d : docs) {
            final SimilarityResult res = findSimilar(d, maxResults, minScore, withinInput, securityOptions, overrideConfiguration);
            results.add(res);
        }

        // delete temporary docs
        DeleteByQueryRequest request = generateDeleteRequest(getIndexToUse(docs.getFirst(), overrideConfiguration), batchID);

        try {
            this.getClient().deleteByQuery(request);
        } catch (Exception e) {
            LOGGER.error("Exception while deleting batch", e);
            // no need to propagate - it is not that crucial, but we should clean up after.
            flagFutureDeleteForCleanUp();
        }

        return new SimilarityResults(results);
    }

    @Override
    public void addConfig(String type, String entry, String id) {
        if (FullModel.TYPE.equalsIgnoreCase(type)) {
            IndexMapper.addIndexFullModelEntry(getClient(), id, entry);
        } else {
            CachedIndexMapper.addIndexEntry(getClient(), type, id, entry);
        }
    }

    @Override
    public void updateConfig(String type, String entry, String id) {
        if (FullModel.TYPE.equalsIgnoreCase(type)) {
            IndexMapper.updateIndexFullModelEntry(getClient(), id, entry);
        } else {
            CachedIndexMapper.updateIndexEntry(getClient(), type, id, entry);
        }
    }

    @Override
    public void deleteConfig(String type, String id) {
        if (FullModel.TYPE.equalsIgnoreCase(type)) {
            IndexMapper.deleteIndexFullModelEntry(getClient(), id);
        } else {
            CachedIndexMapper.deleteIndexEntry(getClient(), type, id);
        }
    }

    @Override
    public String readConfig(String type, String id) {
        if (FullModel.TYPE.equalsIgnoreCase(type)) {
            return IndexMapper.getFullModelIndexEntry(getClient(), id);
        } else {
            return CachedIndexMapper.getIndexEntry(getClient(), type, id);
        }
    }

    @Override
    public String readAllConfig(String type) {
        if (FullModel.TYPE.equalsIgnoreCase(type)) {
            return IndexMapper.getAllIndexFullModelEntriesAsString(getClient());
        } else {
            return CachedIndexMapper.getAllIndexEntriesAsString(getClient(), type);
        }
    }

    @Override
    public String validateConfig(String type, String id, String index) {
        return IndexMapper.validateIndexEntry(getClient(), type, id, index);
    }

    private String indexDocumentsTemporarilyIntoSimilarityIndex(final List<Document> docs, CanonicalTypeConfiguration override) {

        // batch index - with unique ID
        final String batchID = UUID.randomUUID().toString();

        final BulkRequest.Builder br = new BulkRequest.Builder();

        String index = getIndexToUse(docs.getFirst(), override);

        for (Document doc : docs) {
            processDocumentID(doc);
            // check that the document has an id field - which is a convention
            String id = (String) doc.getProperty("id");
            Document copy = Document.copy(doc);
            // add a distinguishing feature
            copy.setProperty(TEMP_INDEXING_SIMILARITY_FIELD, batchID);

            br.operations(op -> op.index(idx -> idx.index(index).id(id).document(copy)));
        }

        br.refresh(Refresh.True);

        try {
            this.getClient().bulk(br.build());
        } catch (ElasticsearchException e) {
            LOGGER.error("ElasticsearchException found while indexing bulk", e);
            throw fromElasticException(e, "Similarity search temporarily indexing docs");
        } catch (Exception e) {
            LOGGER.error("Error found while indexing bulk ", e);
            throw new SearchException(e);
        }

        return batchID;
    }

    /**
     * To avoid accidental matches - generate truly unique ID store the original ID as "IDSourceEntity"
     *
     * @param document Given document for searching with
     */
    private void processDocumentID(Document document) {
        String uniqueId = UUID.randomUUID().toString();
        String stringId = (String) document.getProperty("id");
        if (StringUtils.isNotBlank(stringId)) {
            document.setProperty("originalId", stringId);
        } else {
            document.setProperty("originalId", uniqueId);
        }
        document.setProperty("id", uniqueId);
    }

    String getIndexToUse(Document doc, CanonicalTypeConfiguration override) {
        if (null != override && StringUtils.isNotBlank(override.index)) {
            return override.index;
        }
        return QueryGeneratorResolver.resolveIndex(doc, this.similarityIndex);
    }

    private CanonicalTypeConfiguration loadAndValidateConfigurationOverride(String configurationOverride, Document doc) {
        CanonicalTypeConfiguration overrideConfiguration = null;
        if (StringUtils.isNotBlank(configurationOverride)) {
            try {
                overrideConfiguration = CanonicalTypeConfiguration.loadFromString(configurationOverride);
                CanonicalTypeConfigurationValidator.validateConfig(
                        this.getClient(), getIndexToUse(doc, overrideConfiguration), overrideConfiguration);
            } catch (ValidationException e) {
                throw new SearchException("Invalid override configuration provided", e);
            }
        }
        return overrideConfiguration;
    }

    /**
     * Generates the delete request to remove the temporary document used in searching.
     * Every X requests we will carry out a "clean-up" and delete any temporary documents.
     * @param index the index to apply the request to
     * @param batchID the identifier batch
     * @return a Delete Request to remove the temporary document(s) from the index
     */
    private static DeleteByQueryRequest generateDeleteRequest(String index, String batchID) {
        DeleteByQueryRequest.Builder requestBuilder = new DeleteByQueryRequest.Builder().index(index)
                                                                                        .refresh(true);

        if (deleteCount >= DELETE_ALL_COUNT) {
            requestBuilder
                    .query(q -> q.exists(t -> t.field(TEMP_INDEXING_SIMILARITY_FIELD)));
            deleteCount = 0;
        } else {
            requestBuilder
                    .maxDocs(1L)
                    .query(q -> q.match(t -> t.field(TEMP_INDEXING_SIMILARITY_FIELD).query(batchID)));
            ++deleteCount;
        }
        return requestBuilder.build();
    }

    /**
     * Helper function to clear out any tmp entries from index on next Deletion.
     */
    public static void flagFutureDeleteForCleanUp() {
        deleteCount = DELETE_ALL_COUNT;
    }

    /**
     * Load a FullModel (model + relations + scores) from the config index.
     *
     * @param modelId id of the model to load
     * @return reconstructed FullModel
     */
    private FullModel loadFullModel(String modelId) {
        // 1) Load the Model
        Model model = CachedIndexMapper.getModelEntry(this.getClient(), modelId);
        if (model == null) {
            throw new SearchException("Unknown modelId: " + modelId);
        }

        FullModel fullModel = new FullModel();
        fullModel.modelId = model.modelId;
        fullModel.index = model.index;

        // 2) Load Relations
        for (String relationId : model.relations) {
            Object obj = CachedIndexMapper.getIndexTypEntryObject(
                    this.getClient(), Relation.TYPE, relationId);
            if (obj instanceof Relation relation) {
                fullModel.relations.add(relation);
            } else {
                throw new SearchException("Missing or invalid relation '" + relationId
                                                  + "' for model '" + modelId + "'");
            }
        }

        // 3) Load Scores (optional but strongly recommended)
        if (model.scores != null && !model.scores.isEmpty()) {
            Object obj = CachedIndexMapper.getIndexTypEntryObject(
                    this.getClient(), Scores.TYPE, model.scores);
            if (obj instanceof Scores scores) {
                fullModel.scores = scores;
            } else {
                throw new SearchException("Missing or invalid scores '" + model.scores
                                                  + "' for model '" + modelId + "'");
            }
        }

        return fullModel;
    }

    @Override
    public SimilarityResult findSimilarV2(Document doc,
                                          int maxResults,
                                          float minScore,
                                          SecurityOptions securityOptions,
                                          String modelId) {

        // Load the full model (index + relations + scores)
        FullModel fullModel = loadFullModel(modelId);

        // Sanity: the resolved index must match the model index
        CanonicalTypeConfiguration overrideConfiguration = null; // v2 uses model, not overrides
        String indexToUse = getIndexToUse(doc, overrideConfiguration);
        if (!fullModel.index.equals(indexToUse)) {
            throw new SearchException("Model '" + modelId + "' is for index '" + fullModel.index
                                              + "' but similarity search uses '" + indexToUse + "'");
        }

        // Index this doc temporarily, as v1 does
        String batchId = indexDocumentsTemporarilyIntoSimilarityIndex(
                Collections.singletonList(doc), overrideConfiguration);

        try {
            SimilarityResult result =
                    findSimilarV2Internal(doc, maxResults, minScore, false,
                                          securityOptions, overrideConfiguration, fullModel);

            // after successful scoring, delete the temporary docs
            DeleteByQueryRequest request = generateDeleteRequest(indexToUse, batchId);
            this.getClient().deleteByQuery(request);
            return result;
        } catch (ElasticsearchException e) {
            throw AbstractClientAdaptor.fromElasticException(e, "Similarity v2 search for doc " + doc);
        } catch (Exception e) {
            throw new SearchException("Error during similarity v2 search", e);
        }
    }

    private SimilarityResult findSimilarV2Internal(Document doc,
                                                   int maxResults,
                                                   float minScore,
                                                   boolean withinInput,
                                                   SecurityOptions securityOptions,
                                                   CanonicalTypeConfiguration overrideConfiguration,
                                                   FullModel fullModel) throws IOException {

        final String id = (String) doc.getProperty("id");
        final String originalId = (String) doc.getProperty("originalId");
        final SimilarityResult sr = new SimilarityResult();
        sr.setIDSourceEntity(originalId);

        final Query query = QueryGeneratorResolver.generateQuery(doc, overrideConfiguration);
        if (query == null) {
            throw new SearchException("Could not generate a query for doc " + doc);
        }

        final SearchRequest.Builder builder = new SearchRequest.Builder();
        String indexToUse = getIndexToUse(doc, overrideConfiguration);
        builder.index(indexToUse).query(query).size(maxResults * 5);

//        SearchOptions withHighlighting = SearchOptions.of(maxResults * 5,SearchResults.FIRST_OFFSET);
//                  ask for more, we’ll re-rank
//                new HighlightingOptions(true),
//                TypeFilterOptions.DISABLED,
//                SecurityOptions.DISABLED,
//                SortOptions.NONE,
//                FieldOptions.DEFAULT);

        LOGGER.info("Starting similarity v2 search for {} in index {}", originalId, indexToUse);

        long start = System.currentTimeMillis();
        SearchResponse<Document> response =
                this.getClient().search(builder.build(), Document.class);
        LOGGER.info("Retrieved {} initial results in {} ms",
                    response.hits().hits().size(),
                    System.currentTimeMillis() - start);

        final List<Hit<Document>> hits = response.hits().hits();
        if (hits.isEmpty()) {
            sr.setHits(new io.telicent.smart.cache.search.model.Hit[0]);
            return sr;
        }

        // 1) Collect matches (candidateId -> matched fields)
        Map<String, Hit<Document>> hitById = new HashMap<>();
        List<Map.Entry<String, List<String>>> matchesForModel = new ArrayList<>();

        for (Hit<Document> hit : hits) {
            String hitId = hit.id();
            Double esScore = hit.score();
            Document source = hit.source();

            if (esScore == null || source == null) {
                LOGGER.warn("Ignoring hit {} missing score or source", hitId);
                continue;
            }

            hitById.put(hitId, hit);

            // matchedQueries() returns the queryName we set in queries (the field name)
            List<String> matchedFields = hit.matchedQueries();
            matchesForModel.add(Map.entry(hitId, matchedFields));
        }

        // 2) Score each candidate using FullModel
        List<Map.Entry<String, Double>> scored =
                fullModel.calculateScores(matchesForModel);

        // 3) Build final hits ordered by model score
        List<io.telicent.smart.cache.search.model.Hit> similarHits = new ArrayList<>();

        for (Map.Entry<String, Double> entry : scored) {
            if (similarHits.size() >= maxResults) {
                break;
            }

            String hitId = entry.getKey();
            double modelScore = entry.getValue();

            if (modelScore < minScore) {
                continue;
            }

            Hit<Document> hit = hitById.get(hitId);
            if (hit == null) {
                continue;
            }

            Document source = hit.source();
            boolean fromTempSet =
                    source.getProperties().containsKey(TEMP_INDEXING_SIMILARITY_FIELD);

            // Exclude the source doc itself
            if (hitId.equals(id)) {
                continue;
            }
            // Exclude temporary docs if withinInput = false
            if (!withinInput && fromTempSet) {
                continue;
            }

            similarHits.add(
                    new io.telicent.smart.cache.search.model.Hit(
                            hitId, modelScore, source));
        }

        sr.setHits(similarHits.toArray(new io.telicent.smart.cache.search.model.Hit[0]));
        return sr;
    }


    @Override
    public SimilarityResults findSimilarV2(List<Document> docs,
                                           int maxResults,
                                           float minScore,
                                           boolean withinInput,
                                           SecurityOptions securityOptions,
                                           String modelId) {

        FullModel fullModel = loadFullModel(modelId);
        CanonicalTypeConfiguration overrideConfiguration = null;

        // Index the documents for similarity comparison
        String batchId = indexDocumentsTemporarilyIntoSimilarityIndex(docs, overrideConfiguration);
        String indexToUse = getIndexToUse(docs.getFirst(), overrideConfiguration);

        final List<SimilarityResult> results = new ArrayList<>();

        try {
            for (Document d : docs) {
                SimilarityResult res = findSimilarV2Internal(
                        d, maxResults, minScore, withinInput,
                        securityOptions, overrideConfiguration, fullModel);
                results.add(res);
            }
        } catch (Exception e) {
            throw new SearchException("Error during similarity v2 search", e);
        } finally {
            // Clean up temp docs, regardless of errors
            try {
                DeleteByQueryRequest request = generateDeleteRequest(indexToUse, batchId);
                this.getClient().deleteByQuery(request);
            } catch (Exception e) {
                LOGGER.error("Exception while deleting batch {}", batchId, e);
                flagFutureDeleteForCleanUp();
            }
        }

        return new SimilarityResults(results);
    }


    /**
     * An ElasticSearch entity resolver builder, suitable for extending.
     *
     * @param <C> the client subclass being built.
     * @param <B> the corresponding builder subclass.
     */
    public abstract static class ElasticEntityResolverBuilder<C extends ElasticSearchEntityResolver, B extends ElasticEntityResolverBuilder<C, B>>
            extends AbstractElasticClientBuilder<C, B> {
        /** The similarity index to be used. */
        protected String similarityIndex;

        /**
         * @param similarityIndex configures the similarity index on the builder, used to construct the client.
         * @return the builder for chaining.
         */
        public B similarityIndex(String similarityIndex) {
            this.similarityIndex = similarityIndex;
            return self();
        }

        protected abstract B self();

        /**
         * Builds the instance.
         * @return the elastic search client subclass instance.
         */
        @Override
        public abstract C build();

        /**
         * Returns useful state information about the builder.
         * @return the builder state.
         */
        @Override
        public String toString() {
            return "ElasticSearchEntityResolver.ElasticEntityResolverBuilder(super=" + super.toString() + ", similarityIndex=" + this.similarityIndex + ")";
        }
    }

    private static final class ElasticEntityResolverBuilderImpl
            extends ElasticEntityResolverBuilder<ElasticSearchEntityResolver, ElasticEntityResolverBuilderImpl> {
        private ElasticEntityResolverBuilderImpl() {
        }

        @Override
        protected ElasticEntityResolverBuilderImpl self() {
            return this;
        }

        @Override
        public ElasticSearchEntityResolver build() {
            return new ElasticSearchEntityResolver(elasticHost, elasticPort, similarityIndex, username, password, makeOpenSearchCompatible);
        }
    }
}
