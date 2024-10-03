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

import io.telicent.smart.cache.configuration.Configurator;
import io.telicent.smart.cache.server.jaxrs.applications.AbstractApplication;
import io.telicent.smart.cache.server.jaxrs.resources.AbstractHealthResource;
import jakarta.ws.rs.ApplicationPath;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.util.Set;

import static io.telicent.smart.caches.configuration.auth.AuthConstants.AUTH_DISABLED;
import static io.telicent.smart.caches.configuration.auth.AuthConstants.ENV_JWKS_URL;

/**
 * Definition of our JAX-RS Search Application
 */
@ApplicationPath("/")
public class EntityResolutionApplication extends AbstractApplication {

    /**
     * Configuration variable for enabling additional configuration.
     *  Note: this is only in place for testing purposes
     */
    public static final String ADDITIONAL_CONFIG_ENABLED = "ADDITIONAL_CONFIG_ENABLED";
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = super.getClasses();
        // Form Multipart Support
        classes.add(MultiPartFeature.class);
        // Resources i.e. actual API paths
        classes.add(HealthResource.class);
        classes.add(SimilarityResource.class);
        // Disabling Additional configuration resources by default as functionality is not fully implemented as yet.
        if (additionalConfigurationEnabled()) {
            classes.add(ModelResource.class);
            classes.add(RelationsResource.class);
            classes.add(ScorerResource.class);
            classes.add(FullModelResource.class);
            classes.add(ValidationResource.class);
        }
        classes.add(CanonicalTypeConfigurationResource.class);
        return classes;
    }

    private boolean additionalConfigurationEnabled() {
        return Boolean.parseBoolean(Configurator.get(new String[]{ADDITIONAL_CONFIG_ENABLED}, "false"));
    }

    @Override
    protected boolean isAuthEnabled() {
        return !StringUtils.equalsIgnoreCase(Configurator.get(ENV_JWKS_URL), AUTH_DISABLED);
    }

    @Override
    protected Class<? extends AbstractHealthResource> getHealthResourceClass() {
        return HealthResource.class;
    }
}
