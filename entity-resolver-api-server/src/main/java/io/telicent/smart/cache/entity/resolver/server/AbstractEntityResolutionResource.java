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

import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.server.jaxrs.model.Problem;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Response;

/**
 * Abstract JAX-RS Resource for Search APIs
 */
public class AbstractEntityResolutionResource {

    /**
     * Sends an HTTP 503 response, can be used if the API is currently unable to service the request
     *
     * @return HTTP 503 Response
     */
    protected Response serviceUnavailable() {
        return new Problem("ServiceUnavailable",
                           null,
                           Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
                           "No search index configured to service search requests",
                           null).toResponse();
    }

    /**
     * Gets the entity resolver, assuming it has been appropriately configured at server startup
     *
     * @param servletContext Servlet Context
     * @return Entity Resolver, of {@code null} if no resolver configured
     */
    protected EntityResolver getEntityResolver(ServletContext servletContext) {
        return (EntityResolver) servletContext.getAttribute(EntityResolver.class.getCanonicalName());
    }
}
