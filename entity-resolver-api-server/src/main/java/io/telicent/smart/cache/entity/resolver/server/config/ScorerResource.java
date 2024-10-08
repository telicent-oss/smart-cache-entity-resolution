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
package io.telicent.smart.cache.entity.resolver.server.config;

import io.telicent.smart.cache.canonical.configuration.Scores;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * REST Endpoints for handling Scorer config
 */
@Path(Scores.TYPE)
public class ScorerResource extends AbstractConfigurationResource {

    /**
     * Get the configuration
     * @param id unique ID
     * @param servletContext container sharing data across app
     * @return A string response of the data
     */
    @GET
    @Path("/{score_id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getScoreByID(@PathParam("score_id") final String id,
                                 @Context final ServletContext servletContext) {
        return getById(id, Scores.TYPE, servletContext);
    }

    /**
     * Get all the stored configuration
     * @param servletContext container sharing data across app
     * @return A string response of the data
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllModels(@Context final ServletContext servletContext) {
        return getAllForType(Scores.TYPE, servletContext);
    }

    /**
     * Delete the given configuration.
     * @param id unique ID
     * @param servletContext container sharing data across app
     * @return a string message saying if config was deleted
     */
    @DELETE
    @Path("/{score_id}")
    public Response deleteScoreByID(@PathParam("score_id") @NotBlank final String id,
                                    @Context final ServletContext servletContext) {
        return deleteById(id, Scores.TYPE, servletContext);
    }

    /**
     * Create new instance of the config
     * @param id unique ID
     * @param entry the JSON representation of the config
     * @param servletContext container sharing data across app
     * @return a string message indicating success
     */
    @POST
    @Path("/{score_id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createScoreByID(@PathParam("score_id") @NotBlank final String id,
                                    @FormDataParam("entry") @DefaultValue("{}") final String entry,
                                    @Context final ServletContext servletContext) {
        return createById(id, Scores.TYPE, entry, servletContext);
    }

    /**
     * Update an existing instance of the config
     * @param id unique ID
     * @param entry the JSON representation of the config
     * @param servletContext container sharing data across app
     * @return a string message indicating success
     * Note: currently is the same as POST
     */
    @PUT
    @Path("/{score_id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateScoreById(@PathParam("score_id") @NotBlank final String id,
                                    @FormDataParam("entry") @DefaultValue("{}") final String entry,
                                    @Context final ServletContext servletContext) {
        return updateById(id, Scores.TYPE, entry, servletContext);
    }
}
