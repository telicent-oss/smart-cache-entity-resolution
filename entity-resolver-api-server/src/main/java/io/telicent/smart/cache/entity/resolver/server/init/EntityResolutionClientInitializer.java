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
package io.telicent.smart.cache.entity.resolver.server.init;

import io.telicent.smart.cache.entity.resolver.EntityResolver;
import io.telicent.smart.cache.entity.resolver.providers.EntityResolvers;
import io.telicent.smart.cache.search.SearchClient;
import io.telicent.smart.cache.server.jaxrs.init.ServerConfigInit;
import jakarta.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the entity resolution client and stashes it in the servlet context for later reuse
 */
public class EntityResolutionClientInitializer implements ServerConfigInit {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityResolutionClientInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Attempting to initialize underlying EntityResolver...");
        EntityResolver resolver = EntityResolvers.load();
        if (resolver == null) {
            LOGGER.error("Unable to initialise EntityResolver, insufficient configuration available in environment");
        } else {
            sce.getServletContext().setAttribute(EntityResolver.class.getCanonicalName(), resolver);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(SearchClient.class.getCanonicalName());
    }

    @Override
    public String getName() {
        return "Search Client";
    }

    @Override
    public int priority() {
        return 50;
    }
}
