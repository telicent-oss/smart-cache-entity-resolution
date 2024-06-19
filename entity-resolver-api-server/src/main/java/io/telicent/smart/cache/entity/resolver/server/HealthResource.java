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

import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import io.telicent.smart.cache.server.jaxrs.resources.AbstractHealthResource;
import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.telicent.servlet.auth.jwt.JwtServletConstants.ATTRIBUTE_JWT_VERIFIER;
import static io.telicent.smart.caches.configuration.auth.AuthConstants.ENV_JWKS_URL;
import static io.telicent.smart.caches.configuration.auth.AuthConstants.ENV_USER_ATTRIBUTES_URL;

/**
 * A resource that provides a {@code /healthz} endpoint, note the JAX-RS annotations are on the base class
 */
public class HealthResource extends AbstractHealthResource {
    @Override
    protected HealthStatus determineStatus(ServletContext context) {
        try {
            EntityResolver resolver = (EntityResolver) context.getAttribute(EntityResolver.class.getCanonicalName());
            if (resolver == null) {
                return new HealthStatus(false,
                                        List.of("No Entity Resolver configured for accessing the underlying search index"),
                                        reportConfiguration(context, null, false));
            }

            // Use the resolvers ready status to determine the servers health status
            Boolean clientHealthy = resolver.isReady();
            boolean healthy = clientHealthy != null ? clientHealthy : false;
            List<String> reasons =
                    healthy ? Collections.emptyList() : List.of("Entity Resolver reported it is not ready");
            return new HealthStatus(healthy,
                                    reasons, reportConfiguration(context, resolver, healthy));

        } catch (ClassCastException e) {
            return new HealthStatus(false, List.of("Not a valid Entity Resolver configured"),
                                    reportConfiguration(context, null, false));
        }
    }

    private Map<String, Object> reportConfiguration(ServletContext context,
                                                    EntityResolver resolver, boolean healthy) {
        // NB: Map.of() DOES NOT permit null values so any value that would be null is coerced to the empty string
        //     instead
        return Map.of("searchIndex", resolver != null ? resolver.toString() : "",
                      "entityResolver", resolver != null ? resolver.getClass().getCanonicalName() : "",
                      "authentication", configAsString(ENV_JWKS_URL),
                      "jwtVerifier", asString(context, ATTRIBUTE_JWT_VERIFIER),
                      "authorization", configAsString(ENV_USER_ATTRIBUTES_URL),
                      "attributesStore", asString(context, AttributesStore.class.getCanonicalName()),
                      "searchClientReady", healthy);
    }

    private String configAsString(String envVar) {
        return Configurator.get(new String[]{envVar}, "");
    }

    private String asString(ServletContext context, String attribute) {
        Object attrValue = context.getAttribute(attribute);
        if (attrValue == null) {
            return "";
        }
        return attrValue.getClass().getCanonicalName();
    }
}
