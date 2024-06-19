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

import io.telicent.smart.cache.canonical.exception.IndexException;
import io.telicent.smart.cache.canonical.exception.ValidationException;
import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.server.jaxrs.model.Problem;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Response;

abstract class AbstractConfigurationResource extends AbstractEntityResolutionResource {

    /**
     * Gets a JSON representation of all instances of given config, empty if missing
     *
     * @param type           the type of config (i.e.  Resolver, Scorer, Model)
     * @param servletContext container sharing data across app
     * @return Response
     */
    protected Response getAllForType(String type, ServletContext servletContext) {
        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }
        String results = client.readAllConfig(type);
        return Response.ok().entity(results).build();
    }

    /**
     * Gets a JSON representation of given config, empty if missing
     *
     * @param id             the name of the config
     * @param type           the type of config (i.e.  Resolver, Scorer, Model)
     * @param servletContext container sharing data across app
     * @return Response
     */
    protected Response getById(String id, String type, ServletContext servletContext) {
        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }
        try {
            String result = client.readConfig(type, id);
            return Response.ok().entity(result).build();
        } catch (IndexException | ValidationException e) {
            return serverError(e);
        }
    }

    /**
     * Deletes the given config
     *
     * @param id             the name of the config
     * @param type           the type of config (i.e.  Resolver, Scorer, Model)
     * @param servletContext container sharing data across app
     * @return Response
     */
    public Response deleteById(String id, String type, ServletContext servletContext) {
        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }
        try {
            client.deleteConfig(type, id);
            return Response.ok().entity(String.format("Deleted %s", id)).build();
        } catch (ValidationException | IndexException e) {
            return serverError(e);
        }
    }

    /**
     * Adds a new instance of the given config, overriding any existing for the ID.
     *
     * @param id             the name of the config
     * @param type           the type of config (i.e.  Resolver, Scorer, Model)
     * @param entry          the string (JSON) representation of the configuration
     * @param servletContext container sharing data across app
     * @return Response
     */
    public Response createById(String id, String type, String entry, ServletContext servletContext) {
        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }
        try {
            client.addConfig(type, entry, id);
            return Response.ok().entity(String.format("Created %s", id)).build();
        } catch (ValidationException | IndexException e) {
            return serverError(e);
        }
    }

    /**
     * Updates an existing instance of the given config, overriding any existing for the ID.
     *
     * @param id             the name of the config
     * @param type           the type of config (i.e.  Resolver, Scorer, Model)
     * @param entry          the string (JSON) representation of the configuration
     * @param servletContext container sharing data across app
     * @return Response Note: at present this is the same as adding new
     */
    public Response updateById(String id, String type, String entry, ServletContext servletContext) {
        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }
        try {
            client.updateConfig(type, entry, id);
            return Response.ok().entity(String.format("Updated %s", id)).build();
        } catch (ValidationException | IndexException e) {
            return serverError(e);
        }
    }

    /**
     * @param id             the name of the config
     * @param type           the type of config (i.e.  Resolver, Scorer, Model)
     * @param index          the index to validate against
     * @param servletContext container sharing data across app
     * @return a String representation of the validation results
     */
    public Response validateById(String id, String type, String index, ServletContext servletContext) {
        final EntityResolver client = getEntityResolver(servletContext);
        if (client == null) {
            return serviceUnavailable();
        }
        String result = client.validateConfig(type, id, index);
        return Response.ok().entity(result).build();
    }

    /**
     * Sends an HTTP 500 response,
     *
     * @param e an Exception that has been thrown and caught
     * @return HTTP 500 Response
     */
    private Response serverError(Exception e) {
        return new Problem("InternalError",
                           null,
                           Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                           e.getMessage(),
                           null).toResponse();
    }
}
