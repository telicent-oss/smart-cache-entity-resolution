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

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for validating config
 */
@Path("/validate")
public class ValidationResource extends AbstractConfigurationResource {
    /**
     * Get the configuration
     * @param type type of configuration
     * @param id unique ID
     * @param index index to validate against
     * @param servletContext container sharing data across app
     * @return A string response of the data
     */
    @GET
    @Path("{entry_type}/{entry_id}/{index_id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validate(@PathParam("entry_type") final String type,
                                 @PathParam("entry_id") final String id,
                                 @PathParam("index_id") final String index,
                                 @Context final ServletContext servletContext) {
        return validateById(id, type, index, servletContext);
    }
}
