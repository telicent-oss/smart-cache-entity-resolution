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
package io.telicent.smart.cache.entity.resolver.server;

import com.fasterxml.jackson.core.type.TypeReference;
import io.telicent.smart.cache.canonical.utility.Mapper;
import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResult;
import io.telicent.smart.cache.entity.resolver.model.SimilarityResults;
import io.telicent.smart.cache.search.model.Document;
import io.telicent.smart.cache.search.options.SecurityOptions;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Endpoint to return entities found to be similar to those passed in the input
 */
@Path("similarity")
public class SimilarityResource extends AbstractEntityResolutionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimilarityResource.class);
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE = new TypeReference<>() {
    };

    /**
     * Returns the most similar entities to the ones passed as input in the JSON file. The file is in ndjson format with
     * one line per entity, the content of the JSON is similar to what is returned by the search endpoint and mimics the
     * structure of the documents in the search backend.
     *
     * @param uploadedInputStream file containing a JSON representation of one of more entities
     * @param servletContext      servlet context
     * @param securityContext     security context
     * @param maxResults          max number of hits per entity (default 1)
     * @param minScore            minimal score that hits must have in order to be returned (default 0)
     * @param withinInput         look for similarities within the input entities (default false)
     * @param overrides           mapping overrides for query (default empty)
     * @return response containing the list of similarities
     **/
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSimilar(@FormDataParam("file") InputStream uploadedInputStream,
                               @QueryParam("maxResults") @Min(1) @DefaultValue("1") final Integer maxResults,
                               @QueryParam("minScore") @Min(0) @DefaultValue("0") final Float minScore,
                               @QueryParam("withinInput") @DefaultValue("false") final Boolean withinInput,
                               @FormDataParam("overrides") @DefaultValue("") final String overrides,
                               @Context ServletContext servletContext, @Context SecurityContext securityContext) throws
            NotFoundException {

        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }

        // TODO As and when we decide to enforce security in this API replace this with appropriate configuration
        SecurityOptions securityOptions = SecurityOptions.DISABLED;

        // step 1 extract the entities from the JSON file
        // temporary
        final List<Document> docs = new ArrayList<>();

        try (InputStreamReader isr = new InputStreamReader(uploadedInputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            Iterator<String> iter = reader.lines().iterator();
            while (iter.hasNext()) {
                String inputLine = iter.next();
                Document d = generateDocumentFromString(inputLine);
                docs.add(d);
            }
        } catch (IOException e) {
            LOGGER.error("Exception while generating documents from input", e);
            return Response.serverError().build();
        }

        SimilarityResults result;

        // single doc
        if (docs.size() == 1) {
            // note that each source document is indexed anyway to normalise the results
            // based on the score
            // of the top result
            // (which should be the score of a doc against itself)

            final List<SimilarityResult> results = new ArrayList<>();

            final SimilarityResult res = client.findSimilar(docs.get(0), maxResults, minScore, securityOptions, overrides);
            results.add(res);

            result = new SimilarityResults(results);
        } else {
            result = client.findSimilar(docs, maxResults, minScore, withinInput, securityOptions, overrides);
        }

        // return the results
        return Response.ok().entity(result).build();
    }

    private Document generateDocumentFromString(String json) {
            Map<String, Object> map = Mapper.loadFromString(JSON_MAP_TYPE, json);
            return new Document(map);
    }
}
