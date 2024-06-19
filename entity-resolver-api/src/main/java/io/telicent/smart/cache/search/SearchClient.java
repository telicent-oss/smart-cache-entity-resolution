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
package io.telicent.smart.cache.search;

import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.model.FacetResults;
import io.telicent.smart.cache.search.model.QueryType;
import io.telicent.smart.cache.search.model.SearchResults;
import io.telicent.smart.cache.search.options.SearchOptions;
import io.telicent.smart.cache.search.options.SecurityOptions;

import java.util.List;

/**
 * Represents the ability to search a search index
 */
public interface SearchClient extends SearchBackend {

    /**
     * Indicates whether the search client supports queries of the given type
     *
     * @param type Query type
     * @return True if supported, false otherwise
     */
    boolean supports(QueryType type);

    /**
     * Indicates whether the search client supports making a query with the given search options applied
     * <p>
     * An implementation should only return {@code false} if the options include a feature that they cannot support e.g.
     * highlighting. If they merely can't guarantee support, e.g. applying a specific limit and offset combination, then
     * they should still return {@code true} since callers may still be able to apply that after the fact.
     * </p>
     *
     * @param options Search options
     * @return True if supported, false otherwise
     */
    boolean supports(SearchOptions options);

    /**
     * Gets a document by its ID
     *
     * @param id              Document ID
     * @param securityOptions Security options
     * @return Document, or {@code null} if a document with that ID does not exist
     * @throws SearchException Thrown if there is a problem retrieving the document
     */
    Document getDocument(String id, SecurityOptions securityOptions);

    /**
     * Performs a search using the underlying index's full querystring syntax
     *
     * @param query   Querystring
     * @param options Search Options
     * @return Search Results
     */
    SearchResults searchByQuery(String query, SearchOptions options);

    /**
     * Performs a search using the underlying index's term query
     *
     * @param terms   Term or terms to match
     * @param options Search Options
     * @return Search results
     */
    SearchResults searchByTerms(String terms, SearchOptions options);

    /**
     * Performs a search using the underlying index's phrase query
     *
     * @param phrase  Phrase
     * @param options Search Options
     * @return Search results
     */
    SearchResults searchByPhrase(String phrase, SearchOptions options);

    /**
     * Performs a search using the underlying index's wildcard query
     *
     * @param prefix  Prefix to use as part of the wildcard query
     * @param options Search Options
     * @return Search results
     */
    SearchResults searchByWildcard(String prefix, SearchOptions options);

    /**
     * Performs a search constrained to specific fields
     *
     * @param phrase  Phrase to provide typeahead completion for
     * @param fields  Fields to constrain the search to, these may be patterns (e.g. {@code *PrimaryName}), specific
     *                fields (e.g. {@code uri}), or this may be an empty list in which case the implementation chooses a
     *                suitable set of fields to constrain the search to.
     * @param options Search options
     * @return Search results
     */
    SearchResults typeahead(String phrase, List<String> fields, SearchOptions options);

    /**
     * Retrieves the states associated pointing to the element with id
     *
     * @param id      Document ID
     * @param options Security options
     * @return Search results
     */
    SearchResults getStates(String id, SearchOptions options);

    /**
     * Calculates the facet results for a given query
     *
     * @param query   Query
     * @param type    Query Type
     * @param facet   Facets
     * @param options Search options
     * @return Facet results
     */
    FacetResults facets(String query, QueryType type, String facet, SearchOptions options);

    /**
     * Pushes a list of synonyms for the search backend to handle
     *
     * @param lines  of text containing the synonym mappings
     * @param delete boolean indicating whether any existing data should be deleted prior to adding the lines
     * @return True if the operation was successful, false if not
     **/
    boolean putSynonyms(String[] lines, boolean delete);

    /**
     * Returns the list of synonyms currently stored
     *
     * @return String array with the current lists of synonyms
     **/
    String[] getSynonyms();

}
